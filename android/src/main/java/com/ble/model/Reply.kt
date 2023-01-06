package com.ble.model

import java.util.*


private const val TAG = "Reply"
internal const val DEFAULT_SCAN_TIMEOUT = 3 * 60 * 1000L // 扫描超时 3min
private const val DEFAULT_REPLY_TIMEOUT = 2_500L  // 数据应答2.5秒超时
private const val DEFAULT_FIRST_REPLY_TIMEOUT = DEFAULT_REPLY_TIMEOUT * 2
private const val DEFAULT_MAX_RETRY_COUNT = 2


interface IReply {
  fun onReply(uuid: UUID?, hex: String)
}

private class TimeoutTask(
  private val block: () -> Unit
): TimerTask() {
  override fun run() { block.invoke() }
}
fun interface ScanReply {
  fun onReply(flag: Boolean, pair: Pair<AdvDevice, Int>?)
}
