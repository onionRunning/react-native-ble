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


class BleModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String {
    return NAME
  }

  // Example method
  // See https://reactnative.dev/docs/native-modules-android
  @ReactMethod
  fun multiply(a: Double, b: Double, promise: Promise) {
    promise.resolve(a * b)
  }

  companion object {
    const val NAME = "Ble"
  }

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
              description?.let { putString("data", it) }
          }
      )
  }
}


internal const val CODE_FAILURE_NONSUPPORT = 0
internal const val CODE_FAILURE_BLE_NOT_OPEN = 2
internal const val CODE_FAILURE_PERMISSIONS_DENIED = 1
internal const val CODE_SUCCESS = 200
internal const val CODE = "code"
