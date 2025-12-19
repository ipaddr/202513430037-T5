package com.example.eberas.ui.consumer

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.eberas.data.firebase.FirebaseHelper
import com.example.eberas.data.model.Transaksi
import com.example.eberas.data.model.Pengguna
import com.example.eberas.data.model.Notifikasi
import com.example.eberas.databinding.ActivityOrderDetailConsumerBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderDetailConsumerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderDetailConsumerBinding
    private val firestore: FirebaseFirestore = FirebaseHelper.firestore
    private val auth = FirebaseHelper.auth

    private var currentTransaction: Transaksi? = null

    private val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale("in", "ID"))
    private val formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply { maximumFractionDigits = 0 }
    private val TAG = "OrderDetailConsumer"

    companion object {
        const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityOrderDetailConsumerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = "Detail Pesanan"
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val transactionId = intent.getStringExtra(EXTRA_TRANSACTION_ID)

        if (transactionId.isNullOrEmpty()) {
            Toast.makeText(this, "ID Transaksi tidak ditemukan.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadTransactionDetail(transactionId)
    }

    private fun loadTransactionDetail(id: String) {
        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("transaksi").document(id).get()
            .addOnSuccessListener { document ->
                binding.progressBar.visibility = View.GONE
                if (document.exists()) {
                    val transaction = document.toObject(Transaksi::class.java)
                    if (transaction != null) {
                        displayTransaction(transaction)
                        setupMarkCompleteButton(transaction)
                    }
                } else {
                    Toast.makeText(this, "Pesanan tidak ditemukan.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e(TAG, "Gagal memuat detail transaksi: ${e.message}", e)
                Toast.makeText(this, "Error memuat data.", Toast.LENGTH_LONG).show()
            }
    }

    private fun displayTransaction(transaction: Transaksi) {
        binding.apply {
            tvInvoiceNumber.text = "#INV${transaction.id_transaksi.take(8).uppercase()}"
            tvOrderDate.text = dateFormat.format(Date(transaction.tanggal_transaksi))
            tvOrderStatus.text = mapStatusToConsumer(transaction.status_transaksi)

            tvProductName.text = transaction.nama_produk_snapshot
            val formattedPriceUnit = formatRupiah.format(transaction.harga_per_unit_snapshot)
            tvProductPriceUnit.text = "${formattedPriceUnit}/kg"
            tvProductQuantity.text = "${transaction.jumlah} kg"

            val subtotal = transaction.harga_per_unit_snapshot * transaction.jumlah
            tvSubtotal.text = formatRupiah.format(subtotal)

            tvTotalPayment.text = formatRupiah.format(transaction.total_harga)
            tvPaymentMethod.text = transaction.metode_pembayaran

            getSellerName(transaction.id_penjual) { name ->
                tvSellerName.text = "Toko Penjual: $name"
            }
        }
    }

    private fun setupMarkCompleteButton(transaction: Transaksi) {
        if (transaction.status_transaksi == "dikirim") {
            binding.btnMarkCompleteConsumer.visibility = View.VISIBLE
            binding.btnMarkCompleteConsumer.setOnClickListener {
                showCompletionConfirmationDialog(transaction)
            }
        } else {
            binding.btnMarkCompleteConsumer.visibility = View.GONE
        }
    }

    private fun showCompletionConfirmationDialog(transaction: Transaksi) {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Penerimaan")
            .setMessage("Apakah Anda yakin barang pesanan #${transaction.id_transaksi.take(8).uppercase()} sudah Anda terima dan ingin menyelesaikan pesanan?")
            .setPositiveButton("Ya, Selesai") { dialog, _ ->
                markOrderAsComplete(transaction)
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun markOrderAsComplete(transaction: Transaksi) {
        val transactionId = transaction.id_transaksi

        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("transaksi").document(transactionId)
            .update("status_transaksi", "selesai")
            .addOnSuccessListener {
                Toast.makeText(this, "Pesanan berhasil diselesaikan!", Toast.LENGTH_LONG).show()

                // 🚀 SKENARIO 2: BUAT NOTIFIKASI UNTUK PENJUAL (Pesanan Selesai)
                createNotificationForSeller(transaction)

                loadTransactionDetail(transactionId)
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e(TAG, "Gagal menyelesaikan pesanan: ${e.message}", e)
                Toast.makeText(this, "Gagal menyelesaikan pesanan.", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Mencatat notifikasi ke koleksi 'notifikasi' untuk Penjual (Skenario 2: Order Complete).
     */
    private fun createNotificationForSeller(transaction: Transaksi) {
        val txShortId = transaction.id_transaksi.take(8).uppercase()
        val sellerId = transaction.id_penjual
        val buyerId = auth.currentUser?.uid ?: "Pembeli Tidak Dikenal"

        val title = "Pesanan Diterima Konsumen!"
        val message = "Pesanan #${txShortId} telah dikonfirmasi selesai oleh konsumen. Dana siap dicairkan."

        val notification = Notifikasi(
            id_notifikasi = firestore.collection("notifikasi").document().id,
            id_pengguna = sellerId, // TARGET: ID Penjual
            judul = title,
            pesan = message,
            tanggal = System.currentTimeMillis(),
            is_read = false,
            type = "ORDER_COMPLETE", // Tipe notifikasi
            id_transaksi = transaction.id_transaksi // ID Transaksi untuk navigasi
        )

        firestore.collection("notifikasi").document(notification.id_notifikasi)
            .set(notification)
            .addOnFailureListener { e ->
                Log.e(TAG, "Gagal membuat notifikasi untuk Penjual: ${e.message}", e)
            }
    }

    private fun mapStatusToConsumer(status: String): String {
        return when (status) {
            "menunggu" -> "Menunggu Konfirmasi Penjual"
            "diproses" -> "Sedang Diproses"
            "dikirim" -> "Dikirim"
            "selesai" -> "Pesanan Selesai"
            "dibatalkan" -> "Dibatalkan"
            else -> status
        }
    }

    private fun getSellerName(sellerId: String, callback: (String) -> Unit) {
        firestore.collection("pengguna").document(sellerId).get()
            .addOnSuccessListener { document ->
                val name = document.getString("nama_lengkap") ?: "Toko Tidak Dikenal"
                callback(name)
            }
            .addOnFailureListener {
                callback("Toko (Gagal Load)")
            }
    }
}