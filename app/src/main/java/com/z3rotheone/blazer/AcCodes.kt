package com.z3rotheone.blazer

enum class AcMode(val label: String) { COOL("Cool"), HEAT("Heat"), DRY("Dry"), FAN("Fan"), AUTO("Auto") }

enum class AcFan(val label: String) { AUTO("Auto"), LOW("Low"), MED("Med"), HIGH("High") }

/**
 * Air conditioners don't use button codes: every press sends the remote's
 * whole state (power, mode, temperature, fan). Temperature is in °C.
 */
data class AcState(
    val power: Boolean = false,
    val mode: AcMode = AcMode.COOL,
    val temp: Int = 24,
    val fan: AcFan = AcFan.AUTO,
)

class AcProfile(
    val id: String,
    val brand: String,
    val name: String,
    val minTemp: Int,
    val maxTemp: Int,
    private val encoder: (AcState) -> IrSignal,
) {
    fun encode(state: AcState): IrSignal = encoder(state.copy(temp = state.temp.coerceIn(minTemp, maxTemp)))

    /** Scan test: turn on, cool, 24°C, auto fan. */
    val testSignal: IrSignal by lazy { encode(AcState(power = true)) }
}

object AcIr {
    /** Gree (also Cooper & Hunter, Tosot, Soleus and many mini-splits): 8 bytes in two blocks. */
    fun gree(s: AcState): IrSignal {
        val mode = when (s.mode) {
            AcMode.AUTO -> 0; AcMode.COOL -> 1; AcMode.DRY -> 2; AcMode.FAN -> 3; AcMode.HEAT -> 4
        }
        val fan = when (s.fan) { AcFan.AUTO -> 0; AcFan.LOW -> 1; AcFan.MED -> 2; AcFan.HIGH -> 3 }
        val b = IntArray(8)
        b[0] = mode or ((if (s.power) 1 else 0) shl 3) or (fan shl 4)
        b[1] = (s.temp - 16) and 0x0F
        b[2] = 0x60 // light on, model A
        b[3] = 0x50
        b[5] = 0x20
        var sum = 10
        for (i in 0..3) sum += b[i] and 0x0F
        for (i in 4..6) sum += b[i] shr 4
        b[7] = (sum and 0x0F) shl 4

        return Pulses().mark(9000).space(4500)
            .bytes(b.copyOfRange(0, 4), false, 620, 1600, 540)
            .bits(0b010, 3, false, 620, 1600, 540)
            .mark(620).space(19980)
            .bytes(b.copyOfRange(4, 8), false, 620, 1600, 540)
            .mark(620)
            .build(38000)
    }

    private val coolixTemps = intArrayOf(
        0b0000, 0b0001, 0b0011, 0b0010, 0b0110, 0b0111, 0b0101,
        0b0100, 0b1100, 0b1101, 0b1001, 0b1000, 0b1010, 0b1011,
    ) // 17..30 °C

    /** Midea / "Coolix" family (Midea, Senville, Comfee, Pioneer and many others). */
    fun coolix(s: AcState): IrSignal {
        val code = if (!s.power) 0xB27BE0 else {
            val modeBits = when (s.mode) {
                AcMode.COOL -> 0b00; AcMode.DRY, AcMode.FAN -> 0b01; AcMode.AUTO -> 0b10; AcMode.HEAT -> 0b11
            }
            val tempBits = if (s.mode == AcMode.FAN) 0b1110 else coolixTemps[s.temp - 17]
            val fanBits = if (s.mode == AcMode.AUTO || s.mode == AcMode.DRY) 0b000 else when (s.fan) {
                AcFan.AUTO -> 0b101; AcFan.LOW -> 0b100; AcFan.MED -> 0b010; AcFan.HIGH -> 0b001
            }
            (0xB2 shl 16) or (((fanBits shl 5) or 0x1F) shl 8) or (tempBits shl 4) or (modeBits shl 2)
        }
        val p = Pulses()
        repeat(2) { n ->
            p.mark(4692).space(4416)
            for (shift in intArrayOf(16, 8, 0)) {
                val byte = (code shr shift) and 0xFF
                p.bits(byte.toLong(), 8, true, 552, 1656, 552)
                p.bits((byte xor 0xFF).toLong(), 8, true, 552, 1656, 552)
            }
            p.mark(552)
            if (n == 0) p.space(5244)
        }
        return p.build(38000)
    }

