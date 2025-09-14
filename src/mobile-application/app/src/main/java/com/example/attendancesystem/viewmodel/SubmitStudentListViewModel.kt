package com.example.attendancesystem.viewmodel

import androidx.lifecycle.*
import com.example.attendancesystem.api.*
import com.example.attendancesystem.api.repository.SubmitStudentListRepository
import com.example.attendancesystem.models.StudentDetailsItem

class SubmitStudentListViewModel(private val repository: SubmitStudentListRepository) : ViewModel() {
    
    private val setSubmitStudentListAttachmentApiResponse = MutableLiveData<ApiResponse?>()
    
    fun apiCallForSubmitStudentList(studentList: ArrayList<StudentDetailsItem>) {
        Coroutines.ioThenMain({ repository.apiCallSubmitStudentList(studentList) }) { response -> setSubmitStudentListAttachmentApiResponse.value = response }
    }
    
    val getSubmitStudentListApiResponse: LiveData<ApiResponse?> get() = setSubmitStudentListAttachmentApiResponse
    
}