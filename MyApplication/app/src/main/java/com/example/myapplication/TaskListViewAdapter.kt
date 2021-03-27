package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.*

class TaskListViewAdapter(private val dataSet: ArrayList<*>, mContext: Context) :
        ArrayAdapter<Any?>(mContext, R.layout.tasklist_row_item, dataSet) {

    private class ViewHolder {
        lateinit var txtName: TextView
        lateinit var pbar: ProgressBar
        lateinit var delete: ImageButton
    }
    override fun getCount(): Int {
        return dataSet.size
    }
    override fun getItem(position: Int): TaskViewModel {
        return dataSet[position] as TaskViewModel
    }
    override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
    ): View {
        var convertView = convertView
        val viewHolder: ViewHolder
        val result: View
        if (convertView == null) {
            viewHolder = ViewHolder()
            convertView =
                    LayoutInflater.from(parent.context).inflate(R.layout.tasklist_row_item, parent, false)
            viewHolder.txtName =
                    convertView.findViewById(R.id.taskName)
            viewHolder.pbar =
                    convertView.findViewById(R.id.taskListPBar)
            viewHolder.delete =
                    convertView.findViewById(R.id.deleteTaskButton)
            result = convertView
            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
            result = convertView
        }
        val item: TaskViewModel = getItem(position)
        viewHolder.txtName.text = item.name
        viewHolder.pbar.progress = item.progress

        return result
    }
}