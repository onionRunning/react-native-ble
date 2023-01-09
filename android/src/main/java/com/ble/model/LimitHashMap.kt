package com.ble.model

class LimitHashMap<K, V>(
    private val limit: Int
): LinkedHashMap<K, V>() {

    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
        return size > limit
    }
}

inline fun <reified K, V> limitMapOf(limit: Int): LimitHashMap<K, V> = LimitHashMap(limit)
