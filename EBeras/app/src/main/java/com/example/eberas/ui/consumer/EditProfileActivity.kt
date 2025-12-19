package com.example.eberas.ui.consumer

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.eberas.data.firebase.FirebaseHelper
import com.example.eberas.data.model.Pengguna
import com.example.eberas.databinding.ActivityEditProfileBinding
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val firestore: FirebaseFirestore = FirebaseHelper.firestore
    private val auth = FirebaseHelper.auth
    private val TAG = "EditProfileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        loadCurrentProfile()
        setupListeners()

        // Daftarkan Activity ini di AndroidManifest.xml jika belum
        // <activity android:name=".ui.consumer.EditProfileActivity" android:exported="false" />
    }

    private fun loadCurrentProfile() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Anda harus login untuk mengedit profil.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("pengguna").document(userId).get()
            .addOnSuccessListener { document ->
                binding.progressBar.visibility = View.GONE
                val user = document.toObject(Pengguna::class.java)

                if (user != null) {
                    // Isi field dengan data yang sudah ada
                    binding.etEditName.setText(user.nama_lengkap)
                    binding.etEditPhone.setText(user.nomor_telepon)
                    binding.etEditAddress.setText(user.alamat)
                } else {
                    // Jika data di Firestore kosong, biarkan field kosong (default)
                    Toast.makeText(this, "Data profil awal tidak ditemukan. Silakan isi.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e(TAG, "Gagal memuat profil: ${e.message}", e)
                Toast.makeText(this, "Gagal memuat data profil.", Toast.LENGTH_LONG).show()
            }
    }

    private fun setupListeners() {
        binding.btnSaveProfile.setOnClickListener {
            saveProfileChanges()
        }
    }

    private fun saveProfileChanges() {
        val userId = auth.currentUser?.uid
        if (userId == null) return

        val newName = binding.etEditName.text.toString().trim()
        val newPhone = binding.etEditPhone.text.toString().trim()
        val newAddress = binding.etEditAddress.text.toString().trim()

        if (newName.isEmpty() || newPhone.isEmpty() || newAddress.isEmpty()) {
            Toast.makeText(this, "Semua kolom wajib diisi.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSaveProfile.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        val updatedData = mapOf(
            "nama_lengkap" to newName,
            "nomor_telepon" to newPhone,
            "alamat" to newAddress
        )

        firestore.collection("pengguna").document(userId).update(updatedData)
            .addOnSuccessListener {
                Toast.makeText(this, "Profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Profil diperbarui untuk user ID: $userId")
                finish() // Kembali ke ProfileActivity
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Gagal memperbarui profil: ${e.message}", e)
                Toast.makeText(this, "Gagal memperbarui: ${e.message}", Toast.LENGTH_LONG).show()
                binding.btnSaveProfile.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
    }
}