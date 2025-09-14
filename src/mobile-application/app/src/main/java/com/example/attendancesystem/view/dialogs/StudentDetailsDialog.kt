package com.example.attendancesystem.view.dialogs

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import com.example.attendancesystem.R
import com.example.attendancesystem.databinding.FragmentStudentDetailsDialogBinding
import com.example.attendancesystem.models.StudentInformation
import com.example.attendancesystem.utils.toast
import com.example.attendancesystem.view.listeners.OnItemClickListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class StudentDetailsDialog(private val listeners: OnItemClickListener) : BottomSheetDialogFragment() {
    
    private lateinit var binding: FragmentStudentDetailsDialogBinding
    
    // Dummy data
    private val batchList = listOf("Batch*", "2021-2022", "2022-2023", "2023-2024", "2024-2025")
    private val departmentList = listOf("Department*", "Computer Engineering", "Information Technology", "Mechanical", "Civil")
    private val semesterList = listOf("Semester*", "Semester 1", "Semester 2", "Semester 3", "Semester 4", "Semester 5", "Semester 6", "Semester 7", "Semester 8")
    private val classList = listOf("Class*", "Class A", "Class B", "Class C", "Class D")
    
    // Selection tracking
    private var intPickBatch = -1
    private var intPickDepartment = -1
    private var intPickSemester = -1
    private var intPickDivision = -1
    private var studentBatch: String = ""
    private var studentDepartment: String = ""
    private var studentSemester: String = ""
    private var studentDivision: String = ""
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentStudentDetailsDialogBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setAdapters()
        onClicks()
    }
    
    private fun setAdapters() {
        binding.apply { // Batch
            spPickBatch.adapter = object : ArrayAdapter<String>(requireContext(), R.layout.row_item_spinner, batchList) {
                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getDropDownView(position, convertView, parent)
                    view.setBackgroundColor(if (position == intPickBatch) ContextCompat.getColor(requireContext(), R.color.blueMediumLight)
                    else ContextCompat.getColor(requireContext(), R.color.white))
                    return view
                }
            }
            
            spPickBatch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    intPickBatch = position
                    studentBatch = batchList[position]
                }
                
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            } // Department
            spPickDepartment.adapter = object : ArrayAdapter<String>(requireContext(), R.layout.row_item_spinner, departmentList) {
                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getDropDownView(position, convertView, parent)
                    view.setBackgroundColor(if (position == intPickDepartment) ContextCompat.getColor(requireContext(), R.color.blueMediumLight)
                    else ContextCompat.getColor(requireContext(), R.color.white))
                    return view
                }
            }
            
            spPickDepartment.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    intPickDepartment = position
                    studentDepartment = departmentList[position]
                }
                
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            } // Semester
            spPickSemester.adapter = object : ArrayAdapter<String>(requireContext(), R.layout.row_item_spinner, semesterList) {
                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getDropDownView(position, convertView, parent)
                    view.setBackgroundColor(if (position == intPickSemester) ContextCompat.getColor(requireContext(), R.color.blueMediumLight)
                    else ContextCompat.getColor(requireContext(), R.color.white))
                    return view
                }
            }
            
            spPickSemester.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    intPickSemester = position
                    studentSemester = semesterList[position]
                }
                
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            } // Division
            spPickDivision.adapter = object : ArrayAdapter<String>(requireContext(), R.layout.row_item_spinner, classList) {
                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getDropDownView(position, convertView, parent)
                    view.setBackgroundColor(if (position == intPickDivision) ContextCompat.getColor(requireContext(), R.color.blueMediumLight)
                    else ContextCompat.getColor(requireContext(), R.color.white))
                    return view
                }
            }
            
            spPickDivision.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    intPickDivision = position
                    studentDivision = classList[position]
                }
                
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }
    
    private fun onClicks() {
        binding.btnSubmit.setOnClickListener {
            Log.d("TAG", "onClicks: $studentDivision $studentSemester $studentBatch $studentDepartment ")
            if (isValidData()) {
                val studentInfo = StudentInformation(studentBatch = studentBatch, studentDepartment = studentDepartment, studentSemester = studentSemester, studentDivision = studentDivision)
                listeners.onItemClick(studentInfo)
                
            } else {
                Toast.makeText(requireContext(), "Please select all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun isValidData(): Boolean {
        if (studentBatch.contains("*")) {
            requireActivity().toast("Please Select Batch")
            return false
        } else if (studentDepartment.contains("*")) {
            requireActivity().toast("Please Select Department")
            return false
        } else if (studentSemester.contains("*")) {
            requireActivity().toast("Please Select Semester")
            return false
        } else if (studentDivision.contains("*")) {
            requireActivity().toast("Please Select Class")
            return false
        } else {
            return true
        }
    }
    
}
