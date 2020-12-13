package com.example.time

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SchedulesAdapter(private val context: Context, private val schedules: ArrayList<Schedule>) : RecyclerView.Adapter<SchedulesAdapter.SchedulesViewHolder>() {
    class SchedulesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val startTime: TextView = view.findViewById(R.id.startTime)
        val endTime: TextView = view.findViewById(R.id.endTime)
        val title: TextView = view.findViewById(R.id.title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SchedulesViewHolder =
        SchedulesViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.schedule, parent, false))

    override fun getItemCount(): Int = schedules.size

    override fun onBindViewHolder(holder: SchedulesViewHolder, position: Int) {
        holder.title.text = schedules[position].title
        if (schedules[position].allDay) {
            holder.startTime.text = "終日"
            holder.endTime.text = "00:00"
            holder.endTime.visibility = View.INVISIBLE
        } else {
            holder.startTime.text = schedules[position].startTime
            holder.endTime.text = schedules[position].endTime
        }
    }
}
