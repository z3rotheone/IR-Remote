package com.z3rotheone.blazer

/** A ready-to-send IR burst: carrier frequency (Hz) + on/off durations in microseconds. */
class IrSignal(val frequency: Int, val pattern: IntArray) {
    val durationMs: Long get() = pattern.sum() / 1000L

    fun sameAs(other: IrSignal) = frequency == other.frequency && pattern.contentEquals(other.pattern)
}

/** Builds an alternating mark/space pattern; consecutive marks or spaces are merged. */
class Pulses {
    private val p = ArrayList<Int>()

    fun mark(us: Int) = apply { if (p.size % 2 == 1) p[p.size - 1] += us else p += us }
    fun space(us: Int) = apply {
        if (p.isEmpty()) return@apply
        if (p.size % 2 == 0) p[p.size - 1] += us else p += us
    }

    fun bits(value: Long, count: Int, msbFirst: Boolean, bitMark: Int, one: Int, zero: Int) = apply {
        for (i in 0 until count) {
            val shift = if (msbFirst) count - 1 - i else i
            mark(bitMark)
            space(if ((value shr shift) and 1L == 1L) one else zero)
        }
    }

    fun bytes(data: IntArray, msbFirst: Boolean, bitMark: Int, one: Int, zero: Int) = apply {
        data.forEach { bits(it.toLong() and 0xFF, 8, msbFirst, bitMark, one, zero) }
    }

    fun build(frequency: Int): IrSignal {
        if (p.isNotEmpty() && p.size % 2 == 0) p.removeAt(p.size - 1) // end on a mark
        return IrSignal(frequency, p.toIntArray())
    }
}

enum class Key {
    POWER, VOL_UP, VOL_DOWN, MUTE, CH_UP, CH_DOWN,
    MENU, SOURCE, HOME, BACK, UP, DOWN, LEFT, RIGHT, OK
}

/** Encoders for the common consumer IR protocols. */
object Ir {
    /** NEC (LG, many others). Standard: address + ~address. Extended: 16-bit address (Roku). */
    fun nec(address: Int, command: Int, extended: Boolean = false): IrSignal {
        val bytes = if (extended) {
            intArrayOf(address and 0xFF, (address shr 8) and 0xFF, command and 0xFF, command.inv() and 0xFF)
        } else {
            intArrayOf(address and 0xFF, address.inv() and 0xFF, command and 0xFF, command.inv() and 0xFF)
        }
        return pulseDistance(9000, 4500, bytes)
    }

    /** Samsung32 (Samsung TVs): like NEC but 4.5ms header and address sent twice. */
    fun samsung(address: Int, command: Int): IrSignal =
        pulseDistance(4500, 4500, intArrayOf(address and 0xFF, address and 0xFF, command and 0xFF, command.inv() and 0xFF))

    private fun pulseDistance(headerMark: Int, headerSpace: Int, bytes: IntArray): IrSignal {
        val p = ArrayList<Int>(68)
        p += headerMark; p += headerSpace
        for (b in bytes) {
            for (i in 0 until 8) {
                p += 560
                p += if ((b shr i) and 1 == 1) 1690 else 560
            }
        }
        p += 560 // stop bit
        return IrSignal(38000, p.toIntArray())
    }

    /** Sony SIRC 12-bit (7 command bits + 5 address bits, LSB first), sent 3x at 40kHz. */
    fun sony12(address: Int, command: Int): IrSignal {
        val frame = ArrayList<Int>()
        frame += 2400; frame += 600
        for (i in 0 until 7) { frame += if ((command shr i) and 1 == 1) 1200 else 600; frame += 600 }
        for (i in 0 until 5) { frame += if ((address shr i) and 1 == 1) 1200 else 600; frame += 600 }
        val frameLen = frame.sum()
        val out = ArrayList<Int>()
        repeat(3) { n ->
            out.addAll(frame)
            if (n < 2) out[out.size - 1] = 600 + (45000 - frameLen) // pad to 45ms frame period
            else out.removeAt(out.size - 1) // end on a mark
        }
        return IrSignal(40000, out.toIntArray())
    }

