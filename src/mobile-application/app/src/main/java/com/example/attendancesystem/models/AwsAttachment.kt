package com.example.attendancesystem.models

import java.io.Serializable

data class AwsAttachment(
    var awsUrl: String? = null,
    var attachmentId: String? = null
) : Serializable