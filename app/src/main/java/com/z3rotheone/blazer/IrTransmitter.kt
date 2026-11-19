package com.z3rotheone.blazer

import android.content.Context
import android.hardware.ConsumerIrManager
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/** Sends IR bursts through the phone's IR blaster on a background thread. */
class IrTransmitter(context: Context) {
    private val manager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    private val executor = Executors.newSingleThreadExecutor()
    private val pending = AtomicInteger(0)

    val available: Boolean = runCatching { manager?.hasIrEmitter() == true }.getOrDefault(false)

    /** [droppable] sends (held-button repeats) are skipped if the blaster is still busy. */
    fun send(signal: IrSignal, droppable: Boolean = false) {
        val ir = manager ?: return
        if (!available) return
        if (droppable && pending.get() > 0) return
        pending.incrementAndGet()
        executor.execute {
            try {
                ir.transmit(signal.frequency, signal.pattern)
            } catch (e: Exception) {
                Log.w("Blazer", "IR transmit failed", e)
            } finally {
                pending.decrementAndGet()
            }
        }
    }
}
