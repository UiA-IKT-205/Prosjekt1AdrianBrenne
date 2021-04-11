package com.example.myapplication.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.myapplication.R
import com.example.myapplication.data.TaskListViewModel
import java.util.*

class TaskListViewAdapter(private val dataSet: ArrayList<*>, mContext: Context) :
        ArrayAdapter<Any?>(mContext, R.layout.tasklist_row_item, dataSet) {

     class ViewHolder {
        lateinit var txtName: TextView
        lateinit var pbar: ProgressBar
        lateinit var progressText: TextView
    }
    override fun getCount(): Int {
        return dataSet.size
    }
    override fun getItem(position: Int): TaskListViewModel {
        return dataSet[position] as TaskListViewModel
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
            viewHolder.progressText =
                    convertView.findViewById(R.id.taskListPBarProgress)
            result = convertView
            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
            result = convertView
        }
        val item: TaskListViewModel = getItem(position)
        viewHolder.txtName.text = item.name
        viewHolder.pbar.progress = item.progress
        viewHolder.progressText.text = "${viewHolder.pbar.progress}%"

        return result
    }
}
