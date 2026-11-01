# 폰 ↔ 워치 통신

폰이 러닝을 기록하고 판단한다. 워치는 받은 값을 보여 주고 누른 것을 폰에 넘길 뿐, 서버와 직접 통신하지 않는다.
애플워치도 같은 메시지를 쓴다 — 바꿀 땐 폰·갤럭시·애플을 함께 고친다.

## 통로

- Wear OS `MessageClient`, 경로 `/running` 하나. 본문은 UTF-8 JSON `{"event": ..., "data": {...}}` — 서버 WebSocket과 같은 봉투다.
- 받는 기기는 `NodeClient.connectedNodes` 중 `isNearby`인 것. 폰 앱에 capability 선언은 두지 않는다.
- 연결돼 있을 때만 보낸다. 끊긴 동안 쌓아 두지 않고, 다시 연결되면 `WATCH_SYNC`로 맞춘다.
- 모르는 `event`·필드는 무시한다. enum 값을 늘릴 땐 양쪽을 함께 바꾼다 — 모르는 값이 든 메시지는 통째로 버려진다.

## 메시지

보내는 쪽이 워치면 현재형, 폰이면 과거형이다. 명령에 따로 답하지 않고 바뀐 상태로 알린다(서버도 일시정지·재개엔 답이 없다).

| event | 방향 | data | 언제 |
|---|---|---|---|
| `WATCH_SYNC` | 워치 → 폰 | `syncId` | 앱을 열 때, 다시 연결할 때, 모르는 번호를 받았을 때 |
| `WATCH_SYNCED` | 폰 → 워치 | 상태 전체 + `syncId`·`birthday`·`photos` | `WATCH_SYNC`를 받으면 바로 |
| `WATCH_STATE_UPDATED` | 폰 → 워치 | 상태 전체 | 아래 「보내는 규칙」 |
| `WATCH_PAUSE` | 워치 → 폰 | 명령 공통 | 일시정지 |
| `WATCH_RESUME` | 워치 → 폰 | 명령 공통 | 계속 달리기 |
| `WATCH_FINISH` | 워치 → 폰 | 명령 공통 + `forced` | 종료. `forced`는 폰 종료 버튼과 같은 규칙 |
| `WATCH_COMBO_TRIGGERED` | 폰 → 워치 | `eventId`·`userId`·`comboCount`·`kind` | 콤보가 오르거나 끊긴 순간 |
| `WATCH_VISIBILITY_UPDATE` | 워치 → 폰 | `isVisible` | 화면이 켜지거나 꺼질 때, 켜진 동안 10초마다 |
| `ERROR` | 폰 → 워치 | `code`·`message`·`sourceType`·`requestId` | 명령을 거절할 때만 |

명령 공통 data는 `requestId`·`phoneInstanceId`·`runningRoomId`·`statusVersion`(누를 때 워치가 본 값)이다.

## 번호

| 이름 | 누가 | 뜻 |
|---|---|---|
| `phoneInstanceId` | 폰 | 폰 앱이 켜질 때마다 새 값. 워치는 바뀐 걸 보면 SYNC, 폰은 다른 값이 실린 명령을 거절 |
| `stateSequence` | 폰 | 상태를 보낼 때마다 +1(SYNCED 포함). 워치는 지난 번호를 버린다. 서버 `sequence`(좌표 순번)와 다르다 |
| `statusVersion` | 폰 | 일시정지·재개·종료 단계가 바뀔 때만 +1. 명령에 실린 값과 다르면 거절 |
| `lastRequestId` | 폰 | 마지막으로 처리한 워치 명령. 워치는 이걸로 명령 완료를 판정한다 |
| `requestId` | 워치 | 명령마다 새 값. 폰은 같은 값이 두 번 와도 한 번만 처리한다 |
| `syncId` | 워치 | SYNC마다 새 값. SYNCED가 그대로 돌려준다 |
| `eventId` | 폰 | 콤보 순간마다 새 값. 워치는 본 값을 다시 받으면 버린다 |

## 상태

`WATCH_STATE_UPDATED`의 data다. `WATCH_SYNCED`는 여기에 `syncId`·`birthday`·`photos`를 더한다.

```json
{
  "phoneInstanceId": "3f2a9c1e",
  "stateSequence": 1532,
  "statusVersion": 4,
  "lastRequestId": "b71d0e42",
  "run": {
    "runningRoomId": 812,
    "type": "MATCH",
    "status": "RUNNING",
    "targetDistanceMeters": 5000,
    "isServerConnected": true,
    "isPenaltyApplicable": true,
    "me": {
      "distanceMeters": 3421,
      "elapsedSeconds": 1450,
      "currentPaceSecondsPerKm": 332,
      "caloriesKcal": 210,
      "cadenceSpm": 172
    },
    "players": [
      {
        "userId": "5b1f…",
        "nickname": "콩나물",
        "colorSlot": 1,
        "distanceMeters": 3541,
        "currentPaceSecondsPerKm": 318,
        "status": "RUNNING",
        "gapMeters": 120,
        "comboCount": 12,
        "maxComboCount": 20
      }
    ],
    "playersAgeSeconds": 3,
    "finish": null,
    "result": null
  }
}
```

