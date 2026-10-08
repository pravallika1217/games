package com.alibi.engine.fermi

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt

/** The bonus question: nobody knows the exact answer, so you make a smart estimate. */
data class FermiQuestion(
    val prompt: String,
    val answer: Double,
    val unit: String,
    /** The step-by-step reasoning shown after the guess. */
    val explanation: String,
    /** Range of the guess slider. */
    val min: Double = 1.0,
    val max: Double = 1_000_000.0,
) {
    init { require(min > 0 && answer in min..max) { "Answer must be inside the slider range" } }
}

enum class FermiVerdict(val emoji: String, val label: String, val maxRatio: Double) {
    BULLSEYE("🎯", "Bullseye: within 10%", 1.1),
    HOT("🔥", "Hot: within 50%", 1.5),
    WARM("👍", "Warm: within 2×", 2.0),
    COLD("🧊", "Cold: within 5×", 5.0),
    MISS("❌", "Way off", Double.POSITIVE_INFINITY),
}

data class FermiOutcome(val guess: Double, val answer: Double, val score: Int, val verdict: FermiVerdict)

object FermiScorer {
    /** How many times off the guess is, in either direction. 1.0 is perfect. */
    fun ratio(guess: Double, answer: Double): Double {
        require(answer > 0) { "Answer must be positive" }
        if (guess <= 0) return Double.POSITIVE_INFINITY
        return max(guess / answer, answer / guess)
    }

    /** 100 for a perfect guess, falling off with how many times off you are; 0 at 10× off. */
    fun score(guess: Double, answer: Double): Int {
        val r = ratio(guess, answer)
        if (r.isInfinite()) return 0
        return (100 * (1 - log10(r))).coerceIn(0.0, 100.0).roundToInt()
    }

    fun outcome(guess: Double, question: FermiQuestion): FermiOutcome {
        val r = ratio(guess, question.answer)
        return FermiOutcome(
            guess = guess,
            answer = question.answer,
            score = score(guess, question.answer),
            verdict = FermiVerdict.entries.first { r <= it.maxRatio },
        )
    }
}
