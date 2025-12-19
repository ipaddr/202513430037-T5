package com.example.eberas.ui.consumer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.eberas.R
import com.example.eberas.data.firebase.FirebaseHelper
import com.example.eberas.data.model.Produk
import com.example.eberas.databinding.ActivityHomeBinding
import com.example.eberas.ui.auth.AuthCheckerActivity
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import androidx.core.widget.addTextChangedListener
import android.widget.LinearLayout
import android.widget.Button
import android.widget.TextView
import android.widget.EditText
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.QuerySnapshot
import android.widget.FrameLayout // <<< TAMBAHKAN INI

/**
 * Activity utama untuk tampilan Konsumen (Katalog Produk).
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var productAdapter: ProductAdapter

    private val firestore = FirebaseHelper.firestore
    private val auth = FirebaseHelper.auth
    private var productsListener: ListenerRegistration? = null
    // Tambahkan listener untuk badge notifikasi
    private var notificationBadgeListener: ListenerRegistration? = null

    private var allProducts: List<Produk> = emptyList()

    private val currentUserId: String? get() = auth.currentUser?.uid

    // State untuk Filter dan Search
    private var currentFilter: String? = null
    private var currentSearchQuery: String? = null
    private val TAG = "HomeActivity"

    companion object {
        private const val MENU_PROFILE = 100
        private const val MENU_HISTORY = 101
        private const val MENU_LOGOUT = 102
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityHomeBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Setup Toolbar
            val toolbar = binding.appBarLayout.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)

            setupRecyclerView()
            setupListeners()
            setupBottomNavigation()
            loadProducts()
        } catch (e: Exception) {
            Log.e(TAG, "Aplikasi Crash saat memuat Home: ${e.message}", e)
            Toast.makeText(this, "Kesalahan fatal saat memuat tampilan. Lihat Logcat.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.nav_home
        filterProducts()
        // Panggil listener badge di onResume
        startNotificationBadgeListener()
    }

    override fun onPause() {
        super.onPause()
        productsListener?.remove()
        // Hapus listener badge di onPause
        notificationBadgeListener?.remove()
    }

    /**
     * FUNGSI BARU: Mendengarkan jumlah notifikasi yang belum dibaca secara real-time.
     */
    private fun startNotificationBadgeListener() {
        val userId = currentUserId
        if (userId == null) return

        notificationBadgeListener?.remove()

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

    /**
     * FUNGSI BARU: Memperbarui UI badge.
     */
    private fun updateNotificationBadge(count: Int) {
        val badge = binding.appBarLayout.findViewById<TextView>(R.id.tv_notification_badge)

        if (count > 0) {
            badge.visibility = View.VISIBLE
            badge.text = if (count > 9) "9+" else count.toString()
        } else {
            badge.visibility = View.GONE
        }
    }


    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(emptyList()) { product ->
            navigateToProductDetail(product)
        }

        binding.rvProducts.apply {
            layoutManager = GridLayoutManager(this@HomeActivity, 2)
            adapter = productAdapter
        }
    }

    private fun setupListeners() {

        // MENGATUR LISTENER UNTUK SEARCH BAR (EditText)
        binding.llSearchAndFilter.findViewById<EditText>(R.id.et_search_input)?.addTextChangedListener { editable ->
            currentSearchQuery = editable.toString().trim()
            filterProducts()
        }

        // MENGATUR LISTENER UNTUK FILTER CHIPS
        val chipContainer = binding.llSearchAndFilter.findViewById<LinearLayout>(R.id.chip_container)

        val btnPutih = chipContainer?.getChildAt(0) as? Button
        val btnMerah = chipContainer?.getChildAt(1) as? Button
        val btnKetan = chipContainer?.getChildAt(2) as? Button

        btnPutih?.setOnClickListener { setFilter("Beras Putih", btnPutih, btnMerah, btnKetan) }
        btnMerah?.setOnClickListener { setFilter("Beras Merah", btnMerah, btnPutih, btnKetan) }
        btnKetan?.setOnClickListener { setFilter("Beras Ketan", btnKetan, btnPutih, btnMerah) }

        updateChipAppearance(btnPutih, btnPutih, btnMerah, btnKetan)

        // Tombol Notifikasi (Perbaikan Listener)
        // Gunakan container FrameLayout sebagai clickable area
        binding.appBarLayout.findViewById<FrameLayout>(R.id.fl_notification_container)?.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }
    }

    private fun setFilter(filterValue: String, selectedButton: Button?, vararg otherButtons: Button?) {
        if (currentFilter == filterValue) {
            currentFilter = null
            updateChipAppearance(null, selectedButton, *otherButtons)
        } else {
            currentFilter = filterValue
            updateChipAppearance(selectedButton, selectedButton, *otherButtons)
        }

        binding.llSearchAndFilter.findViewById<EditText>(R.id.et_search_input)?.setText("")
        currentSearchQuery = null

        filterProducts()
    }

    private fun updateChipAppearance(selectedButton: Button?, vararg allButtons: Button?) {
        val white = ContextCompat.getColor(this, R.color.white)
        val black = ContextCompat.getColor(this, R.color.black)

        allButtons.forEach { button ->
            button?.let { btn ->
                val filterText = btn.text.toString()

                if (currentFilter == filterText) {
                    btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.green_primary)
                    btn.setTextColor(white)
                } else {
                    btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.light_gray)
                    btn.setTextColor(black)
                }
            }
        }
    }

    private fun loadProducts() {
        productsListener?.remove()

        val query = firestore.collection("produk")
            .orderBy("nama_produk", Query.Direction.ASCENDING)

        productsListener = query.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Log.e(TAG, "Gagal memuat produk: ${error.message}", error)
                Toast.makeText(this, "Gagal memuat katalog. Error: ${error.message}", Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }

            if (snapshots != null) {
                allProducts = snapshots.toObjects(Produk::class.java)
                filterProducts()

                if (allProducts.isEmpty()) {
                    Toast.makeText(this, "Belum ada produk yang tersedia saat ini.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun filterProducts() {
        var filteredList = allProducts

        val queryText = currentSearchQuery.orEmpty().lowercase()
        val filterActive = currentFilter.orEmpty().lowercase()


        if (filterActive.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.jenis_beras.lowercase().contains(filterActive)
            }
        }

        if (queryText.isNotEmpty()) {
            filteredList = filteredList.filter { produk ->
                produk.nama_produk.lowercase().contains(queryText) ||
                        produk.jenis_beras.lowercase().contains(queryText) ||
                        produk.lokasi_petani.lowercase().contains(queryText)
            }
        }

        productAdapter.updateData(filteredList)

        if (filteredList.isEmpty() && (filterActive.isNotEmpty() || queryText.isNotEmpty())) {
            Toast.makeText(this, "Tidak ditemukan produk dengan filter/pencarian ini.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_cart -> {
                    startActivity(Intent(this, CartActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun performLogout() {
        auth.signOut()
        Toast.makeText(this, "Berhasil logout.", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, AuthCheckerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }


    private fun navigateToProductDetail(product: Produk) {
        val intent = Intent(this, ProductDetailActivity::class.java).apply {
            putExtra("PRODUCT_ID", product.id_produk)
        }
        startActivity(intent)
    }
}