    /** Sharp (Aquos): 5-bit address + 8-bit command, LSB first; sent again with command bits inverted. */
    fun sharp(address: Int, command: Int): IrSignal {
        val b = Pulses()
        fun frame(cmd: Int, exp: Int, chk: Int) {
            b.bits(address.toLong(), 5, false, 320, 1680, 680)
            b.bits(cmd.toLong() and 0xFF, 8, false, 320, 1680, 680)
            b.bits(exp.toLong(), 1, false, 320, 1680, 680)
            b.bits(chk.toLong(), 1, false, 320, 1680, 680)
            b.mark(320)
        }
        frame(command, 1, 0)
        b.space(40000)
        frame(command.inv(), 0, 1)
        return b.build(38000)
    }

    /** Panasonic (Kaseikyo 48-bit): 16-bit manufacturer + 32-bit data, MSB first. */
    fun panasonic(data: Long, manufacturer: Int = 0x4004): IrSignal {
        val bytes = intArrayOf(
            manufacturer shr 8, manufacturer,
            (data shr 24).toInt(), (data shr 16).toInt(), (data shr 8).toInt(), data.toInt(),
        )
        return Pulses().mark(3456).space(1728)
            .bytes(bytes, true, 432, 1296, 432)
            .mark(432)
            .build(36700)
    }
}

/**
 * A set of codes for one kind of device. [testKey] is what "Scan" sends to
 * check whether this code set works with the user's device.
 */
class RemoteProfile(
    val id: String,
    val brand: String,
    val name: String,
    val testKey: Key,
    val keys: Map<Key, IrSignal>,
) {
    fun label(key: Key): String = when {
        key == Key.MENU && brand == "Roku" -> "Options"
        key == Key.SOURCE && brand == "LG" -> "Input"
        else -> defaultLabels.getValue(key)
    }

    companion object {
        private val defaultLabels = mapOf(
            Key.POWER to "Power", Key.VOL_UP to "Vol +", Key.VOL_DOWN to "Vol −", Key.MUTE to "Mute",
            Key.CH_UP to "Ch +", Key.CH_DOWN to "Ch −", Key.MENU to "Menu", Key.SOURCE to "Source",
            Key.HOME to "Home", Key.BACK to "Back", Key.UP to "Up", Key.DOWN to "Down",
            Key.LEFT to "Left", Key.RIGHT to "Right", Key.OK to "OK",
        )
    }
}

object Profiles {
    private fun samsungTv(): Map<Key, IrSignal> {
        val c = mapOf(
            Key.POWER to 0x02, Key.VOL_UP to 0x07, Key.VOL_DOWN to 0x0B, Key.MUTE to 0x0F,
            Key.CH_UP to 0x12, Key.CH_DOWN to 0x10, Key.MENU to 0x1A, Key.SOURCE to 0x01,
            Key.HOME to 0x79, Key.BACK to 0x58, Key.UP to 0x60, Key.DOWN to 0x61,
            Key.LEFT to 0x65, Key.RIGHT to 0x62, Key.OK to 0x68,
        )
        return c.mapValues { Ir.samsung(0x07, it.value) }
    }

    private fun lgTv(): Map<Key, IrSignal> {
        val c = mapOf(
            Key.POWER to 0x08, Key.VOL_UP to 0x02, Key.VOL_DOWN to 0x03, Key.MUTE to 0x09,
            Key.CH_UP to 0x00, Key.CH_DOWN to 0x01, Key.MENU to 0x43, Key.SOURCE to 0x0B,
            Key.HOME to 0x7C, Key.BACK to 0x28, Key.UP to 0x40, Key.DOWN to 0x41,
            Key.LEFT to 0x07, Key.RIGHT to 0x06, Key.OK to 0x44,
        )
        return c.mapValues { Ir.nec(0x04, it.value) }
    }

