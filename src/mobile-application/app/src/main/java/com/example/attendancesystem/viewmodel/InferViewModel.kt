package com.example.attendancesystem.viewmodel

import androidx.lifecycle.*
import com.example.attendancesystem.api.*
import com.example.attendancesystem.api.repository.InferRepository
import com.example.attendancesystem.models.Attachments
import com.google.gson.JsonObject
import okhttp3.RequestBody

class InferViewModel(private val repository: InferRepository) : ViewModel() {
    
    private val setUploadInferAttachmentApiResponse = MutableLiveData<ApiResponse?>()
    
    fun apiCallForUploadOInferAttachment(attachmentList: ArrayList<Attachments>, requestBody : RequestBody) {
        Coroutines.ioThenMain({ repository.apiCallForInferImageUpload(attachmentList, requestBody) }) { response -> setUploadInferAttachmentApiResponse.value = response }
    }
    
    val getUploadInferAttachmentApiResponse: LiveData<ApiResponse?> get() = setUploadInferAttachmentApiResponse
    
}