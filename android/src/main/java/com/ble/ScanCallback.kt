package com.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Build
import androidx.annotation.RequiresApi

class SimpleScanCallbackBelow21(
    private val onLeScanListener: OnLeScanListener? = null
): BluetoothAdapter.LeScanCallback {

    override fun onLeScan(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?) {
        device ?: return
        onLeScanListener?.onLeScanResult(device, rssi, scanRecord)
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class SimpleScanCallback(
    private val onLeScanListener: OnLeScanListener? = null
): ScanCallback() {

    override fun onScanResult(callbackType: Int, result: ScanResult?) {
        super.onScanResult(callbackType, result)
        result?.let { onLeScanListener?.onLeScanResult(it.device, it.rssi, it.scanRecord?.bytes) }
    }

}

interface OnLeScanListener {
    fun onLeScanResult(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray?)
}
