package com.example.eberas.data.model

/**
 * Model data untuk Transaksi/Pesanan
 * Disesuaikan untuk menampung data di CheckoutActivity.
 */
data class Transaksi(
    val id_transaksi: String = "", // ID Transaksi
    val id_pembeli: String = "", // ID Pengguna Pembeli
    val id_penjual: String = "", // ID Pengguna Penjual (Petani)
    val id_produk: String = "", // ID Produk
    val jumlah: Int = 0, // Jumlah produk (misal: kg)

    // PERBAIKAN: Ubah dari Double menjadi Long
    val total_harga: Long = 0,

    // PERBAIKAN: Menggunakan Long untuk timestamp
    val tanggal_transaksi: Long = System.currentTimeMillis(),
    val status_transaksi: String = "menunggu",
    val metode_pembayaran: String = "",
    val nama_produk_snapshot: String = "",

    // PERBAIKAN: Ubah dari Double menjadi Long
    val harga_per_unit_snapshot: Long = 0
)