package com.runiverse.app.phone

import android.content.Context
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Wear OS `MessageClient`로 폰과 메시지를 주고받는다. */
class PhoneChannel(context: Context) {

    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    val messages: Flow<PhoneMessage> = callbackFlow {
        val listener = MessageClient.OnMessageReceivedListener { event ->
            if (event.path != PATH) return@OnMessageReceivedListener
            decodeOrNull(event.data)?.let { trySend(it) }
        }
        // 등록이 실패하면 아무것도 못 받은 채 조용히 남지 않게, 받는 쪽에 예외로 넘긴다.
        // 등록을 기다리는 중에 취소돼도 등록 자체는 끝까지 진행된다 — 등록이 끝난 뒤에 해제해야 남지 않는다
        val registration = messageClient.addListener(listener)
        try {
            registration.await()
            awaitClose()
        } finally {
            registration.addOnCompleteListener { messageClient.removeListener(listener) }
        }
    }

    // 실패를 되묻지 않는다 — 답이 안 오면 PhoneLink가 SYNC로 다시 맞춘다
    suspend fun send(message: WatchMessage) {
        try {
            val phone = nodeClient.connectedNodes.await().firstOrNull { it.isNearby }
            if (phone == null) {
                Log.d(TAG, "가까이 연결된 폰 없음 — ${message::class.simpleName} 안 보냄")
                return
            }
            messageClient.sendMessage(phone.id, PATH, PhoneMessageCodec.encode(message)).await()
        } catch (e: ApiException) {
            Log.w(TAG, "폰으로 보내기 실패 — ${message::class.simpleName}", e)
        }
    }

    // 깨진 메시지 하나로 받기를 멈추지 않는다 — 다음 상태가 곧 다시 온다.
    // SerializationException도 IllegalArgumentException이다
    private fun decodeOrNull(bytes: ByteArray): PhoneMessage? = try {
        PhoneMessageCodec.decode(bytes)
    } catch (e: IllegalArgumentException) {
        Log.w(TAG, "폰 메시지 해석 실패", e)
        null
    }

    private companion object {
        const val PATH = "/running"
        const val TAG = "PhoneChannel"
    }
}
