# FCM(Firebase Cloud Messaging) 연동 가이드

본 문서는 Catchmate 프로젝트에서 FCM을 연동하고 사용하는 방법에 대해 설명합니다.

## 1. Firebase 프로젝트 설정

1. [Firebase Console](https://console.firebase.google.com/)에서 프로젝트를 생성합니다.
2. **프로젝트 설정 > 서비스 계정** 탭으로 이동합니다.
3. **새 비공개 키 생성** 버튼을 클릭하여 JSON 키 파일을 다운로드합니다.
4. 다운로드한 파일의 이름을 `firebase-adminsdk.json`으로 변경합니다.
5. 해당 파일을 프로젝트의 `src/main/resources/` 디렉토리에 배치합니다.
   - **주의**: 이 파일에는 민감한 정보가 포함되어 있으므로, 보안에 유의해야 합니다. (현재 `.gitignore`에 포함되어 있는지 확인 필요)

## 2. 의존성 추가

`build.gradle` 파일에 다음과 같은 의존성이 추가되어 있습니다.

```gradle
dependencies {
    // FCM
    implementation 'com.google.firebase:firebase-admin:9.3.0'
}
```

## 3. 설정 및 초기화

### FirebaseConfig
`src/main/java/com/back/catchmate/global/config/cloud/FirebaseConfig.java` 클래스에서 애플리케이션 시작 시 Firebase SDK를 초기화합니다.

```java
@Configuration
public class FirebaseConfig {
    @PostConstruct
    public void initialize() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                return;
            }
            InputStream serviceAccount = new ClassPathResource("firebase-adminsdk.json").getInputStream();
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            FirebaseApp.initializeApp(options);
        } catch (IOException e) {
            log.error("Firebase init failed", e);
        }
    }
}
```

## 4. 알림 전송 로직

### FcmNotificationSender
알림 전송은 `FcmNotificationSender` 클래스에서 담당하며, `NotificationSenderPort` 인터페이스를 구현합니다.

- **기능**: 사용자가 오프라인인 경우에만 FCM 알림을 전송합니다.
- **재시도 메커니즘**: `@Retryable` 어노테이션을 사용하여 전송 실패 시 최대 3번까지 재시도합니다.
- **영구 실패 처리**: 유효하지 않은 토큰 등 영구적인 실패(`UNREGISTERED`, `INVALID_ARGUMENT` 등) 발생 시 `PermanentNotificationFailureException`을 던져 재시도를 중단합니다.

## 5. 알림 전송 흐름 (Transactional Outbox)

Catchmate는 알림의 신뢰성을 보장하기 위해 **Transactional Outbox** 패턴을 사용합니다.

1. **비즈니스 로직 실행**: 게시글 작성, 채팅 메시지 전송 등의 이벤트가 발생합니다.
2. **이벤트 및 Outbox 저장**: 동일한 트랜잭션 내에서 `NotificationEvent`가 발행되고, 알림 정보가 `notification_outbox` 테이블에 저장됩니다.
3. **이벤트 리스너**: `@TransactionalEventListener(phase = AFTER_COMMIT)`를 통해 트랜잭션 성공 후 알림 처리를 시작합니다.
4. **알림 발송**: `OutboxDispatcher`가 `FcmNotificationSender`를 호출하여 실제 푸시 알림을 전송합니다.
5. **상태 업데이트**: 전송 성공 시 Outbox 상태를 `PROCESSED`로 업데이트합니다.

## 6. 클라이언트 토큰 발급 및 등록

FCM 토큰은 클라이언트(Web, Android, iOS)에서 Firebase SDK를 사용하여 직접 발급받아야 합니다.

### 6.1 클라이언트에서 토큰 발급 (Web 예시)
Web 환경에서는 `firebase/messaging` 라이브러리의 `getToken` 함수를 사용하여 토큰을 발급받습니다.

```javascript
import { getMessaging, getToken } from "firebase/messaging";

const messaging = getMessaging();
getToken(messaging, { vapidKey: 'YOUR_VAPID_KEY' })
  .then((currentToken) => {
    if (currentToken) {
      console.log('FCM Token:', currentToken);
      // 서버에 토큰 등록 API 호출
    } else {
      console.log('No registration token available. Request permission to generate one.');
    }
  }).catch((err) => {
    console.log('An error occurred while retrieving token. ', err);
  });
```

### 6.2 서버에 토큰 등록
클라이언트에서 발급받은 토큰은 백엔드 API를 통해 저장해야 알림을 보낼 수 있습니다.

- **Endpoint**: `PUT /api/users/me/fcm-token`
- **Method**: `PUT`
- **Request Body**:
  ```json
  {
    "fcmToken": "발급받은_토큰_문자열"
  }
  ```

## 7. 사용 방법 (백엔드)

새로운 알림 이벤트를 추가하려면 다음과 같이 진행합니다.

1. `NotificationEvent`를 상속받거나 활용하여 이벤트를 정의합니다.
2. 필요한 도메인 서비스에서 `ApplicationEventPublisher`를 통해 이벤트를 발행합니다.
3. `NotificationEventListener`에서 해당 이벤트를 구독하고 알림 저장 및 발송 로직을 호출합니다.

```java
// 예시: 알림 이벤트 발행
eventPublisher.publishEvent(new NotificationEvent(userId, title, content, alarmType, data));
```

## 7. 모니터링

`FcmNotificationSender`는 알림 전송 실패 시 `notification.fcm.send.failure` 메트릭을 기록합니다. 이를 통해 알림 전송 상태를 모니터링할 수 있습니다.
