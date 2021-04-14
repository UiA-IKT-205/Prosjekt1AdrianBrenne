package com.example.myapplication

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.text.InputType
import android.util.Log
import android.widget.*
import androidx.annotation.MainThread
import androidx.appcompat.widget.Toolbar
import com.example.myapplication.adapters.TaskListViewAdapter
import com.example.myapplication.data.TaskListViewModel
import com.example.myapplication.services.FireBaseUploadService
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.security.cert.CertPath
import kotlin.collections.ArrayList


class TaskListViewActivity : AppCompatActivity() {
    private val listOfTasks = mutableListOf<String>()
    private val mapOfTaskAndId = mutableMapOf<Int,String>()
    private var taskNameList = mutableSetOf<String>()

    private var dataModel: ArrayList<TaskListViewModel>? = null

    private var doesFileExist = false
    private var uTaskId = 0
    private val ADD_ELEMENT_REQUEST = 2

    private lateinit var taskListView: ListView
    private lateinit var adapter: TaskListViewAdapter
    private lateinit var toolbar: Toolbar
    private lateinit var taskName: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        taskListView = findViewById<View>(R.id.taskListView) as ListView

        dataModel = ArrayList()

        adapter = TaskListViewAdapter(dataModel!!, applicationContext)

        taskListView.adapter = adapter

        loadElements()
        createMapOnStartup()

        loadProgressBar()

