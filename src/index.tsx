import {NativeModules} from 'react-native'
import type {BleModuleApi} from './const'
const Ble = NativeModules?.Ble || {}

export const bleModuleApi: BleModuleApi = {
  judgePermissionOk: Ble.isAllOk,
  requestPermission: Ble.requestBlePermission,
}
