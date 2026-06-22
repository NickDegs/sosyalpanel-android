package com.nickdegs.sosyalpanel.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nickdegs.sosyalpanel.AppViewModel
import com.nickdegs.sosyalpanel.R
import com.nickdegs.sosyalpanel.ui.theme.Brand
import com.nickdegs.sosyalpanel.ui.theme.Mint

// iOS ProView karşılığı — abonelik paywall'ı (Play Billing).
@Composable
fun ProDialog(vm: AppViewModel, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val products by vm.billing.products.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.go_pro), fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                feature(stringResource(R.string.feat_ai_chat), stringResource(R.string.feat_ai_chat_desc))
                feature(stringResource(R.string.feat_unlimited), stringResource(R.string.feat_unlimited_desc))
                feature(stringResource(R.string.feat_advanced), stringResource(R.string.feat_advanced_desc))
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.pro_studio_title), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                val tools = listOf("Yazı","Görsel","Video","Logo","Ürün Foto","Çeviri","SEO","Seslendirme")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tools.take(4).forEach { ToolChip(it, Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tools.drop(4).forEach { ToolChip(it, Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(12.dp))
                if (products.isEmpty()) {
                    Text(stringResource(R.string.subs_unavailable), fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                } else {
                    products.forEach { p ->
                        val price = p.subscriptionOfferDetails?.firstOrNull()
                            ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: ""
                        Button(onClick = { (ctx as? Activity)?.let { vm.billing.purchase(it, p) } },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text("${p.title}  $price")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.sub_terms), fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        },
        confirmButton = {
            TextButton(onClick = { vm.billing.queryEntitlements() }) { Text(stringResource(R.string.restore_purchases)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}

@Composable
private fun ToolChip(name: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Brand.copy(alpha = 0.12f))
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(name, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Brand, maxLines = 1)
    }
}

@Composable
private fun feature(title: String, desc: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Filled.Check, null, tint = Mint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}
