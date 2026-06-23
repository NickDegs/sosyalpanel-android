package com.nickdegs.sosyalpanel.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// SMS girişi (Twilio Verify → Supabase JWT). iOS AuthService.swift karşılığı.
// Token SharedPreferences("auth")'da saklanır; isLoggedIn StateFlow ile gözlenir.
object AuthService {
    private const val PREFS = "auth"
    private const val K_TOKEN = "token"
    private const val K_UID = "uid"
    private const val K_PHONE = "phone"

    @Volatile private var appContext: Context? = null

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    var token: String? = null; private set
    var userId: String? = null; private set
    var phone: String? = null; private set

    fun init(context: Context) {
        appContext = context.applicationContext
        val p = prefs()
        token = p.getString(K_TOKEN, null)
        userId = p.getString(K_UID, null)
        phone = p.getString(K_PHONE, null)
        _isLoggedIn.value = token != null
    }

    private fun prefs() = appContext!!.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    sealed class Result {
        object Ok : Result()
        object BadCode : Result()
        object BadNumber : Result()
        object Network : Result()
    }

    fun normalize(raw: String): String = raw.filter { it.isDigit() }

    /** OTP gönderir. */
    suspend fun sendCode(rawPhone: String): Result = withContext(Dispatchers.IO) {
        val phone = normalize(rawPhone)
        if (phone.length < 8) return@withContext Result.BadNumber
        val (code, _) = post("/api/auth/send", JSONObject().put("phone", phone))
        when {
            code == 400 -> Result.BadNumber
            code in 200..299 -> Result.Ok
            else -> Result.Network
        }
    }

    /** Kodu doğrular, başarıda token saklar. */
    suspend fun verify(rawPhone: String, otp: String): Result = withContext(Dispatchers.IO) {
        val phoneN = normalize(rawPhone)
        val (status, json) = post("/api/auth/verify", JSONObject().put("phone", phoneN).put("code", otp.trim()))
        when {
            status == 401 -> Result.BadCode
            status == 200 && json != null && json.has("token") && json.has("uid") -> {
                val tok = json.getString("token")
                val uid = json.getString("uid")
                val ph = json.optString("phone").ifBlank { phoneN }
                token = tok; userId = uid; phone = ph
                prefs().edit().putString(K_TOKEN, tok).putString(K_UID, uid).putString(K_PHONE, ph).apply()
                _isLoggedIn.value = true
                Result.Ok
            }
            else -> Result.Network
        }
    }

    fun signOut() {
        token = null; userId = null; phone = null
        prefs().edit().clear().apply()
        _isLoggedIn.value = false
    }

    private fun post(path: String, body: JSONObject): Pair<Int, JSONObject?> {
        val conn = URL("${Backend.AUTH_BASE}$path").openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15000; conn.readTimeout = 30000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            code to runCatching { JSONObject(text) }.getOrNull()
        } catch (_: Exception) {
            0 to null
        } finally {
            conn.disconnect()
        }
    }
}
