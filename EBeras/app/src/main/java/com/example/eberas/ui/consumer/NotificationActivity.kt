package com.example.eberas.ui.consumer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eberas.data.firebase.FirebaseHelper
import com.example.eberas.data.model.Notifikasi
import com.example.eberas.databinding.ActivityNotificationBinding
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.example.eberas.ui.consumer.OrderDetailConsumerActivity

/**
 * Activity untuk menampilkan daftar notifikasi konsumen.
 */
class NotificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationBinding
    private lateinit var notificationsAdapter: NotificationAdapter

    private val firestore = FirebaseHelper.firestore
    private val auth = FirebaseHelper.auth
    private var listenerRegistration: ListenerRegistration? = null

    private val TAG = "NotificationActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = "Notifikasi"
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupRecyclerView()
        loadNotifications()
    }

    override fun onResume() {
        super.onResume()
        // 🟢 PERBAIKAN: Pastikan listener aktif lagi dan data dimuat ulang saat kembali
        loadNotifications()
        // Force refresh adapter jika diperlukan (walaupun listener sudah ada, ini mengatasi edge cases)
        if (::notificationsAdapter.isInitialized) {
            notificationsAdapter.notifyDataSetChanged()
        }
    }

    override fun onPause() {
        super.onPause()
        listenerRegistration?.remove()
    }

    private fun setupRecyclerView() {
        notificationsAdapter = NotificationAdapter(emptyList()) { notification ->
            markAsReadAndNavigate(notification)
        }

        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(this@NotificationActivity)
            adapter = notificationsAdapter
        }
    }

    private fun loadNotifications() {
        val userId = auth.currentUser?.uid
        if (userId == null) return

        // 💡 Pastikan listener lama dilepas sebelum membuat yang baru di onResume
        listenerRegistration?.remove()

        listenerRegistration = firestore.collection("notifikasi")
            .whereEqualTo("id_pengguna", userId)
            .orderBy("tanggal", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots: QuerySnapshot?, error ->
                if (error != null) {
                    Log.e(TAG, "Gagal memuat notifikasi: ${error.message}")
                    Toast.makeText(this, "Gagal memuat notifikasi.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val notifications = snapshots.toObjects(Notifikasi::class.java)
                    notificationsAdapter.updateData(notifications)

                    if (notifications.isEmpty()) {
                        binding.tvEmptyNotifications.visibility = View.VISIBLE
                        binding.rvNotifications.visibility = View.GONE
                    } else {
                        binding.tvEmptyNotifications.visibility = View.GONE
                        binding.rvNotifications.visibility = View.VISIBLE
                    }
                }
            }
    }

    /**
     * Menandai notifikasi sebagai sudah dibaca dan menavigasi ke detail pesanan.
     */
    private fun markAsReadAndNavigate(notification: Notifikasi) {

        // 1. Mark as Read (Asinkron)
        if (!notification.is_read) {

            // Perbaikan untuk mengatasi konflik field _read dan is_read di database
            // Kami hanya menggunakan is_read, tetapi memastikan _read juga di-update untuk mencegah konflik deserialisasi.
            val updateMap = mapOf(
                "is_read" to true,
                "_read" to true
            )

            firestore.collection("notifikasi").document(notification.id_notifikasi)
                .update(updateMap)
                .addOnFailureListener { e ->
                    Log.e(TAG, "Gagal menandai notifikasi sebagai dibaca: ${e.message}")
                }
        }

        // 2. Navigasi ke Detail Pesanan MENGGUNAKAN FIELD id_transaksi
        val transactionId = notification.id_transaksi

        Log.d(TAG, "Mencoba navigasi. ID Transaksi yang dibawa: $transactionId")

        if (!transactionId.isNullOrEmpty()) {
            val intent = Intent(this, OrderDetailConsumerActivity::class.java).apply {
                putExtra(OrderDetailConsumerActivity.EXTRA_TRANSACTION_ID, transactionId)
            }
            startActivity(intent)
        } else {
            Log.e(TAG, "Navigasi gagal: ID Transaksi kosong untuk notifikasi ini.")
            Toast.makeText(this, "Detail pesanan tidak tersedia untuk notifikasi ini.", Toast.LENGTH_SHORT).show()
        }
    }
}