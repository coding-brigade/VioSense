package com.example.attendancesystem.api

import android.content.Context
import com.example.attendancesystem.App.Companion.gson
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(context: Context) {
    
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient
            .Builder()
            .addInterceptor(AuthenticationInterceptor(context))
            .connectTimeout(2, TimeUnit.MINUTES)
            .readTimeout(2, TimeUnit.MINUTES)
            .writeTimeout(2, TimeUnit.MINUTES)
            .build()
    }
    val retrofit: Retrofit by lazy {
        Retrofit
            .Builder()
            .baseUrl("http://192.168.126.112:5050/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
}