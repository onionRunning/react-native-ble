package com.ble.utils

import android.bluetooth.BluetoothAdapter.*
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile
import androidx.annotation.IntDef
import com.ble.utils.AdapterState.Companion.STATE_BLE_ON
import com.ble.utils.AdapterState.Companion.STATE_BLE_TURNING_OFF
import com.ble.utils.AdapterState.Companion.STATE_BLE_TURNING_ON
import com.ble.utils.ConnectReplyType.Companion.CONNECT_ERROR_BLE_SYSTEM_REASON
import com.ble.utils.ConnectReplyType.Companion.CONNECT_ERROR_UNKNOWN
import com.ble.utils.ConnectReplyType.Companion.CONNECT_SUCCESS
import com.ble.utils.ScanState.Companion.STATE_IDLE
import com.ble.utils.ScanState.Companion.STATE_SCANNED
import com.ble.utils.ScanState.Companion.STATE_SCANNING
import com.ble.utils.ScanState.Companion.STATE_STOP
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*


@Target(CLASS, TYPE_PARAMETER, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER, TYPE, TYPEALIAS)
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
        internal const val STATE_BLE_TURNING_ON = 14
        internal const val STATE_BLE_ON = 15
        internal const val STATE_BLE_TURNING_OFF = 16

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


@Target(CLASS, TYPE_PARAMETER, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER, TYPE, TYPEALIAS)
@Retention(SOURCE)
@IntDef(STATE_IDLE, STATE_SCANNING, STATE_SCANNED, STATE_STOP)
annotation class ScanState {
    companion object {
        internal const val STATE_IDLE = 0
        internal const val STATE_SCANNING = 1
        internal const val STATE_SCANNED = 2
        internal const val STATE_STOP = 3
    }
}


@Target(CLASS, TYPE_PARAMETER, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER, TYPE, TYPEALIAS)
@Retention(SOURCE)
@IntDef(
    BluetoothProfile.STATE_CONNECTING,
    BluetoothProfile.STATE_CONNECTED,
    BluetoothProfile.STATE_DISCONNECTING,
    BluetoothProfile.STATE_DISCONNECTED,
)
annotation class BtProfileState {
    companion object {
        fun connectStateName(@BtProfileState state: Int?): String = when (state) {
            BluetoothGatt.STATE_DISCONNECTED -> "STATE_DISCONNECTED"
            BluetoothGatt.STATE_CONNECTING -> "STATE_CONNECTING"
            BluetoothGatt.STATE_CONNECTED -> "STATE_CONNECTED"
            BluetoothGatt.STATE_DISCONNECTING -> "STATE_DISCONNECTING"
            else -> "STATE_UNKNOWN"
        }
    }
}

@Target(CLASS, TYPE_PARAMETER, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER, TYPE, TYPEALIAS)
@Retention(SOURCE)
@IntDef(
    CONNECT_SUCCESS,
    CONNECT_ERROR_BLE_SYSTEM_REASON,
    CONNECT_ERROR_UNKNOWN
)
annotation class ConnectReplyType {

    companion object {
        internal const val CONNECT_SUCCESS = 5
        internal const val CONNECT_ERROR_BLE_SYSTEM_REASON = 6
        internal const val CONNECT_ERROR_UNKNOWN = 7

        fun connectReplyMessage(@ConnectReplyType type: Int): String {
            return when (type) {
                CONNECT_SUCCESS -> "CONNECT_SUCCESS"
                CONNECT_ERROR_BLE_SYSTEM_REASON -> "CONNECT_ERROR_BLE_SYSTEM_REASON"
                else -> "CONNECT_ERROR_UNKNOWN"
            }
        }
    }
}


