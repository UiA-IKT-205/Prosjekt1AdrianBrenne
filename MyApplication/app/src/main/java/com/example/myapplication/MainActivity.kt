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
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private val taskList = mutableListOf<String>()
    private val adapter by lazy { makeAdapter(taskList) }

    private val ADD_TASK_REQUEST = 1

    private val tickReceiver by lazy { makeBroadcastReceiver() }

    private val PREFS_TASKS = "prefs_tasks"
    private val KEY_TASKS_LIST = "tasks_list"



    companion object {
        private const val LOG_TAG = "MainActivityLog"

        private fun getCurrentTimeStamp(): String {
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val now = Date()
            return simpleDateFormat.format(now)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        taskListView.adapter = adapter

        taskListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                taskSelected(position)
            }

        // Leser den lagrede listen fra SharedPreferences
        // Henter dataene ved å konvertere til en typed array
        val savedList = getSharedPreferences(PREFS_TASKS, Context.MODE_PRIVATE).getString(KEY_TASKS_LIST, null)
        if (savedList != null) {
            val items = savedList.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            taskList.addAll(items)
        }

    }

    override fun onResume() {
        super.onResume()

        // Registrerer broadcast receiveren, og oppdaterer den hvert minutt.
        dateTimeTextView.text = getCurrentTimeStamp()
        registerReceiver(tickReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
    }

    override fun onPause() {
        super.onPause()

        // Skrur av broadcast receiveren når activityen er på pause
        // Ekstra sjekk om den er registrert fra før av
        try {
            unregisterReceiver(tickReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(MainActivity.LOG_TAG, "Time tick Receiver not registered", e)
        }

    }

    override fun onStop() {
        super.onStop()

        // Bygger en komma separert string med alle taskene i listen
        // Lagres i SharedPreferences
        val savedList = StringBuilder()
        for (task in taskList) {
            savedList.append(task)
            savedList.append(",")
        }

        getSharedPreferences(PREFS_TASKS, Context.MODE_PRIVATE).edit()
            .putString(KEY_TASKS_LIST, savedList.toString()).apply()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_TASK_REQUEST) {

            if (resultCode == Activity.RESULT_OK) {

                val task = data?.getStringExtra(TaskDescriptionActivity.EXTRA_TASK_DESCRIPTION)
                task?.let {
                    taskList.add(task)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }


    fun addTaskClicked(view: View) {
        val intent = Intent(this, TaskDescriptionActivity::class.java)
        startActivityForResult(intent, ADD_TASK_REQUEST)

    }

    private fun makeAdapter(list: List<String>): ArrayAdapter<String> =
        ArrayAdapter(this, android.R.layout.simple_list_item_1, list)


    private fun makeBroadcastReceiver(): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                if (intent?.action == Intent.ACTION_TIME_TICK) {
                    dateTimeTextView.text = getCurrentTimeStamp()
                }
            }
        }
    }

    private fun taskSelected(position: Int) {
        // endre
        AlertDialog.Builder(this)
            .setTitle(R.string.alert_title)
            .setMessage(taskList[position])

            .setPositiveButton(R.string.delete)
            { _, _ ->
                taskList.removeAt(position)
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton(R.string.cancel)
            { dialog, _ -> dialog.cancel()
            }
            .create()
            .show()
    }


}


