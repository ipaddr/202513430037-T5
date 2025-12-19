package com.example.eberas.ui.consumer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eberas.R
import com.example.eberas.data.model.CartItem
import com.example.eberas.databinding.ItemCartProductBinding
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter untuk menampilkan item di Keranjang Belanja.
 */
class CartAdapter(
    private var items: List<CartItem>,
    // Tambahkan listener untuk event UI
    private val onDelete: (CartItem) -> Unit,
    private val onQuantityChange: (CartItem, Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    private val formatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID")).apply {
        maximumFractionDigits = 0
    }

    inner class CartViewHolder(private val binding: ItemCartProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CartItem) {
            binding.tvCartName.text = item.produk.nama_produk

            val unitPrice = formatter.format(item.produk.harga)

            // PERBAIKAN: Menggunakan tvCartPriceUnit untuk harga per unit
            binding.tvCartPriceUnit.text = "Harga/kg: $unitPrice"

            // PERBAIKAN: Menggunakan tvCartKuantitas untuk kuantitas
            binding.tvCartKuantitas.text = item.kuantitas.toString()

            val subtotal = item.produk.harga * item.kuantitas
            // PERBAIKAN: Menggunakan tvCartSubtotal untuk total harga item ini
            binding.tvCartSubtotal.text = formatter.format(subtotal)

            // Muat gambar produk
            Glide.with(binding.ivCartImage.context)
                .load(item.produk.foto_produk)
                .placeholder(R.drawable.placeholder_rice)
                .error(R.drawable.placeholder_rice)
                .into(binding.ivCartImage)

            // Listener: HAPUS
            binding.btnDelete.setOnClickListener {
                onDelete(item)
            }

            // Listener: MINUS
            binding.btnMinus.setOnClickListener {
                onQuantityChange(item, item.kuantitas - 1)
            }

            // Listener: PLUS
            binding.btnPlus.setOnClickListener {
                onQuantityChange(item, item.kuantitas + 1)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<CartItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}