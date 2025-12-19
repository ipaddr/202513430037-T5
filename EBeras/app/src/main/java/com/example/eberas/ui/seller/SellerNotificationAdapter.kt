package com.example.eberas.ui.seller

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.eberas.R
import com.example.eberas.data.model.Notifikasi
import com.example.eberas.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter untuk menampilkan daftar Notifikasi di sisi Penjual.
 */
class SellerNotificationAdapter(
    private var notificationList: List<Notifikasi>,
    private val onNotificationClick: (Notifikasi) -> Unit // Callback klik
) : RecyclerView.Adapter<SellerNotificationAdapter.NotificationViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

    fun updateData(newNotificationList: List<Notifikasi>) {
        notificationList = newNotificationList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        // Asumsi menggunakan layout yang sama: item_notification.xml
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
                    indicatorUnread.visibility = View.GONE
                    root.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.white))
                    tvNotificationTitle.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.darker_gray))
                } else {
                    indicatorUnread.visibility = View.VISIBLE
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