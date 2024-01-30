package com.example.zegovideocalldemo.data;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface ApiInterface {

    @Headers("Content-Type: application/json")
    @GET("/")
    Call<ResAiEffectData> getEffectData(
            @Query("Action") String action,
            @Query("AppId") int appId,
            @Query("AuthInfo") String authInfo
    );
}
