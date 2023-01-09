import {NativeEventEmitter, NativeModules} from 'react-native'
import type {BleDeviceModel, BleModuleApi} from './const'
const Ble = NativeModules?.Ble || {}
const BleObserver = new NativeEventEmitter(Ble)

// 扫描蓝牙设备信息
export const requestScanResultListener = (fn: (model: {data: BleDeviceModel}) => void) => {
  if (BleObserver) {
    BleObserver.addListener('EVENT_SCAN_RESULT', fn)
  }
}

export const removeScanResultListener = () => {
  if (BleObserver) {
    BleObserver.removeAllListeners('EVENT_SCAN_RESULT')
  }
}

export const bleModuleApi = {
  // 判断权限
  judgePermissionOk: Ble.isAllOk,
  // 请求权限
  requestPermission: Ble.requestBlePermission,

  // 扫描蓝牙信息
  startScanBle: Ble.scanDevice,

  // 连接蓝牙信息
  startConnectBle: Ble.connectDevice,

  // // 获取扫码结果
  requestScanResultListener,
  // // 取消扫码结果
  removeScanResultListener,
}

export const hello = 'hello world!'
