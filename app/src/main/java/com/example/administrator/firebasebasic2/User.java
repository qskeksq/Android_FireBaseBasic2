package com.example.administrator.firebasebasic2;

/**
 * Created by Administrator on 2017-10-31.
 */

public class User {

    public String id;
    public String token;
    public String email;

    public User() {

    }

    public User(String id, String token, String email) {
        this.id = id;
        this.token = token;
        this.email = email;
    }
}
