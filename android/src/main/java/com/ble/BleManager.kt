package com.ble

import android.bluetooth.*
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.ble.model.ScanReply
import com.ble.model.*
import com.ble.utils.*
import java.util.concurrent.CopyOnWriteArrayList

import com.ble.utils.ScanState.Companion.STATE_IDLE
import com.ble.utils.ScanState.Companion.STATE_SCANNED
import com.ble.utils.ScanState
import com.ble.utils.ScanState.Companion.STATE_SCANNING
import android.bluetooth.le.ScanCallback
import android.content.Intent
import androidx.annotation.RequiresApi
import com.ble.model.DEFAULT_SCAN_TIMEOUT
import com.ble.store.InfoDatastore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import com.ble.utils.BtProfileState.Companion.connectStateName
import com.ble.utils.ConnectReplyType.Companion.CONNECT_ERROR_BLE_SYSTEM_REASON
import com.ble.utils.ConnectReplyType.Companion.CONNECT_ERROR_UNKNOWN
import com.ble.utils.ConnectReplyType.Companion.CONNECT_SUCCESS
import com.ble.utils.ConnectReplyType.Companion.connectReplyMessage
import kotlinx.coroutines.MainScope


@Suppress("ObsoleteSdkInt", "MissingPermission")
class BleManager private constructor(context: Context): BluetoothGattCallback(), OnLeScanListener {

  private val bluetoothManager: BluetoothManager =
    context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
  private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
  private val BluetoothAdapter.isDisable: Boolean
    get() = !isEnabled
  // 多设备管理
  private val connectionMap: LimitHashMap<String, ConnectDeviceInfo> =
    limitMapOf(MAX_DEVICES_COUNT)

  private var scanRely: ScanReply? = null
  private var scanStateChanged: ScanStateChanged? = null
  @ScanState
  private var scanState: Int = STATE_IDLE
    set(value) { field = value.apply { scanStateChanged?.invoke(this) } }

  private var connectReply: ConnectReply? = null

  private val scannedBleDeviceList: CopyOnWriteArrayList<AdvDevice> = CopyOnWriteArrayList()

  private var characterReply: IReply? = null

  private val mainHandler = Handler(context.mainLooper)

  // Android 5.0及以上,使用新的扫描方法和回调
  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  private val scanCallback: ScanCallback = SimpleScanCallback(this@BleManager)
  // Android 5.0以下(不含),使用旧的扫描方法和回调
  private val scanCallbackBelow21 = SimpleScanCallbackBelow21(this@BleManager)

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

  private var isBleSwitchMonitorRegister: Boolean = false
  private val bleSwitchMonitor = BleSwitchMonitor {

  }
  init {
    val intentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
    context.registerReceiver(bleSwitchMonitor, intentFilter)
    isBleSwitchMonitorRegister = true
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

  fun isSupport(): Boolean = bluetoothAdapter != null

  fun isEnable(): Boolean = bluetoothAdapter?.isEnabled ?: false

  fun enable(): Boolean = if (isEnable()) true else bluetoothAdapter?.enable() ?: false

  fun openBle(): Intent? = bluetoothAdapter?.takeIf { it.isDisable }?.let {
    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
  }

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
    mainHandler.post { if (show) toast?.show() }
    return toast == null
  }

  fun scan(reply: ScanReply): Boolean {
    if (scanState != STATE_IDLE) return false
    mainHandler.removeCallbacksAndMessages(null)
    scanRely = reply
    mainHandler.post(scanTask)
    cancel(DEFAULT_SCAN_TIMEOUT)
    return true
  }

  private fun cancel(delay: Long = 0) {
    mainHandler.postDelayed(cancelTask, delay)
  }

  fun findBluetoothDevice(mac: String): AdvDevice? {
    val lowerCaseMAC = mac.lowercase(Locale.US)
    if (!macPattern.matches(lowerCaseMAC)) return null
    return scannedBleDeviceList.find { it.device.address == mac }
  }

  fun connect(sn: String, advDevice: AdvDevice, reply: ConnectReply? = null) {
    cancel()
    if (!someTips(true)) return
    synchronized(stateLock) {
      val cdi = connectionMap[sn]
      val connectState = cdi?.let { bluetoothManager.getConnectionState(it.device,
        BluetoothGatt.GATT
      ) }
      Log.v(TAG, "synchronized:connectState=${connectState?.let { connectStateName(it) }}")
      when {
        cdi == null || connectState == BluetoothGatt.STATE_DISCONNECTED
          || connectState == BluetoothGatt.STATE_DISCONNECTING -> {
          connectReply = reply
          (advDevice.device.connectGatt(
            applicationContext,
            false,
            this@BleManager
          )?.let {
            connectionMap[sn] = ConnectDeviceInfo(
              device = advDevice.device,
              connectState = BluetoothGatt.STATE_CONNECTING,
              adapter = bluetoothAdapter,
              gatt = it
            ).apply {
              CoroutineScope(Dispatchers.IO).launch { InfoDatastore.addSN(0, sn) }
              Log.d(TAG, "connect success:$this")
            }
            CONNECT_SUCCESS
          } ?: disconnect(sn).let {
            CONNECT_ERROR_BLE_SYSTEM_REASON
          }).apply {
            Log.e(TAG, "wtf:$this")
          }
        }
        connectState == BluetoothGatt.STATE_CONNECTING || connectState == BluetoothGatt.STATE_CONNECTED ->
          CONNECT_SUCCESS
        else -> CONNECT_ERROR_UNKNOWN
      }.takeIf {
        Log.w(TAG, " -------> $it")
        it != CONNECT_SUCCESS
      }?.let {
        Log.w(TAG, "connect failed($it):${connectReplyMessage(it)}")
        connectReply?.onReply(it)
      }
    }
  }

