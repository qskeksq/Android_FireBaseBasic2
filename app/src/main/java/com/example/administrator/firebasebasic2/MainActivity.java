package com.example.administrator.firebasebasic2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseDatabase database;
    private DatabaseReference userRef;

    private String TAG = getClass().getSimpleName().toString();
    private EditText signUpEmail;
    private EditText signUpPw;
    private EditText signUpPwChk;
    private EditText signInEmail;
    private EditText signInPw;

    private boolean isValidEmail, isValidPw, isSamePassword;
    private TextView isValidEmailTxt;
    private TextView isValidPwTxt;
    private TextView isSamePwTxt;

    private SharedPreferences sp;
    private SharedPreferences.Editor editor;

    private String refreshedToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        userRef = database.getReference("users");

        initView();
        setListener();
    }

    private void initView() {
        signUpEmail = (EditText) findViewById(R.id.signUpEmail);
        signUpPw = (EditText) findViewById(R.id.signUpPw);
        signUpPwChk = (EditText) findViewById(R.id.signUpPwChk);
        signInEmail = (EditText) findViewById(R.id.signInEmail);
        signInPw = (EditText) findViewById(R.id.signInPw);

        isValidEmailTxt = (TextView) findViewById(R.id.isValidEmailTxt);
        isValidPwTxt = (TextView) findViewById(R.id.isValidPwTxt);
        isSamePwTxt = (TextView) findViewById(R.id.isSamePwTxt);
    }

    private void setListener() {
        // 가입 이메일 확인
        signUpEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                isValidEmail = isValidEmailCheck(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!isValidEmail) {
                    isValidEmailTxt.setVisibility(View.VISIBLE);
                } else {
                    isValidEmailTxt.setVisibility(View.GONE);
                }
            }
        });

        // 가입 비밀번호 확인
        signUpPw.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                isValidPw = isValidPasswordCheck(charSequence.toString());
                isSamePassword = isSamePasswordCheck(charSequence.toString(), signUpPwChk.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!isValidPw) {
                    isValidPwTxt.setVisibility(View.VISIBLE);
                } else {
                    isValidPwTxt.setVisibility(View.GONE);
                }

                if (!isSamePassword) {
                    isSamePwTxt.setVisibility(View.VISIBLE);
                } else {
                    isSamePwTxt.setVisibility(View.GONE);
                }
            }
        });

        // 비밀번호 일치 확인
        signUpPwChk.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                isSamePassword = isSamePasswordCheck(signUpPw.getText().toString(), charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!isSamePassword) {
                    isSamePwTxt.setVisibility(View.VISIBLE);
                } else {
                    isSamePwTxt.setVisibility(View.GONE);
                }
            }
        });
    }

    /**
     * 서버, DB, 스트림, 연결 관련은 반드시 생명주기를 관리해 줘야 한다다
     */
    @Override
    public void onStart() {
        super.onStart();
        // 이미 로그인 되어 있으면 사용자를 파이어베이스에서 가져온다
        currentUser = mAuth.getCurrentUser();
    }

    /**
     * 회원가입
     */
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
                // 완료 확인 리스너
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
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
                            String refreshedToken = FirebaseInstanceId.getInstance().getToken();
                            Log.e("토큰", refreshedToken);
                            userRef.child(user.getUid()).setValue(refreshedToken);
                        }
                    }
                });
    }

    /**
     * 로그인
     */
    public void signIn(View view) {

        String email = signInEmail.getText().toString();
        String password = signInPw.getText().toString();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if(user.isEmailVerified()){
                                Toast.makeText(MainActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(MainActivity.this, StorageActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(MainActivity.this, "이메일을 확인해주세요", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "로그인 실패", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * 사용자정보
     */
    public void getUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Name, email address, and profile photo Url
            String name = user.getDisplayName();
            String email = user.getEmail();
            Uri photoUrl = user.getPhotoUrl();
            String uid = user.getUid();

            // 이메일 검증 완료 여부
            boolean emailVerified = user.isEmailVerified();
        }
    }

    /**
     * 로그인 요휴 처리
     */
    public static boolean isValidEmailCheck(String email) {
        /*
            [_Aa-sas9-].[Ad8-] * @
            []는 임의로 덩어리로 붙여준 것
            *의 의미는 samsung @ 가 될 수도 있고 samsung.go@ 될 수도 있다는 말

            samsung @ ssd.samsumg.com
            samsung.go @ ssd.samsung.com

            samsung @ samsung.com
            samsung.go @ samsung.com
         */

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

    /**
     * 비밀번호 요휴 처리
     */
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

    /**
     * 비밀번호 이중 체크
     */
    private boolean isSamePasswordCheck(String pw1, String pw2) {
        return pw1.equals(pw2);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

}
