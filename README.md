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
                    // 무조건 기본 생성자로 호출해줘야 함
                    User member = new User();
                    member.id = user.getUid();
                    member.token = refreshedToken;
                    member.email = user.getEmail();
                    Log.e("토큰", refreshedToken);
                    userRef.child(user.getUid()).setValue(member);
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

    - 직접 기기에서 기기로 메시지를 전달할 수 없고 현재기기에서 목적지가 되는 기기의 토큰 값(기기의 식별자)를 서버에 보내주면 서버가 그 토큰으로 원하는 기기에 메시지를 대신 전달해주는 과정을 거친다

    - 파이어베이스는 기기에서 직접 서버에 접근해서 전달하지 않도록 권고하고 있다. 토큰값을 탈취당할 수 있다는 이유에서이다. 따라서 개인 서버를 만들고 파이어베이스의 서버키를 가진 개인 서버만이 접근할 수 있도록 한다

![]()

### 3.1 가입할 때 유저 정보와 함께 토큰 값 데이터베이스에 저장
    - 토큰은 (파이어베이스) 서버에서 기기를 식별하기 위한 정보이다
    - 데이터베이스에 저장해 두고 토큰을 통해 통신할 수 있다. 

![]()

```java
String refreshedToken = FirebaseInstanceId.getInstance().getToken();
```

### 3.2 클라이언트에서 개인서버에 메시지 전달 요청
    - 1. 목적지 기기의 토큰 2. 전달하려는 메시지를 jsonString에 담아서 서버에 전달한다

```java
// 레트로핏 생성
Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://192.168.1.85:8090/")
        .build();
// 인터페이스와 결합
IRemote remote = retrofit.create(IRemote.class);
// 서비스로 서버 연결 준비
RequestBody requestBody = RequestBody.create(MediaType.parse("plain/text"), json);
Call<ResponseBody> call = remote.sendNotification(requestBody);
// 연결 후 데이터 비동기 처리
call.enqueue(new Callback<ResponseBody>() {
    @Override
    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
        if(response.isSuccessful()){
            String result = null;
            try {
                result = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(StorageActivity.this, result, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFailure(Call<ResponseBody> call, Throwable t) {
        Log.e("연결 실패", t.getMessage().toString());

    }
});
```

### 3.3 서버에서 토큰값과메시지를 받아서 msg 객체 생성, 파이어베이스 서버에 전달, 응답처리

    - 개인서버가 가진 정보
        - 파이어베이스 서버 접근 권한키
        - 파이어베이스 서버 주소
        - 전달할 메시지 객체


```javaScript
// request 모듈
var httpUrlConnection = require('request');
// fcm 설정 : 구글에서 미리 설정해 둔 서버
const fcmServerUrl = 'https://fcm.googleapis.com/fcm/send'; 
// 서버키
const serverKey = 'AAAAo5pdjEc:APA91bHZcE4vDXO1qSBhknzFGuKEVz0grnuhWF0n_o9fNUcrxr_EQM1QAdRvAfHsiGbIjPjH9fdsPoLqvBrnWDqmJGL2SJ3nN68hpeiTYth0o75KmDkRt5hiLO3ipWMQD9XBkTuMfkeX';
// 메시지
var msg = {
    to : '',
    notification : {
        title : '노티바 제목',
        body : '글자'
    }
}
```

```javaScript
var server = http.createServer((request, response)=>{
    // 1. 토큰값, 메시지를 받는다
    if(request.url == '/sendNotification'){
        var postData = '';
        request.on('data', (data)=>{
            postData += data;
        });
        // 2. 토큰값과 메시지를 꺼내서 메시지 객체로 완성
        request.on('end', ()=>{
            var postObj = JSON.parse(postData);
            msg.to = postObj.to;
            msg.notification.body = postObj.msg;
            // 3. reqeust 모듈을 통해 파이어베이스 서버에 연결
            httpUrlConnection(
                // http 메시지 객체
                {
                    url : fcmServerUrl,                     // 파이어베이스 서버 주소
                    method : 'POST',                        // 전달 방식
                    headers : {                             // 헤더(필수)
                        "Authorization" : "key="+serverKey, // 권한 - 서버 접근 권한
                        'Content-Type' : 'application/json' // 데이터 타입
                    },
                    body : JSON.stringify(msg)              // 메시지 객체를 JSON으로 담아서 전달
                },
                // 4. 응답
                (err, answer, body)=>{
                    var result = {
                        code : answer.statusCode,
                        msg : body
                    };
                    response.writeHead(200, {'Content-Type':'plain/text'});
                    response.end(JSON.stringify(result));
                }
            );
        });
    } else {
        response.end('404 page not found');
    }
});
```


### cf) 파이어베이스 개인 서버 작성(Function 사용)

- 1. npm install -g firebase-tools // <- -g로 설치하지 않으면 인식하지 못함
- 2. firebase login
- 3. firebase init -> firebase init functions -> 프로젝트 선택
- 4. function/index.js 파일에 addMessage 설정(반드시 function 아래 index.js 파일이어야 함)
- 5. firebase deploy --only functions:addMessage
- 6. Firebase Functions에 가서 https://us-central1-fir-basic2-caa20.cloudfunctions.net/addMessage 주소값 찾은 후 ?text=hello 주소로 요청
- 7. Database에 Message 노드 생성 확인
- 8. function/index.js 파일에 fcmServerUrl, serverKey, sendNotification()함수 설정

```javaScript
const fun = require('firebase-functions'); // <- firebase-functions 's'붙어야 함
const admin = require('firebase-admin');
admin.initializeApp(fun.config().firebase);

exports.addMessage = fun.https.onRequest((req, res)=>{
    // http 요청에서 ? 다음에 있는 변수 중에 text 변수 값을 가져온다
    var text = req.query.text;
    // 파이어베이스 db의 message 레퍼런스에 그 값을 넣는다
    admin.database.ref('/message')
        .push({msg:text})    // 받아온 값을 msg로 넣어준다
        .then(snapshot=>{   // 받아온 후
            res.redirect(30, snapshot.ref)
        });
});
```

### 3.4 전달된 메시지 처리 - 앱이 떠 있는 상태에서 메시지가 전달될 경우

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


### 3.5 토큰 관리 - 앱이 떠 있는 상태에서 메시지가 전달될 경우토근이 만료되어 갱신해야 하는 경우

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