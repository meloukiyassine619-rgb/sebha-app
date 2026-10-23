package com.example.sebha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

// ---------- الألوان ----------
private val Night = Color(0xFF0D1411)
private val Thread = Color(0xFF34453B)
private val Sand = Color(0xFFE9DFC8)
private val Amber = Color(0xFFC9A25B)
private val AmberDark = Color(0xFF6E4F23)

internal data class Dhikr(val text: String, val target: Int)

internal val azkar = listOf(
    Dhikr("سبحان الله", 33),
    Dhikr("الحمد لله", 33),
    Dhikr("الله أكبر", 34),
    Dhikr("أستغفر الله", 100),
    Dhikr("اللهم صلِّ على محمد", 100),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = darkColorScheme(background = Night)) {
                    SebhaScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun SebhaScreen() {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val decay = rememberSplineBasedDecay<Float>()
    val scope = rememberCoroutineScope()

    var selected by remember { mutableIntStateOf(0) }
    var count by remember { mutableIntStateOf(0) }
    var rounds by remember { mutableIntStateOf(0) }
    var eyesClosed by remember { mutableStateOf(false) }

    // ---- الحفظ ----
    val context = LocalContext.current
    val store = remember { SebhaStore(context.applicationContext) }
    var loaded by remember { mutableStateOf(false) }
    var daily by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var pending by remember { mutableIntStateOf(0) } // تسبيحات لم تُحفظ في إحصائية اليوم بعد
    var showStats by remember { mutableStateOf(false) }

    // ---- الصوت ----
    var soundOn by remember { mutableStateOf(true) }
    val beadSound = remember { BeadSound(context.applicationContext) }
    DisposableEffect(Unit) { onDispose { beadSound.release() } }

    LaunchedEffect(Unit) {
        val st = store.state.first()
        selected = st.selected.coerceIn(0, azkar.lastIndex)
        count = st.count
        rounds = st.rounds
        daily = st.daily
        loaded = true
    }
    // إن غيّر الويدجت العدّ والتطبيق في الخلفية، أعد القراءة عند العودة
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (loaded && pending == 0) scope.launch {
            val st = store.state.first()
            selected = st.selected.coerceIn(0, azkar.lastIndex)
            count = st.count
            rounds = st.rounds
            daily = st.daily
        }
    }
    LaunchedEffect(loaded) {
        if (!loaded) return@LaunchedEffect
        snapshotFlow { listOf(selected, count, rounds, pending) }
            .debounce(400)
            .collect {
                store.saveProgress(selected, count, rounds)
                val n = pending
                if (n > 0) {
                    pending -= n
                    store.addToday(n)
                    daily = store.state.first().daily
                }
                SebhaWidget().updateAll(context)
            }
    }

    // موضع السبحة بوحدة "حبة": الجزء الصحيح = عدد الحبات التي مرّت
    var pos by remember { mutableFloatStateOf(0f) }
    var flingJob by remember { mutableStateOf<Job?>(null) }
    val spacingPx = with(density) { 56.dp.toPx() }
    val tracker = remember { VelocityTracker() }

    val dhikr = azkar[selected]

    // العدّ + الاهتزاز عند عبور كل حبة (للأمام فقط)
    LaunchedEffect(Unit) {
        var last = floor(pos).toInt()
        snapshotFlow { floor(pos).toInt() }.collect { now ->
            if (now > last) {
                repeat(now - last) {
                    count++
                    pending++
                    if (soundOn) beadSound.play()
                    if (count >= azkar[selected].target) {
                        count = 0
                        rounds++
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            }
            last = now
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(if (eyesClosed) Color.Black else Night)
            .systemBarsPadding()
    ) {
        if (!eyesClosed) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                itemsIndexed(azkar) { i, z ->
                    FilterChip(
                        selected = i == selected,
                        onClick = { selected = i; count = 0 },
                        label = { Text(z.text) }
                    )
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(dhikr.text, color = Sand, fontSize = 28.sp)
                Text("$count / ${dhikr.target}", color = Amber, fontSize = 40.sp)
                if (rounds > 0) Text("الدورات: $rounds", color = Sand.copy(alpha = .6f), fontSize = 14.sp)
            }
        }

        // منطقة السبحة والسحب
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInputDrag(
                    onStart = { flingJob?.cancel(); tracker.resetTracking() },
                    onDelta = { dy, change ->
                        tracker.addPosition(change.uptimeMillis, change.position)
                        pos += -dy / spacingPx
                    },
                    onEnd = {
                        val v = -tracker.calculateVelocity().y / spacingPx
                        flingJob = scope.launch {
                            Animatable(pos).apply {
                                animateDecay(v, decay) { pos = value }
                                // استقرار على أقرب حبة
                                snapTo(pos)
                                animateTo(pos.roundToInt().toFloat(), spring(stiffness = Spring.StiffnessLow)) {
                                    pos = value
                                }
                            }
                        }
                    }
                )
        ) {
            if (!eyesClosed) BeadsCanvas(pos)
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("صوت الحبات", color = Sand)
            Switch(checked = soundOn, onCheckedChange = { soundOn = it })
        }
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (!eyesClosed) TextButton(onClick = { showStats = true }) { Text("الإحصائيات", color = Amber) }
            Text("وضع العينين المغمضتين", color = Sand)
            Switch(checked = eyesClosed, onCheckedChange = { eyesClosed = it })
        }
    }

    if (showStats) {
        val today = java.time.LocalDate.now().toString()
        val live = daily + (today to ((daily[today] ?: 0) + pending)) // يشمل ما لم يُحفظ بعد
        StatsSheet(daily = live, onDismiss = { showStats = false })
    }
}

// ---------- رسم المسار والحبات ----------
private const val BEADS = 12

private fun curvePoint(u: Float, w: Float, h: Float): Offset {
    val x = w * (0.5f + 0.2f * sin(u * 2f * PI.toFloat()))
    val y = h * (0.94f - 0.88f * u)
    return Offset(x, y)
}

@Composable
private fun BeadsCanvas(pos: Float) {
    Canvas(Modifier.fillMaxSize()) {
        // الخيط
        val path = Path()
        for (k in 0..80) {
            val p = curvePoint(k / 80f, size.width, size.height)
            if (k == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(path, Thread, style = Stroke(width = 3.dp.toPx()))

        val frac = pos - floor(pos)
        for (i in 0 until BEADS) {
            val u = (i + frac) / BEADS
            val c = curvePoint(u, size.width, size.height)
            // الحبة الأقرب للمستخدم (أسفل) أكبر قليلًا، وتتلاشى عند الأطراف
            val r = 22.dp.toPx() * (0.65f + 0.5f * (1f - u))
            val alpha = (minOf(u, 1f - u) / 0.1f).coerceIn(0f, 1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Amber.copy(alpha = alpha), AmberDark.copy(alpha = alpha)),
                    center = Offset(c.x - r * 0.3f, c.y - r * 0.3f),
                    radius = r * 1.5f
                ),
                radius = r,
                center = c
            )
        }
    }
}

// ---------- أداة السحب ----------
private fun Modifier.pointerInputDrag(
    onStart: () -> Unit,
    onDelta: (dy: Float, change: androidx.compose.ui.input.pointer.PointerInputChange) -> Unit,
    onEnd: () -> Unit,
): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { onStart() },
            onDragEnd = { onEnd() },
            onDragCancel = { onEnd() },
        ) { change, drag ->
            change.consume()
            onDelta(drag.y, change)
        }
    }
)
