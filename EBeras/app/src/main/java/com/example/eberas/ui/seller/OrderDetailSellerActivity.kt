package com.example.eberas.ui.seller

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.eberas.data.firebase.FirebaseHelper
import com.example.eberas.data.model.Pengguna
import com.example.eberas.data.model.Transaksi
import com.example.eberas.data.model.Notifikasi // WAJIB: Import model Notifikasi
import com.example.eberas.databinding.ActivityOrderDetailSellerBinding
import com.example.eberas.R
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Activity untuk detail pesanan yang dilihat oleh Penjual.
 * Memungkinkan pembaruan status pesanan.
 */
class OrderDetailSellerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderDetailSellerBinding
    private val firestore = FirebaseHelper.firestore
    private val formatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
        maximumFractionDigits = 0
    }
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("in", "ID"))
    private val TAG = "OrderDetailSeller"

    private var currentTransaction: Transaksi? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityOrderDetailSellerBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

            // Menggunakan key "TRANSACTION_ID" sesuai dengan yang dikirim dari SellerNotificationActivity
            val transactionId = intent.getStringExtra("TRANSACTION_ID")
            if (transactionId != null) {
                loadTransactionDetail(transactionId)
            } else {
                Toast.makeText(this, "ID Transaksi tidak ditemukan.", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Aplikasi Crash saat memuat Detail Pesanan: ${e.message}", e)
            Toast.makeText(this, "KESALAHAN FATAL: Layout/Binding Detail Pesanan Korup.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun loadTransactionDetail(transactionId: String) {
        firestore.collection("transaksi").document(transactionId).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Toast.makeText(this, "Dokumen transaksi tidak ditemukan di Firestore.", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                currentTransaction = document.toObject(Transaksi::class.java)
                currentTransaction?.let { tx ->
                    renderTransaction(tx)
                    loadBuyerInfo(tx.id_pembeli)
                    setupStatusButton(tx)
                } ?: finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat detail transaksi.", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadBuyerInfo(buyerId: String) {
        firestore.collection("pengguna").document(buyerId).get()
            .addOnSuccessListener { document ->
                val buyer = document.toObject(Pengguna::class.java)
                if (buyer != null) {
                    binding.tvBuyerName.text = "Nama: ${buyer.nama_lengkap}"
                    binding.tvBuyerAddress.text = "Alamat: ${buyer.alamat}"
                    binding.tvBuyerPhone.text = "Telp: ${buyer.nomor_telepon}"
                }
            }
    }

    private fun renderTransaction(tx: Transaksi) {
        val total = tx.total_harga * tx.jumlah

        val date = Date(tx.tanggal_transaksi)

        binding.tvInvoice.text = "#${tx.id_transaksi.take(8)} (${tx.status_transaksi})"
        binding.tvOrderDate.text = "Tanggal: ${dateFormat.format(date)}"
        binding.tvProductOrderedName.text = tx.nama_produk_snapshot
        binding.tvProductOrderedQty.text = "Jumlah: ${tx.jumlah} Kg @ ${formatter.format(tx.harga_per_unit_snapshot)}/Kg"
        binding.tvProductOrderedTotal.text = "Total: ${formatter.format(total)}"
        binding.tvCurrentStatus.text = "Status Saat Ini: ${tx.status_transaksi.uppercase(Locale.ROOT)}"
    }

    private fun setupStatusButton(tx: Transaksi) {
        binding.btnUpdateStatus.visibility = View.GONE
        binding.btnCompleteOrder.visibility = View.GONE

        when (tx.status_transaksi) {
            "menunggu" -> {
                binding.btnUpdateStatus.visibility = View.VISIBLE
                binding.btnUpdateStatus.text = "Konfirmasi Pesanan (Diproses)"
                binding.btnUpdateStatus.setOnClickListener {
                    updateOrderStatus(tx, "diproses")
                }
            }
            "diproses" -> {
                binding.btnUpdateStatus.visibility = View.VISIBLE
                binding.btnUpdateStatus.text = "Siap Dikirim (Dikirim)"
                binding.btnUpdateStatus.setOnClickListener {
                    updateOrderStatus(tx, "dikirim")
                }
                binding.btnCompleteOrder.visibility = View.VISIBLE
                binding.btnCompleteOrder.text = "Batalkan Pesanan"
                binding.btnCompleteOrder.backgroundTintList = ContextCompat.getColorStateList(this, R.color.red_light)
                binding.btnCompleteOrder.setOnClickListener {
                    updateOrderStatus(tx, "dibatalkan")
                }
            }
            "dikirim" -> {
                binding.btnUpdateStatus.visibility = View.VISIBLE
                binding.btnUpdateStatus.text = "Tandai Selesai (Selesai)"
                binding.btnUpdateStatus.backgroundTintList = ContextCompat.getColorStateList(this, R.color.green_accent)
                binding.btnUpdateStatus.setOnClickListener {
                    updateOrderStatus(tx, "selesai")
                }
            }
            "selesai", "dibatalkan" -> {
                binding.tvCurrentStatus.append(" - Aksi Tidak Tersedia.")
            }
        }
    }

    private fun updateOrderStatus(transaction: Transaksi, newStatus: String) {
        val transactionId = transaction.id_transaksi
        val updates = mapOf("status_transaksi" to newStatus)

        firestore.collection("transaksi").document(transactionId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Status berhasil diperbarui menjadi $newStatus", Toast.LENGTH_SHORT).show()
                // >>> PERBAIKAN: Setelah sukses update status, BUAT NOTIFIKASI BARU <<<
                createNotification(transaction, newStatus)
                // Muat ulang detail
                loadTransactionDetail(transactionId)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memperbarui status: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * FUNGSI BARU: Mencatat notifikasi ke koleksi 'notifikasi' di Firestore.
     * PERBAIKAN: id_transaksi DITAMBAHKAN di sini.
     */
    private fun createNotification(transaction: Transaksi, newStatus: String) {
        val txShortId = transaction.id_transaksi.take(8).uppercase()
        val buyerId = transaction.id_pembeli

        val (title, message) = when (newStatus) {
            "diproses" -> Pair(
                "Pesanan Dikonfirmasi!",
                "Pesanan #${txShortId} Anda telah dikonfirmasi dan sedang disiapkan oleh penjual."
            )
            "dikirim" -> Pair(
                "Pesanan Sedang Dikirim!",
                "Pesanan #${txShortId} sedang dalam perjalanan ke alamat Anda."
            )
            "selesai" -> Pair(
                "Pesanan Selesai",
                "Pesanan #${txShortId} telah ditandai selesai. Terima kasih telah berbelanja!"
            )
            "dibatalkan" -> Pair(
                "Pesanan Dibatalkan",
                "Pesanan #${txShortId} telah dibatalkan oleh penjual."
            )
            else -> return
        }

        val notification = Notifikasi(
            id_notifikasi = firestore.collection("notifikasi").document().id,
            id_pengguna = buyerId,
            judul = title,
            pesan = message,
            tanggal = System.currentTimeMillis(),
            is_read = false,
            type = "ORDER_STATUS",
            // 🔴 PERBAIKAN KRITIS: Tambahkan ID Transaksi di sini
            id_transaksi = transaction.id_transaksi
        )

        // Simpan notifikasi ke Firestore
        firestore.collection("notifikasi").document(notification.id_notifikasi)
            .set(notification)
            .addOnFailureListener { e ->
                Log.e(TAG, "Gagal membuat notifikasi untuk Pembeli: ${e.message}", e)
            }
    }
}