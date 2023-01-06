package com.minbay.pixelartboard.model

import android.bluetooth.BluetoothDevice
import android.os.Parcelable
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import com.ble.utils.toHexString
import kotlinx.parcelize.Parcelize

@Parcelize
data class AdvDevice(
    val device: BluetoothDevice,
    var rssi: Int? = null,
    var scanRecord: ByteArray?
): Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AdvDevice
        if (device != other.device) return false
        if (!scanRecord.contentEquals(other.scanRecord)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = device.hashCode()
        result = 31 * result + scanRecord.contentHashCode()
        return result
    }

    fun toMap(): WritableMap = Arguments.createMap().apply {
        putString("mac", device.address)
        rssi?.let { putInt("rssi", it) }
        putString("scanRecord", wrapSerializeNumber(scanRecord))
    }

    private fun wrapSerializeNumber(scanRecord: ByteArray?): String {
        return scanRecord.toHexString().let { l ->
            val tempList = l.split(" ")
            if (tempList.size >= 15 && tempList[0] == "0E" && tempList[1] == "FF") {
                tempList.subList(2, 15).map { s ->
                    s.toInt(16).toChar()
                }.joinToString("")
            } else l
        }
    }

    override fun toString(): String =
        "AdvDevice[device:$device, RSSI:$rssi, scanRecord:${wrapSerializeNumber(scanRecord)}]"

}
