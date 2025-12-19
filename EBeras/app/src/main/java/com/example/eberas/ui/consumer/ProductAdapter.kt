package com.example.eberas.ui.consumer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eberas.R
import com.example.eberas.data.firebase.FirebaseHelper
import com.example.eberas.data.model.Produk
import com.example.eberas.data.model.Pengguna
import com.example.eberas.databinding.ItemProductBinding
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter untuk menampilkan daftar produk beras di RecyclerView.
 */
class ProductAdapter(
    private var productList: List<Produk>,
    private val onItemClick: (Produk) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    // Cache untuk menyimpan nama penjual (Map: ID Penjual -> Nama)
    private val sellerNameCache = mutableMapOf<String, String>()
    private val firestore = FirebaseHelper.firestore

    // Digunakan untuk memperbarui data di RecyclerView
    fun updateData(newProductList: List<Produk>) {
        productList = newProductList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(productList[position])
    }

    override fun getItemCount(): Int = productList.size

    inner class ProductViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Produk) {
            binding.apply {
                tvProductName.text = product.nama_produk

                // Format harga ke Rupiah
                val formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
                tvProductPrice.text = formatRupiah.format(product.harga) + " / Kg"

                // Ambil dan tampilkan nama penjual
                getSellerName(product.id_penjual) { name ->
                    tvSellerName.text = name
                }

                // Menggunakan Glide untuk memuat gambar dari URL/path
                Glide.with(itemView.context)
                    .load(product.foto_produk)
                    .placeholder(com.example.eberas.R.drawable.placeholder_rice)
                    .into(ivProductImage)

                // Listener ketika item diklik (untuk pindah ke Detail Produk)
                root.setOnClickListener {
                    onItemClick(product)
                }
            }
        }

        /**
         * Mengambil nama penjual dari Firestore atau Cache.
         */
        private fun getSellerName(sellerId: String, callback: (String) -> Unit) {
            // 1. Cek Cache
            if (sellerNameCache.containsKey(sellerId)) {
                callback(sellerNameCache[sellerId]!!)
                return
            }

            // 2. Ambil dari Firestore
            firestore.collection("pengguna").document(sellerId).get()
                .addOnSuccessListener { document ->
                    val seller = document.toObject(Pengguna::class.java)
                    val name = seller?.nama_lengkap ?: "Toko Tidak Dikenal"

                    // Simpan ke Cache
                    sellerNameCache[sellerId] = name
                    callback(name)
                }
                .addOnFailureListener {
                    callback("Toko (Gagal Load)")
                }
        }
    }
}