package com.example.eberas.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.eberas.R // Import R untuk mengakses resource (tema)
import com.example.eberas.data.firebase.FirebaseHelper
import com.example.eberas.data.model.Pengguna
import com.example.eberas.ui.consumer.HomeActivity
import com.example.eberas.ui.seller.SellerDashboardActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * Activity pengecek status login.
 * Menentukan apakah pengguna harus ke Role Selection atau langsung ke Home/Dashboard.
 */
class AuthCheckerActivity : AppCompatActivity() {

    private val auth = FirebaseHelper.auth
    private val firestore = FirebaseHelper.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        // PERBAIKAN KRITIS:
        // Set tema ke tema utama (AppCompat) sebelum memanggil super.onCreate()
        // Ini menyelesaikan konflik tema dengan Theme.App.Starting (yang non-AppCompat)
        setTheme(R.style.Theme_EBerasPadang)

        super.onCreate(savedInstanceState)

        // Cek status login saat Activity dimulai
        checkAuthenticationStatus()
    }

    private fun checkAuthenticationStatus() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            // Belum login, arahkan ke Role Selection
            navigateToRoleSelection()
        } else {
            // Sudah login, verifikasi peran pengguna
            verifyUserRoleAndNavigate(currentUser.uid)
        }
    }

    private fun verifyUserRoleAndNavigate(userId: String) {
        firestore.collection("pengguna").document(userId).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(Pengguna::class.java)

                if (user != null && user.peran != null) {
                    // Peran ditemukan, langsung arahkan
                    navigateToApp(user.peran)
                } else {
                    // Data peran hilang/korup, arahkan ke login/role selection
                    auth.signOut()
                    Toast.makeText(this, "Data peran tidak ditemukan, silakan login ulang.", Toast.LENGTH_LONG).show()
                    navigateToRoleSelection()
                }
            }
            .addOnFailureListener {
                // Gagal mengambil data, mungkin karena koneksi/rules. Arahkan ke login.
                auth.signOut()
                Toast.makeText(this, "Gagal memverifikasi akun, silakan coba lagi.", Toast.LENGTH_LONG).show()
                navigateToRoleSelection()
            }
    }

    private fun navigateToRoleSelection() {
        startActivity(Intent(this, RoleSelectionActivity::class.java))
        finish() // Tutup checker
    }

    private fun navigateToApp(role: String) {
        val destinationActivity = when (role) {
            "penjual" -> SellerDashboardActivity::class.java
            else -> HomeActivity::class.java
        }
        startActivity(Intent(this, destinationActivity))
        finish() // Tutup checker
    }
}