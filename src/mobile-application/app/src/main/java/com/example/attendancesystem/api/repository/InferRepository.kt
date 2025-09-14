package com.example.attendancesystem.api.repository

import android.net.Uri
import android.util.Log
import com.example.attendancesystem.api.ApiRequestResponse
import com.example.attendancesystem.api.listener.InferApiRequest
import com.example.attendancesystem.models.Attachments
import com.example.attendancesystem.utils.Constants.ExtraKey.PUBLIC_UPLOAD
import com.example.attendancesystem.utils.prepareFilePart
import com.google.gson.*
import okhttp3.*

class InferRepository(private val apiRequest: InferApiRequest?) : ApiRequestResponse() {
    
    suspend fun apiCallForInferImageUpload(attachmentList: ArrayList<Attachments>, requestBody: RequestBody) = apiRequest {
        val attachment = ArrayList<MultipartBody.Part>()
        
        if (attachmentList.isNotEmpty()) {
            attachmentList.forEach { item ->
                if (item.path?.contains(PUBLIC_UPLOAD) == false) {
                    attachment.add(prepareFilePart(Uri.parse(item.path)))
                }
            }
        }
        Log.d("TAG", "apiCallForInferImageUpload>> ${Gson().toJson(attachment)} ")
        apiRequest?.apiCallForInferImage(attachments = attachment.ifEmpty { null }, requestBody)
    }
}