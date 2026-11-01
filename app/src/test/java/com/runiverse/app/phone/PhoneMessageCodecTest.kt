package com.runiverse.app.phone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class PhoneMessageCodecTest {

    @Test
    fun `상태 메시지를 문서의 모양 그대로 읽는다`() {
        // given
        val body = """
            {"event":"WATCH_STATE_UPDATED","data":{
              "phoneInstanceId":"3f2a9c1e","stateSequence":1532,"statusVersion":4,"lastRequestId":"b71d0e42",
              "run":{
                "runningRoomId":812,"type":"MATCH","status":"RUNNING","targetDistanceMeters":5000,
                "isServerConnected":true,"isPenaltyApplicable":true,
                "me":{"distanceMeters":3421,"elapsedSeconds":1450,"currentPaceSecondsPerKm":332,
                      "caloriesKcal":210,"cadenceSpm":172},
                "players":[{"userId":"u2","nickname":"콩나물","colorSlot":1,"distanceMeters":3541,
                            "currentPaceSecondsPerKm":318,"status":"RUNNING","gapMeters":120,
                            "comboCount":12,"maxComboCount":20}],
                "playersAgeSeconds":3,
                "finish":{"stage":"GOAL_PENDING","remainingMeters":420},
                "result":null
              }
            }}
        """.trimIndent()

        // when
        val message = PhoneMessageCodec.decode(body.encodeToByteArray())

        // then
        val expected = WatchStateUpdated(
            PhoneState(
                phoneInstanceId = "3f2a9c1e",
                stateSequence = 1532,
                statusVersion = 4,
                lastRequestId = "b71d0e42",
                run = RunState(
                    runningRoomId = 812,
                    type = RoomType.MATCH,
                    status = RunStatus.RUNNING,
                    targetDistanceMeters = 5000,
                    isServerConnected = true,
                    isPenaltyApplicable = true,
                    me = MyRecord(
                        distanceMeters = 3421,
                        elapsedSeconds = 1450,
                        currentPaceSecondsPerKm = 332,
                        caloriesKcal = 210,
                        cadenceSpm = 172,
                    ),
                    players = listOf(
                        Player(
                            userId = "u2",
                            nickname = "콩나물",
                            colorSlot = 1,
                            distanceMeters = 3541,
                            currentPaceSecondsPerKm = 318,
                            status = RunStatus.RUNNING,
                            gapMeters = 120,
                            comboCount = 12,
                            maxComboCount = 20,
                        ),
                    ),
                    playersAgeSeconds = 3,
                    finish = FinishProgress(stage = FinishStage.GOAL_PENDING, remainingMeters = 420),
                    result = null,
                ),
            ),
        )
        assertEquals(expected, message)
    }

    @Test
    fun `SYNCED는 같은 높이의 상태 필드와 사진·생일을 함께 읽는다`() {
        // given
        val body = """
            {"event":"WATCH_SYNCED","data":{
              "syncId":"s1","phoneInstanceId":"p1","stateSequence":7,"statusVersion":0,"lastRequestId":null,
              "run":null,"birthday":"1999-03-02","photos":[{"userId":"u2","jpegBase64":"/9j/"}]
            }}
        """.trimIndent()

        // when
        val message = PhoneMessageCodec.decode(body.encodeToByteArray())

        // then
        val expected = WatchSynced(
            syncId = "s1",
            state = PhoneState(phoneInstanceId = "p1", stateSequence = 7, statusVersion = 0),
            birthday = "1999-03-02",
            photos = listOf(PlayerPhoto(userId = "u2", jpegBase64 = "/9j/")),
        )
        assertEquals(expected, message)
    }

    @Test
    fun `모르는 필드는 무시한다`() {
        // given
        val body = """
            {"event":"WATCH_COMBO_TRIGGERED","data":{
              "eventId":"e1","userId":"u2","comboCount":3,"kind":"MILESTONE","newField":1
            }}
        """.trimIndent()

        // when
        val message = PhoneMessageCodec.decode(body.encodeToByteArray())

        // then
        val expected = WatchComboTriggered(eventId = "e1", userId = "u2", comboCount = 3, kind = ComboKind.MILESTONE)
        assertEquals(expected, message)
    }

    @Test
    fun `모르는 event는 null`() {
        // given
        val body = """{"event":"WATCH_SOMETHING_NEW","data":{}}"""

        // when
        val message = PhoneMessageCodec.decode(body.encodeToByteArray())

        // then
        assertNull(message)
    }

    @Test
    fun `깨진 본문은 IllegalArgumentException`() {
        // given
        val brokenBodies = listOf(
            "not json",
            """{"data":{}}""",
            """{"event":"ERROR"}""",
            """{"event":"WATCH_STATE_UPDATED","data":{"phoneInstanceId":"p1"}}""",
        )

        // when & then
        brokenBodies.forEach { body ->
            assertThrows(IllegalArgumentException::class.java) { PhoneMessageCodec.decode(body.encodeToByteArray()) }
        }
    }

    @Test
    fun `종료 명령을 봉투에 담아 쓴다`() {
        // given
        val message = WatchFinish(
            requestId = "r1",
            phoneInstanceId = "p1",
            runningRoomId = 812,
            statusVersion = 4,
            forced = false,
        )

        // when
        val body = PhoneMessageCodec.encode(message).decodeToString()

        // then
        val expected = """{"event":"WATCH_FINISH","data":""" +
            """{"requestId":"r1","phoneInstanceId":"p1","runningRoomId":812,"statusVersion":4,"forced":false}}"""
        assertEquals(expected, body)
    }
}
