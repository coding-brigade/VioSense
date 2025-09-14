package com.example.attendancesystem

import android.app.Application
import com.google.gson.*

class App : Application() {
    
    companion object {
        
        var app: App? = null
        val gson: Gson
            get() {
                return GsonBuilder().setLenient().create()
            }
    }
    
    override fun onCreate() {
        super.onCreate()
        init()
    }
    
    private fun init() {
        app = this@App
    }
    
}