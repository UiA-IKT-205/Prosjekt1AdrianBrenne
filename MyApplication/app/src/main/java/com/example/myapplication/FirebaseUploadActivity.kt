package com.example.myapplication
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.net.toUri
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class FirebaseUploadActivity() : AppCompatActivity() {

    private lateinit var auth:FirebaseAuth
    private lateinit var file:File
    private val INDVTAG:String = "IndividualTaskActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_firebase_upload)

        val elementFile = intent.extras?.get("elementfile")
        val taskFile = intent.extras?.get("taskfile")

        if(elementFile == null){
            file = taskFile as File
            fileUploadToTask(file)
        } else{
            file = elementFile as File
            fileUploadToElements(file)
        }

        auth = Firebase.auth
        signInAnonymously()

        finish()

    }

    private fun fileUploadToElements(file:File){
        val ref = FirebaseStorage.getInstance().reference.child("Elements/${file.toUri().lastPathSegment}")
        val uploadTask = ref.putFile(file.toUri())

        uploadTask.addOnSuccessListener {
            Log.d(INDVTAG, "Success in saving file")
        }.addOnFailureListener {
            Log.d(INDVTAG, "Error in saving file")
        }


    }

    private fun fileUploadToTask(file:File){
        val ref = FirebaseStorage.getInstance().reference.child("Tasks/${file.toUri().lastPathSegment}")
        val uploadTask = ref.putFile(file.toUri())

        uploadTask.addOnSuccessListener {
            Log.d(INDVTAG, "Success in saving file")
        }.addOnFailureListener {
            Log.d(INDVTAG, "Error in saving file")
        }


    }

    private fun signInAnonymously(){
        auth.signInAnonymously().addOnSuccessListener {
            Log.d(INDVTAG,"Login success")
        }.addOnFailureListener {
            Log.d(INDVTAG,"Login failed")
        }
    }
}