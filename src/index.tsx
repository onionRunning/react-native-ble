import {NativeModules} from 'react-native'

const Ble = NativeModules?.Ble || {}

console.info(Ble, '------')
export const bleModuleApi = {
  isPermissionOk: Ble.isAllOk,
}
