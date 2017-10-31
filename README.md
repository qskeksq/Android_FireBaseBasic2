# FireBase AuthenTication, Storage, FCM, 보안 규칙
- 파이어베이스 로그인 인증처리
- 이미지 파일 업로드
- 실시간 데이터베이스
- 클라우드 메시징

## (1) 로그인, 인증처리(Firebase Authentication)

### onCreate
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    mAuth = FirebaseAuth.getInstance();
}
```


### onStart
- 서버, DB, 스트림, 연결 관련은 반드시 생명주기를 관리해 줘야 한다다

```java
@Override
public void onStart() {
    super.onStart();
    // 이미 로그인 되어 있으면 사용자를 파이어베이스에서 가져온다
    currentUser = mAuth.getCurrentUser();
}
```

### 회원가입

- 1. 이메일 형식 체크
- 2. 비밀번호 형식 체크
- 3. 비밀번호 확인
- FireBase sendEmailVerification()를 통해 이메일 인증

```java
public void signUp(View view) {

    String email = signUpEmail.getText().toString();
    String password = signUpPw.getText().toString();

    // 이메일 형식 체크
    if (!isValidEmail) {
        Toast.makeText(this, "이메일 형식이 잘못되었습니다", Toast.LENGTH_SHORT).show();
        return;
    } else if(!isValidPw){
        Toast.makeText(this, "비밀번호 형식이 잘못되었습니다", Toast.LENGTH_SHORT).show();
        return;
    } else if(!isSamePassword){
        Toast.makeText(this, "2차 비밀번호가 잘못되었습니다", Toast.LENGTH_SHORT).show();
        return;
    }

    // 파이어베이스 인증모듈로 사용자를 생성
    mAuth.createUserWithEmailAndPassword(email, password)
        // 유저 생성 완료 확인 리스너
        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    // 1. 유저가 생성되면 유저에게 인증 메일을 보내준다
                    final FirebaseUser user = mAuth.getCurrentUser();
                    user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            // 이메일 인증
                            if (task.isSuccessful()) {
                                Toast.makeText(MainActivity.this, "이메일이 발송되었습니다", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this, "이메일 발송 실패", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    // 2. 생성 완료된 유저를 데이터베이스에 저장한다
                    // sendEmailVerification()의 addOnCompleteListener()로 들어가면 데이터베이스에 입력이 안 됨
                    String refreshedToken = FirebaseInstanceId.getInstance().getToken();
                    Log.e("토큰", refreshedToken);
                    userRef.child(user.getUid()).setValue(refreshedToken);
                }
            }
        });
}
```

### 로그인
-입력된 정보와 mAuth.getCurrentUser() Firebase 서버의 정보 비교

```java
public void signIn(View view) {

    String email = signUpEmail.getText().toString();
    String password = signUpPw.getText().toString();

    mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        // 로그인 하기 전에 생성된 유저가 이메일 인증을 마쳤는지 확인한다
                        FirebaseUser user = mAuth.getCurrentUser();
                        if(user.isEmailVerified()){
                            Toast.makeText(MainActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "이메일을 확인해주세요", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "로그인 실패", Toast.LENGTH_SHORT).show();
                    }
                }
            });
}
```
### 로그인 유효처리

```java
public static boolean isValidEmailCheck(String email) {
    boolean err = false;
    // ^ 텍스트 시작
    // _허용, a-z허용, 0-9허용, -허용(^는 불허) : 반드시 문자가 하나 들어간다
    // .허용
    // _허용, a-z허용, 0-9허용, -허용 : 없을 수도 있고 무한히 많을 수도 있다
    // * @앞의 문자가 무한정 많을 수도 없을 수도 있음, @허용
    // \\w 영문자 허용
    // $ 텍스트 종료
    String regex = "^[_a-z0-9-]+(.[_a-z0-9-]+)*@(?:\\w+\\.)+\\w+$";
    Pattern p = Pattern.compile(regex);
    Matcher m = p.matcher(email);
    if (m.matches()) {
        err = true;
    }
    return err;
}
```

### 비밀번호 유효처리

```java
public static boolean isValidPasswordCheck(String password) {
    boolean err = false;
    // 영문자와 숫자만 허용
    // {n, } n 이상
    String regex = "^[a-zA-Z0-9]{6,}$";

    Pattern p = Pattern.compile(regex);
    Matcher m = p.matcher(password);
    if (m.matches()) {
        err = true;
    }
    return err;
}
```
```java
private boolean isSamePasswordCheck(String pw1, String pw2) {
    return pw1.equals(pw2);
}
```

## (2) 저장소(Firebase Storage)


```java
mStorageRef = FirebaseStorage.getInstance().getReference();
```

### 파일 선택

```java
public void chooseFile(View view){
    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    intent.setType("audio/*"); // 갤러리 image/*, 동영상 video/*, 음악 audio/*
    startActivityForResult(intent.createChooser(intent, "Select App"), 999);
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if(resultCode == RESULT_OK){
        Uri uri = data.getData();
        upload(uri);
    }
}
```

### 업로드

```java
public void upload(Uri file) {
    // 결국 파이어베이스에서 파일을 저장소에 넘길 때는 Uri 객체가 필요하다
    // 데이터를 받아올 때도 Uri, Url 두 가지로 받아올 수 있다)
    String[] temp = file.getPath().split("/");
    String filename = temp[temp.length-1];
    // 저장될 노트 설정
    riversRef = mStorageRef.child("files/"+filename);
    // Uri 객체만 putFile()에 넘겨주면 된다
    riversRef.putFile(file)
            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Toast.makeText(StorageActivity.this, "업로드 성공", Toast.LENGTH_SHORT).show();
                    // 2.+ 대 버전에서는 추가 예외처리를 해줘야 한다
                    @SuppressWarnings("VisibleForTests")
                    // 
                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Toast.makeText(StorageActivity.this, "업로드 실패", Toast.LENGTH_SHORT).show();
                }
            });
}
```

## (3) 클라우드 메시징

### 토근 값 가져오기 & 데이터베이스에 저장

- 데이터베이스에 저장해 두고 토큰을 통해 통신할 수 있다

```java
// 토큰 값 받아오기
String refreshedToken = FirebaseInstanceId.getInstance().getToken();
// 데이터베이스에 저장
userRef.child(user.getUid()).setValue(refreshedToken);
```

### 앱이 떠 있는 상태에서 메시지가 전달될 경우

- onMessageReceived() 호출
- 앱이 떠 있지 않은 상태에서 메시지가 전달되면 Noti로 전달됨

```java
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    /**
     * 내 앱이 화면에 현재 떠있으면 noti 가 전송됬을 때 이 함수가 호출
     * 떠 있지 않으면 noti 로 날아감
     * @param remoteMessage
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            if (remoteMessage.getData().size() > 0) {
                // 여기서 노티피케이션 메시지를 받아서 처리
                // 그대로 Noti를 띄워줄 수 있다.
                sendNotification(remoteMessage);
            }

            if(remoteMessage.getNotification() != null){

            }
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    private void sendNotification(String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId = "default channel";

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setContentTitle("FCM Message")
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}
```


### 토근이 만료되어 갱신해야 하는 경우

- onTokenRefresh()가 호출되고, 여기서 기존에 데이터베이스에 저장해서 사용하던 토큰을 갱신해 줄 수 있다

```java
public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    private static final String TAG = "MyFirebaseIIDService";

    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        sendRegistrationToServer(refreshedToken);
    }

    private void sendRegistrationToServer(String token) {
        // 내 데이터베이스의 사용자 token 값을 여기서 갱신
//        String user_node = "user/사용자아이디/token";
//        user_node.setValue(token);

    }
}
```

