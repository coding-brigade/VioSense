package com.example.attendancesystem.api.listener

import okhttp3.*
import retrofit2.Response
import retrofit2.http.*

interface InferApiRequest {
    
    @Multipart
    @POST("infer")
    suspend fun apiCallForInferImage(@Part attachments: List<MultipartBody.Part>? = null, @Part("studentInfo") studentInfo: RequestBody? = null): Response<Any?>?
    
}