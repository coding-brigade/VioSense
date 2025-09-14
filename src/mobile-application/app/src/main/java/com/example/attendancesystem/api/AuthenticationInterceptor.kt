package com.example.attendancesystem.api

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.attendancesystem.utils.*
import com.example.attendancesystem.utils.Constants.Api.AUTHORIZATION
import com.example.attendancesystem.utils.Constants.Preference.LOGIN_CODE
import com.example.attendancesystem.utils.Constants.Preference.TOKEN
import okhttp3.*

class AuthenticationInterceptor(private val context: Context) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = context.prefManager().getStringPreference(TOKEN)
        Log.d("API>>", "REQUEST>>HEADER>>TOKEN>> Authorization:$token")
        val originalRequest = chain.request().newBuilder().header(AUTHORIZATION, token).header(LOGIN_CODE, "No Need").build()
        val response = chain.proceed(originalRequest)
        
        if (response.code == 401) {
            (context as Activity).logout()
        }
        
        return response
    }
    
}