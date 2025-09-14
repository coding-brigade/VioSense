package com.example.attendancesystem.view.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.attendancesystem.databinding.RowItemAttendanceDetailsBinding
import com.example.attendancesystem.models.StudentInfo
import com.example.attendancesystem.utils.setText

class StudentInformationAdapter(private var arrayStudentDetailsL: ArrayList<StudentInfo>) : RecyclerView.Adapter<StudentInformationAdapter.Holder>() {
    
    inner class Holder(val binding: RowItemAttendanceDetailsBinding) : RecyclerView.ViewHolder(binding.root)
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(RowItemAttendanceDetailsBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val student = arrayStudentDetailsL[position]
        
        holder.binding.apply {
            setText(tvSem, student.semester)
            setText(tvDepartment, student.department)
            setText(tvDivision, student.division)
            setText(tvTotalStudent, student.totalStudents.toString())
            setText(tvPresentStudent, student.presentStudents.toString())
            setText(tvAbsentStudent, student.absentStudents.toString())
        }
    }
    
    override fun getItemCount(): Int = arrayStudentDetailsL.size
    
    
}