  fun listenBleSwitcherState(listener: BleSwitchListener? = null) {
    if (isBleSwitchMonitorRegister) applicationContext?.unregisterReceiver(bleSwitchMonitor)
    bleSwitchMonitor.listener = listener
    val intentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
    applicationContext?.registerReceiver(bleSwitchMonitor, intentFilter)
    isBleSwitchMonitorRegister = true
  }

  fun disconnect(sn: String?, callback: ((Int) -> Unit)? = null) {
    synchronized(stateLock) {
      (sn.takeUnless { it.isNullOrBlank() }?.let { s ->
        val cdi = connectionMap[s] ?: return@let DISCONNECT_ERROR_CONNECTION_NOT_FOUND
        var connectState = bluetoothManager.getConnectionState(cdi.device, BluetoothGatt.GATT)
        when {
          connectState == BluetoothGatt.STATE_DISCONNECTED || connectState == BluetoothGatt.STATE_DISCONNECTING ->
            DISCONNECT_SUCCESS
          cdi.gatt == null -> DISCONNECT_ERROR_CONNECTION_NOT_FOUND
          else -> {
            cdi.gatt.disconnect()
            DISCONNECT_SUCCESS
          }
        }.apply {
          connectState = bluetoothManager.getConnectionState(cdi.device, BluetoothGatt.GATT)
          connectionMap[s] = cdi.copy(connectState = connectState)
        }

      } ?: DISCONNECT_ERROR_SN_NULL_OR_EMPTY).let {
        callback?.invoke(it)
      }
    }
  }

  fun getConnectStateBySn(sn: String) = connectionMap[sn]?.let {
    it to bluetoothManager.getConnectionState(it.device, BluetoothGatt.GATT)
  } ?: null to null

  override fun onLeScanResult(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray?) {
    val (advDevice, type) = scannedBleDeviceList.find { device.address == it.device.address }?.let { ad ->
      val index = scannedBleDeviceList.indexOf(ad)
      if (index !in scannedBleDeviceList.indices) return
      scannedBleDeviceList[index].rssi = rssi
      scannedBleDeviceList[index].scanRecord = scanRecord
      scannedBleDeviceList[index] to DEVICE_UPDATE
    } ?: run {
      val advDevice = AdvDevice(device = device, rssi = rssi, scanRecord = scanRecord)
      scannedBleDeviceList.add(advDevice)
      advDevice to DEVICE_ADD
    }
    scanRely?.onReply(true, advDevice to type)
  }

  private fun findConnectionDeviceInfo(
    gatt: BluetoothGatt?
  ): Map.Entry<String, ConnectDeviceInfo>? {
    gatt ?: return null
    return connectionMap.find { cdi -> cdi.device.address == gatt.device.address }
  }

