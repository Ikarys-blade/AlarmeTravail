package com.nary.alarmetravail

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.nary.alarmetravail.databinding.ActivityAlarmRingBinding

/**
 * Ecran plein écran affiché quand une alarme sonne, même téléphone verrouillé.
 * Contient les deux actions demandées :
 *  - "Arrêter" : stoppe la sonnerie en cours (comme un réveil classique)
 *  - "J'ai terminé le travail" : stoppe la sonnerie ET relance le compte à
 *    rebours de 3h à partir de maintenant
 */
class AlarmRingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmRingBinding
    private var alarmId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Affiche l'activité par-dessus l'écran verrouillé et allume l'écran
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        binding = ActivityAlarmRingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        alarmId = intent.getLongExtra(AlarmRingService.EXTRA_ALARM_ID, -1L)
        val profile = AlarmStore.findById(this, alarmId)
        binding.tvPersonName.text = profile?.personName ?: "—"

        binding.btnStop.setOnClickListener {
            sendServiceAction(AlarmRingService.ACTION_STOP)
            finish()
        }

        binding.btnDone.setOnClickListener {
            sendServiceAction(AlarmRingService.ACTION_DONE)
            finish()
        }

        // Empêche de fermer l'écran d'alarme avec le bouton retour :
        // on force l'utilisateur à choisir "Arrêter" ou "J'ai terminé le travail"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Ne rien faire volontairement
            }
        })
    }

    private fun sendServiceAction(action: String) {
        val intent = Intent(this, AlarmRingService::class.java).apply {
            this.action = action
            putExtra(AlarmRingService.EXTRA_ALARM_ID, alarmId)
        }
        startService(intent)
    }
}