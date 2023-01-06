package com.ble

import android.bluetooth.*
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.ble.model.ScanReply
import java.util.concurrent.CopyOnWriteArrayList

import com.ble.utils.ScanState.Companion.STATE_IDLE
import com.ble.utils.ScanState.Companion.STATE_SCANNED
import com.ble.utils.ScanState
import com.ble.utils.ScanState.Companion.STATE_SCANNING
import com.ble.utils.*
import android.bluetooth.le.ScanCallback
import androidx.annotation.RequiresApi
import com.ble.model.DEFAULT_SCAN_TIMEOUT

@Suppress("ObsoleteSdkInt", "MissingPermission")
class BleManager private constructor(context: Context): BluetoothGattCallback(), OnLeScanListener {

  private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
  private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
  private val BluetoothAdapter.isDisable: Boolean
      get() = !isEnabled
  // 多设备管理
  private var scanRely: ScanReply? = null

  private var scanStateChanged: ScanStateChanged? = null
  @ScanState
  private var scanState: Int = STATE_IDLE
    set(value) { field = value.apply { scanStateChanged?.invoke(this) } }


  private val mainHandler = applicationContext?.let { Handler(it.mainLooper) }
  // Android 5.0及以上,使用新的扫描方法和回调
  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  private val scanCallback: ScanCallback = SimpleScanCallback(this@BleManager)
  // Android 5.0以下(不含),使用旧的扫描方法和回调
  private val scanCallbackBelow21 = SimpleScanCallbackBelow21(this@BleManager)

  @Suppress("DEPRECATION")
  private val cancelTask = Runnable {
    Log.d(TAG, "stop BLE scan")
    scanState = STATE_SCANNED
    scanRely?.onReply(false, null)
    if (!someTips(false)) return@Runnable
    when {
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
      else -> bluetoothAdapter?.stopLeScan(scanCallbackBelow21)
    }
    scanState = STATE_IDLE
  }

  @Suppress("DEPRECATION")
  private val scanTask = Runnable {
    Log.d(TAG, "start BLE scan")
    scanState = STATE_SCANNING
    if (!someTips(true)) return@Runnable
    when {
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> bluetoothAdapter?.bluetoothLeScanner?.startScan(scanCallback)
      else -> bluetoothAdapter?.startLeScan(scanCallbackBelow21)
    }
  }
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
    fun install(context: Context) {
      applicationContext = context.applicationContext
    }
  }


  // 是否支持
  fun isSupport(): Boolean = bluetoothAdapter != null

  // 是否可用
  fun isEnable(): Boolean = bluetoothAdapter?.isEnabled ?: false

  fun enable(): Boolean = if (isEnable()) true else bluetoothAdapter?.enable() ?: false

  private fun someTips(show: Boolean): Boolean {
    val toast = when {
      !isSupport() ->
        Toast.makeText(applicationContext, "该设备不支持蓝牙.", Toast.LENGTH_LONG)
      !isEnable() && !enable() ->
        Toast.makeText(applicationContext, "无法开启蓝牙.", Toast.LENGTH_LONG)
      !isBlePermissionGranted(applicationContext!!) ->
        Toast.makeText(applicationContext, "BLE定位权限未授予.", Toast.LENGTH_LONG)
      else -> null
    }
    mainHandler?.post { if (show) toast?.show() }
    return toast == null
  }

  override fun onLeScanResult(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray?) {

  }

  fun scan(reply: ScanReply): Boolean {
    if (scanState != STATE_IDLE) return false
    mainHandler?.removeCallbacksAndMessages(null)
    scanRely = reply
    mainHandler?.post(scanTask)
    cancel(DEFAULT_SCAN_TIMEOUT)
    return true
  }

  private fun cancel(delay: Long = 0) {
    mainHandler?.postDelayed(cancelTask, delay)
  }
}


typealias ScanStateChanged = (state: @ScanState Int) -> Unit
