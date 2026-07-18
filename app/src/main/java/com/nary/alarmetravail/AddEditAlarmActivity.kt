package com.nary.alarmetravail

import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nary.alarmetravail.databinding.ActivityAddEditAlarmBinding

/**
 * Ecran de création ou modification d'une alarme pour une personne.
 * Permet de choisir le nom et l'intervalle (1h à 24h, 3h par défaut).
 */
class AddEditAlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditAlarmBinding
    private var editingId: Long? = null

    // Palette de couleurs attribuée automatiquement pour distinguer les personnes
    private val palette = listOf("#3DDC84", "#58A6FF", "#F0883E", "#D2A8FF", "#F778BA", "#79C0FF")

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editingId = intent.getLongExtra(EXTRA_ALARM_ID, -1L).takeIf { it != -1L }

        binding.seekInterval.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val hours = progress + 1
                binding.tvIntervalValue.text = "$hours heure${if (hours > 1) "s" else ""}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        editingId?.let { id ->
            val existing = AlarmStore.findById(this, id)
            if (existing != null) {
                binding.tvHeader.text = "Modifier l'alarme"
                binding.etName.setText(existing.personName)
                val hours = (existing.intervalMillis / (60 * 60 * 1000L)).toInt().coerceIn(1, 24)
                binding.seekInterval.progress = hours - 1
                binding.tvIntervalValue.text = "$hours heure${if (hours > 1) "s" else ""}"
            }
        }

        binding.btnSave.setOnClickListener { save() }
    }

    private fun save() {
        val name = binding.etName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Entrez un nom (ex: Papa, Moi...)", Toast.LENGTH_SHORT).show()
            return
        }

        val hours = binding.seekInterval.progress + 1
        val intervalMillis = hours * 60 * 60 * 1000L

        val id = editingId ?: AlarmStore.nextId(this)
        val existing = editingId?.let { AlarmStore.findById(this, it) }

        val profile = AlarmProfile(
            id = id,
            personName = name,
            intervalMillis = intervalMillis,
            nextTriggerAt = existing?.nextTriggerAt ?: 0L,
            isEnabled = existing?.isEnabled ?: true,
            colorHex = existing?.colorHex ?: palette[(id % palette.size).toInt()]
        )

        AlarmStore.update(this, profile)

        if (profile.isEnabled) {
            AlarmScheduler.scheduleFromNow(this, profile)
        }

        Toast.makeText(this, "Alarme enregistrée pour ${profile.personName}", Toast.LENGTH_SHORT).show()
        finish()
    }
}