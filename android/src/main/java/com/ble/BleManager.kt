package com.ble

import android.bluetooth.*
import android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED
import android.bluetooth.BluetoothGatt.*
import android.bluetooth.BluetoothGattCharacteristic.*
import android.bluetooth.le.ScanCallback
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Handler
import android.util.Log
import android.widget.Toast

@Suppress("ObsoleteSdkInt", "MissingPermission")
class BleManager private constructor(context: Context): BluetoothGattCallback() {
  private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
  private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
  private val BluetoothAdapter.isDisable: Boolean
      get() = !isEnabled
  // 多设备管理

  companion object {
    private const val TAG = "BleManager"

    private const val MAX_DEVICES_COUNT = 5
    private const val DEVICE_ADD = 0
    private const val DEVICE_UPDATE = 1

    private val stateLock = Any()
    private const val PATTERN_FORMAT = "([a-z0-9]{2}:){5}[a-z0-9]{2}"
    private val macPattern = PATTERN_FORMAT.toRegex()

    @Volatile
    private var instance: BleManager? = null

    private var applicationContext: Context? = null

    fun install(context: Context) {
      applicationContext = context.applicationContext
    }

    fun getInstance(): BleManager {
      return instance ?: synchronized(BleManager::class.java) {
        instance ?: run {
          applicationContext?.let { ctx ->
            instance = BleManager(ctx)
            return instance!!
          } ?: throw Exception("$TAG haven't been init context!" +
            "please ensure invoke $TAG.install(Context) in Application.")
        }
      }
    }
  }

  // 是否支持
  fun isSupport(): Boolean = bluetoothAdapter != null

  // 是否可用
  fun isEnable(): Boolean = bluetoothAdapter?.isEnabled ?: false


}
