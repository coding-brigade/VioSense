package com.example.attendancesystem.api.listener

import com.example.attendancesystem.models.StudentDetailsItem
import retrofit2.Response
import retrofit2.http.*

interface SubmitStudentListApiRequest {
    
    @POST("submit_students")
    suspend fun apiCallSubmitStudentList(@Body studentList: ArrayList<StudentDetailsItem>): Response<Any?>?
    
}