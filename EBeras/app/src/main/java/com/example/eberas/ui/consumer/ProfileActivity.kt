package com.example.eberas.ui.consumer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.eberas.R
import com.example.eberas.data.firebase.FirebaseHelper
import com.example.eberas.databinding.ActivityProfileBinding
import com.example.eberas.data.model.Pengguna
import com.example.eberas.ui.auth.AuthCheckerActivity
import android.view.View
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Activity untuk Profil Konsumen (dan implementasi Logout).
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val auth = FirebaseHelper.auth
    private val firestore: FirebaseFirestore = FirebaseHelper.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = "Profil Saya"
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // PANGGIL loadUserProfile() DARI ONRESUME BUKAN DI SINI
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        // PERBAIKAN KRITIS: Memuat ulang data setiap kali Activity kembali ke foreground
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid
        val userEmail = auth.currentUser?.email

        binding.tvProfileEmail.text = userEmail ?: "Email Tidak Ditemukan"
        // Anda bisa menampilkan ProgressBar di sini jika ada: binding.progressBar.visibility = View.VISIBLE

        if (userId == null) {
            binding.tvProfileName.text = "Error Data"
            return
        }

        firestore.collection("pengguna").document(userId).get()
            .addOnSuccessListener { document ->
                // Jika ada ProgressBar, sembunyikan di sini: binding.progressBar.visibility = View.GONE

                val user = document.toObject(Pengguna::class.java)
                if (user != null) {
                    // Tampilkan data lengkap
                    binding.tvProfileName.text = user.nama_lengkap
                    binding.tvProfilePhone.text = user.nomor_telepon ?: "-"
                    binding.tvProfileAddress.text = user.alamat ?: "Alamat belum diatur"
                } else {
                    binding.tvProfileName.text = "Lengkapi Profil"
                    binding.tvProfilePhone.text = "-"
                    binding.tvProfileAddress.text = "Alamat belum diatur"
                }
            }
            .addOnFailureListener { e ->
                // Jika ada ProgressBar, sembunyikan di sini: binding.progressBar.visibility = View.GONE
                Log.e("Profile", "Gagal memuat data pengguna: ${e.message}")
                binding.tvProfileName.text = "Gagal Memuat Data"
                binding.tvProfilePhone.text = "Error"
                binding.tvProfileAddress.text = "Error"
            }
    }

    private fun setupListeners() {
        // Tombol Riwayat Pesanan
        binding.btnOrderHistory.setOnClickListener {
            startActivity(Intent(this, OrderHistoryActivity::class.java))
        }

        // Tombol Edit Profil
        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // Tombol Logout
        binding.btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun performLogout() {
        auth.signOut()
        Toast.makeText(this, "Berhasil logout.", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, AuthCheckerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}