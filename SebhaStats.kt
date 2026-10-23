package com.example.sebha

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate

private val arabicDays = mapOf(
    DayOfWeek.SATURDAY to "سبت", DayOfWeek.SUNDAY to "أحد", DayOfWeek.MONDAY to "اثنين",
    DayOfWeek.TUESDAY to "ثلاثاء", DayOfWeek.WEDNESDAY to "أربعاء",
    DayOfWeek.THURSDAY to "خميس", DayOfWeek.FRIDAY to "جمعة",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsSheet(daily: Map<String, Int>, onDismiss: () -> Unit) {
    val today = LocalDate.now()
    // آخر 7 أيام: الأقدم أولًا
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val values = days.map { daily[it.toString()] ?: 0 }
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF16201B)) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("اليوم", color = Color(0xFFE9DFC8).copy(alpha = .7f), fontSize = 14.sp)
            Text("${values.last()}", color = Color(0xFFC9A25B), fontSize = 44.sp)
            Text(
                "المجموع هذا الأسبوع: ${values.sum()}",
                color = Color(0xFFE9DFC8), fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                val slot = size.width / 7f
                val barW = slot * 0.5f
                values.forEachIndexed { i, v ->
                    val h = size.height * (v / max.toFloat())
                    val x = size.width - (i + 1) * slot + (slot - barW) / 2f // من اليمين لليسار
                    drawRoundRect(
                        color = if (i == 6) Color(0xFFC9A25B) else Color(0xFF3F5247),
                        topLeft = Offset(x, size.height - h.coerceAtLeast(4.dp.toPx())),
                        size = Size(barW, h.coerceAtLeast(4.dp.toPx())),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                // اتجاه RTL: الأقدم في اليمين واليوم في اليسار، مطابق للأعمدة
                days.forEach { d ->
                    Text(
                        arabicDays[d.dayOfWeek] ?: "",
                        color = Color(0xFFE9DFC8).copy(alpha = .6f),
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
