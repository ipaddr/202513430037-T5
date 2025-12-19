package com.example.eberas.ui.consumer

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eberas.R
import com.example.eberas.data.firebase.FirebaseHelper
import com.example.eberas.data.model.Transaksi
import com.example.eberas.databinding.ActivityOrderHistoryBinding
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.Locale

/**
 * Activity untuk menampilkan riwayat pesanan Konsumen.
 * Melacak pesanan yang sudah dibayar dan sedang dalam proses.
 */
class OrderHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderHistoryBinding
    private lateinit var orderHistoryAdapter: OrderHistoryAdapter

    private val firestore = FirebaseHelper.firestore
    private val auth = FirebaseHelper.auth
    private var historyListener: ListenerRegistration? = null

    private val currentBuyerId: String? get() = auth.currentUser?.uid
    // Tambahkan TAG untuk logging
    private val TAG = "OrderHistoryActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = "Riwayat Pesanan"
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        loadOrderHistory()
    }

    override fun onPause() {
        super.onPause()
        historyListener?.remove()
    }

    private fun setupRecyclerView() {
        // OrderHistoryAdapter adalah adapter yang mirip OrderAdapter (Penjual)
        orderHistoryAdapter = OrderHistoryAdapter(emptyList())

        binding.rvOrderHistory.apply {
            layoutManager = LinearLayoutManager(this@OrderHistoryActivity)
            adapter = orderHistoryAdapter
        }
    }

    private fun loadOrderHistory() {
        val buyerId = currentBuyerId
        if (buyerId == null) {
            Log.w(TAG, "Buyer ID is null. User not logged in.")
            binding.tvEmptyHistory.text = "Anda harus login untuk melihat riwayat pesanan."
            binding.tvEmptyHistory.visibility = View.VISIBLE
            return
        }

        Log.d(TAG, "Mencoba memuat riwayat pesanan untuk Buyer ID: $buyerId")

        historyListener = firestore.collection("transaksi")
            .whereEqualTo("id_pembeli", buyerId)
            .orderBy("tanggal_transaksi", Query.Direction.DESCENDING) // Urutkan dari yang terbaru
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Gagal memuat riwayat: ${error.message}", error)
                    // Cek di Logcat jika ada error PERMISSION_DENIED
                    Toast.makeText(this, "Gagal memuat riwayat pesanan. Cek Logcat untuk error Security Rules.", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val orders = snapshots.toObjects(Transaksi::class.java)

                    if (orders.isEmpty()) {
                        Log.i(TAG, "Tidak ditemukan riwayat pesanan untuk Buyer ID: $buyerId")
                        binding.tvEmptyHistory.text = "Anda belum memiliki riwayat pesanan."
                        binding.tvEmptyHistory.visibility = View.VISIBLE
                        binding.rvOrderHistory.visibility = View.GONE
                    } else {
                        Log.d(TAG, "Ditemukan ${orders.size} pesanan. Memperbarui adapter.")
                        orderHistoryAdapter.updateData(orders)
                        binding.rvOrderHistory.visibility = View.VISIBLE
                        binding.tvEmptyHistory.visibility = View.GONE
                    }
                }
            }
    }
}