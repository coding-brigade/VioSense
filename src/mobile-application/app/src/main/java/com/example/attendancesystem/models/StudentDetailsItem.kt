package com.example.attendancesystem.models

import java.io.Serializable

data class StudentDetailsItem(
    val detection_confidence: String? = null,
    val location_xyxy: List<Double>? = null,
    val name: String? = null,
    val recognition_confidence: String? = null,
    var status: Boolean = true
) : Serializable