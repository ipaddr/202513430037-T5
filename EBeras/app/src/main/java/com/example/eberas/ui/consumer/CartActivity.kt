package com.example.eberas.ui.consumer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eberas.R
import com.example.eberas.data.local.CartManager
import com.example.eberas.data.model.CartItem
import com.example.eberas.databinding.ActivityCartBinding
import java.text.NumberFormat
import java.util.Locale

/**
 * Activity untuk menampilkan dan mengelola Keranjang Belanja.
 */
class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    // Catatan: Anda perlu membuat kelas CartAdapter sendiri
    private lateinit var cartAdapter: CartAdapter
    private val TAG = "CartActivity"

    private val formatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
        maximumFractionDigits = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Keranjang Belanja"

        setupRecyclerView()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadCartItems()
    }

    private fun setupRecyclerView() {
        // 🌟 PERBAIKAN DI SINI 🌟
        // 1. Menyediakan daftar item awal (emptyList()).
        // 2. Menggunakan nama parameter yang benar (asumsi: onDelete).
        cartAdapter = CartAdapter(
            emptyList(), // Item 1: items (List<CartItem>)
            onQuantityChange = { item, newQty -> updateCartItemQuantity(item, newQty) },
            onDelete = { item -> deleteCartItem(item) } // Item 3: onDelete
        )

        binding.rvCart.apply {
            layoutManager = LinearLayoutManager(this@CartActivity)
            adapter = cartAdapter
        }
    }

    private fun setupListeners() {
        binding.btnCheckout.setOnClickListener {
            if (CartManager.getItems().isNotEmpty()) {
                startActivity(Intent(this, CheckoutActivity::class.java))
            } else {
                Toast.makeText(this, "Keranjang Anda kosong.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.llEmptyCart.findViewById<View>(R.id.btn_start_shopping)?.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }
    }

    private fun loadCartItems() {
        val items = CartManager.getItems()
        cartAdapter.updateData(items)

        if (items.isEmpty()) {
            binding.rvCart.visibility = View.GONE
            binding.layoutSummary.visibility = View.GONE
            binding.llEmptyCart.visibility = View.VISIBLE
        } else {
            binding.rvCart.visibility = View.VISIBLE
            binding.layoutSummary.visibility = View.VISIBLE
            binding.llEmptyCart.visibility = View.GONE
        }

        updateTotal()
    }

    // --- Fungsi Helper Keranjang ---

    private fun updateCartItemQuantity(item: CartItem, newQty: Int) {
        CartManager.updateQuantity(item.produk.id_produk, newQty)
        loadCartItems()
    }

    private fun deleteCartItem(item: CartItem) {
        CartManager.removeItem(item.produk.id_produk)
        Toast.makeText(this, "${item.produk.nama_produk} dihapus dari keranjang.", Toast.LENGTH_SHORT).show()
        loadCartItems()
    }

    private fun updateTotal() {
        val total = CartManager.calculateTotal()
        binding.tvCartTotal.text = formatter.format(total)
    }
}