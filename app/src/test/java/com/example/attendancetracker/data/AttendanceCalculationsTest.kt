package com.example.attendancetracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [AttendanceCalculations].
 *
 * Formula under test for safeToSkip:
 *   A / (T + M) >= P   =>   M = floor(A / P - T)
 *
 * Formula under test for computeNeeded:
 *   (A + N) / (T + N) >= P   =>   N = ceil((P*T - A) / (1 - P))
 */
class AttendanceCalculationsTest {

    // ─── computeSafeToSkip — user-specified cases ────────────────────────────

    @Test
    fun `safeToSkip - 6 attended 6 total 85 target gives 1`() {
        assertEquals(1, AttendanceCalculations.computeSafeToSkip(6, 6, 85f))
    }

    @Test
    fun `safeToSkip - 8 attended 10 total 85 target gives 0`() {
        assertEquals(0, AttendanceCalculations.computeSafeToSkip(10, 8, 85f))
    }

    @Test
    fun `safeToSkip - 9 attended 10 total 85 target gives 0`() {
        assertEquals(0, AttendanceCalculations.computeSafeToSkip(10, 9, 85f))
    }

    @Test
    fun `safeToSkip - 10 attended 10 total 85 target gives 1`() {
        assertEquals(1, AttendanceCalculations.computeSafeToSkip(10, 10, 85f))
    }

    @Test
    fun `safeToSkip - 17 attended 20 total 85 target gives 0`() {
        assertEquals(0, AttendanceCalculations.computeSafeToSkip(20, 17, 85f))
    }

    @Test
    fun `safeToSkip - 18 attended 20 total 85 target gives 1`() {
        assertEquals(1, AttendanceCalculations.computeSafeToSkip(20, 18, 85f))
    }

    @Test
    fun `safeToSkip - 20 attended 20 total 85 target gives 3`() {
        assertEquals(3, AttendanceCalculations.computeSafeToSkip(20, 20, 85f))
    }

    // ─── computeSafeToSkip — boundary & edge cases ───────────────────────────

    @Test
    fun `safeToSkip - 0 total gives 0`() {
        assertEquals(0, AttendanceCalculations.computeSafeToSkip(0, 0, 85f))
    }

    @Test
    fun `safeToSkip - 0 attended gives 0`() {
        // Missing all classes — no safe skips possible.
        assertEquals(0, AttendanceCalculations.computeSafeToSkip(5, 0, 85f))
    }

    @Test
    fun `safeToSkip - attendance exactly at target gives 0`() {
        // 17/20 = 85% exactly: missing one more makes it 17/21 = 80.95% < 85%.
        assertEquals(0, AttendanceCalculations.computeSafeToSkip(20, 17, 85f))
    }

    @Test
    fun `safeToSkip - attendance below target gives 0`() {
        // 8/10 = 80% which is below 85%.
        assertEquals(0, AttendanceCalculations.computeSafeToSkip(10, 8, 85f))
    }

    @Test
    fun `safeToSkip never lets attendance fall below target - 75 percent`() {
        // 3/4 = 75.0% exactly at target.
        val skip = AttendanceCalculations.computeSafeToSkip(4, 3, 75f)
        // Verify: attended / (total + skip) must be >= 75%
        val resultPct = 3.0 / (4 + skip) * 100.0
        assertTrue("Expected >= 75% but got $resultPct%", resultPct >= 75.0)
    }

    @Test
    fun `safeToSkip never lets attendance fall below target - 90 percent`() {
        val total = 10; val attended = 10; val target = 90f
        val skip = AttendanceCalculations.computeSafeToSkip(total, attended, target)
        val resultPct = attended.toDouble() / (total + skip) * 100.0
        assertTrue("Expected >= $target% but got $resultPct%", resultPct >= target.toDouble())
    }

    @Test
    fun `safeToSkip with 75 percent target - 4 attended 4 total gives 1`() {
        // 4/5 = 80% >= 75%, 4/6 = 66.7% < 75%  =>  can miss 1
        assertEquals(1, AttendanceCalculations.computeSafeToSkip(4, 4, 75f))
    }

    @Test
    fun `safeToSkip with 80 percent target - 4 attended 4 total gives 1`() {
        // 4/5 = 80% exactly meets 80%, 4/6 = 66.7% < 80%  =>  can miss 1
        assertEquals(1, AttendanceCalculations.computeSafeToSkip(4, 4, 80f))
    }

    @Test
    fun `safeToSkip result plus one always drops below target`() {
        // Fuzz-style invariant check over multiple configurations
        val cases = listOf(
            Triple(6, 6, 85f),
            Triple(10, 10, 85f),
            Triple(20, 18, 85f),
            Triple(20, 20, 85f),
            Triple(4, 4, 75f),
            Triple(10, 10, 90f),
        )
        for ((total, attended, target) in cases) {
            val skip = AttendanceCalculations.computeSafeToSkip(total, attended, target)
            val pctWithSkip = attended.toDouble() / (total + skip) * 100.0
            val pctOneMore  = attended.toDouble() / (total + skip + 1) * 100.0
            assertTrue(
                "[$attended/$total @ $target%] skip=$skip: pct with skip ($pctWithSkip) should be >= target",
                pctWithSkip >= target.toDouble() - 0.001  // tiny epsilon for float arithmetic
            )
            assertTrue(
                "[$attended/$total @ $target%] skip=$skip: pct with skip+1 ($pctOneMore) should be < target",
                pctOneMore < target.toDouble()
            )
        }
    }

    // ─── computeNeeded — classes to attend to reach target ───────────────────

    @Test
    fun `computeNeeded - already above target returns non-positive`() {
        // 9/10 = 90% is above 85% target — should not require more classes.
        val needed = AttendanceCalculations.computeNeeded(10, 9, 85f)
        assertTrue("Expected <= 0 but got $needed", needed <= 0)
    }

    @Test
    fun `computeNeeded - below target returns positive`() {
        // 8/10 = 80% below 85%
        val needed = AttendanceCalculations.computeNeeded(10, 8, 85f)
        assertTrue("Expected > 0 but got $needed", needed > 0)
    }

    @Test
    fun `computeNeeded - result actually brings attendance to target`() {
        // 5/10 = 50%, target 75%.
        val total = 10; val attended = 5; val target = 75f
        val needed = AttendanceCalculations.computeNeeded(total, attended, target)
        assertTrue("needed should be positive", needed > 0)
        val resultPct = (attended + needed).toDouble() / (total + needed) * 100.0
        assertTrue("Result $resultPct% should be >= $target%", resultPct >= target.toDouble() - 0.001)
        // One less should be below target
        if (needed > 1) {
            val oneLessPct = (attended + needed - 1).toDouble() / (total + needed - 1) * 100.0
            assertTrue("One less ($oneLessPct%) should be < $target%", oneLessPct < target.toDouble())
        }
    }

    @Test
    fun `computeNeeded - 0 attended 0 total any target returns 0`() {
        // No data → computeNeeded gives 0 (target * 0 - 0 = 0 → ceil(0) = 0)
        assertEquals(0, AttendanceCalculations.computeNeeded(0, 0, 85f))
    }
}
