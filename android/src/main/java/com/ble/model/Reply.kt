package com.ble.model

import android.util.Log
import com.ble.utils.ConnectReplyType
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

fun interface ConnectReply {
  fun onReply(@ConnectReplyType type: Int)
}


/*** 简单命令应答 一般一次请求一次响应即完成数据通讯过程
 * @param commandTag 命令标签 [TAG_COMMAND_DEVICE_INFO]
 * @param replyTimeout 超时阈值 单位ms
 */
abstract class SimpleReply(
  private val commandTag: String,
  replyTimeout: Long = DEFAULT_FIRST_REPLY_TIMEOUT
): IReply {
  private val timer: Timer = Timer(commandTag)
  private val task: TimerTask = object : TimerTask() {
    override fun run() { onTimeout(commandTag) }
  }
  // 达到超时阈值 则会执行onTimeout
  init { timer.schedule(task, replyTimeout) }

  // 取消超时任务 并完成数据通讯
  override fun onReply(uuid: UUID?, hex: String) {
    timer.cancel()
    timer.purge()
    Log.d("filter_$TAG", "Simple:hex=$hex")
    onComplete(commandTag, uuid, hex)
  }

  abstract fun onTimeout(commandTag: String)

  abstract fun onComplete(commandTag: String, uuid: UUID?, hex: String)
}
