package com.example.attendancesystem.models

import java.io.Serializable

data class ErrorBody(
    var status: String? = null,
    var title: String? = null,
    var message: String? = null,
    var messageCode: String? = null
) : Serializable