package com.example.sebha

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore(name = "sebha")

data class SebhaState(
    val selected: Int = 0,
    val count: Int = 0,
    val rounds: Int = 0,
    /** تاريخ (yyyy-MM-dd) ← عدد التسبيحات في ذلك اليوم */
    val daily: Map<String, Int> = emptyMap(),
)

class SebhaStore(private val context: Context) {
    private val kSelected = intPreferencesKey("selected")
    private val kCount = intPreferencesKey("count")
    private val kRounds = intPreferencesKey("rounds")
    private val kDaily = stringPreferencesKey("daily")

    val state: Flow<SebhaState> = context.dataStore.data.map { p ->
        SebhaState(
            selected = p[kSelected] ?: 0,
            count = p[kCount] ?: 0,
            rounds = p[kRounds] ?: 0,
            daily = decode(p[kDaily]),
        )
    }

    suspend fun saveProgress(selected: Int, count: Int, rounds: Int) {
        context.dataStore.edit {
            it[kSelected] = selected
            it[kCount] = count
            it[kRounds] = rounds
        }
    }

    /** يضيف عدد التسبيحات إلى اليوم الحالي ويحتفظ بآخر 60 يومًا فقط. */
    suspend fun addToday(n: Int) {
        if (n <= 0) return
        context.dataStore.edit { p -> bumpDaily(p, n) }
    }

    /**
     * حبة واحدة من الويدجت: تزيد العدّ والإحصائية في عملية واحدة.
     * تُرجع true إذا اكتمل الهدف (دورة جديدة).
     */
    suspend fun increment(targets: List<Int>): Boolean {
        var completed = false
        context.dataStore.edit { p ->
            val sel = (p[kSelected] ?: 0).coerceIn(0, targets.lastIndex)
            var c = (p[kCount] ?: 0) + 1
            var r = p[kRounds] ?: 0
            if (c >= targets[sel]) { c = 0; r++; completed = true }
            p[kCount] = c
            p[kRounds] = r
            bumpDaily(p, 1)
        }
        return completed
    }

    private fun bumpDaily(p: androidx.datastore.preferences.core.MutablePreferences, n: Int) {
        val map = decode(p[kDaily]).toMutableMap()
        val today = LocalDate.now().toString()
        map[today] = (map[today] ?: 0) + n
        val keep = map.keys.sortedDescending().take(60).toSet()
        p[kDaily] = encode(map.filterKeys { it in keep })
    }

    // تخزين مضغوط: "2026-10-23:120,2026-10-22:85"
    private fun encode(m: Map<String, Int>) = m.entries.joinToString(",") { "${it.key}:${it.value}" }

    private fun decode(s: String?): Map<String, Int> =
        if (s.isNullOrBlank()) emptyMap()
        else s.split(",").mapNotNull {
            val parts = it.split(":")
            val v = parts.getOrNull(1)?.toIntOrNull()
            if (parts.size == 2 && v != null) parts[0] to v else null
        }.toMap()
}
