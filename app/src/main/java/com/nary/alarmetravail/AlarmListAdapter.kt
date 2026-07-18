package com.nary.alarmetravail

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nary.alarmetravail.databinding.ItemAlarmBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AlarmListAdapter(
    private var items: MutableList<AlarmProfile>,
    private val onToggle: (AlarmProfile, Boolean) -> Unit,
    private val onDelete: (AlarmProfile) -> Unit,
    private val onClick: (AlarmProfile) -> Unit
) : RecyclerView.Adapter<AlarmListAdapter.VH>() {

    inner class VH(val binding: ItemAlarmBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAlarmBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val profile = items[position]
        val b = holder.binding

        b.tvName.text = profile.personName
        try {
            b.avatarDot.background.setTint(Color.parseColor(profile.colorHex))
        } catch (_: Exception) { /* couleur invalide, on garde la couleur par défaut */ }

        val hours = TimeUnit.MILLISECONDS.toHours(profile.intervalMillis)
        val timeFmt = SimpleDateFormat("HH:mm", Locale.FRANCE)
        val nextText = if (profile.nextTriggerAt > 0) {
            "prochaine à ${timeFmt.format(Date(profile.nextTriggerAt))}"
        } else {
            "pas encore programmée"
        }
        b.tvInfo.text = "Toutes les ${hours}h · $nextText"

        b.switchEnabled.setOnCheckedChangeListener(null)
        b.switchEnabled.isChecked = profile.isEnabled
        b.switchEnabled.setOnCheckedChangeListener { _, checked ->
            onToggle(profile, checked)
        }

        b.btnDelete.setOnClickListener { onDelete(profile) }
        b.root.setOnClickListener { onClick(profile) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<AlarmProfile>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }
}