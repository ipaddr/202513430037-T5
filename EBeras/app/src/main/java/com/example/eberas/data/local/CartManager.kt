package com.example.eberas.data.local

import com.example.eberas.data.model.CartItem

/**
 * Manajer Keranjang Belanja Sementara (Singleton).
 */
object CartManager {

    private val items = mutableListOf<CartItem>()

    fun getItems(): List<CartItem> {
        return items.toList()
    }

    /**
     * Menambahkan produk ke keranjang atau memperbarui kuantitas jika produk sudah ada.
     */
    fun addItem(newItem: CartItem) {
        val existingItem = items.find { it.produk.id_produk == newItem.produk.id_produk }

        if (existingItem != null) {
            // Produk sudah ada, tambahkan kuantitasnya
            existingItem.kuantitas += newItem.kuantitas
        } else {
            // Produk baru, tambahkan ke daftar
            items.add(newItem)
        }
    }

    /**
     * Menghapus item berdasarkan ID Produk.
     */
    fun removeItem(productId: String) {
        items.removeAll { it.produk.id_produk == productId }
    }

    /**
     * Memperbarui kuantitas item yang sudah ada.
     */
    fun updateQuantity(productId: String, newQuantity: Int) {
        val existingItem = items.find { it.produk.id_produk == productId }

        if (existingItem != null) {
            if (newQuantity <= 0) {
                // Jika kuantitas 0 atau kurang, hapus item
                removeItem(productId)
            } else if (newQuantity <= existingItem.produk.stok) {
                // Perbarui kuantitas jika tidak melebihi stok
                existingItem.kuantitas = newQuantity
            }
        }
    }

    fun clearCart() {
        items.clear()
    }

    /**
     * PERBAIKAN: Mengubah tipe return dari Double menjadi Long.
     * Menggunakan sumOf untuk menghitung total harga (Long * Int = Long).
     */
    fun calculateTotal(): Long {
        return items.sumOf { it.produk.harga * it.kuantitas }
    }
}