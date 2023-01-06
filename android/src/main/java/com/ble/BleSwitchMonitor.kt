package com.ble

import android.bluetooth.BluetoothAdapter.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.IntDef
import com.ble.AdapterState.Companion.adapterStateName
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

class BleSwitchMonitor(
    var listener: BleSwitchListener? = null,
    private val internalBlock: ((Int) -> Unit)? = null
): BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (action != ACTION_STATE_CHANGED) return
        val state = intent.getIntExtra(EXTRA_STATE, 0)
        if (adapterStateName(state) == "UNKNOWN_ADAPTER_STATE") return
        internalBlock?.invoke(state)
        listener?.switchState(state)
    }
}

fun interface BleSwitchListener {
    fun switchState(@AdapterState state: Int)
}

internal const val STATE_BLE_TURNING_ON = 14
internal const val STATE_BLE_ON = 15
internal const val STATE_BLE_TURNING_OFF = 16
@Target(TYPE, VALUE_PARAMETER, PROPERTY, FIELD, LOCAL_VARIABLE)
@Retention(SOURCE)
@IntDef(
    STATE_OFF,
    STATE_TURNING_ON,
    STATE_ON,
    STATE_TURNING_OFF,
    STATE_BLE_TURNING_ON,
    STATE_BLE_ON,
    STATE_BLE_TURNING_OFF
)
annotation class AdapterState {
    companion object {
        fun adapterStateName(state: Int): String = when (state) {
            STATE_OFF -> "STATE_OFF"
            STATE_TURNING_ON -> "STATE_TURNING_ON"
            STATE_ON -> "STATE_ON"
            STATE_TURNING_OFF -> "STATE_TURNING_OFF"
            STATE_BLE_TURNING_ON -> "STATE_BLE_TURNING_ON"
            STATE_BLE_ON -> "STATE_BLE_ON"
            STATE_BLE_TURNING_OFF -> "STATE_BLE_TURNING_OFF"
            else -> "UNKNOWN_ADAPTER_STATE"
        }
    }
}
