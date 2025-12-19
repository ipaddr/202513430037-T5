package com.example.eberas.ui.seller

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eberas.data.firebase.FirebaseHelper
import com.example.eberas.data.model.Produk
import com.example.eberas.databinding.ActivityMyProductsBinding
import com.google.firebase.firestore.ListenerRegistration
import com.example.eberas.R
import android.widget.ImageView
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog // Import AlertDialog

/**
 * Activity untuk menampilkan daftar produk yang dijual oleh Penjual.
 */
class MyProductsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyProductsBinding
    private lateinit var productAdapter: SellerProductAdapter

    private val firestore = FirebaseHelper.firestore
    private val auth = FirebaseHelper.auth
    private var productsListener: ListenerRegistration? = null

    private val currentSellerId: String? get() = auth.currentUser?.uid


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupRecyclerView()
        setupListeners()
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        loadSellerProducts()
        binding.bottomNavigationSeller.selectedItemId = R.id.nav_my_products
    }

    override fun onPause() {
        super.onPause()
        productsListener?.remove()
    }

    private fun setupRecyclerView() {
        // PERBAIKAN: Mengganti handleDeleteProduct lama dengan showDeleteConfirmationDialog baru
        productAdapter = SellerProductAdapter(
            emptyList(),
            onEditClick = { product -> openEditProductActivity(product.id_produk) },
            onDeleteClick = { product -> showDeleteConfirmationDialog(product) } // Panggil dialog konfirmasi
        )

        binding.rvSellerProducts.apply {
            layoutManager = LinearLayoutManager(this@MyProductsActivity)
            adapter = productAdapter
        }
    }

    private fun loadSellerProducts() {
        if (currentSellerId == null) {
            Toast.makeText(this, "Anda belum login sebagai penjual.", Toast.LENGTH_SHORT).show()
            return
        }

        productsListener?.remove()

        val query = firestore.collection("produk")
            .whereEqualTo("id_penjual", currentSellerId)

        productsListener = query.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.e("MyProducts", "Gagal memuat produk: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshots != null) {
                val products = snapshots.toObjects(Produk::class.java)
                productAdapter.updateData(products)
            }
        }
    }

    private fun setupListeners() {
        binding.fabAddProductList.setOnClickListener {
            openAddProductActivity()
        }
    }

    private fun openAddProductActivity(productId: String? = null) {
        val intent = Intent(this, ManageProductActivity::class.java).apply {
            if (productId != null) {
                putExtra(ManageProductActivity.EXTRA_PRODUCT_ID, productId)
            }
        }
        startActivity(intent)
    }

    private fun openEditProductActivity(productId: String) {
        openAddProductActivity(productId)
    }

    /**
     * Menampilkan dialog konfirmasi sebelum menghapus produk.
     */
    private fun showDeleteConfirmationDialog(product: Produk) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Produk")
            .setMessage("Apakah Anda yakin ingin menghapus produk '${product.nama_produk}'? Aksi ini tidak dapat dibatalkan.")
            .setPositiveButton("Hapus") { dialog, _ ->
                handleDeleteProduct(product) // Lanjutkan ke proses hapus setelah konfirmasi
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    /**
     * Melakukan penghapusan produk setelah dikonfirmasi.
     */
    private fun handleDeleteProduct(product: Produk) {
        firestore.collection("produk").document(product.id_produk).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Produk ${product.nama_produk} berhasil dihapus.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal menghapus produk: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationSeller.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, SellerDashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_my_products -> true
                R.id.nav_profile_seller -> {
                    startActivity(Intent(this, SellerProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}