package com.example.myapplication

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.TaskDescriptionActivity.Companion.EXTRA_TASK_DESCRIPTION
import kotlinx.android.synthetic.main.activity_individual_task.*
import kotlinx.android.synthetic.main.row_item.*
import kotlinx.android.synthetic.main.row_item.view.*
import kotlinx.android.synthetic.main.toolbar_sheet.*
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader

class IndividualTaskActivity : AppCompatActivity() {

    private val ADD_ELEMENT_REQUEST = 2

    private val elementList = mutableListOf<String>()
    private val addElementList = mutableListOf<String>()
    private val elementsCheckedList = mutableListOf<String>()
    private var doesElementMapFileExist = false
    private var doesCheckListFileExist = false
    var onSave:((file: Uri) -> Unit)? = null

    //private val adapter by lazy { makeAdapter(elementList) }

    private lateinit var elementListView: ListView
    private var dataModel: ArrayList<DataModel>? = null
    private lateinit var adapter : ListViewAdapter

    private var progressStatus = 0
    private var newProgress = 0
    private var numberOfElementsChecked = 0

    private lateinit var toolbar: Toolbar

    private lateinit var element: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_individual_task)
        val title = intent.extras?.getString("TASK_NAME")

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.title = "$title | "

        elementListView = findViewById<View>(R.id.elementListView) as ListView

        dataModel = ArrayList()

        adapter = ListViewAdapter(dataModel!!, applicationContext)

        elementListView.adapter = adapter

        loadElements()


        elementListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, id ->
                checkBoxChecked(position)
            }

    }


