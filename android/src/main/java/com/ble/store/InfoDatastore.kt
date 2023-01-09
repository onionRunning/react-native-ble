package com.ble.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.blankj.utilcode.util.Utils
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


val Context.infoStore: DataStore<Preferences> by preferencesDataStore(name = "info")
private val infoStore = Utils.getApp().infoStore
private val SN_JSON = stringPreferencesKey("sn_json")

object InfoDatastore {
    private fun snFlow() = infoStore.data.map { it[SN_JSON] }.map { s ->
        s.takeUnless { it.isNullOrBlank() }?.let { Json.decodeFromString<List<String>>(it) }
    }

    private suspend fun load(): List<String>? = withContext(IO) {
        runCatching { snFlow().firstOrNull() }.getOrNull()
    }

    suspend fun getCurrentSN(): String? = load()?.firstOrNull()

    suspend fun addSN(index: Int, newSN: String) = withContext(IO) {
        val l = load()?.toMutableList()?.apply { add(index, newSN) } ?: mutableListOf(newSN)
        infoStore.edit {
            it[SN_JSON] = Json.encodeToString(l)
        }
    }

}
