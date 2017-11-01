package com.example.administrator.firebasebasic2;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Created by Administrator on 2017-11-01.
 */

public interface IRemote {

    // 리턴타임 함수명
    @POST("sendNotification")
    Call<ResponseBody> sendNotification(@Body RequestBody postData);

}
