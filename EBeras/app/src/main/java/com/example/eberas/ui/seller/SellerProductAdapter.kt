package com.example.eberas.ui.seller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eberas.R
import com.example.eberas.data.model.Produk
import com.example.eberas.databinding.ItemSellerProductBinding
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter untuk menampilkan daftar produk yang dijual oleh Penjual.
 * Dilengkapi tombol Edit dan Hapus.
 */
class SellerProductAdapter(
    private var productList: List<Produk>,
    private val onEditClick: (Produk) -> Unit,
    private val onDeleteClick: (Produk) -> Unit // PERBAIKAN: Callback ini sekarang memicu konfirmasi di Activity
) : RecyclerView.Adapter<SellerProductAdapter.SellerProductViewHolder>() {

    fun updateData(newProductList: List<Produk>) {
        productList = newProductList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SellerProductAdapter.SellerProductViewHolder {
        val binding = ItemSellerProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SellerProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SellerProductAdapter.SellerProductViewHolder, position: Int) {
        holder.bind(productList[position])
    }

    override fun getItemCount(): Int = productList.size

    inner class SellerProductViewHolder(private val binding: ItemSellerProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Produk) {
            binding.apply {
                tvProductNameSeller.text = product.nama_produk
                tvProductStock.text = "Stok: ${product.stok} Kg"

                val formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
                val formattedPrice = formatRupiah.format(product.harga).replace("Rp", "").trim()
                tvProductPriceSeller.text = "Rp ${formattedPrice} / Kg"

                Glide.with(itemView.context)
                    .load(product.foto_produk)
                    .placeholder(R.drawable.placeholder_rice)
                    .into(ivProductImageSeller)

                // Listener untuk tombol EDIT
                btnEditProduct.setOnClickListener {
                    onEditClick(product)
                }

                // PERBAIKAN: Tombol HAPUS sekarang memanggil callback ke Activity
                btnDeleteProduct.setOnClickListener {
                    onDeleteClick(product) // Memicu konfirmasi dialog di MyProductsActivity
                }

                // Listener klik pada seluruh item (untuk membuka Edit Produk Penuh)
                root.setOnClickListener {
                    onEditClick(product)
                }
            }
        }
    }
}