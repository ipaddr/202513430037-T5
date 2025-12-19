package com.example.eberas.data.model

/**
 * Model data untuk item di dalam Keranjang Belanja.
 * Menggabungkan detail Produk dan Kuantitas yang diinginkan.
 *
 * @property produk Objek Produk yang ditambahkan.
 * @property kuantitas Jumlah produk yang diinginkan pembeli.
 */
data class CartItem(
    val produk: Produk,
    var kuantitas: Int
)