//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == ADD_ELEMENT_REQUEST) {
//
//            if (resultCode == Activity.RESULT_OK) {
//
//                val task = data?.getStringExtra(EXTRA_TASK_DESCRIPTION)
//                task?.let {
//                    addElementList.add(task)
//                    elementList.add(task)
//                    elementsCheckedList.add("false")
//                    dataModel!!.add(DataModel(task,false))
//                    createCheckListFile()
//                    updateProgressBarAfterAdd()
//                    adapter.notifyDataSetChanged()
//                }
//            }
//        }
//    }

    fun addElementClicked(view: View){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Add an element")

        val input = EditText(this)

        input.hint = "Elementname"
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            element = input.text.toString()
            newElementBackendControl(element)
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()

    }

    private fun newElementBackendControl(element:String){
        addElementList.add(element)
        elementList.add(element)
        elementsCheckedList.add("false")
        dataModel!!.add(DataModel(element,false))
        createCheckListFile()
        updateProgressBarAfterAdd()
        adapter.notifyDataSetChanged()
    }


    fun doneAddingElementClicked(view: View) {
        val taskId = intent.extras?.getLong("TASK_ID")
        val filename = "ElementMap.$taskId"
        val path = this.getExternalFilesDir(null)
        createFile(path.toString(), filename, addElementList)

        val result = Intent()
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    private fun createFile(path:String, fileName:String, savedList:MutableList<String> ){
        val file = File(path,fileName)
        FileOutputStream(file, true).bufferedWriter().use { writer ->
            savedList.forEach{
                writer.write("${it.toString()}\n")
            }

        }
        runFirebaseServiceForElements(file)
        this.onSave?.invoke(file.toUri())
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
        FileReader(filePath).forEachLine { elementList.add(it) }
        adapter.notifyDataSetChanged()

    }

    private fun addCheckListFileContentToList(fullFilePath:String){
        FileReader(fullFilePath).forEachLine { elementsCheckedList.add(it) }
        adapter.notifyDataSetChanged()
    }

    private fun loadElements(){
        var numberOfCheckedBoxes = 0
        val path = this.getExternalFilesDir(null)
        val fullFilePath = filePathCreator()

        File(path.toString()).walk().forEach {directoryFile ->
            checkIfElementMapFileExists(directoryFile.toString(),fullFilePath)
        }

        val taskId = intent.extras?.getLong("TASK_ID")
        val filename = "CheckListMap.$taskId"
        val checkListFullPath = path.toString() + "/$filename"

        File(path.toString()).walk().forEach {directoryFile ->
            checkIfCheckListFileExists(directoryFile.toString(),checkListFullPath)
        }



        if(doesElementMapFileExist)
            addElementFileContentToList(fullFilePath)

        if(doesCheckListFileExist){
            addCheckListFileContentToList(checkListFullPath)
            elementList.zip(elementsCheckedList).forEach {
                dataModel!!.add(DataModel(it.first, it.second.toBoolean()))
                if (it.second == "true"){
                    numberOfCheckedBoxes += 1
                }

         }}
        else{
            for (elements in elementList){
                dataModel!!.add(DataModel(elements, false))
                elementsCheckedList.add("false")
        }}

        loadProgressBar(numberOfCheckedBoxes)


    }


    private fun deleteSingleElementInFile() {
        val fullFilePath = filePathCreator()

//        addElementList.forEach {
//            if (it == elementName)
//                addElementList.remove(it)
//        }

        FileOutputStream(fullFilePath, false).bufferedWriter().use { writer ->
            elementList.forEach{
                writer.write("${it.toString()}\n")
            }

        }
        runFirebaseServiceForElements(File(fullFilePath))
        this.onSave?.invoke(fullFilePath.toUri())
    }


    private fun deleteSingleCheckInFile() {
        val taskId = intent.extras?.getLong("TASK_ID")
        val filename = "CheckListMap.$taskId"
        val path = this.getExternalFilesDir(null)
        val fullFilePath = path.toString() + "/$filename"

        FileOutputStream(fullFilePath, false).bufferedWriter().use { writer ->
            elementsCheckedList.forEach {
                writer.write("${it.toString()}\n")
            }
        }
        runFirebaseServiceForChecklist(File(fullFilePath))
        this.onSave?.invoke(fullFilePath.toUri())
    }

    private fun filePathCreator(): String {
        val taskId = intent.extras?.getLong("TASK_ID")
        val filename = "ElementMap.$taskId"
        val path = this.getExternalFilesDir(null)
        val fullFilePath = path.toString() + "/$filename"

        return fullFilePath
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

    private fun checkBoxChecked(position: Int) {
        val dataModel: DataModel = dataModel!![position]
        dataModel.checked = !dataModel.checked

        if(dataModel.checked) {
            newProgress = 100 / elementList.size
            progressStatus += newProgress
            pBar.progress = progressStatus
            pBarProgressText.text = "Task completed $progressStatus% of 100%"
            numberOfElementsChecked += 1
            elementsCheckedList[position] = "true"
            createCheckListFile()
        }else{
            newProgress = 100 / elementList.size
            progressStatus -= newProgress
            pBar.progress = progressStatus
            pBarProgressText.text = "Task completed $progressStatus% of 100%"
            numberOfElementsChecked -= 1
            elementsCheckedList[position] = "false"
            createCheckListFile()
        }

        adapter.notifyDataSetChanged()
    }

    private fun updateProgressBarAfterAdd(){
        newProgress = (100 / elementList.size) * numberOfElementsChecked
        progressStatus = newProgress
        pBar.progress = progressStatus
        pBarProgressText.text = "Task completed $progressStatus% of 100%"
    }

    private fun createCheckListFile(){
        val taskId = intent.extras?.getLong("TASK_ID")
        val filename = "CheckListMap.$taskId"
        val path = this.getExternalFilesDir(null)
        val fullFilePath = path.toString() + "/$filename"

        FileOutputStream(fullFilePath, false).bufferedWriter().use { writer ->
            elementsCheckedList.forEach{
                writer.write("${it.toString()}\n")
            }

        }
        runFirebaseServiceForChecklist(File(fullFilePath))
        this.onSave?.invoke(fullFilePath.toUri())


    }

    private fun loadProgressBar(numberOfCheckedBoxes:Int){
        if(numberOfCheckedBoxes!= 0) {
            newProgress = (100 / elementList.size) * numberOfCheckedBoxes
            progressStatus = newProgress
            pBar.progress = progressStatus
            pBarProgressText.text = "Task completed $progressStatus% of 100%"

            numberOfElementsChecked = numberOfCheckedBoxes
        }
    }

    fun deleteElementClicked(view: View){
        Toast.makeText(applicationContext,"Select the element you want to delete",Toast.LENGTH_SHORT).show()

        elementListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, id ->
            elementList.removeAt(position)
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


