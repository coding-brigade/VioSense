package com.example.attendancesystem.api.factory

import androidx.lifecycle.*
import com.example.attendancesystem.api.repository.InferRepository
import com.example.attendancesystem.viewmodel.InferViewModel

class InferViewModelFactory(private val repository: InferRepository) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return InferViewModel(repository) as T
    }
    
}