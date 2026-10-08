package com.alibi.game

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/** Small sound and vibration cues. Built-in tones only, so the app ships no audio files. */
class Sfx(context: Context) {
    var enabled = true

    private val tones: ToneGenerator? = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 55) }.getOrNull()

    @Suppress("DEPRECATION")
    private val vibrator: Vibrator? = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    private fun tone(type: Int, ms: Int) {
        if (enabled) runCatching { tones?.startTone(type, ms) }
    }

    private fun buzz(ms: Long) {
        if (!enabled) return
        val v = vibrator ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            else @Suppress("DEPRECATION") v.vibrate(ms)
        }
    }

    fun ring() { tone(ToneGenerator.TONE_SUP_RINGTONE, 900); buzz(300) }
    fun siren() = tone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1200)
    fun pin() = tone(ToneGenerator.TONE_PROP_BEEP, 40)
    fun found() { tone(ToneGenerator.TONE_PROP_ACK, 150); buzz(40) }
    fun success() { tone(ToneGenerator.TONE_PROP_BEEP2, 200); buzz(60) }
    fun snap() { tone(ToneGenerator.TONE_PROP_NACK, 220); buzz(120) }
    fun nervous() { tone(ToneGenerator.TONE_CDMA_LOW_L, 400); buzz(220) }
    fun cuffs() { tone(ToneGenerator.TONE_PROP_PROMPT, 120); buzz(80) }
    fun stamp() { tone(ToneGenerator.TONE_DTMF_0, 90); buzz(250) }

    fun release() { tones?.release() }
}