| 필드 | 뜻 |
|---|---|
| `run` | 진행 중인 러닝. 없으면 `null` |
| `type` | `MATCH`·`SOLO`·`INVITE` |
| `status` | 내 상태 `RUNNING`·`PAUSED`·`DISCONNECTED`·`FINISHED` |
| `targetDistanceMeters` | 목표 거리. 목표 없는 솔로는 `null` |
| `isServerConnected` | 폰↔서버 연결. `false`면 워치는 파티원만 흐리게 하고 `서버 연결 확인 중` |
| `isPenaltyApplicable` | 못 채우면 매칭 제한이 걸리는 방 |
| `me` | 폰 화면의 내 값. `currentPaceSecondsPerKm`은 폰처럼 30초 기준, 위치가 안 잡히면 `null`. `caloriesKcal`·`cadenceSpm`은 아직 없으면 `null` |
| `players` | **나를 뺀** 참가자. 서버 `players`는 나를 포함하지만, 워치는 내 값을 `me`에서만 읽게 하려고 뺐다 |
| `colorSlot` | 파티원 색 번호 1~3(0은 나). 폰 `PartyBoard.colorSlotOf` 값이고 러닝 중엔 바뀌지 않는다 |
| `gapMeters` | 상대 − 나. 양수면 상대가 앞. 모르면 `null` |
| `comboCount`·`maxComboCount` | 그 사람과 나 사이의 지금 콤보·최고 콤보. 콤보가 아니면 `comboCount` 0 |
| `playersAgeSeconds` | 파티원 값이 서버에서 온 지 몇 초 |
| `finish` | 종료 단계. `{"stage": "FINISHING"}`(서버 응답 대기) 또는 `{"stage": "GOAL_PENDING", "remainingMeters": 420}`(거리 모자람). 아니면 `null` |
| `result` | 끝난 결과. `{"isServerConfirmed", "totalDistanceMeters", "averagePaceSecondsPerKm", "elapsedSeconds"}` — `elapsedSeconds`는 멈춘 시간을 포함한 화면 시간이라 서버 `totalDurationSeconds`와 다르다 |

`WATCH_SYNCED`에만 있는 것:

| 필드 | 뜻 |
|---|---|
| `birthday` | 프로필 API 값 그대로(`yyyy-MM-dd`), 없으면 `null`. 워치가 심박 구간을 계산한다 |
| `photos` | `[{"userId", "jpegBase64"}]` — 파티원 사진을 128×128 JPEG로 줄인 것. 사진이 없는 사람은 뺀다 |

`WATCH_COMBO_TRIGGERED`의 `kind`는 `TICK`(1 오름)·`MILESTONE`(1·3·5·10·20… 도달)·`BREAK`(끊김)다.

## 보내는 규칙 — 폰

- `WATCH_STATE_UPDATED`는 워치 화면이 켜져 있고 러닝이 끝나기 전이면(일시정지 포함) 1초마다 보낸다. 앞 전송이 안 끝났으면 건너뛰고 최신 하나만.
- 일시정지·재개·종료 단계·결과 확정은 화면이 꺼져 있어도 바로 보낸다.
- 워치 명령을 반영하면 1초를 기다리지 않고 바로 보낸다(`statusVersion` +1, `lastRequestId`).
- `WATCH_COMBO_TRIGGERED`는 화면이 꺼져 있어도 보낸다 — 워치가 진동한다.
- `WATCH_VISIBILITY_UPDATE`(`isVisible: true`)를 받으면 러닝이 끝났어도 지금 상태를 바로 한 번 보낸다 — 화면이 꺼진 사이 놓친 결과 확정을 되찾는다.
- `WATCH_VISIBILITY_UPDATE`가 15초 없으면 화면이 꺼진 것으로 보고 1초 전송을 멈춘다.
- 거절(`ERROR`)은 러닝이 없거나 다른 방일 때(로그아웃·계정 전환 포함), `phoneInstanceId`·`statusVersion`이 다를 때만. 워치 명령은 폰 버튼과 같은 길로 처리한다.
- 폰에서 러닝을 시작하면 워치 앱을 켠다(메시지가 아니라 플랫폼 기능).

## 받는 규칙 — 워치

- SYNC를 보내면 SYNCED를 받을 때까지 버튼을 잠근다. 그사이 오는 상태는 버리고, 화면이 켜져 있으면 3초마다 SYNC를 다시 보낸다.
- 러닝 중 화면이 켜져 있는데 상태를 3초 넘게 못 받으면 `폰 연결 확인 중` — 받은 값 전부(시간 포함) 마지막 값에서 멈추고, 버튼을 잠그고, SYNC를 보낸다.
- `phoneInstanceId`가 바뀌었거나 모르는 `runningRoomId`가 오면 SYNC.
- 명령을 보내면 `lastRequestId`가 그 값인 상태나 같은 `requestId`의 `ERROR`가 올 때까지 버튼을 잠근다. 3초 넘게 없으면 SYNC를 보내고 명령은 다시 보내지 않는다.
- 결과 요약을 닫으면 그 방 번호를 기억해, 다시 연결돼도 다시 띄우지 않는다(폰에 알리지 않는다).
- 종료 뒤 `GOAL_PENDING`이 오면 한 번 진동한다.
