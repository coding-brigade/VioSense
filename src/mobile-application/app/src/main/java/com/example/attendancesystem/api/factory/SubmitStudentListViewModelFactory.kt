package com.example.attendancesystem.api.factory

import androidx.lifecycle.*
import com.example.attendancesystem.api.repository.*
import com.example.attendancesystem.viewmodel.*

class SubmitStudentListViewModelFactory(private val repository: SubmitStudentListRepository) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SubmitStudentListViewModel(repository) as T
    }
    
}