package com.ble.model

import android.bluetooth.*
import com.ble.utils.BtProfileState
import com.ble.utils.BtProfileState.Companion.connectStateName

data class ConnectDeviceInfo(
    val device: BluetoothDevice,
    @BtProfileState
    var connectState: Int,
    val adapter: BluetoothAdapter? = null,
    val gatt: BluetoothGatt? = null,
    val service: BluetoothGattService? = null,
    val characteristics: MutableList<BluetoothGattCharacteristic> = mutableListOf()
) {
    @Suppress("MissingPermission")
    fun simpleString() = "ConnectDeviceInfo{" +
            "\"deviceName\":${device.name}, " +
            "\"deviceMac\":${device.address}, " +
            "\"connectState\":${connectStateName(connectState)}" +
            "}"
}
