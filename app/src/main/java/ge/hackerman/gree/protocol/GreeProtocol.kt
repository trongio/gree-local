package ge.hackerman.gree.protocol

/** Wire constants and value mappings for the Gree local protocol. */
object Gree {

    const val PORT = 7000

    const val POWER = "Pow"
    const val MODE = "Mod"
    const val SET_TEMP = "SetTem"
    const val TEMP_UNIT = "TemUn"
    const val FAN_SPEED = "WdSpd"
    const val FRESH_AIR = "Air"
    const val XFAN = "Blo"
    const val HEALTH = "Health"
    const val SLEEP = "SwhSlp"
    const val LIGHT = "Lig"
    const val SWING_HORIZONTAL = "SwingLfRig"
    const val SWING_VERTICAL = "SwUpDn"
    const val QUIET = "Quiet"
    const val TURBO = "Tur"
    const val SAVE_POWER = "SvSt"
    const val ROOM_TEMP = "TemSen"

    /** Every field we ask for in a status poll. */
    val STATUS_COLUMNS = listOf(
        POWER, MODE, SET_TEMP, TEMP_UNIT, FAN_SPEED, FRESH_AIR, XFAN, HEALTH,
        SLEEP, LIGHT, SWING_HORIZONTAL, SWING_VERTICAL, QUIET, TURBO, SAVE_POWER, ROOM_TEMP,
    )

    /** Both swing axes share a shape: 0 off, 1 full sweep, 2..6 fixed positions. */
    const val SWING_OFF = 0
    const val SWING_FULL = 1
    const val SWING_FIRST_FIXED = 2
    const val SWING_FIXED_COUNT = 5

    const val MIN_TEMP = 16
    const val MAX_TEMP = 30

    /**
     * The internal sensor reports Celsius with a +40 bias. Units without the sensor
     * report 0, which we surface as "unknown" rather than -40.
     */
    fun roomTemperature(raw: Int?): Int? = raw?.takeIf { it > 0 }?.minus(40)
}

enum class GreeMode(val raw: Int, val label: String) {
    AUTO(0, "Auto"),
    COOL(1, "Cool"),
    DRY(2, "Dry"),
    FAN(3, "Fan"),
    HEAT(4, "Heat");

    companion object {
        fun from(raw: Int?): GreeMode = entries.firstOrNull { it.raw == raw } ?: AUTO
    }
}

enum class GreeFan(val raw: Int, val label: String) {
    AUTO(0, "Auto"),
    LOW(1, "Low"),
    MEDIUM_LOW(2, "Med-Low"),
    MEDIUM(3, "Medium"),
    MEDIUM_HIGH(4, "Med-High"),
    HIGH(5, "High");

    companion object {
        fun from(raw: Int?): GreeFan = entries.firstOrNull { it.raw == raw } ?: AUTO
    }
}

/** Vertical louver positions. Values 2..6 are fixed angles, top to bottom. */
enum class GreeSwing(val raw: Int, val label: String) {
    OFF(0, "Off"),
    FULL(1, "Full"),
    FIXED_TOP(2, "Top"),
    FIXED_UPPER(3, "Upper"),
    FIXED_MIDDLE(4, "Middle"),
    FIXED_LOWER(5, "Lower"),
    FIXED_BOTTOM(6, "Bottom");

    companion object {
        fun from(raw: Int?): GreeSwing = entries.firstOrNull { it.raw == raw } ?: OFF
    }
}

/** Horizontal louver positions. Values 2..6 are fixed angles, left to right. */
enum class GreeSwingH(val raw: Int, val label: String) {
    OFF(0, "Off"),
    FULL(1, "Full"),
    LEFT(2, "Left"),
    LEFT_CENTER(3, "Left-center"),
    CENTER(4, "Center"),
    RIGHT_CENTER(5, "Right-center"),
    RIGHT(6, "Right");

    companion object {
        fun from(raw: Int?): GreeSwingH = entries.firstOrNull { it.raw == raw } ?: OFF
    }
}

/** A decoded status snapshot. Unknown fields stay null rather than defaulting to 0. */
data class GreeState(
    val raw: Map<String, Int>,
) {
    val power: Boolean get() = raw[Gree.POWER] == 1
    val mode: GreeMode get() = GreeMode.from(raw[Gree.MODE])
    val targetTemp: Int get() = raw[Gree.SET_TEMP] ?: Gree.MIN_TEMP
    val fan: GreeFan get() = GreeFan.from(raw[Gree.FAN_SPEED])
    val swingVertical: GreeSwing get() = GreeSwing.from(raw[Gree.SWING_VERTICAL])
    val swingHorizontal: GreeSwingH get() = GreeSwingH.from(raw[Gree.SWING_HORIZONTAL])
    val turbo: Boolean get() = raw[Gree.TURBO] == 1
    val quiet: Boolean get() = (raw[Gree.QUIET] ?: 0) > 0
    val light: Boolean get() = raw[Gree.LIGHT] == 1
    val xfan: Boolean get() = raw[Gree.XFAN] == 1
    val health: Boolean get() = raw[Gree.HEALTH] == 1
    val sleep: Boolean get() = raw[Gree.SLEEP] == 1
    val roomTemp: Int? get() = Gree.roomTemperature(raw[Gree.ROOM_TEMP])
}
