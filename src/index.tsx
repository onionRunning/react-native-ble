import {NativeModules} from 'react-native'

const Ble = NativeModules.Ble

export function multiply(a: number, b: number): Promise<number> {
  return Ble.multiply(a, b)
}
