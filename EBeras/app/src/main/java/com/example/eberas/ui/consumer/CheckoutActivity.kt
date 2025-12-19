package com.example.eberas.ui.consumer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.eberas.R
import com.example.eberas.data.firebase.FirebaseHelper
import com.example.eberas.data.local.CartManager
import com.example.eberas.data.model.CartItem
import com.example.eberas.data.model.Pengguna
import com.example.eberas.data.model.Produk
import com.example.eberas.data.model.Transaksi
import com.example.eberas.data.model.Notifikasi // WAJIB: Import Notifikasi
import com.example.eberas.databinding.ActivityCheckoutBinding
import com.google.firebase.firestore.FieldValue
import java.text.NumberFormat
import java.util.Locale

/**
 * Activity untuk memproses pembayaran akhir (Checkout).
 * Menangani mode "Beli Langsung" dan "Checkout Keranjang".
 */
class CheckoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutBinding
    private val firestore = FirebaseHelper.firestore
    private val auth = FirebaseHelper.auth
    private val formatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
        maximumFractionDigits = 0
    }

    private var cartItems: List<CartItem> = emptyList()
    private var isBuyNow: Boolean = false
    private var totalPayment: Long = 0L
    private val TAG = "CheckoutActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityCheckoutBinding.inflate(layoutInflater)
            setContentView(binding.root)

            supportActionBar?.title = "Pembayaran"

            isBuyNow = intent.getBooleanExtra("IS_BUY_NOW", false)

            if (isBuyNow) {
                handleBuyNow(intent)
            } else {
                handleCartCheckout()
            }

            setupListeners()

        } catch (e: Exception) {
            Log.e(TAG, "Aplikasi Crash saat memuat Checkout: ${e.message}", e)
            Toast.makeText(this, "Kesalahan fatal saat memuat Checkout. Lihat Logcat: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserData()

        if (cartItems.isNotEmpty()) {
            displayCheckoutSummary()
        }
    }

    private fun handleBuyNow(intent: Intent) {
        val productId = intent.getStringExtra("PRODUCT_ID")
        val quantity = intent.getIntExtra("QUANTITY", 0)

        if (productId != null && quantity > 0) {
            firestore.collection("produk").document(productId).get()
                .addOnSuccessListener { doc ->
                    val produk = doc.toObject(Produk::class.java)
                    if (produk != null) {
                        cartItems = listOf(CartItem(produk, quantity))
                        displayCheckoutSummary()
                    } else {
                        Toast.makeText(this, "Produk Beli Langsung tidak ditemukan.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal memuat detail produk.", Toast.LENGTH_LONG).show()
                    finish()
                }
        } else {
            Toast.makeText(this, "Data Beli Langsung tidak valid.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun handleCartCheckout() {
        cartItems = CartManager.getItems()
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Keranjang kosong. Tidak dapat melanjutkan pembayaran.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        displayCheckoutSummary()
    }

    private fun displayCheckoutSummary() {
        val subtotal = cartItems.sumOf { it.produk.harga * it.kuantitas }
        val shippingCost = if (binding.rbDelivery.isChecked) 15000L else 0L
        totalPayment = subtotal + shippingCost

        binding.tvSubtotal.text = formatter.format(subtotal)
        binding.tvShippingCost.text = formatter.format(shippingCost)

        val firstProduct = cartItems.firstOrNull()?.produk

        binding.item1.tvProductNameCheckout.text = firstProduct?.nama_produk ?: "Item Campuran"

        val totalQuantity = cartItems.sumOf { it.kuantitas }
        val itemLabel = if (isBuyNow) "${totalQuantity} kg" else "Total ${cartItems.size} item"
        binding.item1.tvProductQuantityCheckout.text = itemLabel

        binding.item1.tvProductPriceCheckout.text = formatter.format(subtotal)

        binding.item1.root.visibility = View.VISIBLE
        binding.item2.root.visibility = View.GONE

        firstProduct?.let { product ->
            Glide.with(this)
                .load(product.foto_produk)
                .placeholder(R.drawable.placeholder_rice)
                .into(binding.item1.ivProductCheckout)
        }

        binding.tvTotalPayment.text = formatter.format(totalPayment)
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User tidak terautentikasi.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        firestore.collection("pengguna").document(userId).get()
            .addOnSuccessListener { doc ->
                val user = doc.toObject(Pengguna::class.java)
                user?.let {
                    binding.tvUserName.text = it.nama_lengkap
                    binding.tvUserAddress.text = it.alamat
                    binding.tvUserPhone.text = it.nomor_telepon
                } ?: run {
                    Toast.makeText(this, "Data pembeli tidak lengkap.", Toast.LENGTH_SHORT).show()
                    binding.tvUserAddress.text = "Mohon lengkapi data profil Anda."
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Gagal memuat data user: ${it.message}")
                Toast.makeText(this, "Gagal memuat data pengguna.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupListeners() {
        binding.btnPayOrder.setOnClickListener {
            processPayment()
        }

        binding.tvChangeAddress.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        binding.rgShippingMethod.setOnCheckedChangeListener { _, _ ->
            displayCheckoutSummary()
        }
        binding.cardDelivery.setOnClickListener {
            binding.rbDelivery.isChecked = true
            binding.rbPickup.isChecked = false
            displayCheckoutSummary()
        }
        binding.cardPickup.setOnClickListener {
            binding.rbPickup.isChecked = true
            binding.rbDelivery.isChecked = false
            displayCheckoutSummary()
        }

        binding.cardTransfer.setOnClickListener {
            binding.rbTransfer.isChecked = true
            binding.rbEwallet.isChecked = false
        }

        binding.cardEwallet.setOnClickListener {
            binding.rbEwallet.isChecked = true
            binding.rbTransfer.isChecked = false
        }
    }

    private fun processPayment() {
        if (cartItems.isEmpty() || totalPayment <= 0L) {
            Toast.makeText(this, "Tidak ada item untuk diproses.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedPaymentMethod = when {
            binding.rbTransfer.isChecked -> "Transfer Bank"
            binding.rbEwallet.isChecked -> "E-Wallet"
            else -> {
                Toast.makeText(this, "Mohon pilih metode pembayaran.", Toast.LENGTH_LONG).show()
                return
            }
        }

        binding.btnPayOrder.isEnabled = false
        binding.btnPayOrder.text = "Memproses..."

        val batch = firestore.batch()
        val transactionCollection = firestore.collection("transaksi")
        val productCollection = firestore.collection("produk")

        var allUpdatesSuccessful = true
        val createdTransactions = mutableListOf<Transaksi>()

        for (item in cartItems) {
            val buyerId = auth.currentUser?.uid ?: return
            val sellerId = item.produk.id_penjual
            val newStock = item.produk.stok - item.kuantitas

            if (newStock < 0) {
                allUpdatesSuccessful = false
                Toast.makeText(this, "Stok ${item.produk.nama_produk} tidak cukup!", Toast.LENGTH_LONG).show()
                binding.btnPayOrder.isEnabled = true
                binding.btnPayOrder.text = "Bayar Pesanan"
                return
            }

            // 1. Buat Objek Transaksi
            val transaction = Transaksi(
                id_transaksi = transactionCollection.document().id,
                id_pembeli = buyerId,
                id_penjual = sellerId,
                id_produk = item.produk.id_produk,
                jumlah = item.kuantitas,
                total_harga = item.produk.harga * item.kuantitas,
                tanggal_transaksi = System.currentTimeMillis(),
                status_transaksi = "menunggu",
                metode_pembayaran = selectedPaymentMethod,
                nama_produk_snapshot = item.produk.nama_produk,
                harga_per_unit_snapshot = item.produk.harga
            )
            createdTransactions.add(transaction)

            // 2. Tambahkan Transaksi ke Batch
            val transactionRef = transactionCollection.document(transaction.id_transaksi)
            batch.set(transactionRef, transaction)

            // 3. Kurangi Stok Produk di Batch
            val productRef = productCollection.document(item.produk.id_produk)
            batch.update(productRef, mapOf(
                "stok" to FieldValue.increment(-item.kuantitas.toLong())
            ))
        }

        if (allUpdatesSuccessful) {
            batch.commit()
                .addOnSuccessListener {
                    if (!isBuyNow) { CartManager.clearCart() }

                    Toast.makeText(this, "Transaksi Berhasil! Menunggu Konfirmasi Penjual.", Toast.LENGTH_LONG).show()

                    // 🚀 SKENARIO 1: BUAT NOTIFIKASI UNTUK PENJUAL (Pesanan Baru)
                    createdTransactions.forEach { tx ->
                        createNotificationForSellerNewOrder(tx)
                    }

                    val intent = Intent(this, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Gagal melakukan Transaksi: ${e.message}")
                    Toast.makeText(this, "Transaksi Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnPayOrder.isEnabled = true
                    binding.btnPayOrder.text = "Bayar Pesanan"
                }
        } else {
            binding.btnPayOrder.isEnabled = true
            binding.btnPayOrder.text = "Bayar Pesanan"
        }
    }

    /**
     * Mencatat notifikasi ke koleksi 'notifikasi' untuk Penjual (Skenario 1: Pesanan Baru).
     */
    private fun createNotificationForSellerNewOrder(transaction: Transaksi) {
        val sellerId = transaction.id_penjual
        val txShortId = transaction.id_transaksi.take(8).uppercase()

        val title = "PESANAN BARU MASUK! 📦"
        val message = "Pesanan #${txShortId} masuk dengan status Menunggu Konfirmasi. Cek Dashboard Penjual Anda."

        val notification = Notifikasi(
            id_notifikasi = firestore.collection("notifikasi").document().id,
            id_pengguna = sellerId, // TARGET: ID Penjual
            judul = title,
            pesan = message,
            tanggal = System.currentTimeMillis(),
            is_read = false,
            type = "NEW_ORDER",
            id_transaksi = transaction.id_transaksi
        )

        firestore.collection("notifikasi").document(notification.id_notifikasi)
            .set(notification)
            .addOnFailureListener { e ->
                Log.e(TAG, "Gagal membuat notifikasi Pesanan Baru untuk Penjual: ${e.message}", e)
            }
    }
}