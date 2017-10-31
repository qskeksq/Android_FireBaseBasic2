package com.example.administrator.firebasebasic2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;

public class StorageActivity extends AppCompatActivity implements UserAdapter.Callback {

    private StorageReference mStorageRef;

    private UserAdapter adapter;

    FirebaseDatabase database;
    DatabaseReference userRef;
    private Button button4;
    private EditText editId;
    private Button button5;
    private TextView textView3;
    private TextView textView4;
    private TextView txtId;
    private TextView txtToken;
    private RecyclerView recycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage);


        mStorageRef = FirebaseStorage.getInstance().getReference();

        database = FirebaseDatabase.getInstance();
        userRef = database.getReference("users");

        initView();
    }

    public void chooseFile(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*"); // 갤러리 image/*, 동영상 video/*
        startActivityForResult(intent.createChooser(intent, "Select App"), 999);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Uri uri = data.getData();
//            String realPath = RealPathUtil.getRealPath(this, uri);
            upload(uri);
        }
    }

    public void upload(Uri file) {
        // 실제 파일이 있는 경로
//        Uri file = Uri.fromFile(new File(realPath));
        // 파이어베이스 스토리지 파일 node
        String[] temp = file.getPath().split("/");
        String filename = temp[temp.length - 1];
        StorageReference riversRef = mStorageRef.child("files/" + filename);

        riversRef.putFile(file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Toast.makeText(StorageActivity.this, "업로드 성공", Toast.LENGTH_SHORT).show();
                        @SuppressWarnings("VisibleForTests")
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

    private void initView() {
        button4 = (Button) findViewById(R.id.button4);
        editId = (EditText) findViewById(R.id.editId);
        button5 = (Button) findViewById(R.id.button5);
        textView3 = (TextView) findViewById(R.id.textView3);
        textView4 = (TextView) findViewById(R.id.textView4);
        txtId = (TextView) findViewById(R.id.txtId);
        txtToken = (TextView) findViewById(R.id.txtToken);
        recycler = (RecyclerView) findViewById(R.id.recycler);

        adapter = new UserAdapter(this);
        recycler.setAdapter(adapter);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<User> data = new ArrayList<User>();
                for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                    User user = new User();
                    String token = snapshot.getValue(String.class);
                    String id = snapshot.getKey();
                    user.token = token;
                    user.id = id;
                    data.add(user);
                }
                adapter.setDataAndRefresh(data);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void setIdAndToken(String id, String token) {
        txtId.setText(id);
        txtToken.setText(token);
    }
}
