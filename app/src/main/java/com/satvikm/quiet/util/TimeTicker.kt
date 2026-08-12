package com.satvikm.quiet.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.ZonedDateTime

/**
 * Emits the current time on every minute tick, plus whenever the clock or
 * timezone changes, without polling. ACTION_TIME_TICK is a protected
 * broadcast that only a runtime-registered receiver can observe.
 */
fun currentTimeFlow(context: Context): Flow<ZonedDateTime> = callbackFlow {
    trySend(ZonedDateTime.now())

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            trySend(ZonedDateTime.now())
        }
    }
    val filter = IntentFilter().apply {
        addAction(Intent.ACTION_TIME_TICK)
        addAction(Intent.ACTION_TIME_CHANGED)
        addAction(Intent.ACTION_TIMEZONE_CHANGED)
        addAction(Intent.ACTION_DATE_CHANGED)
    }
    ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

    awaitClose { context.unregisterReceiver(receiver) }
}
