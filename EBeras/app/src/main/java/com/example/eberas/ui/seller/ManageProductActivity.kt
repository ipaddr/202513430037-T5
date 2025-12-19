package com.example.eberas.ui.seller

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.eberas.R
import com.example.eberas.data.firebase.FirebaseHelper
import com.example.eberas.data.model.Produk
import com.example.eberas.databinding.ActivityManageProductBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Activity untuk menambahkan atau mengedit Produk Beras.
 */
class ManageProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageProductBinding
    private val firestore = FirebaseHelper.firestore
    private val auth = FirebaseHelper.auth
    private val TAG = "ManageProductActivity"

    // --- KONFIGURASI CLOUDINARY (GANTI DENGAN NILAI ANDA) ---
    private val CLOUDINARY_CLOUD_NAME = "disljvttz"
    private val CLOUDINARY_UPLOAD_PRESET = "eberas_preset"
    // ------------------------------------------

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val mainScope = CoroutineScope(Dispatchers.Main)

    private val currentSellerId: String? get() = auth.currentUser?.uid
    private var currentProductId: String? = null
    private var selectedImageUri: Uri? = null
    private var existingImageUrl: String = ""

    private val PLACEHOLDER_IMAGE_URL = "https://placehold.co/600x400/4CAF50/white?text=Gambar+Beras"


    companion object {
        const val EXTRA_PRODUCT_ID = "extra_product_id"
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            // Tampilkan gambar yang baru dipilih
            Glide.with(this).load(uri).into(binding.ivProductImagePreview)
            binding.ivProductImagePreview.visibility = View.VISIBLE
            binding.tvUploadLabel.text = "GANTI FOTO PRODUK"
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            openImagePicker()
        }
        else {
            Toast.makeText(this, "Izin akses galeri ditolak.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        currentProductId = intent.getStringExtra(EXTRA_PRODUCT_ID)

        if (currentProductId != null && currentProductId!!.isNotEmpty()) {
            binding.tvTitle.text = "Edit Produk"
            binding.btnSaveProduct.text = "Simpan Perubahan"
            loadProductData(currentProductId!!)
        } else {
            binding.tvTitle.text = "Tambah Produk Baru"
            binding.btnSaveProduct.text = "Tambah Produk"
            // Mode Tambah: Tampilkan ikon kamera lokal
            loadPlaceholderImage()
        }

        setupListeners()
    }

    // Fungsi baru untuk memuat placeholder ikon kamera
    private fun loadPlaceholderImage() {
        // PERBAIKAN: Menghapus .tint()
        Glide.with(this)
        binding.ivProductImagePreview.visibility = View.VISIBLE
        binding.tvUploadLabel.text = "KETUK UNTUK MEMILIH FOTO"
    }


    private fun setupListeners() {
        binding.btnSaveProduct.setOnClickListener {
            saveProduct()
        }

        binding.cardUploadPhoto.setOnClickListener {
            checkStoragePermission()
        }

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun checkStoragePermission() {
        // >>> PERBAIKAN: Mengoreksi referensi SDK_CODES <<<
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        // <<< END PERBAIKAN >>>

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openImagePicker()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }

    private fun loadProductData(productId: String) {
        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("produk").document(productId).get()
            .addOnSuccessListener { document ->
                binding.progressBar.visibility = View.GONE

                if (document.exists()) {
                    val product = document.toObject(Produk::class.java)
                    if (product != null) {
                        binding.etNamaProduk.setText(product.nama_produk)
                        binding.etJenisBeras.setText(product.jenis_beras)
                        binding.etDeskripsi.setText(product.deskripsi)

                        binding.etHarga.setText(product.harga.toString())
                        binding.etStok.setText(product.stok.toString())
                        binding.etLokasiPetani.setText(product.lokasi_petani)

                        existingImageUrl = product.foto_produk
                        Log.d(TAG, "Existing Image URL: $existingImageUrl")

                        // BAGIAN MEMUAT GAMBAR LAMA
                        if (existingImageUrl.isNotEmpty() && existingImageUrl != PLACEHOLDER_IMAGE_URL) {
                            Glide.with(this)
                                .load(existingImageUrl)
                                .centerCrop()
                                .placeholder(R.drawable.ic_camera_upload)
                                .error(R.drawable.ic_camera_upload)
                                .into(binding.ivProductImagePreview)

                            binding.ivProductImagePreview.visibility = View.VISIBLE
                            binding.tvUploadLabel.text = "GANTI FOTO PRODUK"
                        } else {
                            // Jika URL kosong atau placeholder default, muat ikon kamera
                            loadPlaceholderImage()
                        }
                    } else {
                        Toast.makeText(this, "Gagal mengkonversi data produk.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                } else {
                    Log.e(TAG, "Dokumen dengan ID $productId tidak ada.")
                    Toast.makeText(this, "Produk tidak ditemukan di Firestore.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e(TAG, "Gagal memuat data produk: ${e.message}", e)
                Toast.makeText(this, "Gagal memuat data produk: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }


    private fun uriToFile(uri: Uri): File? {
        val contentResolver = applicationContext.contentResolver
        val tempFile = File(cacheDir, "upload_${UUID.randomUUID()}.jpg")

        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error converting URI to file: ${e.message}")
            return null
        }
    }

    private fun saveProduct() {
        val sellerId = auth.currentUser?.uid
        if (sellerId == null) {
            Toast.makeText(this, "Penjual belum login.", Toast.LENGTH_SHORT).show()
            return
        }

        val namaProduk = binding.etNamaProduk.text.toString().trim()
        val jenisBeras = binding.etJenisBeras.text.toString().trim()
        val deskripsi = binding.etDeskripsi.text.toString().trim()
        val hargaStr = binding.etHarga.text.toString().trim()
        val stokStr = binding.etStok.text.toString().trim()
        val lokasiPetani = binding.etLokasiPetani.text.toString().trim()

        if (namaProduk.isEmpty() || jenisBeras.isEmpty() || hargaStr.isEmpty() || stokStr.isEmpty() || lokasiPetani.isEmpty()) {
            Toast.makeText(this, "Nama Produk, Jenis Beras, Harga, Stok, dan Lokasi Petani wajib diisi.", Toast.LENGTH_LONG).show()
            return
        }

        val harga = hargaStr.toLongOrNull()
        val stok = stokStr.toIntOrNull()

        if (harga == null || stok == null || harga <= 0 || stok < 0) {
            Toast.makeText(this, "Harga harus valid (>0) dan Stok tidak boleh negatif.", Toast.LENGTH_LONG).show()
            return
        }

        if (selectedImageUri != null) {
            // Ada gambar baru yang dipilih, unggah dulu
            uploadImageAndSaveProduct(namaProduk, jenisBeras, deskripsi, harga, stok, lokasiPetani)
        } else {
            // Tidak ada gambar baru yang dipilih, gunakan gambar lama (existingImageUrl)
            val finalImageUrl = existingImageUrl.ifEmpty { PLACEHOLDER_IMAGE_URL }
            saveProductToFirestore(namaProduk, jenisBeras, deskripsi, harga, stok, lokasiPetani, finalImageUrl)
        }
    }

    private fun uploadImageAndSaveProduct(name: String, jenisBeras: String, description: String, price: Long, stock: Int, location: String) {
        val sellerId = currentSellerId ?: return

        val tempFile = uriToFile(selectedImageUri ?: return)
        if (tempFile == null) { Toast.makeText(this, "Gagal menyiapkan file.", Toast.LENGTH_LONG).show(); return }

        mainScope.launch {
            binding.btnSaveProduct.isEnabled = false
            binding.progressBar.visibility = View.VISIBLE
            Toast.makeText(this@ManageProductActivity, "Mengunggah foto ke Cloudinary...", Toast.LENGTH_LONG).show()
        }

        ioScope.launch {
            try {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", tempFile.name,
                        tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                    .addFormDataPart("upload_preset", CLOUDINARY_UPLOAD_PRESET)
                    .addFormDataPart("folder", "eberas/${sellerId}")
                    .build()

                val request = Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/image/upload")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()

                    if (response.isSuccessful) {
                        val jsonResponse = JSONObject(responseBody)
                        val imageUrl = jsonResponse.getString("secure_url")

                        mainScope.launch {
                            saveProductToFirestore(name, jenisBeras, description, price, stock, location, imageUrl)
                        }
                    } else {
                        val errorDetail = try {
                            JSONObject(responseBody).getString("error")
                        } catch (e: Exception) {
                            "Response non-JSON: $responseBody"
                        }

                        mainScope.launch {
                            Log.e(TAG, "Upload Gagal. Response: ${response.code}. Detail: $errorDetail")
                            Toast.makeText(this@ManageProductActivity,
                                "Upload GAGAL: (Code ${response.code}) Cek Cloud Name & Upload Preset Anda. Detail Error: ${errorDetail.take(50)}",
                                Toast.LENGTH_LONG).show()
                            binding.btnSaveProduct.isEnabled = true
                            binding.progressBar.visibility = View.GONE
                        }
                    }
                }
            } catch (e: IOException) {
                mainScope.launch {
                    Log.e(TAG, "Network Error (IOException): ${e.message}")
                    Toast.makeText(this@ManageProductActivity, "Upload GAGAL: Periksa koneksi jaringan. (Timeout/Network Error)", Toast.LENGTH_LONG).show()
                    binding.btnSaveProduct.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                mainScope.launch {
                    Log.e(TAG, "General Error: ${e.message}")
                    Toast.makeText(this@ManageProductActivity, "Upload GAGAL: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnSaveProduct.isEnabled = true
                    binding.progressBar.visibility = View.GONE
                }
            } finally {
                tempFile.delete()
            }
        }
    }


    private fun saveProductToFirestore(name: String, jenisBeras: String, description: String, price: Long, stock: Int, location: String, imageUrl: String) {
        val docRef = if (currentProductId != null) {
            firestore.collection("produk").document(currentProductId!!)
        } else {
            firestore.collection("produk").document()
        }

        val finalProductId = docRef.id

        val productToSave = Produk(
            id_produk = finalProductId,
            nama_produk = name,
            jenis_beras = jenisBeras,
            deskripsi = description,
            harga = price,
            stok = stock,
            lokasi_petani = location,
            foto_produk = imageUrl,
            id_penjual = currentSellerId!!
        )

        docRef.set(productToSave)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                val action = if (currentProductId != null) "diperbarui" else "ditambahkan"
                Toast.makeText(this, "Produk berhasil $action.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                binding.btnSaveProduct.isEnabled = true
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Gagal menyimpan produk: ${it.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error saving product", it)
            }
    }
}