package com.example.attendancesystem.api

import android.util.Log
import com.example.attendancesystem.App.Companion.app
import com.example.attendancesystem.App.Companion.gson
import com.example.attendancesystem.R
import com.example.attendancesystem.models.ErrorBody
import com.example.attendancesystem.utils.getData
import com.google.gson.JsonObject
import retrofit2.Response

abstract class ApiRequestResponse {
    
    suspend fun <T : Any?> apiRequest(call: suspend () -> Response<T>?): ApiResponse? {
        val response = call.invoke()
        
        Log.d("API>>", "REQUEST>>CODE>> ${gson.toJson(response?.code())}")
        Log.d("API>>", "REQUEST>>METHOD>> ${gson.toJson(response?.raw()?.request?.method)}")
        Log.d("API>>", "REQUEST>>URL>> ${gson.toJson(response?.raw()?.request?.url?.toUrl())}")
        Log.d("API>>", "REQUEST>>STATUS_CODE>> ${response?.code()}")
        Log.d("API>>", "REQUEST>>RESPONSE>> ${gson.toJson(response?.body())}")
        /*Log.d("API>>", "REQUEST>>ERROR>> ${getData(response?.errorBody()?.charStream(), JsonObject::class.java)?.get("message")?.asString}")
        Log.d("API>>", "REQUEST>>ERROR>> ${getData(response?.errorBody()?.string(), JsonObject::class.java)?.get("message")?.asString}")*/
        
        return response?.let {
            val jsonObject = getData(response.body(), JsonObject::class.java)
            val apiResponse = ApiResponse()
            apiResponse.statusCode = response.code()
            apiResponse.type = jsonObject?.get("type")?.asString
            
            if (response.isSuccessful) {
                apiResponse.data = jsonObject?.get("data")
                apiResponse.message = jsonObject?.get("message")?.asString
                apiResponse
                
            } else {
                apiResponse.type = app?.getString(R.string.error)
                when (response.code()) {
                    404 -> {
                        apiResponse.message = " 404 Not Found "
                    }
                    
                    500 -> {
                        apiResponse.message = "Internal server error"
                    }
                    
                    502 -> {
                        apiResponse.message = "Server is under maintenance"
                    }
                    
                    else -> {
                        val errorStream = "${getData(response.errorBody()?.charStream(), ErrorBody::class.java)?.message}"
                        val errorString = "${getData(response.errorBody()?.string(), ErrorBody::class.java)?.message}"
                        
                        apiResponse.message = if (errorStream.equals("null", true)) {
                            if (errorString.equals("null", true)) {
                                app?.getString(R.string.e_something_went_wrong)
                                
                            } else {
                                errorString
                            }
                            
                        } else {
                            errorStream
                        }
                    }
                }
                
                apiResponse
            }
        } ?: return null
        
    }
}