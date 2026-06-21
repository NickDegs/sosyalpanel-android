package com.nickdegs.sosyalpanel.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import com.nickdegs.sosyalpanel.data.Repository
import kotlinx.coroutines.flow.first
import java.text.NumberFormat

// Ana ekran widget'ı — toplam erişim + hesap sayısı (Compose/Glance).
class SosyalPanelWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: androidx.glance.appwidget.GlanceId) {
        val accounts = runCatching { Repository.get(context).accounts.first() }.getOrDefault(emptyList())
        val total = accounts.sumOf { it.latest?.followers ?: 0 }
        val count = accounts.size
        // 7 günlük büyüme (varsa)
        val growth = accounts.sumOf { a ->
            val s = a.sorted
            if (s.size < 2) 0 else s.last().followers - s.first().followers
        }
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                        .background(Color(0xFF1A0B2E))
                        .cornerRadius(20.dp)
                        .padding(16.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.Start
                ) {
                    Text("TOPLAM ERİŞİM", style = TextStyle(
                        color = ColorProvider(Color(0xFFB9A8E0)), fontSize = 11.sp, fontWeight = FontWeight.Medium))
                    Text(NumberFormat.getInstance().format(total), style = TextStyle(
                        color = ColorProvider(Color.White), fontSize = 30.sp, fontWeight = FontWeight.Bold))
                    Text(
                        "$count hesap" + if (growth != 0) "  ·  ${if (growth > 0) "+" else ""}${NumberFormat.getInstance().format(growth)}" else "",
                        style = TextStyle(color = ColorProvider(Color(0xFF34D399)), fontSize = 12.sp)
                    )
                }
            }
        }
    }
}

class SosyalPanelWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SosyalPanelWidget()
}
