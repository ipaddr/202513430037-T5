package com.example.eberas.ui.seller

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.eberas.data.firebase.FirebaseHelper
import com.example.eberas.data.model.Pengguna // Harus menggunakan model Pengguna
import com.example.eberas.databinding.ActivitySellerProfileBinding // Menggunakan binding baru
import com.example.eberas.ui.auth.AuthCheckerActivity
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Activity untuk Profil Penjual/Toko.
 */
class SellerProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySellerProfileBinding // Binding baru
    private val auth = FirebaseHelper.auth
    private val firestore: FirebaseFirestore = FirebaseHelper.firestore
    private val TAG = "SellerProfileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivitySellerProfileBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(true)
            supportActionBar?.title = "Profil Toko"
            binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

            // Panggil listener untuk memuat data
            setupListeners()
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memuat Profil Penjual: ${e.message}", e)
            Toast.makeText(this, "Kesalahan fatal saat memuat Profil Toko.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Muat ulang data setiap kali Activity kembali ke foreground
        loadSellerProfile()
    }

    private fun loadSellerProfile() {
        val userId = auth.currentUser?.uid
        val userEmail = auth.currentUser?.email

        binding.tvProfileEmail.text = userEmail ?: "Email Tidak Ditemukan"

        if (userId == null) {
            binding.tvProfileName.text = "Error: Belum Login"
            return
        }

        firestore.collection("pengguna").document(userId).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(Pengguna::class.java)
                if (user != null) {
                    // Tampilkan data lengkap
                    binding.tvProfileName.text = user.nama_lengkap // Nama Toko/Pengguna
                    binding.tvProfilePhone.text = user.nomor_telepon ?: "-"
                    binding.tvProfileAddress.text = user.alamat ?: "Alamat belum diatur"
                } else {
                    binding.tvProfileName.text = "Lengkapi Profil Toko"
                    binding.tvProfilePhone.text = "-"
                    binding.tvProfileAddress.text = "Alamat belum diatur"
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Gagal memuat data penjual: ${e.message}")
                binding.tvProfileName.text = "Gagal Memuat Data"
            }
    }

    private fun setupListeners() {
        // Tombol Kelola Produk
        binding.btnManageProducts.setOnClickListener {
            startActivity(Intent(this, MyProductsActivity::class.java))
        }

        // Tombol Edit Profil Toko (PERBAIKAN)
        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditSellerProfileActivity::class.java))
        }

        // Tombol Logout
        binding.btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun performLogout() {
        auth.signOut()
        Toast.makeText(this, "Berhasil logout akun Penjual.", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, AuthCheckerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}