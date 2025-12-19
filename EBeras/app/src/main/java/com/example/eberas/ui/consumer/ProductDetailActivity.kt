package com.example.eberas.ui.consumer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.eberas.R
import com.example.eberas.data.firebase.FirebaseHelper
import com.example.eberas.data.local.CartManager
import com.example.eberas.data.model.CartItem
import com.example.eberas.data.model.Produk
import com.example.eberas.data.model.Pengguna // Import Pengguna
import com.example.eberas.databinding.ActivityProductDetailBinding
import java.text.NumberFormat
import java.util.Locale

/**
 * Activity untuk menampilkan detail produk dan menambahkan ke keranjang.
 */
class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private val firestore = FirebaseHelper.firestore
    private var currentProduct: Produk? = null
    private var currentKuantitas = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val productId = intent.getStringExtra("PRODUCT_ID")

        if (productId != null) {
            loadProductDetail(productId)
        } else {
            Toast.makeText(this, "ID Produk tidak ditemukan.", Toast.LENGTH_LONG).show()
            finish()
        }

        setupKuantitasPicker()
        setupListeners()
    }

    private fun setupKuantitasPicker() {
        binding.tvQuantity.text = currentKuantitas.toString() // PERBAIKAN BINDING

        binding.btnMinus.setOnClickListener {
            if (currentKuantitas > 1) {
                currentKuantitas--
                binding.tvQuantity.text = currentKuantitas.toString() // PERBAIKAN BINDING
                updateTotal(currentKuantitas)
            }
        }

        binding.btnPlus.setOnClickListener {
            currentProduct?.let { produk ->
                if (currentKuantitas < produk.stok) {
                    currentKuantitas++
                    binding.tvQuantity.text = currentKuantitas.toString() // PERBAIKAN BINDING
                    updateTotal(currentKuantitas)
                } else {
                    Toast.makeText(this, "Stok maksimal yang tersedia: ${produk.stok}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnBuyNow.setOnClickListener { // PERBAIKAN BINDING
            // Beli Langsung: Langsung kirim item ke CheckoutActivity
            currentProduct?.let { product ->
                if (currentKuantitas > 0) {
                    val intent = Intent(this, CheckoutActivity::class.java).apply {
                        putExtra("IS_BUY_NOW", true)
                        putExtra("PRODUCT_ID", product.id_produk)
                        putExtra("QUANTITY", currentKuantitas)
                    }
                    startActivity(intent)
                }
            }
        }

        // Menggunakan tombol 'keranjang' (btn_visit_store) sebagai tombol 'Tambah ke Keranjang'
        // Karena layout aslinya tidak memiliki tombol 'Tambah ke Keranjang' yang terpisah.
        binding.btnVisitStore.setOnClickListener { // PERBAIKAN BINDING
            handleAddToCart()
        }
    }

    private fun handleAddToCart() {
        currentProduct?.let { product ->
            if (currentKuantitas > 0) {
                val newItem = CartItem(product, currentKuantitas)
                CartManager.addItem(newItem)

                Toast.makeText(this, "${currentKuantitas} kg ${product.nama_produk} ditambahkan ke Keranjang.", Toast.LENGTH_LONG).show()

                // Navigasi ke CartActivity setelah menambah
                val intent = Intent(this, CartActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Kuantitas harus lebih dari 0.", Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(this, "Produk belum dimuat sepenuhnya.", Toast.LENGTH_SHORT).show()
    }

    private fun loadProductDetail(productId: String) {
        firestore.collection("produk").document(productId)
            .get()
            .addOnSuccessListener { document ->
                currentProduct = document.toObject(Produk::class.java)
                currentProduct?.let { product ->
                    displayProductDetail(product)
                    loadSellerInfo(product.id_penjual) // Panggil loadSellerInfo setelah produk dimuat
                } ?: run {
                    Toast.makeText(this, "Produk tidak ditemukan di database.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProductDetail", "Gagal memuat detail produk: ${e.message}")
                Toast.makeText(this, "Gagal memuat data produk.", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun loadSellerInfo(sellerId: String) {
        firestore.collection("pengguna").document(sellerId).get()
            .addOnSuccessListener { document ->
                val seller = document.toObject(Pengguna::class.java)
                if (seller != null) {
                    binding.tvStoreName.text = seller.nama_lengkap
                    // Ambil bagian terakhir dari alamat sebagai lokasi
                    binding.tvStoreLocation.text = seller.alamat.split(",").lastOrNull() ?: "Lokasi Tidak Diketahui"
                }
            }
            .addOnFailureListener {
                binding.tvStoreName.text = "Toko (Data Gagal Dimuat)"
                binding.tvStoreLocation.text = ""
            }
    }

    private fun displayProductDetail(product: Produk) {
        val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        formatter.maximumFractionDigits = 0

        // PERBAIKAN BINDING
        binding.tvDetailName.text = product.nama_produk
        binding.tvDetailPrice.text = formatter.format(product.harga) // product.harga sekarang Long
        binding.tvDetailDescription.text = product.deskripsi
        binding.tvStockInfo.text = "Stok tersedia: ${product.stok} kg"

        // Update total saat display
        updateTotal(currentKuantitas)

        // Atur kuantitas awal, cek batasan stok
        currentKuantitas = if (product.stok > 0) 1 else 0
        binding.tvQuantity.text = currentKuantitas.toString()
        binding.btnBuyNow.isEnabled = currentKuantitas > 0
        binding.btnVisitStore.isEnabled = currentKuantitas > 0 // Tombol Add to Cart sementara

        Glide.with(this)
            .load(product.foto_produk)
            .placeholder(R.drawable.placeholder_rice)
            .error(R.drawable.placeholder_rice)
            .into(binding.ivProductImageDetail)

        // Atur Judul di ActionBar
        supportActionBar?.title = product.nama_produk
    }

    private fun updateTotal(quantity: Int) {
        // PERBAIKAN: Perkalian Long dengan Int menghasilkan Long (tidak perlu Double)
        val total = (currentProduct?.harga ?: 0L) * quantity
        val formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))

        binding.tvQuantity.text = quantity.toString()
        binding.tvTotalPriceBottom.text = formatRupiah.format(total) // Format Long
    }
}