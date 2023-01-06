export interface BleModuleApi {
  judgePermissionOk(): Promise<{data: string; code: number}>
  requestPermission(): Promise<string>
}
