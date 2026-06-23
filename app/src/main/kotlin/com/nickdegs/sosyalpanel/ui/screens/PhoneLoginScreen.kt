package com.nickdegs.sosyalpanel.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nickdegs.sosyalpanel.data.AuthService
import com.nickdegs.sosyalpanel.ui.components.AnimatedMeshBackground
import com.nickdegs.sosyalpanel.ui.components.GlassCard
import kotlinx.coroutines.launch

// SMS-only giriş kapısı. Telefon → OTP → doğrula. iOS PhoneLoginView karşılığı.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneLoginScreen(onLoggedIn: suspend () -> Unit) {
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(0) }      // 0=telefon, 1=kod
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        AnimatedMeshBackground()
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.Sms, null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text("Social Panel", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                if (step == 0) "Telefon numaranla giriş yap"
                else "Telefonuna gelen kodu gir",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                AnimatedContent(targetState = step, label = "auth-step") { s ->
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (s == 0) {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it; error = null },
                                label = { Text("Telefon (ülke koduyla)") },
                                placeholder = { Text("+90 5xx xxx xx xx") },
                                singleLine = true,
                                enabled = !loading,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            OutlinedTextField(
                                value = code,
                                onValueChange = { if (it.length <= 6) code = it.filter { c -> c.isDigit() }; error = null },
                                label = { Text("Doğrulama kodu") },
                                placeholder = { Text("123456") },
                                singleLine = true,
                                enabled = !loading,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, textAlign = TextAlign.Center)
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        error = null; loading = true
                        scope.launch {
                            if (step == 0) {
                                when (AuthService.sendCode(phone)) {
                                    AuthService.Result.Ok -> { step = 1 }
                                    AuthService.Result.BadNumber -> error = "Geçersiz telefon numarası."
                                    else -> error = "Bağlantı hatası. Tekrar dene."
                                }
                            } else {
                                when (AuthService.verify(phone, code)) {
                                    AuthService.Result.Ok -> onLoggedIn()
                                    AuthService.Result.BadCode -> error = "Kod hatalı veya süresi doldu."
                                    else -> error = "Bağlantı hatası. Tekrar dene."
                                }
                            }
                            loading = false
                        }
                    },
                    enabled = !loading && (if (step == 0) phone.isNotBlank() else code.length >= 4),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text(if (step == 0) "Kod Gönder" else "Giriş Yap", fontWeight = FontWeight.SemiBold)
                }

                if (step == 1) {
                    TextButton(
                        onClick = { step = 0; code = ""; error = null },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Numarayı değiştir")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Girişin SMS ile güvenli. Verilerin numarana bağlı bulutta saklanır.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                textAlign = TextAlign.Center
            )
        }
    }
}
