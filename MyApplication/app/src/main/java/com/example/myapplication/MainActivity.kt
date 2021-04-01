package com.example.myapplication

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Toast
import androidx.core.view.forEach
import androidx.core.view.iterator
import kotlinx.android.synthetic.main.tasklist_row_item.*
import kotlinx.android.synthetic.main.tasklist_row_item.view.*
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    private val taskList = mutableListOf<String>()
    private val taskMap = mutableMapOf<Int,String>()
    private var keyList = mutableSetOf<String>()
    private var valueList = mutableSetOf<String>()
    private var doesFileExist = false
    private var task_id = 0

    private val ADD_TASK_REQUEST = 1
    private val ADD_ELEMENT_REQUEST = 2

    private lateinit var taskListView: ListView

    private var dataModel: ArrayList<TaskViewModel>? = null

    private lateinit var adapter: TaskListViewAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        taskListView = findViewById<View>(R.id.taskListView) as ListView

        dataModel = ArrayList()

        adapter = TaskListViewAdapter(dataModel!!, applicationContext)

        taskListView.adapter = adapter

        loadElements()
        createMapOnStartup()

        loadProgressBar()

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
                    dataModel!!.add(TaskViewModel(task,0))
                    createFile()
                    saveMap(task_id, task)
                    adapter.notifyDataSetChanged()
                }
            }

        }

        if (requestCode == ADD_ELEMENT_REQUEST){
            if (resultCode == Activity.RESULT_OK){
                println("yes")
                finish()
                startActivity(intent)
            }
        }


    }

    private fun saveTaskId(){
        task_id += 1
        getSharedPreferences("my_save", Activity.MODE_PRIVATE).edit().putInt("task_id", task_id).apply()
    }


    fun addTaskClicked(view: View) {
        val intent = Intent(this, TaskDescriptionActivity::class.java)
        startActivityForResult(intent, ADD_TASK_REQUEST)

    }


    private fun taskSelected(position: Int, id: Long) {

        viewElements(id)

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

    private fun deleteCheckListFile(id:Long){
        val taskId = getTaskid(taskList[id.toInt()])
        val filePath = "/storage/emulated/0/Android/data/com.example.myapplication/files/CheckListMap.$taskId"
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
        //runFirebaseActivity(file)
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

        doesFileExist = false
    }

    private fun checkIfFileExists(directoryFile:String, newFilePath:String) {
        if (directoryFile == newFilePath) {
            doesFileExist = true
        }
    }

    private fun addFileContentToList(filePath: String) {
        FileReader(filePath).forEachLine { taskList.add(it) }
        FileReader(filePath).forEachLine { valueList.add(it) }

        println(taskList)

        adapter.notifyDataSetChanged()

    }

    private fun saveMap(taskid:Int, task:String){
        keyList.add(taskid.toString())
        valueList.add(task)
        deleteThenAddToSharedPreferences()
    }

    private fun createMapOnStartup(){

        //getSharedPreferences("my_save2", Activity.MODE_PRIVATE).edit().remove("key_list").apply()


        task_id = getSharedPreferences("my_save",Activity.MODE_PRIVATE).getInt("task_id",0)
        keyList = getSharedPreferences("my_save2", Activity.MODE_PRIVATE).getStringSet("key_list",keyList) as MutableSet<String>
        //valueList = getSharedPreferences("my_save2", Activity.MODE_PRIVATE).getStringSet("value_list",valueList) as MutableSet<String>

        //println(keyList)
        //println(valueList)

        //Fyller mappet med begge listene
        keyList.zip(valueList).forEach {
            taskMap[it.first.toInt()] = it.second
        }

        println(taskMap)


    }

    private fun saveNewListAfterDeletion(taskid: Int, position: Int){
        keyList.remove("$taskid")
        deleteThenAddToSharedPreferences()
    }

    private fun deleteThenAddToSharedPreferences() {
        getSharedPreferences("my_save2", Activity.MODE_PRIVATE).edit().remove("key_list").apply()
        getSharedPreferences("my_save2", Activity.MODE_PRIVATE).edit().putStringSet("key_list",keyList).apply()
    }

    private fun runFirebaseActivity(file:File){
        val intent = Intent(this, FirebaseUploadActivity::class.java).apply {
            putExtra("taskfile",file)
        }
        startActivity(intent)


    }

    private fun loadProgressBar(){
        var n = 0
        val path = this.getExternalFilesDir(null)

        for (id in taskMap.keys){
            var numberOfCheckedBoxes = 0
            var progress = 0
            var numberOfElements = 0
            val filename = "CheckListMap.$id"
            val checkListFullPath = path.toString() + "/$filename"

            File(path.toString()).walk().forEach {directoryFile ->
                checkIfFileExists(directoryFile.toString(),checkListFullPath)
            }

            if(doesFileExist){
                FileReader(checkListFullPath).forEachLine {
                    if (it == "true")
                        numberOfCheckedBoxes += 1
                    numberOfElements += 1
                }

                progress = (100 / numberOfElements) * numberOfCheckedBoxes
                dataModel!!.add(TaskViewModel(taskList[n],progress))

                n += 1
                doesFileExist = false
            }else{
                dataModel!!.add(TaskViewModel(taskList[n],0))
            }


        }
        adapter.notifyDataSetChanged()

    }

    fun deleteTaskClicked(view: View) {
        Toast.makeText(applicationContext,"Select the task you want to delete",Toast.LENGTH_SHORT).show()

        taskListView.onItemClickListener = AdapterView.OnItemClickListener { _,_, position, id ->
            deleteElementFile(id)
            deleteCheckListFile(id)

            val taskId = getTaskid(taskList[id.toInt()])
            taskMap.remove(taskId)
            taskList.removeAt(position)
            dataModel!!.removeAt(position)

            saveNewListAfterDeletion(taskId, position)
            createFile()

            adapter.notifyDataSetChanged()
            Toast.makeText(applicationContext,"Deleted",Toast.LENGTH_SHORT).show()
            finish()
            startActivity(intent)


        }

    }


}





