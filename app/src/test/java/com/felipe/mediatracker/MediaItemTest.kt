package com.felipe.mediatracker

import com.felipe.mediatracker.domain.MediaStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaItemTest {

    @Test
    fun status_isPlanned_whenNothingConsumed() {
        assertEquals(MediaStatus.PLANNED, sampleItem(total = 100, current = 0).status)
    }

    @Test
    fun status_isInProgress_whenPartiallyConsumed() {
        assertEquals(MediaStatus.IN_PROGRESS, sampleItem(total = 100, current = 40).status)
    }

    @Test
    fun status_isCompleted_whenReachesTotal() {
        assertEquals(MediaStatus.COMPLETED, sampleItem(total = 100, current = 100).status)
    }

    @Test
    fun unknownTotal_neverCompletes_andHasZeroProgress() {
        val item = sampleItem(total = 0, current = 5)
        assertFalse(item.hasKnownTotal)
        assertEquals(0f, item.progressFraction, 0f)
        assertEquals(MediaStatus.IN_PROGRESS, item.status)
    }

    @Test
    fun progressPercent_isRounded() {
        assertEquals(33, sampleItem(total = 3, current = 1).progressPercent)
        assertEquals(67, sampleItem(total = 3, current = 2).progressPercent)
    }

    @Test
    fun withProgress_clampsToValidRange() {
        val item = sampleItem(total = 50)
        assertEquals(0, item.withProgress(-10).currentUnit)
        assertEquals(50, item.withProgress(999).currentUnit)
        assertEquals(20, item.withProgress(20).currentUnit)
    }

    @Test
    fun withRating_clampsAndAllowsNull() {
        val item = sampleItem()
        assertEquals(10, item.withRating(99).rating)
        assertEquals(0, item.withRating(-3).rating)
        assertNull(item.withRating(8).withRating(null).rating)
    }

    @Test
    fun copy_doesNotMutateOriginal() {
        val original = sampleItem(total = 10, current = 1)
        val changed = original.withProgress(9)
        assertEquals(1, original.currentUnit)
        assertTrue(changed !== original)
    }
}
