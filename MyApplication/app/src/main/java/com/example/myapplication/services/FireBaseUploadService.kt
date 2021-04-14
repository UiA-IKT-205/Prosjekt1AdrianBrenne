package com.example.myapplication.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.net.toUri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class FireBaseUploadService : Service() {

    private lateinit var auth: FirebaseAuth
    private lateinit var file: File
    private val ELMNTAG:String = "Elements"
    private val TSKTAG:String = "Tasks"
    private val CHCKTAG:String = "CheckList"

    override fun onBind(intent: Intent): IBinder {

        return Binder()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val elementFile = intent?.extras?.get("elementfile")
        val deleteElementFile = intent?.extras?.get("deleteElementFile")
        val taskFile = intent?.extras?.get("taskfile")
        val checkfile = intent?.extras?.get("checklistfile")
        val deleteCheckfile = intent?.extras?.get("deleteCheckFile")

        auth = Firebase.auth
        signInAnonymously()

        if(taskFile != null) {
            file = taskFile as File
            fileUploadToTask(file)
        }

        if(elementFile != null) {
            file = elementFile as File
            fileUploadToElements(file)
        }

        if(checkfile != null) {
            file = checkfile as File
            fileUploadToChecklist(file)
        }

        if (deleteElementFile != null){
            file = deleteElementFile as File
            deleteElementFile(file)
        }

        if (deleteCheckfile != null){
            file = deleteCheckfile as File
            deleteCheckFile(file)
        }

        return super.onStartCommand(intent, flags, startId)
    }



     fun fileUploadToElements(file: File){
        val ref = FirebaseStorage.getInstance().reference.child("Elements/${file.toUri().lastPathSegment}")
        val uploadTask = ref.putFile(file.toUri())

        uploadTask.addOnSuccessListener {
            Log.d(ELMNTAG, "Success in saving file")
        }.addOnFailureListener {
            Log.d(ELMNTAG, "Error in saving file")
        }

    }

    private fun fileUploadToChecklist(file: File){
        val ref = FirebaseStorage.getInstance().reference.child("CheckLists/${file.toUri().lastPathSegment}")
        val uploadTask = ref.putFile(file.toUri())

        uploadTask.addOnSuccessListener {
            Log.d(CHCKTAG, "Success in saving file")
        }.addOnFailureListener {
            Log.d(CHCKTAG, "Error in saving file")
        }

    }

    private fun fileUploadToTask(file: File){
        val ref = FirebaseStorage.getInstance().reference.child("Tasks/${file.toUri().lastPathSegment}")
        val uploadTask = ref.putFile(file.toUri())

        uploadTask.addOnSuccessListener {
            Log.d(TSKTAG, "Success in saving file")
        }.addOnFailureListener {
            Log.d(TSKTAG, "Error in saving file")
        }


    }

    private fun deleteElementFile(file: File){
        val ref = FirebaseStorage.getInstance().reference.child("Elements/${file.toUri().lastPathSegment}")
        val deleteTask = ref.delete()

        deleteTask.addOnSuccessListener {
            Log.d(ELMNTAG, "Success in deleting file")
        }.addOnFailureListener {
            Log.d(ELMNTAG, "Error in deleting file")
        }

    }

    private fun deleteCheckFile(file: File){
        val ref = FirebaseStorage.getInstance().reference.child("CheckLists/${file.toUri().lastPathSegment}")
        val deleteTask = ref.delete()

        deleteTask.addOnSuccessListener {
            Log.d(CHCKTAG, "Success in deleting file")
        }.addOnFailureListener {
            Log.d(CHCKTAG, "Error in deleting file")
        }

    }


    private fun signInAnonymously(){
        auth.signInAnonymously().addOnSuccessListener {
            Log.d(TSKTAG,"Login success")
        }.addOnFailureListener {
            Log.d(TSKTAG,"Login failed")
        }
    }
}