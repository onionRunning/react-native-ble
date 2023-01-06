package com.ble

import com.facebook.react.ReactInstanceManager
import com.facebook.react.ReactNativeHost
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.uimanager.events.RCTModernEventEmitter
import com.ble.utils.RNNavigationException

/**
 * Boolean -> Bool
 * Integer -> Number
 * Double -> Number
 * Float -> Number
 * String -> String
 * Callback -> function
 * ReadableMap -> Object
 * ReadableArray -> Array
 */
object EventEmitter {

    private var reactNativeHost: ReactNativeHost? = null

    @JvmStatic
    fun requireReactNativeHost(): ReactNativeHost =
        reactNativeHost ?: throw RNNavigationException("must call NavigationManager#install first")

    private val reactInstanceManager: ReactInstanceManager
        @JvmStatic
        get() = requireReactNativeHost().reactInstanceManager

    @JvmStatic
    fun install(rnHost: ReactNativeHost) {
        this.reactNativeHost = rnHost
    }

    @JvmStatic
    fun sendEvent(eventName: String, data: Any?) {
        reactInstanceManager.currentReactContext
            ?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            ?.emit(eventName, data)
    }

    @JvmStatic
    fun receiveEvent(targetTag: Int, eventName: String, event: WritableMap?) {
        reactInstanceManager.currentReactContext
            ?.getJSModule(RCTModernEventEmitter::class.java)
            ?.receiveEvent(-1, targetTag, eventName, event)
    }

    @JvmStatic
    fun sendSpecialEvent(
            reactContext: ReactContext,
            eventName: String,
            params: WritableMap?
    ) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit(eventName, params)
    }
}
