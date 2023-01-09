package com.ble.utils

import java.util.*

internal const val SERVICE_ID = "00010203-0405-0607-0809-0a0b0c0d1910"
internal const val TX_CHARACTERISTIC_ID = "00010203-0405-0607-0809-0a0b0c0d2b10"
internal const val RX_CHARACTERISTIC_ID = "00010203-0405-0607-0809-0a0b0c0d2b11"
internal val SERVICE = UUID.fromString(SERVICE_ID)
// pad --> phone
internal val TX_CHARACTERISTIC = UUID.fromString(TX_CHARACTERISTIC_ID)
// pad <-- phone
internal val RX_CHARACTERISTIC = UUID.fromString(RX_CHARACTERISTIC_ID)


internal const val DISCONNECT_SUCCESS = 0
internal const val DISCONNECT_ERROR_SN_NULL_OR_EMPTY = 1
internal const val DISCONNECT_ERROR_CONNECTION_NOT_FOUND = 2
internal const val DISCONNECT_ERROR_WAS_DISCONNECTED = 3
internal const val DISCONNECT_ERROR_CANNOT_DISCONNECT = 4
