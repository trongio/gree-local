package ge.hackerman.gree

import ge.hackerman.gree.protocol.Gree
import ge.hackerman.gree.protocol.GreeFan
import ge.hackerman.gree.protocol.GreeMode
import ge.hackerman.gree.protocol.GreeState
import ge.hackerman.gree.protocol.GreeSwing
import ge.hackerman.gree.protocol.GreeSwingH
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GreeProtocolTest {

    @Test
    fun `room temperature carries a plus forty bias`() {
        assertEquals(25, Gree.roomTemperature(65))
        assertEquals(0, Gree.roomTemperature(40))
    }

    /** Units without the sensor report 0, which must not surface as -40 degrees. */
    @Test
    fun `a missing sensor reads as unknown rather than minus forty`() {
        assertNull(Gree.roomTemperature(0))
        assertNull(Gree.roomTemperature(null))
    }

    @Test
    fun `enums map their wire values`() {
        assertEquals(GreeMode.COOL, GreeMode.from(1))
        assertEquals(GreeMode.HEAT, GreeMode.from(4))
        assertEquals(GreeFan.HIGH, GreeFan.from(5))
        assertEquals(GreeSwing.FIXED_BOTTOM, GreeSwing.from(6))
        assertEquals(GreeSwingH.RIGHT, GreeSwingH.from(6))
    }

    @Test
    fun `unknown wire values fall back rather than throwing`() {
        assertEquals(GreeMode.AUTO, GreeMode.from(99))
        assertEquals(GreeFan.AUTO, GreeFan.from(null))
        assertEquals(GreeSwing.OFF, GreeSwing.from(-1))
    }

    @Test
    fun `both swing axes share the off, full and fixed layout`() {
        assertEquals(GreeSwing.OFF.raw, Gree.SWING_OFF)
        assertEquals(GreeSwing.FULL.raw, Gree.SWING_FULL)
        assertEquals(GreeSwingH.OFF.raw, Gree.SWING_OFF)
        assertEquals(GreeSwingH.FULL.raw, Gree.SWING_FULL)
        // Five selectable positions starting at 2, on each axis.
        assertEquals(6, Gree.SWING_FIRST_FIXED + Gree.SWING_FIXED_COUNT - 1)
    }

    @Test
    fun `state reads the fields the UI depends on`() {
        val state = GreeState(
            mapOf(
                Gree.POWER to 1,
                Gree.MODE to 1,
                Gree.SET_TEMP to 23,
                Gree.FAN_SPEED to 3,
                Gree.SWING_VERTICAL to 2,
                Gree.SWING_HORIZONTAL to 4,
                Gree.TURBO to 1,
                Gree.QUIET to 0,
                Gree.ROOM_TEMP to 65,
            ),
        )
        assertTrue(state.power)
        assertEquals(GreeMode.COOL, state.mode)
        assertEquals(23, state.targetTemp)
        assertEquals(GreeFan.MEDIUM, state.fan)
        assertEquals(GreeSwing.FIXED_TOP, state.swingVertical)
        assertEquals(GreeSwingH.CENTER, state.swingHorizontal)
        assertTrue(state.turbo)
        assertFalse(state.quiet)
        assertEquals(25, state.roomTemp)
    }

    /** Some firmware answers a status poll without every column it was asked for. */
    @Test
    fun `an empty state does not throw`() {
        val state = GreeState(emptyMap())
        assertFalse(state.power)
        assertEquals(GreeMode.AUTO, state.mode)
        assertEquals(Gree.MIN_TEMP, state.targetTemp)
        assertNull(state.roomTemp)
    }

    @Test
    fun `quiet is truthy for any non-zero, unlike the plain toggles`() {
        assertTrue(GreeState(mapOf(Gree.QUIET to 2)).quiet)
        assertFalse(GreeState(mapOf(Gree.QUIET to 0)).quiet)
    }

    @Test
    fun `every polled column is one the state can read`() {
        assertTrue(Gree.SWING_HORIZONTAL in Gree.STATUS_COLUMNS)
        assertTrue(Gree.ROOM_TEMP in Gree.STATUS_COLUMNS)
        assertEquals(Gree.STATUS_COLUMNS.distinct().size, Gree.STATUS_COLUMNS.size)
    }
}
