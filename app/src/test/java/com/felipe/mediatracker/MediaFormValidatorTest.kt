package com.felipe.mediatracker

import com.felipe.mediatracker.domain.FormError
import com.felipe.mediatracker.domain.MediaFormInput
import com.felipe.mediatracker.domain.MediaFormValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaFormValidatorTest {

    private val year = 2026
    private val valid = MediaFormInput(title = "Vidas Secas", totalUnits = "176")

    private fun validate(input: MediaFormInput) = MediaFormValidator.validate(input, year)

    @Test
    fun validInput_hasNoErrors() {
        assertFalse(validate(valid).hasErrors)
    }

    @Test
    fun blankTitle_isRequired() {
        assertEquals(FormError.REQUIRED, validate(valid.copy(title = "   ")).title)
    }

    @Test
    fun longTitle_isRejected() {
        val longTitle = "a".repeat(MediaFormValidator.MAX_TITLE_LENGTH + 1)
        assertEquals(FormError.TOO_LONG, validate(valid.copy(title = longTitle)).title)
    }

    @Test
    fun total_isRequired_andMustBePositiveNumber() {
        assertEquals(FormError.REQUIRED, validate(valid.copy(totalUnits = "")).totalUnits)
        assertEquals(FormError.NOT_A_NUMBER, validate(valid.copy(totalUnits = "abc")).totalUnits)
        assertEquals(FormError.TOTAL_OUT_OF_RANGE, validate(valid.copy(totalUnits = "0")).totalUnits)
        assertEquals(
            FormError.TOTAL_OUT_OF_RANGE,
            validate(valid.copy(totalUnits = (MediaFormValidator.MAX_TOTAL_UNITS + 1).toString())).totalUnits,
        )
    }

    @Test
    fun year_isOptional_butValidatedWhenPresent() {
        assertNull(validate(valid.copy(year = "")).year)
        assertNull(validate(valid.copy(year = "1999")).year)
        assertEquals(FormError.NOT_A_NUMBER, validate(valid.copy(year = "19x9")).year)
        assertEquals(FormError.YEAR_OUT_OF_RANGE, validate(valid.copy(year = "1700")).year)
        assertEquals(FormError.YEAR_OUT_OF_RANGE, validate(valid.copy(year = "2100")).year)
    }

    @Test
    fun rating_isOptional_andLimitedToZeroToTen() {
        assertNull(validate(valid.copy(rating = "")).rating)
        assertNull(validate(valid.copy(rating = "0")).rating)
        assertNull(validate(valid.copy(rating = "10")).rating)
        assertEquals(FormError.RATING_OUT_OF_RANGE, validate(valid.copy(rating = "11")).rating)
    }

    @Test
    fun multipleErrors_areReportedTogether() {
        val errors = validate(MediaFormInput(title = "", totalUnits = "", year = "x"))
        assertTrue(errors.hasErrors)
        assertEquals(FormError.REQUIRED, errors.title)
        assertEquals(FormError.REQUIRED, errors.totalUnits)
        assertEquals(FormError.NOT_A_NUMBER, errors.year)
    }
}
