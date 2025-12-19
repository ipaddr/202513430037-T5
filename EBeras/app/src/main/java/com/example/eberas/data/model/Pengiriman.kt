package com.example.eberas.data.model

/**
 * Model data untuk Pengiriman (opsional, untuk pelacakan)
 * Berdasarkan dokumen 'MODEL DATA E BERAS PSB.docx' - Data Pengiriman.
 */
data class Pengiriman(
    val id_pengiriman: String = "", // ID unik pengiriman
    val id_transaksi: String = "", // Foreign key ke Transaksi
    val alamat_tujuan: String = "",
    // Status: "Dalam proses", "Dalam perjalanan", "Terkirim"
    val status_pengiriman: String = "Dalam proses",
    val tanggal_pengiriman: Long = System.currentTimeMillis(),
    val nomor_resi: String? = null
)