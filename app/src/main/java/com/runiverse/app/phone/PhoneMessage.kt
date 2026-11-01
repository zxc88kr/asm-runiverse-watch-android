package com.runiverse.app.phone

import kotlinx.serialization.Serializable

/** 워치가 폰으로 보내는 메시지. */
sealed interface WatchMessage

/** 폰이 워치로 보내는 메시지. */
sealed interface PhoneMessage

@Serializable
data class WatchSync(val syncId: String) : WatchMessage

@Serializable
data class WatchPause(
    val requestId: String,
    val phoneInstanceId: String,
    val runningRoomId: Long,
    val statusVersion: Int,
) : WatchMessage

@Serializable
data class WatchResume(
    val requestId: String,
    val phoneInstanceId: String,
    val runningRoomId: Long,
    val statusVersion: Int,
) : WatchMessage

@Serializable
data class WatchFinish(
    val requestId: String,
    val phoneInstanceId: String,
    val runningRoomId: Long,
    val statusVersion: Int,
    val forced: Boolean,
) : WatchMessage

@Serializable
data class WatchVisibilityUpdate(val isVisible: Boolean) : WatchMessage

// 상태 필드는 WATCH_STATE_UPDATED와 같은 높이에 펼쳐져 온다 — 코덱이 한 data에서 두 번 읽는다
data class WatchSynced(
    val syncId: String,
    val state: PhoneState,
    val birthday: String?,
    val photos: List<PlayerPhoto>,
) : PhoneMessage

data class WatchStateUpdated(val state: PhoneState) : PhoneMessage

@Serializable
data class WatchComboTriggered(
    val eventId: String,
    val userId: String,
    val comboCount: Int,
    val kind: ComboKind,
) : PhoneMessage

@Serializable
data class PhoneError(
    val code: String,
    val message: String,
    val sourceType: String,
    val requestId: String? = null,
) : PhoneMessage

@Serializable
data class PhoneState(
    val phoneInstanceId: String,
    val stateSequence: Long,
    val statusVersion: Int,
    val lastRequestId: String? = null,
    val run: RunState? = null,
)

@Serializable
data class RunState(
    val runningRoomId: Long,
    val type: RoomType,
    val status: RunStatus,
    val targetDistanceMeters: Int? = null,
    val isServerConnected: Boolean,
    val isPenaltyApplicable: Boolean,
    val me: MyRecord,
    val players: List<Player>,
    val playersAgeSeconds: Int,
    val finish: FinishProgress? = null,
    val result: RunResult? = null,
)

@Serializable
data class MyRecord(
    val distanceMeters: Int,
    val elapsedSeconds: Int,
    val currentPaceSecondsPerKm: Int? = null,
    val caloriesKcal: Int? = null,
    val cadenceSpm: Int? = null,
)

@Serializable
data class Player(
    val userId: String,
    val nickname: String,
    val colorSlot: Int,
    val distanceMeters: Int,
    val currentPaceSecondsPerKm: Int? = null,
    val status: RunStatus,
    val gapMeters: Int? = null,
    val comboCount: Int,
    val maxComboCount: Int,
)

@Serializable
data class FinishProgress(
    val stage: FinishStage,
    val remainingMeters: Int? = null,
)

@Serializable
data class RunResult(
    val isServerConfirmed: Boolean,
    val totalDistanceMeters: Int,
    val averagePaceSecondsPerKm: Int? = null,
    val elapsedSeconds: Int,
)

@Serializable
data class PlayerPhoto(
    val userId: String,
    val jpegBase64: String,
)

@Serializable
enum class RoomType { MATCH, SOLO, INVITE }

@Serializable
enum class RunStatus { RUNNING, PAUSED, DISCONNECTED, FINISHED }

@Serializable
enum class FinishStage { FINISHING, GOAL_PENDING }

@Serializable
enum class ComboKind { TICK, MILESTONE, BREAK }
