package com.example.attendancesystem.view.listeners

interface OnItemClickListener {
    
    fun onItemClick(value: Any?) {}
    
    fun onItemClick(key: Any?, value: Any?) {}
    
    fun onCardClick(key: Any?, value: Any?) {}
    
}