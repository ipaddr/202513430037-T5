package com.example.eberas.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Model data untuk Notifikasi.
 */
data class Notifikasi(
    val id_notifikasi: String = "",
    val id_pengguna: String = "",
    val judul: String = "",
    val pesan: String = "",
    val tanggal: Long = System.currentTimeMillis(),

    // 🔴 PERBAIKAN KRITIS: Tambahkan anotasi @PropertyName
    // Ini memastikan Firebase HANYA menggunakan nama 'is_read' di database
    @get:PropertyName("is_read")
    @set:PropertyName("is_read")
    var is_read: Boolean = false,

    val type: String = "",
    val id_transaksi: String = "",
)