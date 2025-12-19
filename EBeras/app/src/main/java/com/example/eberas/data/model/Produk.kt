package com.example.eberas.data.model

import com.google.firebase.firestore.DocumentId // <-- PENTING: Tambahkan import ini

/**
 * Model data untuk Produk Beras (item yang dijual oleh petani/penjual).
 */
data class Produk(
    // PERBAIKAN KRITIS: Gunakan @DocumentId untuk mendapatkan ID dokumen Firestore
    @DocumentId
    val id_produk: String = "", // <-- Nilai defaultnya harus kosong atau String kosong

    val nama_produk: String = "",
    // >>> FIELD BARU UNTUK FILTER KATALOG <<<
    val jenis_beras: String = "",
    val deskripsi: String = "",

    // PERBAIKAN: Ubah dari Double menjadi Long
    val harga: Long = 0,

    var stok: Int = 0,
    val foto_produk: String = "",
    val lokasi_petani: String = "",
    val id_penjual: String = ""
)