package com.example.sebha

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlin.random.Random

/** صوت نقرة قصيرة لكل حبة، مع تغيير طفيف في النغمة حتى لا يبدو آليًا. */
class BeadSound(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private var soundId = 0
    private var ready = false

    init {
        pool.setOnLoadCompleteListener { _, _, status -> ready = status == 0 }
        soundId = pool.load(context, R.raw.bead_click, 1)
    }

    fun play() {
        if (!ready) return
        pool.play(soundId, 0.7f, 0.7f, 1, 0, 0.94f + Random.nextFloat() * 0.12f)
    }

    fun release() = pool.release()
}
