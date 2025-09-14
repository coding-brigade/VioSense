package com.example.attendancesystem.api.repository

import android.net.Uri
import android.util.Log
import com.example.attendancesystem.api.ApiRequestResponse
import com.example.attendancesystem.api.listener.*
import com.example.attendancesystem.models.*
import com.example.attendancesystem.utils.Constants.ExtraKey.PUBLIC_UPLOAD
import com.example.attendancesystem.utils.prepareFilePart
import com.google.gson.*
import okhttp3.*

class SubmitStudentListRepository(private val apiRequest: SubmitStudentListApiRequest?) : ApiRequestResponse() {
    
    suspend fun apiCallSubmitStudentList(studentList: ArrayList<StudentDetailsItem>) = apiRequest {
        apiRequest?.apiCallSubmitStudentList(studentList)
    }
}