package com.example.myapplication

import android.content.Intent
import com.example.myapplication.adapters.TaskListViewAdapter
import com.example.myapplication.data.ElementListViewModel
import org.junit.Test
import com.example.myapplication.data.TaskListViewModel
import com.example.myapplication.services.FireBaseUploadService
import com.google.firebase.FirebaseApp
import org.junit.Assert.*
import java.io.File

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class DataUnitTest {

    @Test
    fun inputToTaskListModelTest(){
        val model = TaskListViewModel("test1",20)

        assertEquals(model.name,"test1")
        assertEquals(model.progress,20)
    }

    @Test
    fun inputToElementListModelTest(){
        val model = ElementListViewModel("test1",true)

        assertEquals(model.name,"test1")
        assertEquals(model.checked,true)
    }

    @Test
    fun addToModelTest(){
        val dataModel : ArrayList<TaskListViewModel> = ArrayList()
        val model = TaskListViewModel("test1",20)
        dataModel.add(model)

        assertEquals(1,dataModel.size)
    }

    @Test
    fun removeFromModelTest(){
        val dataModel : ArrayList<TaskListViewModel> = ArrayList()
        val model = TaskListViewModel("test1",20)
        dataModel.add(model)
        assertEquals(1,dataModel.size)

        dataModel.remove(model)
        assertEquals(0,dataModel.size)
    }


}