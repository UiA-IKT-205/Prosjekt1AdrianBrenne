package com.example.myapplication

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import com.example.myapplication.adapters.ElementListViewAdapter
import com.example.myapplication.data.ElementListViewModel
import com.example.myapplication.services.FireBaseUploadService
import kotlinx.android.synthetic.main.activity_element_list_view.*
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader

class ElementListViewActivity : AppCompatActivity() {
    private val listOfElements = mutableListOf<String>()
    private val elementsCheckedList = mutableListOf<String>()
    private var doesElementMapFileExist = false
    private var doesCheckListFileExist = false

    private var dataModel: ArrayList<ElementListViewModel>? = null
    private var progressBarStatus = 0

    private var newProgressBarStatus = 0
    private var numberOfElementsChecked = 0

    private lateinit var elementListView: ListView
    private lateinit var adapter : ElementListViewAdapter
    private lateinit var toolbar: Toolbar
    private lateinit var elementName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_element_list_view)
        val title = intent.extras?.getString("TASK_NAME")

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.title = "$title | "

        elementListView = findViewById<View>(R.id.elementListView) as ListView

        dataModel = ArrayList()

        adapter = ElementListViewAdapter(dataModel!!, applicationContext)

        elementListView.adapter = adapter

        loadElements()

        elementListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                checkBoxClicked(position)
            }
    }

    fun addElementClicked(view: View){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Add an element")

        val input = EditText(this)

        input.hint = "Element name"
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            elementName = input.text.toString()
            newElementBackendControl(elementName)
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()

    }

    private fun newElementBackendControl(element:String){
        listOfElements.add(element)
        elementsCheckedList.add("false")
        dataModel!!.add(ElementListViewModel(element,false))
        createCheckListFile()
        updateProgressBarAfterAdd()
        adapter.notifyDataSetChanged()
    }


    fun doneAddingElementClicked(view: View) {
        val taskId = intent.extras?.getLong("TASK_ID")
        val filename = "ElementList.$taskId"
        val path = this.getExternalFilesDir(null)
        createFile(path.toString(), filename, listOfElements)

        val result = Intent()
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    private fun createFile(path:String, fileName:String, savedList:MutableList<String> ){
        val file = File(path,fileName)
        FileOutputStream(file, false).bufferedWriter().use { writer ->
            savedList.forEach{
                writer.write("${it}\n")
            }
        }
        runFirebaseServiceForElements(file)
    }

    private fun checkIfElementMapFileExists(directoryFile:String, newFilePath:String) {
        if (directoryFile == newFilePath) {
            doesElementMapFileExist = true
        }
    }


    private fun checkIfCheckListFileExists(directoryFile:String, newFilePath:String) {
        if (directoryFile == newFilePath) {
            doesCheckListFileExist = true
        }
    }

    private fun addElementFileContentToList(filePath: String) {
        FileReader(filePath).forEachLine { listOfElements.add(it) }
    }

    private fun addCheckListFileContentToList(fullFilePath:String){
        FileReader(fullFilePath).forEachLine { elementsCheckedList.add(it) }
    }

    private fun loadElements(){
        var numberOfCheckedBoxes = 0
        val path = this.getExternalFilesDir(null)
        val fullFilePath = elementFilePathCreator()

        File(path.toString()).walk().forEach {directoryFile ->
            checkIfElementMapFileExists(directoryFile.toString(),fullFilePath)
        }

        val taskId = intent.extras?.getLong("TASK_ID")
        val filename = "CheckList.$taskId"
        val checkListFullPath = path.toString() + "/$filename"

        File(path.toString()).walk().forEach {directoryFile ->
            checkIfCheckListFileExists(directoryFile.toString(),checkListFullPath)
        }

        if(doesElementMapFileExist)
            addElementFileContentToList(fullFilePath)

        if(doesCheckListFileExist){
            addCheckListFileContentToList(checkListFullPath)
            listOfElements.zip(elementsCheckedList).forEach {
                dataModel!!.add(ElementListViewModel(it.first, it.second.toBoolean()))
                if (it.second == "true"){
                    numberOfCheckedBoxes += 1
                }
         }}
        else{
            for (elements in listOfElements){
                dataModel!!.add(ElementListViewModel(elements, false))
                elementsCheckedList.add("false")
        }}
        loadProgressBar(numberOfCheckedBoxes)
    }

    private fun deleteSingleElementInFile() {
        val fullFilePath = elementFilePathCreator()

        FileOutputStream(fullFilePath, false).bufferedWriter().use { writer ->
            listOfElements.forEach{
                writer.write("${it}\n")
            }

        }
        runFirebaseServiceForElements(File(fullFilePath))
    }

    private fun deleteSingleCheckInFile() {
        val taskId = intent.extras?.getLong("TASK_ID")
        val filename = "CheckList.$taskId"
        val path = this.getExternalFilesDir(null)
        val fullFilePath = path.toString() + "/$filename"

        FileOutputStream(fullFilePath, false).bufferedWriter().use { writer ->
            elementsCheckedList.forEach {
                writer.write("${it.toString()}\n")
            }
        }
        runFirebaseServiceForChecklist(File(fullFilePath))
    }

    private fun elementFilePathCreator(): String {
        val taskId = intent.extras?.getLong("TASK_ID")
        val filename = "ElementList.$taskId"
        val path = this.getExternalFilesDir(null)

        return path.toString() + "/$filename"
    }

    private fun runFirebaseServiceForElements(file:File){
        val intent = Intent(this, FireBaseUploadService::class.java).apply {
            putExtra("elementfile",file)
        }
        startService(intent)

    }

    private fun runFirebaseServiceForChecklist(file:File){
        val intent = Intent(this, FireBaseUploadService::class.java).apply {
            putExtra("checklistfile",file)
        }
        startService(intent)
    }

    private fun checkBoxClicked(position: Int) {
        val dataModel: ElementListViewModel = dataModel!![position]
        dataModel.checked = !dataModel.checked

        if(dataModel.checked) {
            checkBoxChecked(position)
            createCheckListFile()
        }else{
            checkBoxUnChecked(position)
            createCheckListFile()
        }
        adapter.notifyDataSetChanged()
    }

    private fun checkBoxChecked(position: Int){
        newProgressBarStatus = 100 / listOfElements.size
        progressBarStatus += newProgressBarStatus
        pBar.progress = progressBarStatus
        pBarProgressText.text = "Task completed $progressBarStatus% of 100%"
        numberOfElementsChecked += 1
        elementsCheckedList[position] = "true"
    }

    private fun checkBoxUnChecked(position: Int){
        newProgressBarStatus = 100 / listOfElements.size
        progressBarStatus -= newProgressBarStatus
        pBar.progress = progressBarStatus
        pBarProgressText.text = "Task completed $progressBarStatus% of 100%"
        numberOfElementsChecked -= 1
        elementsCheckedList[position] = "false"
    }

    private fun updateProgressBarAfterAdd(){
        newProgressBarStatus = (100 / listOfElements.size) * numberOfElementsChecked
        progressBarStatus = newProgressBarStatus
        pBar.progress = progressBarStatus
        pBarProgressText.text = "Task completed $progressBarStatus% of 100%"
    }

    private fun createCheckListFile(){
        val taskId = intent.extras?.getLong("TASK_ID")
        val filename = "CheckList.$taskId"
        val path = this.getExternalFilesDir(null)
        val fullFilePath = path.toString() + "/$filename"

        FileOutputStream(fullFilePath, false).bufferedWriter().use { writer ->
            elementsCheckedList.forEach{
                writer.write("${it}\n")
            }

        }
        runFirebaseServiceForChecklist(File(fullFilePath))
    }

    private fun loadProgressBar(numberOfCheckedBoxes:Int){
        if(numberOfCheckedBoxes!= 0) {
            newProgressBarStatus = (100 / listOfElements.size) * numberOfCheckedBoxes
            progressBarStatus = newProgressBarStatus
            pBar.progress = progressBarStatus
            pBarProgressText.text = "Task completed $progressBarStatus% of 100%"

            numberOfElementsChecked = numberOfCheckedBoxes
        }
    }

    fun deleteElementClicked(view: View){
        Toast.makeText(applicationContext,"Select the element you want to delete",Toast.LENGTH_SHORT).show()

        elementListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, id ->
            listOfElements.removeAt(position)
            elementsCheckedList.removeAt(position)
            dataModel!!.removeAt(position)

            deleteSingleElementInFile()
            deleteSingleCheckInFile()

            adapter.notifyDataSetChanged()

            val result = Intent()
            setResult(Activity.RESULT_OK, result)
            finish()
            startActivity(intent)
        }
    }
}


