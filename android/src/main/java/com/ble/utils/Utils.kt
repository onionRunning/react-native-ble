package com.ble.utils

import android.Manifest
import android.content.Context
import com.hjq.permissions.XXPermissions
import android.os.Build
import com.hjq.permissions.OnPermissionCallback


// 低功耗蓝牙必要权限
internal val bleLocationPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) arrayOf(
  Manifest.permission.BLUETOOTH_SCAN,
  Manifest.permission.BLUETOOTH_CONNECT,
) else arrayOf(
  Manifest.permission.ACCESS_COARSE_LOCATION,
  Manifest.permission.ACCESS_FINE_LOCATION
)

/*** 判断蓝牙定位权限是否授予
 * @param context 上下文
 * @return 权限是否授予
 */
fun isBlePermissionGranted(context: Context): Boolean =
    XXPermissions.isGranted(context, bleLocationPermissions)


/*** byte数组转成十六进制字符串*/
fun ByteArray?.toHexString(): String {
  if (this == null || this.isEmpty()) return "[empty]"
  return joinToString(separator = " ") { "%02X".format(it) }
}

/*** 未授予蓝牙定位存储权限时 请求权限 */
fun ensureBlePermissionGranted(context: Context, callback: OnPermissionCallback) {
  if (!isBlePermissionGranted(context)) {
    XXPermissions.with(context)
      .permission(bleLocationPermissions)
      .request(callback)
  } else {
    // this is bad...
    callback.onGranted(bleLocationPermissions.asList(), true)
  }
}
