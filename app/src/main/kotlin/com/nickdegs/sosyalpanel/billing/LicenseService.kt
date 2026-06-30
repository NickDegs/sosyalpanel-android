package com.nickdegs.sosyalpanel.billing

import android.content.Context
import android.util.Base64
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.nickdegs.sosyalpanel.data.AuthService
import com.nickdegs.sosyalpanel.data.Backend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import kotlin.coroutines.resume

// Sunucu-otoriteli Pro doğrulama (korsan/crack/modlanmış APK önleme).
// Play Integrity token + satın alma + kullanıcı JWT → sunucu Apple/Google'dan doğrular,
// cihaz/app gerçek mi bakar, kullanıcıya bağlı kısa ömürlü entitlement döner.
// STRICT modda Pro yalnızca sunucu onayıyla açılır → internetsiz/crack Pro çalışmaz.
// Kademeli geçiş: sunucu /config strict=false iken yalnızca gözlemler (lokal davranış aynı).
object LicenseService {
    private const val PREFS = "license"
    private var appContext: Context? = null

    @Volatile var strict = false; private set
    @Volatile private var serverPro = false
    @Volatile private var comped = false           // manuel premium (comp) — satın alma gerekmez
    @Volatile private var expMillis = 0L
    @Volatile private var lastVerified = 0L
    private const val GRACE_MS = 3L * 86_400_000   // online doğrulama arası tolerans
    @Volatile var onChange: (() -> Unit)? = null   // Pro değişince UI'yi tazele

    fun init(ctx: Context) {
        appContext = ctx.applicationContext
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        expMillis = p.getLong("exp", 0); lastVerified = p.getLong("verified", 0)
        comped = p.getBoolean("comped", false) && expMillis > System.currentTimeMillis()
        serverPro = expMillis > System.currentTimeMillis()
    }

    // Satın alma olmadan Pro (comp/manuel premium) — giriş yapılmışsa.
    suspend fun refreshEntitlement() = withContext(Dispatchers.IO) {
        val token = AuthService.token ?: return@withContext
        runCatching {
            val c = URL("${Backend.LICENSE_BASE}/entitlement").openConnection() as HttpURLConnection
            c.connectTimeout = 15000; c.readTimeout = 15000
            c.setRequestProperty("Authorization", "Bearer $token")
            if (c.responseCode == 200) {
                val j = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
                val isComp = j.optBoolean("comp", false)
                comped = isComp
                val p = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                if (isComp) {
                    serverPro = true
                    expMillis = j.optLong("exp", 0) * 1000L
                    lastVerified = System.currentTimeMillis()
                    p?.edit()?.putBoolean("comped", true)?.putLong("exp", expMillis)
                        ?.putLong("verified", lastVerified)?.apply()
                } else {
                    p?.edit()?.putBoolean("comped", false)?.apply()
                }
            }
            c.disconnect()
        }
        onChange?.invoke()
        Unit
    }

    suspend fun loadConfig() = withContext(Dispatchers.IO) {
        runCatching {
            val c = URL("${Backend.LICENSE_BASE}/config").openConnection() as HttpURLConnection
            c.connectTimeout = 12000; c.readTimeout = 12000
            if (c.responseCode == 200) {
                val j = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
                strict = j.optBoolean("strict", false)
            }
            c.disconnect()
        }
        Unit
    }

    // Giriş ZORUNLU değil: token varsa uid'ye bağlanır, yoksa makbuza/Integrity'ye (guest-dostu).
    suspend fun verify(productId: String, purchaseToken: String) = withContext(Dispatchers.IO) {
        val token = AuthService.token
        val integrity = runCatching { integrityToken() }.getOrNull()
        runCatching {
            val c = URL("${Backend.LICENSE_BASE}/license/verify").openConnection() as HttpURLConnection
            c.requestMethod = "POST"; c.doOutput = true
            c.connectTimeout = 15000; c.readTimeout = 15000
            c.setRequestProperty("Content-Type", "application/json")
            if (token != null) c.setRequestProperty("Authorization", "Bearer $token")
            val body = JSONObject()
                .put("platform", "android").put("productId", productId)
                .put("purchaseToken", purchaseToken)
            if (integrity != null) body.put("integrityToken", integrity)
            c.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = c.responseCode
            val txt = (if (code in 200..299) c.inputStream else c.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: "{}"
            c.disconnect()
            val j = JSONObject(txt)
            appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
                ?.putBoolean("ever", true)?.apply()
            if (code == 200) {
                serverPro = j.optBoolean("pro", false)
                expMillis = (j.optLong("exp", 0)) * 1000L
                lastVerified = System.currentTimeMillis()
                appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
                    ?.putLong("exp", expMillis)?.putLong("verified", lastVerified)?.apply()
            }
        }
        Unit
    }

    private suspend fun integrityToken(): String? {
        val ctx = appContext ?: return null
        val nonceBytes = ByteArray(24).also { SecureRandom().nextBytes(it) }
        val nonce = Base64.encodeToString(nonceBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val manager = IntegrityManagerFactory.create(ctx)
        return suspendCancellableCoroutine { cont ->
            manager.requestIntegrityToken(IntegrityTokenRequest.builder().setNonce(nonce).build())
                .addOnSuccessListener { resp -> cont.resume(resp.token()) }
                .addOnFailureListener { _ -> cont.resume(null) }
        }
    }

    // Etkin Pro kararı. STRICT değilse lokal davranış korunur (gözlem modu).
    // STRICT'te: lokal entitlement + geçerli SUNUCU onayı şart (giriş zorunlu değil — guest-dostu).
    fun effectivePro(local: Boolean, loggedIn: Boolean = false): Boolean {
        if (comped) return true              // manuel premium — koşulsuz Pro
        if (!strict) return local
        if (!local) return false
        val now = System.currentTimeMillis()
        if (expMillis > now) return serverPro
        if (now - lastVerified < GRACE_MS) return serverPro
        return false
    }
}
