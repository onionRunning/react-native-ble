package com.ble

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import android.util.Log

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.*
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*
import com.ble.BleManager
import com.ble.utils.*
import com.facebook.react.bridge.Arguments
import com.hjq.permissions.OnPermissionCallback


@Suppress("Unused")
class BleModule constructor(
    private val reactContext: ReactApplicationContext
): ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String {
    return NAME
  }

  @ReactMethod
  fun multiply(a: Double, b: Double, promise: Promise) {
    promise.resolve(a * b)
  }

  companion object {
    const val NAME = "Ble"
  }

  @ReactMethod
  fun requestBlePermission(promise: Promise) {
    reactContext.currentActivity?.let {
      ensureBlePermissionGranted(it, object: OnPermissionCallback {
        override fun onGranted(permissions: MutableList<String>, all: Boolean) {
          if (isBlePermissionGranted(it)) {
            promise.resolve(true)
            return
          }
          promise.resolve(false)
        }
        override fun onDenied(permissions: MutableList<String>, never: Boolean) {
          promise.resolve(true)
        }
      })
    } ?: promise.reject(Exception("Activity is null"))
  }

  // 判断当前连接是否具有权限!
  @ReactMethod
  fun isAllOk(promise: Promise) {
      Log.e("repeat_error", "rn --> isAllOk")
      val (code, description) = when {
          !BleManager.getInstance().isSupport() ->
              CODE_FAILURE_NONSUPPORT to "Android Device is not support BLE."
          !BleManager.getInstance().isEnable() ->
              CODE_FAILURE_BLE_NOT_OPEN to "BLE is not open"
          !isBlePermissionGranted(reactContext) ->
              CODE_FAILURE_PERMISSIONS_DENIED to "BLE permissions is not granted."
          else -> CODE_SUCCESS to null
      }
      promise.resolve(
          Arguments.createMap().apply {
              putInt(CODE, code)
              description?.let { putString("data",  it) }
          }
      )
  }

//  扫描设备
  @ReactMethod
  fun scanDevice() {
    Log.e("repeat_error", "rn --> scanDevice")
    BleManager.getInstance().scan { flag, p ->
      EventEmitter.sendEvent(
        EVENT_SCAN_RESULT,
        Arguments.createMap().apply {
          putInt(CODE, if (flag) CODE_SUCCESS else CODE_FAILURE_TIMEOUT)
          if (flag) putMap(DATA, p?.first?.toMap()) else putString(DATA, "Scan Timeout")
        }
      )
    }
  }
}


internal const val CODE_FAILURE_NONSUPPORT = 0
internal const val CODE_FAILURE_BLE_NOT_OPEN = 2
internal const val CODE_FAILURE_PERMISSIONS_DENIED = 1
internal const val CODE_SUCCESS = 200
internal const val CODE = "code"
internal const val EVENT_SCAN_RESULT = "EVENT_SCAN_RESULT"
internal const val CODE_FAILURE_TIMEOUT = 6
internal const val DATA = "data"
