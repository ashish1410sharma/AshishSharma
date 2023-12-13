package com.ashishsharma.appupdate

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming


interface MyApi {
    @GET("{filename}")
    @Streaming
    fun downloadFile(@Path("filename") filename:String): Call<ResponseBody>
}