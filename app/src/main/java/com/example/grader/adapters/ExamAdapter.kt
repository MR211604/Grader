package com.example.grader.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.grader.R
import com.example.grader.models.Exam

class ExamAdapter(
    private var examList: List<Exam>
) : RecyclerView.Adapter<ExamAdapter.ExamViewHolder>() {

    var onItemClick: ((Exam) -> Unit)? = null

    class ExamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtTitle: TextView = itemView.findViewById(R.id.txtExamTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExamViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exam, parent, false)
        return ExamViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExamViewHolder, position: Int) {
        val exam = examList[position]

        holder.txtTitle.text = exam.title

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(exam)
        }
    }

    override fun getItemCount(): Int = examList.size

    fun setData(newList: List<Exam>) {
        examList = newList
        notifyDataSetChanged()
    }
}