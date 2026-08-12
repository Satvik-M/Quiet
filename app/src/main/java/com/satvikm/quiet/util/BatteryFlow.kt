package com.satvikm.quiet.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private fun batteryPercentFrom(intent: Intent?): Int {
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    return if (level >= 0 && scale > 0) (level * 100) / scale else 0
}

/** Emits the current battery percentage, and again on every level/charging change. */
fun batteryLevelFlow(context: Context): Flow<Int> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            trySend(batteryPercentFrom(intent))
        }
    }
    val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    val sticky = ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    trySend(batteryPercentFrom(sticky))

    awaitClose { context.unregisterReceiver(receiver) }
}