  override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
    super.onConnectionStateChange(gatt, status, newState)
    Log.d(
      TAG,
      "onConnectionStateChange:status=${gattStatusName(status)}," +
        " connect state:${connectStateName(newState)}"
    )
    val (deviceName, mac) = gatt?.device?.name to gatt?.device?.address
    findConnectionDeviceInfo(gatt)?.let { (sn, cdi) ->
      connectionMap[sn] = cdi.copy(connectState = newState)
      if (status != BluetoothGatt.GATT_SUCCESS) { disconnect(sn); return }
      if (newState == BluetoothGatt.STATE_CONNECTED) gatt?.discoverServices()
    } ?: throw Exception("onConnectionStateChange:cannot found Device:$deviceName($mac)")
  }

  override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
    super.onServicesDiscovered(gatt, status)
    Log.v(TAG, "onServicesDiscovered:status=${gattStatusName(status)}")
    val (deviceName, mac) = gatt?.device?.name to gatt?.device?.address
    findConnectionDeviceInfo(gatt)?.let { (sn, cdi) ->
      if (status != BluetoothGatt.GATT_SUCCESS) { disconnect(sn); return }
      gatt.enableNotification()
      connectionMap[sn] = cdi.copy(service = gatt?.getService(SERVICE))
      connectReply?.onReply(CONNECT_SUCCESS)
    } ?: throw Exception("onServicesDiscovered:cannot found Device:$deviceName($mac)")
  }

  /*** 使得指定的Descriptor可被监听*/
  private fun BluetoothGatt?.enableNotification() {
    this?.getService(SERVICE)?.characteristics?.find { c ->
      c.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
        || c.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
        && TX_CHARACTERISTIC == c.uuid
    }?.let { c ->
      setCharacteristicNotification(c, true)
    }
  }

  private var lastResponse: String? = null
  override fun onCharacteristicChanged(
    gatt: BluetoothGatt?,
    characteristic: BluetoothGattCharacteristic?
  ) {
    super.onCharacteristicChanged(gatt, characteristic)
    val hexValue = characteristic?.value.toHexString()
    Log.w(
      "filter_$TAG",
      "onCharacteristicChanged:$TX_CHARACTERISTIC_ID, value=$hexValue"
    )
    findConnectionDeviceInfo(gatt)?.let { (sn, cdi) ->
      characteristic?.let { c ->
        val newCharacteristics = cdi.characteristics.apply { add(c) }
        connectionMap[sn] = cdi.copy(characteristics = newCharacteristics)
        // 重复响应已处理 不需要onReply
//        if (lastResponse == hexValue) {
//          Log.d(TAG, "same reply deprecated.")
//          return
//        }
        lastResponse = hexValue
        Log.v("filter_$TAG", "reply ===>")
        characterReply?.onReply(c.uuid, hexValue)
      }
    }
  }


  override fun onCharacteristicWrite(
    gatt: BluetoothGatt?,
    characteristic: BluetoothGattCharacteristic?,
    status: Int
  ) {
    super.onCharacteristicWrite(gatt, characteristic, status)
    characteristic ?: return
    if (characteristic.service.uuid != SERVICE) return
    val value = characteristic.value
    val hexValue = value.toHexString()
    val statusName = gattStatusName(status)
    Log.i(
      "filter_$TAG",
      "onCharacteristicWrite($statusName):${characteristic.uuid}, $hexValue"
    )
  }

  private suspend fun getSNAndCheckConnectHandle() = runCatching {
    val sn = InfoDatastore.getCurrentSN()
      ?: throw Exception("write:Cannot get sn from Store")
    val cdi = connectionMap[sn] ?: throw Exception("write:Cannot found connection handle")
    val gatt = cdi.gatt ?: throw Exception("write:Cannot found gatt handle")
    val service = (cdi.service ?: gatt.getService(SERVICE).also {
      connectionMap[sn] = cdi.copy(service = it)
    }) ?: throw Exception("write:Cannot found service handle")
    gatt to service
  }.getOrThrow()

  fun write(data: ByteArray, reply: IReply? = null) {
    MainScope().launch {
      getSNAndCheckConnectHandle().let { (gatt, service) ->
        findWritableCharacteristic(service, RX_CHARACTERISTIC)?.let { c ->
          characterReply = reply
          c.value = data
          c.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
          val writeStatus = gatt.writeCharacteristic(c)
          val commandString = data.toHexString()
          if (writeStatus) {
            Log.d(
              "filter_$TAG",
              "write command succeed:$commandString"
            )
          } else {
            Log.e(
              "filter_$TAG",
              "write command failed:$commandString"
            )
          }
        }
      }
    }
  }

  private fun findWritableCharacteristic(
    service: BluetoothGattService,
    characteristicUUID: UUID,
    writeProperty: Int = BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
  ): BluetoothGattCharacteristic? {
    return service.characteristics?.find { c ->
      c.properties and writeProperty != 0 && characteristicUUID == c.uuid
    }
  }

  override fun onCharacteristicRead(
    gatt: BluetoothGatt?,
    characteristic: BluetoothGattCharacteristic?,
    status: Int
  ) {
    super.onCharacteristicRead(gatt, characteristic, status)
    characteristic ?: return
    if (characteristic.service.uuid != SERVICE) return
    val value = characteristic.value
    val hexValue = value.toHexString()
    val statusName = gattStatusName(status)
    Log.w(TAG, "onCharacteristicRead($statusName):${characteristic.uuid}, value=$hexValue")
  }

  fun read() {
    MainScope().launch {
      getSNAndCheckConnectHandle().let { (gatt, service) ->
        service.getCharacteristic(TX_CHARACTERISTIC)?.let { c ->
          gatt.readCharacteristic(c)
        }
      }
    }
  }

  override fun onDescriptorRead(
    gatt: BluetoothGatt?,
    descriptor: BluetoothGattDescriptor?,
    status: Int
  ) {
    super.onDescriptorRead(gatt, descriptor, status)
    Log.w(TAG, "onDescriptorRead:${descriptor?.value.toHexString()}")
  }

  override fun onDescriptorWrite(
    gatt: BluetoothGatt?,
    descriptor: BluetoothGattDescriptor?,
    status: Int
  ) {
    super.onDescriptorWrite(gatt, descriptor, status)
    Log.w(TAG, "onDescriptorWrite:${descriptor?.value.toHexString()}")
  }

  private fun gattStatusName(status: Int): String = when (status) {
    BluetoothGatt.GATT_SUCCESS -> "GATT_SUCCESS"
    BluetoothGatt.GATT_FAILURE -> "GATT_FAILURE"
    else -> "OTHER STATUS"
  }
}

typealias ScanStateChanged = (state: @ScanState Int) -> Unit
