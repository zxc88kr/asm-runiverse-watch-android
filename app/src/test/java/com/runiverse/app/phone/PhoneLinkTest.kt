package com.runiverse.app.phone

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhoneLinkTest {

    @Test
    fun `앱을 열면 SYNC를 보내고 SYNCED를 받을 때까지 잠근다`() = runTest {
        // given
        val fixture = Fixture(this)

        // when
        fixture.link.start()
        runCurrent()

        // then
        val sync = fixture.sent.single() as WatchSync
        assertTrue(fixture.link.state.value.isLocked)

        // when -> 폰이 답한다
        fixture.link.onReceived(synced(sync.syncId, state(sequence = 1)))

        // then
        val linkState = fixture.link.state.value
        assertFalse(linkState.isLocked)
        assertEquals(1L, linkState.phoneState?.stateSequence)
    }

    @Test
    fun `보낸 적 없는 syncId의 SYNCED는 버린다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.link.start()
        runCurrent()

        // when
        fixture.link.onReceived(synced("모르는-id", state(sequence = 1)))

        // then
        assertTrue(fixture.link.state.value.isSyncing)
        assertNull(fixture.link.state.value.phoneState)
    }

    @Test
    fun `러닝 중 화면이 켜져 있는데 3초 동안 못 받으면 폰 연결 확인 중으로 바꾸고 SYNC를 보낸다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(sequence = 1))
        fixture.link.onScreenVisibilityChanged(isVisible = true)
        runCurrent()
        fixture.sent.clear()

        // when
        advanceTimeBy(3_001)

        // then
        val linkState = fixture.link.state.value
        assertFalse(linkState.isPhoneReachable)
        assertTrue(linkState.isLocked)
        assertEquals(1L, linkState.phoneState?.stateSequence)
        assertTrue(fixture.sent.any { it is WatchSync })
    }

    @Test
    fun `보내기가 끝나지 않아도 3초마다 SYNC를 다시 보낸다`() = runTest {
        // given
        val fixture = Fixture(this, isSendHanging = true)
        fixture.link.onScreenVisibilityChanged(isVisible = true)
        fixture.link.start()

        // when
        advanceTimeBy(6_001)

        // then
        assertEquals(3, fixture.sent.count { it is WatchSync })
        assertFalse(fixture.link.state.value.isPhoneReachable)
    }

    @Test
    fun `화면이 꺼져 있으면 오래 못 받아도 연결 상태를 바꾸지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(sequence = 1))

        // when
        advanceTimeBy(60_000)

        // then
        assertTrue(fixture.link.state.value.isPhoneReachable)
        assertFalse(fixture.link.state.value.isLocked)
    }

    @Test
    fun `상태가 계속 오면 3초가 지나도 연결된 채로 둔다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(sequence = 1))
        fixture.link.onScreenVisibilityChanged(isVisible = true)

        // when
        for (sequence in 2L..5L) {
            advanceTimeBy(1_000)
            fixture.link.onReceived(WatchStateUpdated(state(sequence = sequence)))
        }
        advanceTimeBy(2_000)

        // then
        assertTrue(fixture.link.state.value.isPhoneReachable)
        assertEquals(5L, fixture.link.state.value.phoneState?.stateSequence)
    }

    @Test
    fun `지난 번호의 상태는 버린다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(sequence = 5))

        // when
        fixture.link.onReceived(WatchStateUpdated(state(sequence = 4, distanceMeters = 999)))

        // then
        assertEquals(5L, fixture.link.state.value.phoneState?.stateSequence)
    }

    @Test
    fun `폰 앱이 다시 켜져 phoneInstanceId가 바뀌면 SYNC를 보내고 그 상태는 쓰지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(sequence = 5))

        // when
        fixture.link.onReceived(WatchStateUpdated(state(sequence = 1, phoneInstanceId = "p2")))
        runCurrent()

        // then
        assertEquals("p1", fixture.link.state.value.phoneState?.phoneInstanceId)
        assertTrue(fixture.link.state.value.isSyncing)
        assertTrue(fixture.sent.single() is WatchSync)
    }

    @Test
    fun `모르는 방 번호가 오면 SYNC를 보낸다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(sequence = 1, runningRoomId = 812))

        // when
        fixture.link.onReceived(WatchStateUpdated(state(sequence = 2, runningRoomId = 900)))
        runCurrent()

        // then
        assertEquals(812L, fixture.link.state.value.phoneState?.run?.runningRoomId)
        assertTrue(fixture.sent.single() is WatchSync)
    }

    @Test
    fun `명령은 처리됐다는 상태가 올 때까지 잠근다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(sequence = 1, statusVersion = 4))

        // when
        val isSent = fixture.link.pause()
        runCurrent()

        // then
        assertTrue(isSent)
        val pause = fixture.sent.single() as WatchPause
        assertEquals(WatchPause(pause.requestId, "p1", 812, 4), pause)
        assertTrue(fixture.link.state.value.isLocked)

        // when -> 폰이 반영한 상태를 보낸다
        fixture.link.onReceived(
            WatchStateUpdated(state(sequence = 2, statusVersion = 5, lastRequestId = pause.requestId)),
        )

        // then
        assertFalse(fixture.link.state.value.isLocked)
    }

    @Test
    fun `다른 명령이 처리된 상태로는 잠금을 풀지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(sequence = 1))
        fixture.link.pause()

        // when
        fixture.link.onReceived(WatchStateUpdated(state(sequence = 2, lastRequestId = "폰에서-누른-것")))

        // then
        assertTrue(fixture.link.state.value.isLocked)
    }

    @Test
    fun `명령에 3초 동안 답이 없으면 SYNC를 보내고 명령은 다시 보내지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(sequence = 1))
        fixture.link.finish(forced = false)
        runCurrent()

        // when
        advanceTimeBy(3_001)

        // then
        assertTrue(fixture.link.state.value.isCommandOverdue)
        assertEquals(1, fixture.sent.count { it is WatchFinish })
        assertTrue(fixture.sent.last() is WatchSync)
    }

    @Test
    fun `거절 ERROR가 오면 잠금을 푼다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(sequence = 1))
        fixture.link.resume()
        runCurrent()
        val resume = fixture.sent.single() as WatchResume

        // when
        fixture.link.onReceived(
            PhoneError(
                code = "STATUS_VERSION_MISMATCH",
                message = "상태가 바뀌었습니다.",
                sourceType = "WATCH_RESUME",
                requestId = resume.requestId,
            ),
        )

        // then
        assertFalse(fixture.link.state.value.isLocked)
    }

    @Test
    fun `잠겨 있으면 명령을 보내지 않는다`() = runTest {
        // given
        val fixture = Fixture(this)
        fixture.connect(state(sequence = 1))
        fixture.link.pause()
        runCurrent()

        // when
        val isSent = fixture.link.finish(forced = true)
        runCurrent()

        // then
        assertFalse(isSent)
        assertEquals(1, fixture.sent.size)
    }

    @Test
    fun `같은 eventId의 콤보는 한 번만 내보낸다`() = runTest {
        // given
        val fixture = Fixture(this)
        val received = mutableListOf<WatchComboTriggered>()
        backgroundScope.launch { fixture.link.combos.collect { received += it } }
        runCurrent()
        val combo = WatchComboTriggered(eventId = "e1", userId = "u2", comboCount = 3, kind = ComboKind.MILESTONE)

        // when
        fixture.link.onReceived(combo)
        fixture.link.onReceived(combo)
        runCurrent()

        // then
        assertEquals(listOf(combo), received)
    }

    private class Fixture(private val testScope: TestScope, isSendHanging: Boolean = false) {
        val sent = mutableListOf<WatchMessage>()
        private var nextId = 0
        val link = PhoneLink(
            scope = testScope.backgroundScope,
            send = {
                sent += it
                if (isSendHanging) awaitCancellation()
            },
            newId = { "id-${++nextId}" },
        )

        fun connect(initial: PhoneState) {
            link.start()
            testScope.runCurrent()
            val sync = sent.last() as WatchSync
            link.onReceived(synced(sync.syncId, initial))
            sent.clear()
        }
    }

    private companion object {
        fun synced(syncId: String, state: PhoneState) =
            WatchSynced(syncId = syncId, state = state, birthday = null, photos = emptyList())

        fun state(
            sequence: Long,
            phoneInstanceId: String = "p1",
            runningRoomId: Long = 812,
            statusVersion: Int = 0,
            lastRequestId: String? = null,
            distanceMeters: Int = 1000,
        ) = PhoneState(
            phoneInstanceId = phoneInstanceId,
            stateSequence = sequence,
            statusVersion = statusVersion,
            lastRequestId = lastRequestId,
            run = RunState(
                runningRoomId = runningRoomId,
                type = RoomType.MATCH,
                status = RunStatus.RUNNING,
                targetDistanceMeters = 5000,
                isServerConnected = true,
                isPenaltyApplicable = true,
                me = MyRecord(distanceMeters = distanceMeters, elapsedSeconds = 300),
                players = emptyList(),
                playersAgeSeconds = 0,
            ),
        )
    }
}
