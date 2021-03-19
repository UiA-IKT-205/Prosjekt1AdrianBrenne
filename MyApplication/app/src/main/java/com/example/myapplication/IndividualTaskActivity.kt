package com.example.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.net.toUri
import com.example.myapplication.TaskDescriptionActivity.Companion.EXTRA_TASK_DESCRIPTION
import kotlinx.android.synthetic.main.activity_individual_task.*
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter

class IndividualTaskActivity : AppCompatActivity() {


    private val ADD_ELEMENT_REQUEST = 2

    private val PREFS_TASKS = "prefs_elements"
    private val KEY_TASKS_LIST = "elements_list"

    private val elementList = mutableListOf<String>()
    private var doesFileExist = false

    var onSave:((file: Uri) -> Unit)? = null

    private var map = mutableMapOf<Long?,String>()
    private val adapter by lazy { makeAdapter(elementList) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_individual_task)
        elementListView.adapter = adapter

        title = intent.extras?.getString("TASK_NAME")

        val taskId = intent.extras?.getLong("TASK_ID")
        val filename = "ElementMap.$taskId"
        val path = this.getExternalFilesDir(null)
        val fullFilePath = path.toString() + "/$filename"

        File(path.toString()).walk().forEach {directoryFile ->
            checkIfFileExists(directoryFile.toString(),fullFilePath)
        }

        if(doesFileExist)
            addFileContentToList(fullFilePath)

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_ELEMENT_REQUEST) {

            if (resultCode == Activity.RESULT_OK) {

                val task = data?.getStringExtra(EXTRA_TASK_DESCRIPTION)
                task?.let {

                    val taskId = intent.extras?.getLong("TASK_ID")
                    map[taskId] = task
                    sortMapValues()

                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    fun addElementClicked(view: View){
        val intent = Intent(this, TaskDescriptionActivity::class.java)
        startActivityForResult(intent, ADD_ELEMENT_REQUEST)
    }

    private fun makeAdapter(list: List<String>): ArrayAdapter<String> =
        ArrayAdapter(this, android.R.layout.simple_list_item_1, list)


    fun doneAddingElementClicked(view: View) {

        val savedList = mutableListOf<String>()
        for (map in map.values) {
            savedList.add(map)
        }


        val taskId = intent.extras?.getLong("TASK_ID")
        val filename = "ElementMap.$taskId"
        val path = this.getExternalFilesDir(null)
        println(path.toString())

        createFile(path.toString(), filename, savedList)

        finish()


    }

    private fun sortMapValues() {
        val taskId = intent.extras?.getLong("TASK_ID")
        for (key in map.keys)
            if(key == taskId)
                map[key]?.let { it1 -> elementList.add(it1) }
    }


    private fun createFile(path:String, fileName:String, savedList:MutableList<String> ){
        val file = File(path,fileName)
        FileOutputStream(file, true).bufferedWriter().use { writer ->
            // bufferdWriter lever her
            savedList.forEach{
                writer.write("${it.toString()}\n")
            }

        }

        this.onSave?.invoke(file.toUri())
    }

    private fun checkIfFileExists(directoryFile:String, newFilePath:String) {
        if (directoryFile == newFilePath) {
            doesFileExist = true

        }

    }

    private fun addFileContentToList(filePath: String) {

        FileReader(filePath).forEachLine { elementList.add(it) }
        adapter.notifyDataSetChanged()

    }

}