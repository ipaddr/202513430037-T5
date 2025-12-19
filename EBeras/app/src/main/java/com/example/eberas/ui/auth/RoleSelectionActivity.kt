package com.example.eberas.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.eberas.databinding.ActivityRoleSelectionBinding

/**
 * Activity pertama untuk memilih peran pengguna: Konsumen (Pembeli) atau Penjual (Petani).
 * Berdasarkan Gambar 2.jpg.
 */
class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inisialisasi View Binding
        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        // Logika saat Konsumen dipilih
        binding.cardConsumer.setOnClickListener {
            navigateToLogin("pembeli")
        }

        // Logika saat Penjual dipilih
        binding.cardSeller.setOnClickListener {
            navigateToLogin("penjual")
        }


    }

    /**
     * Memindahkan pengguna ke Login Activity dengan membawa data peran (role).
     */
    private fun navigateToLogin(role: String) {
        // Di sini Anda akan memulai LoginActivity.
        // Anda harus membuat LoginActivity.kt terlebih dahulu di paket ui.auth
        val intent = Intent(this, LoginActivity::class.java).apply {
            putExtra("USER_ROLE", role) // Mengirim peran yang dipilih
        }
        startActivity(intent)
        // Menonaktifkan Activity ini agar pengguna tidak bisa kembali
        // finish()
    }
}