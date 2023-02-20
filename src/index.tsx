import {NativeEventEmitter, NativeModules, Platform} from 'react-native'
import type {BleDeviceModel, BleModuleApi} from './const'

const isAndroid = Platform.OS === 'android'

const Ble = isAndroid ? NativeModules?.Ble : NativeModules?.BleModule
const BleObserver = new NativeEventEmitter(Ble) || {}
// 扫描蓝牙设备信息
export const requestScanResultListener = (
  fn: (model: {data: BleDeviceModel; code: number}) => void
) => {
  if (isAndroid && BleObserver) {
    BleObserver.addListener('EVENT_SCAN_RESULT', fn)
  }
}

export const removeScanResultListener = () => {
  if (isAndroid && BleObserver) {
    BleObserver.removeAllListeners('EVENT_SCAN_RESULT')
  }
}

export const startConnectBleFn = async (sn: string, fn: (...s: any) => void) => {
  if (isAndroid) {
    Ble.scanDevice()
    requestScanResultListener(async res => {
      const {code, data} = res
      if (code === 200) {
        if (data.scanRecord === sn) {
          // 开始连接同时关闭扫描
          removeScanResultListener()
          const connectRes = await Ble.connectDevice(sn, data?.mac)
          fn(connectRes)
          console.info(connectRes, 'connectRes')
        }
      }
    })
  } else {
    // ios connect
    const iosRes = await Ble.connectDevice(sn, sn)
    if (iosRes.code === 200) {
      fn(iosRes)
    }
  }
}

// 发送通知给蓝牙设备
export const sendCommandToBle = async (command: string[]) => {
  const Type = isAndroid ? 'send_msg_to_ble' : 'TAG_COMMAND_DEVICE_SERIAL_NUMBER'
  return Ble.sendCommandWithCallback(Type, command)
}

export const bleModuleApi: BleModuleApi = {
  // 判断权限
  judgePermissionOk: Ble.isAllOk,
  // 请求权限
  requestPermission: Ble.requestBlePermission,

  // 扫描蓝牙信息
  // startScanBle: Ble.scanDevice,

  // 连接蓝牙信息
  startConnectBle: Ble.connectDevice,

  // // // 获取扫码结果
  // requestScanResultListener,
  // // // 取消扫码结果
  // removeScanResultListener,

  startConnectBleFn,
  // 断开蓝牙设备
  disConnectBle: Ble.disconnect,
  sendCommandToBle,
}
