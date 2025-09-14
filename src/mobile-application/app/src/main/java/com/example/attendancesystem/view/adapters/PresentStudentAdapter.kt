package com.example.attendancesystem.view.adapters

import android.annotation.SuppressLint
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import com.example.attendancesystem.R
import com.example.attendancesystem.databinding.RowItemPresentStudentBinding
import com.example.attendancesystem.models.StudentDetailsItem
import com.example.attendancesystem.utils.setText
import com.example.attendancesystem.view.listeners.OnItemClickListener

class PresentStudentAdapter(private val arrayPresentStudent: ArrayList<StudentDetailsItem>, private val listeners: OnItemClickListener) : RecyclerView.Adapter<PresentStudentAdapter.Holder>() {
    
    inner class Holder(val binding: RowItemPresentStudentBinding) : RecyclerView.ViewHolder(binding.root)
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(RowItemPresentStudentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
    
    @SuppressLint("ResourceAsColor")
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = arrayPresentStudent[position]
        holder.binding.apply {
            setText(tvStudentName, item.name)
            if (item.status) {
                setText(tvPresentOrAbsent, "Present")
                tvPresentOrAbsent.setTextColor(R.color.present_color)
                
            } else {
                setText(tvPresentOrAbsent, "Absent")
                tvPresentOrAbsent.setTextColor(R.color.absent_color)
            }
            
            root.setOnClickListener {
                listeners.onItemClick(position, item)
            }
        }
    }
    
    override fun getItemCount(): Int = arrayPresentStudent.size
}