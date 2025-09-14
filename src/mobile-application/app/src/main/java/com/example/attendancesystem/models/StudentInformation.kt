package com.example.attendancesystem.models

import java.io.Serializable

data class StudentInformation(
    var studentBatch: String? = null,
    var studentDepartment: String? = null,
    var studentSemester: String? = null,
    var studentDivision: String? = null
) : Serializable
