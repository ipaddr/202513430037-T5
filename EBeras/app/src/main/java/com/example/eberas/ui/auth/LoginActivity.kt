package com.example.eberas.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.eberas.data.firebase.FirebaseHelper
import com.example.eberas.data.model.Pengguna
import com.example.eberas.databinding.ActivityLoginBinding
import com.example.eberas.ui.consumer.HomeActivity
import com.example.eberas.ui.seller.SellerDashboardActivity
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Activity untuk menangani proses Login.
 * Menerima 'USER_ROLE' dari RoleSelectionActivity.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var userRole: String = "pembeli" // Default jika tidak ada peran yang dikirim
    private val auth = FirebaseHelper.auth
    private val firestore = FirebaseHelper.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ambil peran dari Intent
        userRole = intent.getStringExtra("USER_ROLE") ?: "pembeli"

        // Perbarui judul berdasarkan peran
        binding.tvLoginSubtitle.text = "Masuk sebagai ${userRole.replaceFirstChar { it.uppercase() }}"

        setupListeners()
    }

    private fun setupListeners() {
        // Tombol Login
        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        // Link ke Register
        binding.tvRegisterLink.setOnClickListener {
            navigateToRegister()
        }
    }

    /**
     * Melakukan validasi input dan mencoba proses login Firebase.
     */
    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email dan Kata Sandi harus diisi.", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Tampilkan loading
        setLoading(true)

        // 2. Login dengan Firebase Authentication
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                // Jika Login berhasil, verifikasi peran pengguna di Firestore
                val userId = authResult.user?.uid
                if (userId != null) {
                    checkUserRoleAndNavigate(userId)
                } else {
                    setLoading(false)
                    Toast.makeText(this, "Login gagal: ID pengguna tidak ditemukan.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                setLoading(false)
                Toast.makeText(this, "Login Gagal: Periksa email atau kata sandi Anda.", Toast.LENGTH_LONG).show()
            }
    }

    private fun checkUserRoleAndNavigate(userId: String) {
        // 3. Ambil data Pengguna dari Firestore
        firestore.collection("pengguna").document(userId).get()
            .addOnSuccessListener { document ->
                setLoading(false)
                val user = document.toObject(Pengguna::class.java)

                if (user?.peran == userRole) {
                    // Peran sesuai, arahkan ke Home
                    navigateToHome(user.peran)
                }
                // PERBAIKAN BUG KONSUMEN: Cek jika user.peran adalah NULL (kemungkinan NPE/data kosong)
                else if (user == null || user.peran == null) {
                    auth.signOut()
                    Toast.makeText(this, "Data peran pengguna tidak lengkap. Silakan coba mendaftar ulang.", Toast.LENGTH_LONG).show()
                }
                else {
                    // Peran tidak sesuai (misal: penjual login di form pembeli)
                    auth.signOut() // Logout pengguna yang baru login
                    Toast.makeText(this, "Peran akun tidak sesuai. Silakan masuk sebagai ${userRole}.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { exception ->
                setLoading(false)
                Toast.makeText(this, "Gagal memverifikasi peran. ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun navigateToRegister() {
        // Pindah ke Register Activity, bawa peran yang sama
        val intent = Intent(this, RegisterActivity::class.java).apply {
            putExtra("USER_ROLE", userRole)
        }
        startActivity(intent)
    }

    /**
     * Mengarahkan pengguna ke Activity yang sesuai (Katalog atau Dashboard)
     */
    /**
     * Mengarahkan pengguna ke Activity yang sesuai (Katalog atau Dashboard)
     */
    private fun navigateToHome(role: String) {
        val destinationActivity = when (role) {
            "penjual" -> SellerDashboardActivity::class.java
            else -> HomeActivity::class.java
        }
        // Gunakan FLAG agar Activity baru menjadi Awal dari task dan membersihkan stack
        val intent = Intent(this, destinationActivity).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        // Hapus finishAffinity() dari sini karena FLAG sudah melakukannya.
        // finishAffinity()
    }

    private fun setLoading(isLoading: Boolean) {
        // Tambahkan logika untuk menampilkan/menyembunyikan progress bar atau tombol loading
        // binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
    }
}