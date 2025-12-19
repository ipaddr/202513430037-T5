package com.example.eberas.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage // Baris ini memerlukan dependency di Gradle

/**
 * Kelas bantuan (Singleton) untuk menyediakan instance Firebase
 * di seluruh aplikasi (Auth, Firestore, Storage).
 */
object FirebaseHelper {

    /**
     * Mendapatkan instance otentikasi (untuk login, register).
     */
    val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    /**
     * Mendapatkan instance database Firestore (untuk menyimpan Pengguna, Produk, Transaksi).
     */
    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    /**
     * Mendapatkan instance Storage (untuk menyimpan foto produk/profil).
     */
    val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }
}