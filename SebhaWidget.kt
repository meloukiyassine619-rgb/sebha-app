package com.example.sebha

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.flow.first

private val WNight = ColorProvider(Color(0xFF0D1411))
private val WSand = ColorProvider(Color(0xFFE9DFC8))
private val WAmber = ColorProvider(Color(0xFFC9A25B))

class SebhaWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val st = SebhaStore(context.applicationContext).state.first()
        provideContent { WidgetContent(st) }
    }
}

@Composable
private fun WidgetContent(st: SebhaState) {
    val z = azkar[st.selected.coerceIn(0, azkar.lastIndex)]
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WNight)
            .cornerRadius(24.dp)
            .padding(12.dp)
            .clickable(actionRunCallback<BeadAction>()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(z.text, style = TextStyle(color = WSand, fontSize = 16.sp))
        Text(
            "${st.count} / ${z.target}",
            style = TextStyle(color = WAmber, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        )
        Text("المس للتسبيح", style = TextStyle(color = WSand, fontSize = 11.sp))
    }
}

/** كل لمسة على الويدجت = حبة واحدة، مع اهتزاز خفيف (وأطول عند اكتمال الهدف). */
class BeadAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val completed = SebhaStore(context.applicationContext).increment(azkar.map { it.target })
        vibrate(context, completed)
        SebhaWidget().update(context, glanceId)
    }

    private fun vibrate(context: Context, completed: Boolean) {
        val v = context.getSystemService(Vibrator::class.java) ?: return
        val effect = if (completed)
            VibrationEffect.createWaveform(longArrayOf(0, 60, 80, 60), -1)
        else
            VibrationEffect.createOneShot(18, 120)
        v.vibrate(effect)
    }
}

class SebhaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SebhaWidget()
}
