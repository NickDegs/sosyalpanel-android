package com.nickdegs.sosyalpanel.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nickdegs.sosyalpanel.AppViewModel
import com.nickdegs.sosyalpanel.R
import com.nickdegs.sosyalpanel.data.AppOpener
import com.nickdegs.sosyalpanel.data.LocaleHelper
import com.nickdegs.sosyalpanel.data.Platform
import com.nickdegs.sosyalpanel.ui.components.GlassCard
import com.nickdegs.sosyalpanel.ui.components.PlatformBadge
import com.nickdegs.sosyalpanel.ui.theme.Danger
import com.nickdegs.sosyalpanel.ui.theme.Gold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val accounts by vm.accounts.collectAsState()
    val isPro by vm.billing.isPro.collectAsState()
    var showLang by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }
    var showPro by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_settings), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pro
            item {
                GlassCard(modifier = Modifier.clickable(enabled = !isPro) { showPro = true }, tint = Gold) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.WorkspacePremium, null, tint = Gold)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (isPro) "Social Panel Pro" else stringResource(R.string.go_pro), fontWeight = FontWeight.SemiBold)
                            Text(if (isPro) stringResource(R.string.all_features_active) else stringResource(R.string.unlimited_accounts_adv),
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        if (isPro) Icon(Icons.Filled.Verified, null, tint = com.nickdegs.sosyalpanel.ui.theme.Mint)
                        else Icon(Icons.Filled.ChevronRight, null)
                    }
                }
            }

            // Language
            item {
                GlassCard(modifier = Modifier.clickable { showLang = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Language, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.app_language), Modifier.weight(1f))
                        Text(LocaleHelper.SUPPORTED[LocaleHelper.current(ctx)] ?: stringResource(R.string.system_lang),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
                        Icon(Icons.Filled.ChevronRight, null)
                    }
                }
            }

            // Open platforms
            item {
                GlassCard {
                    Text(stringResource(R.string.open_platforms), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Platform.entries.forEach { p ->
                        Row(Modifier.fillMaxWidth().clickable { AppOpener.open(ctx, p) }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            PlatformBadge(p, 28); Spacer(Modifier.width(10.dp))
                            Text(p.displayName, Modifier.weight(1f), fontSize = 14.sp)
                            Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            // Data / backup
            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CloudDone, null, tint = com.nickdegs.sosyalpanel.ui.theme.BrandBlue)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.auto_backup), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(stringResource(R.string.auto_backup_desc), fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showClear = true }) {
                        Icon(Icons.Filled.DeleteForever, null, tint = Danger)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.delete_all_data), color = Danger)
                    }
                }
            }

            // About
            item {
                GlassCard {
                    aboutRow(Icons.Filled.PrivacyTip, stringResource(R.string.privacy_policy)) {
                        openUrl(ctx, "https://panel.realvirtuality.app/privacy")
                    }
                    aboutRow(Icons.Filled.Description, stringResource(R.string.terms_of_use)) {
                        openUrl(ctx, "https://panel.realvirtuality.app/terms")
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.version), Modifier.weight(1f), fontSize = 14.sp)
                        Text(appVersion(ctx), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showLang) LanguagePicker(ctx, onDismiss = { showLang = false })
    if (showPro) ProDialog(vm) { showPro = false }
    if (showClear) AlertDialog(
        onDismissRequest = { showClear = false },
        title = { Text(stringResource(R.string.delete_all_data)) },
        text = { Text(stringResource(R.string.delete_all_warning)) },
        confirmButton = { TextButton(onClick = { vm.deleteAll(); showClear = false }) { Text(stringResource(R.string.delete), color = Danger) } },
        dismissButton = { TextButton(onClick = { showClear = false }) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun aboutRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f), fontSize = 14.sp)
        Icon(Icons.Filled.ChevronRight, null, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun LanguagePicker(ctx: android.content.Context, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_language)) },
        text = {
            LazyColumn {
                LocaleHelper.SUPPORTED.forEach { (code, name) ->
                    item {
                        Row(Modifier.fillMaxWidth().clickable {
                            LocaleHelper.persist(ctx, code)
                            (ctx as? Activity)?.recreate()
                            onDismiss()
                        }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (code.isEmpty()) stringResource(R.string.system_lang) else name, Modifier.weight(1f))
                            if (LocaleHelper.current(ctx) == code) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}

private fun openUrl(ctx: android.content.Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun appVersion(ctx: android.content.Context): String =
    runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.1.0" }.getOrDefault("1.1.0")