    /** Roku remotes (Roku TVs from TCL, Hisense, Insignia… and IR Roku players). Roku has no Source key. */
    private fun roku(): Map<Key, IrSignal> {
        val c = mapOf(
            Key.POWER to 0x17, Key.VOL_UP to 0x0F, Key.VOL_DOWN to 0x10, Key.MUTE to 0x20,
            Key.MENU to 0x61, Key.HOME to 0x03, Key.BACK to 0x66,
            Key.UP to 0x19, Key.DOWN to 0x33, Key.LEFT to 0x1E, Key.RIGHT to 0x2D, Key.OK to 0x2A,
        )
        return c.mapValues { Ir.nec(0xC7EA, it.value, extended = true) }
    }

    private fun sonyTv(): Map<Key, IrSignal> {
        val c = mapOf(
            Key.POWER to 21, Key.VOL_UP to 18, Key.VOL_DOWN to 19, Key.MUTE to 20,
            Key.CH_UP to 16, Key.CH_DOWN to 17, Key.MENU to 96, Key.SOURCE to 37,
            Key.UP to 116, Key.DOWN to 117, Key.LEFT to 52, Key.RIGHT to 51, Key.OK to 101,
        )
        return c.mapValues { Ir.sony12(1, it.value) }
    }

    /** Vizio uses the same NEC address as LG, with its own Input/Menu/navigation codes. */
    private fun vizioTv(): Map<Key, IrSignal> {
        val c = mapOf(
            Key.POWER to 0x08, Key.VOL_UP to 0x02, Key.VOL_DOWN to 0x03, Key.MUTE to 0x09,
            Key.CH_UP to 0x00, Key.CH_DOWN to 0x01, Key.MENU to 0x4F, Key.SOURCE to 0x2F,
            Key.BACK to 0x4A, Key.UP to 0x45, Key.DOWN to 0x46,
            Key.LEFT to 0x47, Key.RIGHT to 0x48, Key.OK to 0x44,
        )
        return c.mapValues { Ir.nec(0x04, it.value) }
    }

    private fun sharpTv(): Map<Key, IrSignal> {
        val c = mapOf(
            Key.POWER to 0x16, Key.VOL_UP to 0x14, Key.VOL_DOWN to 0x15, Key.MUTE to 0x17,
            Key.CH_UP to 0x11, Key.CH_DOWN to 0x12, Key.SOURCE to 0x13,
        )
        return c.mapValues { Ir.sharp(1, it.value) }
    }

    private fun panasonicTv(): Map<Key, IrSignal> {
        val c = mapOf(
            Key.POWER to 0x0100BCBDL, Key.VOL_UP to 0x01000405L, Key.VOL_DOWN to 0x01008485L,
            Key.MUTE to 0x01004C4DL, Key.CH_UP to 0x01002C2DL, Key.CH_DOWN to 0x0100ACADL,
            Key.SOURCE to 0x0100A0A1L, Key.MENU to 0x01004A4BL, Key.BACK to 0x01002B2AL,
            Key.UP to 0x01005253L, Key.DOWN to 0x0100D2D3L, Key.LEFT to 0x01007273L,
            Key.RIGHT to 0x0100F2F3L, Key.OK to 0x01009293L,
        )
        return c.mapValues { Ir.panasonic(it.value) }
    }

    val all: List<RemoteProfile> = listOf(
        RemoteProfile("samsung_tv", "Samsung", "Samsung TV", Key.POWER, samsungTv()),
        RemoteProfile("lg_tv", "LG", "LG TV", Key.POWER, lgTv()),
        RemoteProfile("roku_tv", "Roku", "Roku TV", Key.POWER, roku()),
        RemoteProfile("roku_player", "Roku", "Roku streaming player", Key.HOME, roku()),
        RemoteProfile("sony_tv", "Sony", "Sony TV", Key.POWER, sonyTv()),
        RemoteProfile("vizio_tv", "Vizio", "Vizio TV", Key.POWER, vizioTv()),
        RemoteProfile("sharp_tv", "Sharp", "Sharp Aquos TV", Key.POWER, sharpTv()),
        RemoteProfile("panasonic_tv", "Panasonic", "Panasonic TV", Key.POWER, panasonicTv()),
    )

    fun byId(id: String): RemoteProfile? = all.firstOrNull { it.id == id }
}
