package com.example.eberas.ui.seller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.eberas.data.firebase.FirebaseHelper
import com.example.eberas.data.model.Transaksi
import com.example.eberas.data.model.Pengguna // Impor Pengguna
import com.example.eberas.databinding.ItemRecentOrderBinding
import java.text.NumberFormat
import java.util.Locale

/**
 * Adapter untuk menampilkan item di Keranjang Belanja.
 */
class OrderAdapter(
    private var orderList: List<Transaksi>,
    // Tambahkan listener untuk event UI: menerima klik biasa dan long click
    private val onItemClick: (Transaksi) -> Unit,
    private val onItemLongClick: (Transaksi) -> Boolean // Tambahkan Long Click Listener
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    // Cache untuk menyimpan nama pembeli (Map: ID Pembeli -> Nama)
    private val buyerNameCache = mutableMapOf<String, String>()
    private val firestore = FirebaseHelper.firestore


    fun updateData(newOrderList: List<Transaksi>) {
        orderList = newOrderList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemRecentOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orderList[position])
    }

    override fun getItemCount(): Int = orderList.size

    inner class OrderViewHolder(private val binding: ItemRecentOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaksi) {
            binding.apply {
                // PERBAIKAN KRITIS DI SINI: Format ID Transaksi menjadi #INVXXXX
                val shortId = transaction.id_transaksi.take(8).uppercase()
                tvInvoiceNumber.text = "#INV$shortId"

                // Ambil dan tampilkan nama pembeli
                getBuyerName(transaction.id_pembeli) { name ->
                    tvCustomerName.text = name
                }

                tvOrderStatus.text = transaction.status_transaksi

                // Format total harga
                val formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
                val formattedPrice = formatRupiah.format(transaction.total_harga).replace("Rp", "").trim()
                tvOrderTotal.text = "Rp ${formattedPrice}"

                // Beri warna pada status pesanan
                when (transaction.status_transaksi) {
                    "menunggu" -> tvOrderStatus.setTextColor(0xFFFFA000.toInt()) // Amber
                    "diproses" -> tvOrderStatus.setTextColor(0xFF00BCD4.toInt()) // Cyan
                    "dikirim" -> tvOrderStatus.setTextColor(0xFF00BCD4.toInt()) // Cyan
                    "selesai" -> tvOrderStatus.setTextColor(0xFF4CAF50.toInt()) // Green
                    "dibatalkan" -> tvOrderStatus.setTextColor(0xFFF44336.toInt()) // Merah
                    else -> tvOrderStatus.setTextColor(0xFF000000.toInt())
                }

                // Listener klik biasa (untuk detail pesanan)
                root.setOnClickListener {
                    onItemClick(transaction)
                }

                // Listener long click (untuk opsi hapus histori)
                root.setOnLongClickListener {
                    onItemLongClick(transaction)
                }
            }
        }

        /**
         * Mengambil nama pembeli dari Firestore atau Cache.
         */
        private fun getBuyerName(buyerId: String, callback: (String) -> Unit) {
            // 1. Cek Cache
            if (buyerNameCache.containsKey(buyerId)) {
                callback(buyerNameCache[buyerId]!!)
                return
            }

            // 2. Ambil dari Firestore
            firestore.collection("pengguna").document(buyerId).get()
                .addOnSuccessListener { document ->
                    val buyer = document.toObject(Pengguna::class.java)
                    val name = buyer?.nama_lengkap ?: "Pembeli Tidak Dikenal"

                    // Simpan ke Cache
                    buyerNameCache[buyerId] = name
                    callback(name)
                }
                .addOnFailureListener {
                    callback("Pembeli (Gagal Load)")
                }
        }
    }
}