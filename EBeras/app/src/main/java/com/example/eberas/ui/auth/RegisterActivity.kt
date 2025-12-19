package com.example.eberas.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.eberas.data.firebase.FirebaseHelper
import com.example.eberas.data.model.Pengguna
import com.example.eberas.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.SetOptions
import android.util.Log // Import Log
import com.google.firebase.firestore.WriteBatch // Import WriteBatch

/**
 * Activity untuk menangani proses Registrasi.
 * Mengumpulkan data Pengguna sebelum mendaftar.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private var userRole: String = "pembeli"
    private val auth = FirebaseHelper.auth
    private val firestore = FirebaseHelper.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ambil peran dari Intent
        userRole = intent.getStringExtra("USER_ROLE") ?: "pembeli"

        // Perbarui sub-judul dengan peran yang dipilih
        binding.tvRegisterSubtitle.append(userRole.replaceFirstChar { it.uppercase() })

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener {
            performRegister()
        }
    }

    /**
     * Mengumpulkan semua input dan memanggil fungsi registrasi Firebase.
     */
    private fun performRegister() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmailReg.text.toString().trim()
        val password = binding.etPasswordReg.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Semua kolom wajib diisi.", Toast.LENGTH_LONG).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(this, "Kata sandi minimal 6 karakter.", Toast.LENGTH_LONG).show()
            return
        }

        // Mulai proses registrasi
        registerUser(name, email, password, phone, address)
    }

    private fun registerUser(name: String, email: String, password: String, phone: String, address: String) {

        // 1. Buat Akun Pengguna di Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val firebaseUser = authResult.user
                if (firebaseUser != null) {

                    // 2. Buat MAP data pengguna
                    val userDataMap = mapOf(
                        "id_pengguna" to firebaseUser.uid,
                        "nama_lengkap" to name,
                        "email" to email,
                        "nomor_telepon" to phone,
                        "alamat" to address,
                        "peran" to userRole,
                        "foto_profil" to "" // Kosongkan, akan diisi belakangan
                    )

                    // 3. Simpan data Pengguna di Firestore menggunakan MAP (lebih aman)
                    saveUserToFirestore(firebaseUser.uid, userDataMap)
                }
            }
            .addOnFailureListener { exception ->
                // Tangani error registrasi (misal: email sudah terdaftar)
                if (exception is FirebaseAuthUserCollisionException) {
                    Toast.makeText(this, "Gagal: Email sudah terdaftar.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Registrasi Gagal: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveUserToFirestore(userId: String, userDataMap: Map<String, Any>) {

        // Menggunakan BATCH WRITE untuk memastikan operasi atomic (walaupun hanya satu dokumen)
        // Ini adalah cara yang sangat tangguh untuk menulis dokumen di Firestore.
        val batch = firestore.batch()
        val userRef = firestore.collection("pengguna").document(userId)

        batch.set(userRef, userDataMap)

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "Registrasi berhasil sebagai ${userDataMap["peran"]}!", Toast.LENGTH_LONG).show()

                // Setelah sukses, kembali ke Login
                finish()
            }
            .addOnFailureListener { exception ->
                Log.e("RegisterActivity", "Firestore Gagal: ${exception.message}", exception) // Log error
                Toast.makeText(this, "Gagal menyimpan data pengguna: ${exception.message}", Toast.LENGTH_LONG).show()

                // --- PERBAIKAN KRITIS: HAPUS AKUN AUTH JIKA FIRESTORE GAGAL ---
                auth.currentUser?.delete()
                    ?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Akun Auth dibersihkan. Coba daftar lagi.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Gagal membersihkan akun Auth. Hubungi Admin.", Toast.LENGTH_LONG).show()
                        }
                    }
                // -----------------------------------------------------------
            }
    }
}