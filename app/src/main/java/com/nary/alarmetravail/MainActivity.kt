package com.nary.alarmetravail

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.nary.alarmetravail.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: AlarmListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = AlarmListAdapter(
            items = AlarmStore.loadAll(this),
            onToggle = { profile, checked -> toggleAlarm(profile, checked) },
            onDelete = { profile -> deleteAlarm(profile) },
            onClick = { profile -> openEdit(profile.id) }
        )
        binding.recyclerAlarms.layoutManager = LinearLayoutManager(this)
        binding.recyclerAlarms.adapter = adapter

        binding.fabAdd.setOnClickListener { openEdit(null) }

        requestPermissionsIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        val list = AlarmStore.loadAll(this)
        adapter.updateData(list)
        binding.tvEmpty.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun toggleAlarm(profile: AlarmProfile, enabled: Boolean) {
        profile.isEnabled = enabled
        if (enabled) {
            AlarmScheduler.scheduleFromNow(this, profile)
        } else {
            AlarmScheduler.cancel(this, profile)
            AlarmStore.update(this, profile)
        }
        refreshList()
    }

    private fun deleteAlarm(profile: AlarmProfile) {
        AlarmScheduler.cancel(this, profile)
        AlarmStore.delete(this, profile.id)
        refreshList()
    }

    private fun openEdit(alarmId: Long?) {
        val intent = Intent(this, AddEditAlarmActivity::class.java)
        if (alarmId != null) intent.putExtra(AddEditAlarmActivity.EXTRA_ALARM_ID, alarmId)
        startActivity(intent)
    }

    /** Demande la permission notifications (Android 13+) et les alarmes exactes (Android 12+). */
    private fun requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}