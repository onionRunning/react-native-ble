export interface BleModuleApi {
  judgePermissionOk(): Promise<{data: string; code: number}>
  requestPermission(): Promise<string>
  // 开始扫描蓝牙信息
  startScanBle(): void

  // 获取扫码结果
  requestScanResultListener(fn: (m: {data: BleDeviceModel}) => void): void
  // 移除扫码结果
  removeScanResultListener(): void

  startConnectBle(mac: string, sn: string): Promise<{data: any; code: number}>

  startConnectBleFn(sn: string, fn: (...s: any) => void): void

  disConnectBle(sn: string): void
}

export interface BleDeviceModel {
  mac: string // 蓝牙设备MAC地址
  rssi?: number // 型号强度, 例如 -99db, 可选数据
  scanRecord?: string // 扫描记录信息, 可选数据
}
