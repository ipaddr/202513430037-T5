package com.example.eberas.ui.consumer

import android.content.Intent // Tambahkan import ini
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.eberas.data.firebase.FirebaseHelper
import com.example.eberas.data.model.Transaksi
import com.example.eberas.data.model.Pengguna
import com.example.eberas.databinding.ItemRecentOrderBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.Toast

/**
 * Adapter untuk menampilkan riwayat pesanan (Transaksi) Konsumen.
 * Menggunakan layout item_recent_order.xml.
 */
class OrderHistoryAdapter(
    private var orderList: List<Transaksi>
) : RecyclerView.Adapter<OrderHistoryAdapter.OrderHistoryViewHolder>() {

    private val firestore = FirebaseHelper.firestore
    private val buyerNameCache = mutableMapOf<String, String>() // Cache untuk nama penjual
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("in", "ID"))

    // Tambahkan companion object untuk kunci intent
    companion object {
        const val EXTRA_TRANSACTION_ID = "extra_transaction_id"
    }

    fun updateData(newOrderList: List<Transaksi>) {
        orderList = newOrderList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderHistoryViewHolder {
        val binding = ItemRecentOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderHistoryViewHolder, position: Int) {
        holder.bind(orderList[position])
    }

    override fun getItemCount(): Int = orderList.size

    inner class OrderHistoryViewHolder(private val binding: ItemRecentOrderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaksi) {
            binding.apply {
                val shortId = transaction.id_transaksi.takeIf { it.isNotEmpty() }?.take(8)?.uppercase() ?: "UNKNOWN"
                tvInvoiceNumber.text = "#INV$shortId"

                val formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
                val formattedPrice = formatRupiah.format(transaction.total_harga).replace("Rp", "").trim()
                tvOrderTotal.text = "Rp ${formattedPrice}"

                val date = Date(transaction.tanggal_transaksi)
                val formattedDate = dateFormat.format(date)

                if (transaction.id_penjual.isNotEmpty()) {
                    getSellerName(transaction.id_penjual) { name ->
                        tvCustomerName.text = "Toko: $name - $formattedDate"
                    }
                } else {
                    tvCustomerName.text = "Toko: ID Penjual Hilang - $formattedDate"
                }

                tvOrderStatus.text = mapStatusToConsumer(transaction.status_transaksi)

                when (transaction.status_transaksi) {
                    "menunggu" -> tvOrderStatus.setTextColor(0xFFFFA000.toInt()) // Amber (Menunggu Konfirmasi)
                    "diproses" -> tvOrderStatus.setTextColor(0xFF00BCD4.toInt()) // Cyan (Diproses Penjual)
                    "dikirim" -> tvOrderStatus.setTextColor(0xFF4CAF50.toInt()) // Green (Dalam Perjalanan)
                    "selesai" -> tvOrderStatus.setTextColor(0xFF388E3C.toInt()) // Dark Green (Selesai)
                    "dibatalkan" -> tvOrderStatus.setTextColor(0xFFF44336.toInt()) // Red (Dibatalkan)
                    else -> tvOrderStatus.setTextColor(0xFF000000.toInt())
                }

                // Listener klik (Bisa diarahkan ke Order Detail Konsumen jika diimplementasikan)
                root.setOnClickListener {
                    val context = it.context
                    val intent = Intent(context, OrderDetailConsumerActivity::class.java).apply {
                        // >>> PERBAIKAN: Gunakan ID Transaksi PENUH <<<
                        putExtra(OrderHistoryAdapter.EXTRA_TRANSACTION_ID, transaction.id_transaksi)
                    }
                    context.startActivity(intent)
                }
            }
        }

        /**
         * Mengubah status internal menjadi bahasa konsumen yang lebih mudah dimengerti.
         */
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

        /**
         * Mengambil nama penjual dari Firestore atau Cache.
         */
        private fun getSellerName(sellerId: String, callback: (String) -> Unit) {
            if (buyerNameCache.containsKey(sellerId)) {
                callback(buyerNameCache[sellerId]!!)
                return
            }

            firestore.collection("pengguna").document(sellerId).get()
                .addOnSuccessListener { document ->
                    val seller = document.toObject(Pengguna::class.java)
                    val name = seller?.nama_lengkap ?: "Toko Tidak Dikenal"

                    buyerNameCache[sellerId] = name
                    callback(name)
                }
                .addOnFailureListener {
                    callback("Toko (Gagal Load)")
                }
        }
    }
}