package com.example.myapplication

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.example.myapplication.TaskDescriptionActivity.Companion.EXTRA_TASK_DESCRIPTION
import kotlinx.android.synthetic.main.activity_individual_task.*
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.*

class IndividualTaskActivity : AppCompatActivity() {

    private val ADD_ELEMENT_REQUEST = 2

    private val elementList = mutableListOf<String>()
    private val addElementList = mutableListOf<String>()
    private var doesFileExist = false

    var onSave:((file: Uri) -> Unit)? = null

    private val adapter by lazy { makeAdapter(elementList) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_individual_task)
        elementListView.adapter = adapter

        title = intent.extras?.getString("TASK_NAME")

        loadElements()

        elementListView.onItemClickListener = AdapterView.OnItemClickListener { _,_, position, id ->
            elementSelected(position)
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_ELEMENT_REQUEST) {

            if (resultCode == Activity.RESULT_OK) {

                val task = data?.getStringExtra(EXTRA_TASK_DESCRIPTION)
                task?.let {
                    addElementList.add(task)
                    elementList.add(task)
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
        val taskId = intent.extras?.getLong("TASK_ID")
        val filename = "ElementMap.$taskId"
        val path = this.getExternalFilesDir(null)
        createFile(path.toString(), filename, addElementList)
        finish()

    }

    private fun createFile(path:String, fileName:String, savedList:MutableList<String> ){
        val file = File(path,fileName)
        FileOutputStream(file, true).bufferedWriter().use { writer ->
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

    private fun loadElements(){
        val path = this.getExternalFilesDir(null)
        val fullFilePath = filePathCreator()

        File(path.toString()).walk().forEach {directoryFile ->
            checkIfFileExists(directoryFile.toString(),fullFilePath)
        }

        if(doesFileExist)
            addFileContentToList(fullFilePath)
    }


    private fun elementSelected(position: Int){

        AlertDialog.Builder(this)
                .setTitle(R.string.alert_element_title)
                .setMessage(elementList[position])

                .setPositiveButton(R.string.delete)
                { _, _ ->
                    val elementName = elementList[position]
                    elementList.removeAt(position)
                    deleteSingleElementInFile(elementName)
                    adapter.notifyDataSetChanged()
                }
                .setNegativeButton(R.string.cancel)
                { dialog, _ -> dialog.cancel()
                }

                .create()
                .show()
    }


    private fun deleteSingleElementInFile(elementName:String) {
        val fullFilePath = filePathCreator()

        addElementList.forEach {
            if (it == elementName)
                addElementList.remove(it)
        }

        FileOutputStream(fullFilePath, false).bufferedWriter().use { writer ->
            elementList.forEach{
                writer.write("${it.toString()}\n")
            }

        }

        this.onSave?.invoke(fullFilePath.toUri())
    }

    private fun filePathCreator(): String {
        val taskId = intent.extras?.getLong("TASK_ID")
        val filename = "ElementMap.$taskId"
        val path = this.getExternalFilesDir(null)
        val fullFilePath = path.toString() + "/$filename"

        return fullFilePath
    }


}