        taskListView.onItemClickListener =
                AdapterView.OnItemClickListener { _, _, _, id ->
                    taskSelected(id)
                }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_ELEMENT_REQUEST){
            if (resultCode == Activity.RESULT_OK){
                finish()
                startActivity(intent)
            }
        }
    }

     private fun saveTaskId(){
        uTaskId += 1
        getSharedPreferences("my_save", Activity.MODE_PRIVATE).edit().putInt("task_id", uTaskId).apply()
    }


    fun addTaskClicked(view: View) {
        println(mapOfTaskAndId)
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Add a task")

        val input = EditText(this)

        input.hint = "Task name"
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            taskName = input.text.toString()
            newTaskBackendControl(taskName)
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun newTaskBackendControl(task:String){
        saveTaskId()
        mapOfTaskAndId[uTaskId] = task
        listOfTasks.add(task)
        dataModel!!.add(TaskListViewModel(task,0))

        createFile(getPath())

        saveMap(uTaskId)
        adapter.notifyDataSetChanged()
    }

    private fun taskSelected(id: Long) {
        viewElements(id)
    }

    private fun viewElements(id:Long){
        val taskId = getTaskId(listOfTasks[id.toInt()])

        val intent = Intent(this, ElementListViewActivity::class.java)
        intent.putExtra("TASK_NAME", listOfTasks[id.toInt()])
        intent.putExtra("TASK_ID",taskId.toLong())
        startActivityForResult(intent, ADD_ELEMENT_REQUEST)
    }

    private fun deleteElementFile(id:Long){
        val taskId = getTaskId(listOfTasks[id.toInt()])
        val filePath = "/storage/emulated/0/Android/data/com.example.myapplication/files/ElementList.$taskId"
        val path = this.getExternalFilesDir(null)
        val file = File(filePath)

        File(path.toString()).walk().forEach {directoryFile ->
            checkIfFileExists(directoryFile.toString(),filePath)
        }

        if (doesFileExist){
            val intent = Intent(this, FireBaseUploadService::class.java).apply {
                putExtra("deleteElementFile",file)
            }
            startService(intent)
        }else
            Log.d("ElementFile","No elementfile found, no delete needed.")
        File(filePath).delete()

        doesFileExist = false
    }

    private fun deleteCheckListFile(id:Long){
        val taskId = getTaskId(listOfTasks[id.toInt()])
        val filePath = "/storage/emulated/0/Android/data/com.example.myapplication/files/CheckList.$taskId"
        val path = this.getExternalFilesDir(null)
        val file = File(filePath)

        File(path.toString()).walk().forEach {directoryFile ->
            checkIfFileExists(directoryFile.toString(),filePath)
        }

        if(doesFileExist){
            val intent = Intent(this, FireBaseUploadService::class.java).apply {
                putExtra("deleteCheckFile",file)
            }
            startService(intent)
        }else
            Log.d("CheckFile","No checkfile found, no delete needed.")

        File(filePath).delete()

        doesFileExist = false
    }

     fun createFile(path:String){
        val fileName = "TaskList"
        val file = File(path,fileName)

        FileOutputStream(file, false).bufferedWriter().use { writer ->
            listOfTasks.forEach{
                writer.write("${it}\n")
            }
        }
        runFirebaseService(file)
    }

    private fun getPath(): String {
        return this.getExternalFilesDir(null).toString()
    }

    private fun getTaskId(taskName:String): Int {
        return mapOfTaskAndId.filterValues { it == taskName }.keys.first()
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

     fun checkIfFileExists(directoryFile:String, newFilePath:String) {
        if (directoryFile == newFilePath) {
            doesFileExist = true
        }
    }

    private fun addFileContentToList(filePath: String) {
        FileReader(filePath).forEachLine { listOfTasks.add(it) }
    }

    private fun saveMap(taskId:Int){
        taskNameList.add(taskId.toString())
        deleteThenAddToSharedPreferences()
    }

    private fun createMapOnStartup(){
        uTaskId = getSharedPreferences("my_save",Activity.MODE_PRIVATE).getInt("task_id",0)
        taskNameList = getSharedPreferences("my_save2", Activity.MODE_PRIVATE).getStringSet("key_list",taskNameList) as MutableSet<String>

        taskNameList.zip(listOfTasks).forEach {
            mapOfTaskAndId[it.first.toInt()] = it.second
        }
    }

    private fun saveNewListAfterDeletion(taskId: Int){
        taskNameList.remove("$taskId")
        deleteThenAddToSharedPreferences()
    }

    private fun deleteThenAddToSharedPreferences() {
        getSharedPreferences("my_save2", Activity.MODE_PRIVATE).edit().remove("key_list").apply()
        getSharedPreferences("my_save2", Activity.MODE_PRIVATE).edit().putStringSet("key_list",taskNameList).apply()
    }

    private fun runFirebaseService(file:File){
        val intent = Intent(this, FireBaseUploadService::class.java).apply {
            putExtra("taskfile",file)
        }
        startService(intent)
    }

    private fun loadProgressBar(){
        var indexCounter = 0
        val path = this.getExternalFilesDir(null)

        for (id in mapOfTaskAndId.keys){
            var numberOfCheckedBoxes = 0
            var progress: Int
            var numberOfElements = 0
            val filename = "CheckList.$id"
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
                dataModel!!.add(TaskListViewModel(listOfTasks[indexCounter],progress))

                indexCounter += 1
                doesFileExist = false
            }else{
                dataModel!!.add(TaskListViewModel(listOfTasks[indexCounter],0))
                indexCounter += 1
            }
        }
        adapter.notifyDataSetChanged()
    }

    fun deleteTaskClicked(view: View) {

        Toast.makeText(this,"Select the task you want to delete",Toast.LENGTH_SHORT).show()
        taskListView.onItemClickListener = AdapterView.OnItemClickListener { _,_, position, id ->
            deleteElementFile(id)
            deleteCheckListFile(id)

            val taskId = getTaskId(listOfTasks[id.toInt()])
            mapOfTaskAndId.remove(taskId)
            listOfTasks.removeAt(position)
            dataModel!!.removeAt(position)

            saveNewListAfterDeletion(taskId)
            createFile(getPath())

            adapter.notifyDataSetChanged()
            Toast.makeText(this,"Deleted",Toast.LENGTH_SHORT).show()
            finish()
            startActivity(intent)
        }
    }
}






