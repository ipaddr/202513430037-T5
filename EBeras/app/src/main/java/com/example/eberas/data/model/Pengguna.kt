package com.example.eberas.data.model

/**
 * Model data untuk Pengguna (Pembeli, Penjual/Petani, Admin)
 * Berdasarkan dokumen 'MODEL DATA E BERAS PSB.docx' - Data Pengguna.
 */
data class Pengguna(
    // ID unik setiap pengguna (akan diisi oleh Firebase Auth atau Firestore)
    val id_pengguna: String = "",
    val nama_lengkap: String = "",
    val email: String = "",
    // Catatan: 'password' tidak disimpan di Data Class karena sensitif, hanya disimpan hash-nya di sistem autentikasi
    val nomor_telepon: String = "",
    val alamat: String = "",
    // Peran: "pembeli", "petani", atau "admin"
    val peran: String = "pembeli",
    // URL/Path foto profil (opsional)
    val foto_profil: String? = null
) {
}