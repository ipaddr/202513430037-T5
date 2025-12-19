package com.example.eberas.ui.seller

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView // Impor untuk TextView badge
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eberas.R
import com.example.eberas.data.firebase.FirebaseHelper
import com.example.eberas.data.model.Transaksi
import com.example.eberas.data.model.Pengguna
import com.example.eberas.databinding.ActivitySellerDashboardBinding
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot // Impor untuk QuerySnapshot
import java.text.NumberFormat
import java.util.Locale

/**
 * Activity utama untuk tampilan Penjual (Dashboard).
 */
class SellerDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySellerDashboardBinding
    private lateinit var orderAdapter: OrderAdapter

    private val firestore = FirebaseHelper.firestore
    private val auth = FirebaseHelper.auth
    private var orderListener: ListenerRegistration? = null
    private var productCountListener: ListenerRegistration? = null
    // 🌟 BARU: Listener untuk badge notifikasi
    private var notificationBadgeListener: ListenerRegistration? = null

    private val currentSellerId: String? get() = auth.currentUser?.uid
    private val TAG = "SellerDashboard"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivitySellerDashboardBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupBottomNavigation()
            setupRecyclerView()
            setupListeners()
        } catch (e: Exception) {
            Log.e(TAG, "Aplikasi Crash saat memuat Dashboard: ${e.message}", e)
            Toast.makeText(this, "Kesalahan fatal saat memuat tampilan Penjual. Lihat Logcat.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadSellerName()
        loadDashboardData()

        // 🌟 AKTIVASI: Mulai listener untuk badge notifikasi
        startNotificationBadgeListener()

        binding.bottomNavigationSeller.selectedItemId = R.id.nav_dashboard
    }

    override fun onPause() {
        super.onPause()
        orderListener?.remove()
        productCountListener?.remove()
        // 🌟 NON-AKTIF: Hapus listener badge saat activity berhenti
        notificationBadgeListener?.remove()
    }

    override fun onDestroy() {
        super.onDestroy()
        orderListener?.remove()
        productCountListener?.remove()
        notificationBadgeListener?.remove()
    }

    private fun setupRecyclerView() {
        orderAdapter = OrderAdapter(
            emptyList(),
            onItemClick = { transaction -> handleOrderClick(transaction) },
            onItemLongClick = { transaction -> handleOrderLongClick(transaction) }
        )

        binding.rvRecentOrders.apply {
            layoutManager = LinearLayoutManager(this@SellerDashboardActivity)
            adapter = orderAdapter
        }
    }

    private fun loadSellerName() {
        val userId = currentSellerId
        if (userId == null) {
            binding.tvSellerName.text = "Error: Belum Login"
            return
        }

        firestore.collection("pengguna").document(userId).get()
            .addOnSuccessListener { document ->
                val seller = document.toObject(Pengguna::class.java)
                val sellerName = seller?.nama_lengkap ?: "Toko Tidak Ditemukan"

                binding.tvSellerName.text = sellerName
            }
            .addOnFailureListener {
                Log.e(TAG, "Gagal memuat nama penjual: ${it.message}")
                binding.tvSellerName.text = "Toko (Gagal Load)"
            }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationSeller.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_my_products -> {
                    startActivity(Intent(this, MyProductsActivity::class.java))
                    true
                }
                R.id.nav_profile_seller -> {
                    startActivity(Intent(this, SellerProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun loadDashboardData() {
        if (currentSellerId == null) {
            Toast.makeText(this, "Silakan login sebagai penjual.", Toast.LENGTH_LONG).show()
            return
        }

        orderListener?.remove()
        productCountListener?.remove()

        val ordersQuery = firestore.collection("transaksi")
            .whereEqualTo("id_penjual", currentSellerId)

        orderListener = ordersQuery.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.e(TAG, "Gagal memuat pesanan: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshots != null) {
                val allOrders = snapshots.toObjects(Transaksi::class.java)

                val newOrders = allOrders.count { it.status_transaksi == "menunggu" }

                val totalRevenue: Long = allOrders
                    .filter { it.status_transaksi == "selesai" }
                    .sumOf { it.total_harga }

                binding.tvNewOrdersCount.text = newOrders.toString()
                binding.tvTotalRevenue.text = formatRupiah(totalRevenue)

                val recentOrders = allOrders.sortedByDescending { it.tanggal_transaksi }.take(3)
                orderAdapter.updateData(recentOrders)
            }
        }

        val productsQuery = firestore.collection("produk")
            .whereEqualTo("id_penjual", currentSellerId)

        productCountListener = productsQuery.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.e(TAG, "Gagal memuat produk aktif: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshots != null) {
                binding.tvActiveProductsCount.text = snapshots.size().toString()
            }
        }
    }

    // 🌟 FUNGSI BARU: Mendengarkan jumlah notifikasi yang belum dibaca secara real-time.
    private fun startNotificationBadgeListener() {
        val userId = currentSellerId
        if (userId == null) return

        notificationBadgeListener?.remove()

        // Filter notifikasi yang ditujukan ke penjual saat ini DAN belum dibaca
        notificationBadgeListener = firestore.collection("notifikasi")
            .whereEqualTo("id_pengguna", userId)
            .whereEqualTo("is_read", false)
            .addSnapshotListener { snapshots: QuerySnapshot?, error ->
                if (error != null) {
                    Log.e(TAG, "Gagal menghitung notifikasi: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    updateNotificationBadge(snapshots.size())
                }
            }
    }

    // 🌟 FUNGSI BARU: Memperbarui UI badge.
    private fun updateNotificationBadge(count: Int) {
        // Mengakses TextView badge dari app bar layout menggunakan ID baru
        val badge = binding.appBarLayoutSeller.findViewById<TextView>(R.id.tv_notification_badge_seller)

        if (count > 0) {
            badge?.visibility = View.VISIBLE
            // Tampilkan "9+" jika count > 9
            badge?.text = if (count > 9) "9+" else count.toString()
        } else {
            badge?.visibility = View.GONE
        }
    }


    private fun setupListeners() {
        binding.fabAddProduct.setOnClickListener {
            startActivity(Intent(this, ManageProductActivity::class.java))
        }

        // 🌟 PERUBAHAN: Listener dipasang di FrameLayout (fl_notification_container_seller)
        // untuk menangkap klik tombol notifikasi.
        binding.appBarLayoutSeller.findViewById<View>(R.id.fl_notification_container_seller)?.setOnClickListener {
            val intent = Intent(this, SellerNotificationActivity::class.java)
            startActivity(intent)
        }
    }

    private fun handleOrderClick(transaction: Transaksi) {
        val intent = Intent(this, OrderDetailSellerActivity::class.java).apply {
            putExtra("TRANSACTION_ID", transaction.id_transaksi)
        }
        startActivity(intent)
    }

    private fun handleOrderLongClick(transaction: Transaksi): Boolean {
        val status = transaction.status_transaksi

        if (status == "selesai" || status == "dibatalkan") {
            showDeleteConfirmationDialog(transaction)
            return true
        } else {
            Toast.makeText(this, "Pesanan masih aktif ($status). Tidak bisa dihapus.", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    private fun showDeleteConfirmationDialog(transaction: Transaksi) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Histori Pesanan")
            .setMessage("Apakah Anda yakin ingin menghapus histori pesanan #${transaction.id_transaksi.take(8).uppercase()}? Aksi ini tidak dapat dibatalkan.")
            .setPositiveButton("Hapus") { dialog, _ ->
                deleteOrderHistory(transaction.id_transaksi)
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteOrderHistory(transactionId: String) {
        firestore.collection("transaksi").document(transactionId).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Histori pesanan berhasil dihapus.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal menghapus histori: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun formatRupiah(value: Long): String {
        val formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        val formatted = formatRupiah.format(value).replace("Rp", "").trim()
        return "Rp ${formatted}"
    }
}