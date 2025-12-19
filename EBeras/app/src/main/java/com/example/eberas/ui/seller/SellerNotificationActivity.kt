package com.example.eberas.ui.seller

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

/**
 * Activity untuk menampilkan daftar notifikasi Penjual.
 */
class SellerNotificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationBinding
    private lateinit var notificationsAdapter: SellerNotificationAdapter

    private val firestore = FirebaseHelper.firestore
    private val auth = FirebaseHelper.auth
    private var listenerRegistration: ListenerRegistration? = null

    private val TAG = "SellerNotification"

    // Anda bisa menghapus companion object ini jika tidak digunakan di tempat lain
    // Jika tetap dipertahankan, nilainya tidak akan digunakan dalam putExtra
    companion object {
        const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityNotificationBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(true)
            supportActionBar?.title = "Notifikasi Pesanan"
            binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

            setupRecyclerView()
            loadNotifications()
        } catch (e: Exception) {
            Log.e(TAG, "FATAL CRASH di onCreate: ${e.message}", e)
            Toast.makeText(this, "Kesalahan saat memuat layar notifikasi.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
        if (::notificationsAdapter.isInitialized) {
            notificationsAdapter.notifyDataSetChanged()
        }
    }

    override fun onPause() {
        super.onPause()
        listenerRegistration?.remove()
    }

    private fun setupRecyclerView() {
        notificationsAdapter = SellerNotificationAdapter(emptyList()) { notification ->
            markAsReadAndNavigate(notification)
        }

        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(this@SellerNotificationActivity)
            adapter = notificationsAdapter
        }
    }

    private fun loadNotifications() {
        val currentSellerId = auth.currentUser?.uid
        if (currentSellerId == null) return

        listenerRegistration?.remove()

        listenerRegistration = firestore.collection("notifikasi")
            .whereEqualTo("id_pengguna", currentSellerId)
            .orderBy("tanggal", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots: QuerySnapshot?, error ->
                if (error != null) {
                    Log.e(TAG, "Gagal memuat notifikasi Penjual: ${error.message}", error)
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
     * Menandai notifikasi sebagai sudah dibaca dan menavigasi ke detail pesanan penjual.
     */
    private fun markAsReadAndNavigate(notification: Notifikasi) {

        // 1. Mark as Read (Asinkron)
        if (!notification.is_read) {
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

        // 2. Navigasi ke Detail Pesanan Penjual
        val transactionId = notification.id_transaksi

        if (!transactionId.isNullOrEmpty()) {
            val intent = Intent(this, OrderDetailSellerActivity::class.java).apply {
                // 🔴 PERBAIKAN: Menggunakan kunci "TRANSACTION_ID" agar sesuai dengan OrderDetailSellerActivity.kt
                putExtra("TRANSACTION_ID", transactionId)
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, "Detail pesanan tidak tersedia.", Toast.LENGTH_SHORT).show()
        }
    }
}