    /** LG air conditioners: 28 bits. Two timing variants exist across LG remotes. */
    fun lg(s: AcState, type2: Boolean): IrSignal {
        val code = if (!s.power) 0x88C0051 else {
            val mode = when (s.mode) {
                AcMode.COOL -> 0; AcMode.DRY -> 1; AcMode.FAN -> 2; AcMode.AUTO -> 3; AcMode.HEAT -> 4
            }
            val fan = when (s.fan) { AcFan.AUTO -> 5; AcFan.LOW -> 0; AcFan.MED -> 2; AcFan.HIGH -> 4 }
            val body = (0x88 shl 20) or (mode shl 12) or ((s.temp - 15) shl 8) or (fan shl 4)
            var sum = 0
            var v = body shr 4
            repeat(6) { sum += v and 0xF; v = v shr 4 }
            body or (sum and 0xF)
        }
        val p = Pulses()
        if (type2) p.mark(3200).space(9900) else p.mark(8500).space(4250)
        p.bits(code.toLong(), 28, true, if (type2) 480 else 550, 1600, 550)
        p.mark(if (type2) 480 else 550)
        return p.build(38000)
    }

    /** Mitsubishi Electric mini-splits: 18 bytes, sent twice. */
    fun mitsubishi(s: AcState): IrSignal {
        val b = IntArray(18)
        b[0] = 0x23; b[1] = 0xCB; b[2] = 0x26; b[3] = 0x01
        b[5] = if (s.power) 0x20 else 0x00
        val modeBits = when (s.mode) {
            AcMode.HEAT -> 0b001; AcMode.DRY -> 0b010; AcMode.COOL -> 0b011; AcMode.AUTO -> 0b100; AcMode.FAN -> 0b111
        }
        b[6] = modeBits shl 3
        b[7] = (s.temp - 16) and 0x0F
        b[8] = when (s.mode) {
            AcMode.COOL -> 0x36; AcMode.DRY -> 0x32; AcMode.FAN -> 0x37; else -> 0x30
        }
        b[9] = when (s.fan) { AcFan.AUTO -> 0x80; AcFan.LOW -> 0x01; AcFan.MED -> 0x03; AcFan.HIGH -> 0x05 }
        b[17] = b.copyOfRange(0, 17).sum() and 0xFF

        val p = Pulses()
        repeat(2) { n ->
            p.mark(3400).space(1750)
            p.bytes(b, false, 450, 1300, 420)
            p.mark(440)
            if (n == 0) p.space(17100)
        }
        return p.build(38000)
    }
}

object AcProfiles {
    val all: List<AcProfile> = listOf(
        AcProfile("ac_gree", "Gree", "Gree / Cooper & Hunter / Tosot", 16, 30, AcIr::gree),
        AcProfile("ac_midea", "Midea", "Midea / Senville / Comfee / Pioneer", 17, 30, AcIr::coolix),
        AcProfile("ac_mitsubishi", "Mitsubishi", "Mitsubishi Electric", 16, 31, AcIr::mitsubishi),
        AcProfile("ac_lg1", "LG", "LG air conditioner (type 1)", 16, 30) { AcIr.lg(it, type2 = false) },
        AcProfile("ac_lg2", "LG", "LG air conditioner (type 2)", 16, 30) { AcIr.lg(it, type2 = true) },
    )

    fun byId(id: String): AcProfile? = all.firstOrNull { it.id == id }
}

enum class Category(val title: String) { TV("TV & media"), AC("Air conditioner") }

/** One code set Scan can try. */
class ScanCandidate(
    val id: String,
    val brand: String,
    val name: String,
    val category: Category,
    val testLabel: String,
    val testSignal: IrSignal,
)

object Catalog {
    val candidates: List<ScanCandidate> =
        Profiles.all.map { p ->
            ScanCandidate(
                p.id, p.brand, p.name, Category.TV,
                if (p.testKey == Key.POWER) "Power" else "Home",
                p.keys.getValue(p.testKey),
            )
        } + AcProfiles.all.map { a ->
            ScanCandidate(a.id, a.brand, a.name, Category.AC, "Power on (Cool 24°C)", a.testSignal)
        }

    fun brands(category: Category): List<String> =
        candidates.filter { it.category == category }.map { it.brand }.distinct()

    fun isKnown(id: String) = candidates.any { it.id == id }

    fun byId(id: String) = candidates.firstOrNull { it.id == id }
}
