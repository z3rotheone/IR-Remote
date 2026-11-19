package com.z3rotheone.blazer

/** A ready-to-send IR burst: carrier frequency (Hz) + on/off durations in microseconds. */
class IrSignal(val frequency: Int, val pattern: IntArray) {
    val durationMs: Long get() = pattern.sum() / 1000L
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

    val all: List<RemoteProfile> = listOf(
        RemoteProfile("samsung_tv", "Samsung", "Samsung TV", Key.POWER, samsungTv()),
        RemoteProfile("lg_tv", "LG", "LG TV", Key.POWER, lgTv()),
        RemoteProfile("roku_tv", "Roku", "Roku TV", Key.POWER, roku()),
        RemoteProfile("roku_player", "Roku", "Roku streaming player", Key.HOME, roku()),
        RemoteProfile("sony_tv", "Sony", "Sony TV", Key.POWER, sonyTv()),
    )

    val brands: List<String> = all.map { it.brand }.distinct()

    fun byId(id: String): RemoteProfile? = all.firstOrNull { it.id == id }
}
