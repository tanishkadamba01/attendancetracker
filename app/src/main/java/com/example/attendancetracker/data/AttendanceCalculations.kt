package com.example.attendancetracker.data

import kotlin.math.ceil
import kotlin.math.floor

/**
 * Pure, stateless attendance calculation helpers.
 * Extracted here so they can be unit-tested independently of Android / ViewModel machinery.
 */
object AttendanceCalculations {

    /**
     * Computes how many **consecutive future classes must be attended** to reach [targetPct].
     *
     * Solves: (attended + N) / (total + N) >= target  for N.
     *
     * Returns:
     *  - positive  → must attend this many more classes
     *  - zero      → exactly at target
     *  - negative  → already above target (magnitude is NOT safe-to-skip; use [computeSafeToSkip])
     */
    fun computeNeeded(total: Int, attended: Int, targetPct: Float): Int {
        val targetFraction = targetPct / 100f
        if (targetFraction >= 1f) {
            // 100% target: need to attend every single future class; if already missing any, return
            // a large sentinel so callers show "you need to attend all classes".
            return if (attended < total) Int.MAX_VALUE else 0
        }
        val raw = (targetFraction * total - attended) / (1f - targetFraction)
        return ceil(raw).toInt()
    }

    /**
     * Computes the maximum number of **future classes that can be missed** while keeping
     * attendance at or above [targetPct].
     *
     * Derivation:
     *   attended / (total + M) >= P
     *   total + M <= attended / P
     *   M <= attended / P - total
     *   M = floor(attended / P - total)
     *
     * Always returns a non-negative integer (0 means no margin — any miss would drop below target).
     * Returns 0 if [total] == 0, [attended] == 0, or current attendance is already below target.
     */
    fun computeSafeToSkip(total: Int, attended: Int, targetPct: Float): Int {
        if (total == 0 || attended == 0) return 0
        val targetFraction = targetPct / 100f
        if (targetFraction <= 0f) return 0   // degenerate: 0% target — treat conservatively
        // A / (T + M) >= P  =>  M = floor(A/P - T)
        val maxMissable = floor(attended / targetFraction - total).toInt()
        return maxOf(0, maxMissable)
    }
}
