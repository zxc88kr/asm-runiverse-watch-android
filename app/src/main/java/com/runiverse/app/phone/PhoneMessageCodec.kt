package com.runiverse.app.phone

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 메시지 본문 `{"event": ..., "data": {...}}`과 메시지 객체를 바꾼다.
 *
 * 깨진 본문은 [IllegalArgumentException]([kotlinx.serialization.SerializationException] 포함)을 던진다.
 */
object PhoneMessageCodec {

    // 폰 앱이 새 필드를 먼저 보내도 지금 워치가 읽을 수 있어야 한다
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(message: WatchMessage): ByteArray {
        val (event, data) = when (message) {
            is WatchSync -> "WATCH_SYNC" to json.encodeToJsonElement(WatchSync.serializer(), message)
            is WatchPause -> "WATCH_PAUSE" to json.encodeToJsonElement(WatchPause.serializer(), message)
            is WatchResume -> "WATCH_RESUME" to json.encodeToJsonElement(WatchResume.serializer(), message)
            is WatchFinish -> "WATCH_FINISH" to json.encodeToJsonElement(WatchFinish.serializer(), message)
            is WatchVisibilityUpdate ->
                "WATCH_VISIBILITY_UPDATE" to json.encodeToJsonElement(WatchVisibilityUpdate.serializer(), message)
        }
        val envelope = JsonObject(mapOf("event" to JsonPrimitive(event), "data" to data))
        return json.encodeToString(JsonElement.serializer(), envelope).encodeToByteArray()
    }

    /** 모르는 event면 null — 폰 앱이 먼저 업데이트돼 새 메시지를 보내는 경우다. */
    fun decode(bytes: ByteArray): PhoneMessage? {
        val envelope = json.parseToJsonElement(bytes.decodeToString()).jsonObject
        val event = requireNotNull(envelope["event"]) { "event 없음" }.jsonPrimitive.content
        val data = requireNotNull(envelope["data"]) { "data 없음" }
        return when (event) {
            "WATCH_SYNCED" -> {
                val extras = json.decodeFromJsonElement(SyncedExtras.serializer(), data)
                WatchSynced(
                    syncId = extras.syncId,
                    state = json.decodeFromJsonElement(PhoneState.serializer(), data),
                    birthday = extras.birthday,
                    photos = extras.photos,
                )
            }
            "WATCH_STATE_UPDATED" -> WatchStateUpdated(json.decodeFromJsonElement(PhoneState.serializer(), data))
            "WATCH_COMBO_TRIGGERED" -> json.decodeFromJsonElement(WatchComboTriggered.serializer(), data)
            "ERROR" -> json.decodeFromJsonElement(PhoneError.serializer(), data)
            else -> null
        }
    }

    @Serializable
    private data class SyncedExtras(
        val syncId: String,
        val birthday: String? = null,
        val photos: List<PlayerPhoto> = emptyList(),
    )
}
