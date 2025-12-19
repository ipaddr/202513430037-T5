package com.example.eberas.ui.consumer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.eberas.R // Pastikan ini diimpor untuk mengakses R.color.light_gray
import com.example.eberas.data.model.Notifikasi
import com.example.eberas.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter untuk menampilkan daftar Notifikasi.
 */
class NotificationAdapter(
    private var notificationList: List<Notifikasi>,
    private val onNotificationClick: (Notifikasi) -> Unit // Callback klik
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

    fun updateData(newNotificationList: List<Notifikasi>) {
        notificationList = newNotificationList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notificationList[position])
    }

    override fun getItemCount(): Int = notificationList.size

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notifikasi) {
            binding.apply {
                tvNotificationTitle.text = notification.judul
                tvNotificationMessage.text = notification.pesan

                // Format tanggal
                tvNotificationDate.text = dateFormat.format(Date(notification.tanggal))

                // Atur status baca/belum baca
                if (notification.is_read) {
                    // 🟢 Kunci Perbaikan: Sembunyikan indikator
                    indicatorUnread.visibility = View.GONE

                    // Warna untuk item yang sudah dibaca
                    root.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.white))
                    tvNotificationTitle.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.darker_gray))
                } else {
                    // Tampilkan indikator
                    indicatorUnread.visibility = View.VISIBLE

                    // Warna untuk item yang belum dibaca (menggunakan R.color.light_gray yang sudah dikonfirmasi)
                    root.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.light_gray))
                    tvNotificationTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                }

                // Listener klik untuk menandai sudah dibaca dan menavigasi
                binding.root.setOnClickListener {
                    onNotificationClick(notification)
                }
            }
        }
    }
}