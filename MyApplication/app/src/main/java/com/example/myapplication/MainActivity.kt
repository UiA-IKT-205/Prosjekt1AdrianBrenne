package com.example.myapplication

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.*



class MainActivity : AppCompatActivity() {
    private val taskList = mutableListOf<String>()
    private val taskMap = mutableMapOf<Int,String>()
    private var keyList = mutableSetOf<String>()
    private var valueList = mutableSetOf<String>()
    private val adapter by lazy { makeAdapter(taskList) }
    private var doesFileExist = false
    private var task_id = 0

    private val ADD_TASK_REQUEST = 1
    private val ADD_ELEMENT_REQUEST = 2


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        taskListView.adapter = adapter
        println(taskMap)
        createMapOnStartup()
        loadElements()

        taskListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, id ->
                taskSelected(position,id)
            }


    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_TASK_REQUEST) {

            if (resultCode == Activity.RESULT_OK) {

                val task = data?.getStringExtra(TaskDescriptionActivity.EXTRA_TASK_DESCRIPTION)
                task?.let {
                    saveTaskId()
                    taskMap[task_id] = task
                    taskList.add(task)
                    createFile()
                    saveMap(task_id, task)
                    adapter.notifyDataSetChanged()
                }
            }
        }

    }

    fun saveTaskId(){
        task_id += 1
        getSharedPreferences("my_save", Activity.MODE_PRIVATE).edit().putInt("task_id", task_id).apply()
    }


    fun addTaskClicked(view: View) {
        val intent = Intent(this, TaskDescriptionActivity::class.java)
        startActivityForResult(intent, ADD_TASK_REQUEST)

    }

    private fun makeAdapter(list: List<String>): ArrayAdapter<String> =
        ArrayAdapter(this, android.R.layout.simple_list_item_1, list)


    private fun taskSelected(position: Int, id: Long) {
        AlertDialog.Builder(this)
                .setTitle(R.string.alert_task_title)
                .setMessage(taskList[position])
                .setNeutralButton(R.string.view_elements)
                {
                    _,_ ->
                   viewElements(id)
                }
                .setPositiveButton(R.string.delete)
                { _, _ ->
                    deleteElementFile(id)
                    val taskId = getTaskid(taskList[id.toInt()])
                    saveNewListAfterDeletion(taskId, position)
                    taskList.removeAt(position)
                    createFile()
                    taskMap.remove(taskId)
                    adapter.notifyDataSetChanged()
                }
                .setNegativeButton(R.string.cancel)
                { dialog, _ -> dialog.cancel()
                }

                .create()
                .show()

    }

    private fun viewElements(id:Long){
        val taskId = getTaskid(taskList[id.toInt()])

        val intent = Intent(this, IndividualTaskActivity::class.java)
        intent.putExtra("TASK_NAME", taskList[id.toInt()])
        intent.putExtra("TASK_ID",taskId.toLong())
        startActivityForResult(intent, ADD_ELEMENT_REQUEST)
    }

    private fun deleteElementFile(id:Long){
        val taskId = getTaskid(taskList[id.toInt()])
        val filePath = "/storage/emulated/0/Android/data/com.example.myapplication/files/ElementMap.$taskId"
        File(filePath).delete()

    }

    private fun createFile(){
        val path = this.getExternalFilesDir(null)
        val fileName = "TaskList"
        val file = File(path,fileName)

        FileOutputStream(file, false).bufferedWriter().use { writer ->
            taskList.forEach{
                writer.write("${it.toString()}\n")
            }

        }
    }

    private fun getTaskid(taskName:String): Int {
        val taskId = taskMap.filterValues { it == taskName }.keys.first()

        return taskId

    }

    private fun loadElements(){
        val path = this.getExternalFilesDir(null)
        val fullFilePath = path.toString() + "/TaskList"

        File(path.toString()).walk().forEach {directoryFile ->
            checkIfFileExists(directoryFile.toString(),fullFilePath)
        }

        if(doesFileExist)
            addFileContentToList(fullFilePath)
    }

    private fun checkIfFileExists(directoryFile:String, newFilePath:String) {
        if (directoryFile == newFilePath) {
            doesFileExist = true
        }
    }

    private fun addFileContentToList(filePath: String) {
        FileReader(filePath).forEachLine { taskList.add(it) }

        adapter.notifyDataSetChanged()

    }

    private fun saveMap(taskid:Int, task:String){
        keyList.add(taskid.toString())
        valueList.add(task)
        deleteThenAddToSharedPreferences()
    }

    private fun createMapOnStartup(){
        task_id = getSharedPreferences("my_save",Activity.MODE_PRIVATE).getInt("task_id",0)
        keyList = getSharedPreferences("my_save2", Activity.MODE_PRIVATE).getStringSet("key_list",keyList) as MutableSet<String>
        valueList = getSharedPreferences("my_save2", Activity.MODE_PRIVATE).getStringSet("value_list",valueList) as MutableSet<String>

        //Fyller mappet med begge listene
        keyList.zip(valueList).forEach {
            taskMap[it.first.toInt()] = it.second
        }

        println(taskMap)
    }

    private fun saveNewListAfterDeletion(taskid: Int, position: Int){
        keyList.remove("$taskid")
        valueList.remove(taskList[position])
        deleteThenAddToSharedPreferences()
    }

    private fun deleteThenAddToSharedPreferences() {
        getSharedPreferences("my_save2", Activity.MODE_PRIVATE).edit().remove("key_list").apply()
        getSharedPreferences("my_save2", Activity.MODE_PRIVATE).edit().remove("value_list").apply()

        getSharedPreferences("my_save2", Activity.MODE_PRIVATE).edit().putStringSet("key_list",keyList).apply()
        getSharedPreferences("my_save2", Activity.MODE_PRIVATE).edit().putStringSet("value_list",valueList).apply()

    }

}





