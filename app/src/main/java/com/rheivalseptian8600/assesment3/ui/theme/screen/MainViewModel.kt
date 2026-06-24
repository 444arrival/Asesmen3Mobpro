package com.rheivalseptian8600.assesment3.ui.theme.screen

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rheivalseptian8600.assesment3.data.AppDatabase
import com.rheivalseptian8600.assesment3.data.ProductEntity
import com.rheivalseptian8600.assesment3.model.Product
import com.rheivalseptian8600.assesment3.network.ProductApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val productDao = AppDatabase.getInstance(application).productDao

    var data = mutableStateOf(emptyList<Product>())
        private set

    var status = MutableStateFlow(ProductApi.ApiStatus.LOADING)
        private set

    var errorMessage = mutableStateOf<String?>(null)

    private fun Bitmap.toMultipartBody(): MultipartBody.Part {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, stream)
        val byteArray = stream.toByteArray()
        val requestBody = byteArray.toRequestBody(
            "image/jpeg".toMediaTypeOrNull(), 0, byteArray.size
        )
        return MultipartBody.Part.createFormData(
            "image", "product_image.jpg", requestBody
        )
    }

    private fun saveBitmapToInternalStorage(bitmap: Bitmap, id: Int): String? {
        return try {
            val context = getApplication<Application>().applicationContext
            val file = File(context.filesDir, "product_$id.jpg")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.flush()
            stream.close()
            file.absolutePath
        } catch (e: Exception) {
            Log.e("MainViewModel", "Gagal menyimpan gambar lokal: ${e.message}")
            null
        }
    }

    fun clearMessage() { errorMessage.value = null }

    fun retrieveData(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            status.value = ProductApi.ApiStatus.LOADING
            try {
                val response = ProductApi.service.getProducts()

                val entities = response.products.map {
                    ProductEntity(
                        id = it.id ?: 0,
                        title = it.title,
                        price = it.price,
                        description = it.description,
                        thumbnail = it.thumbnail as? String,
                        mine = "0"
                    )
                }

                productDao.insertProducts(entities)
                loadLocalDatabase()
            } catch (e: Exception) {
                Log.d("MainViewModel", "Internet Failure, switching to Local Room: ${e.message}")
                loadLocalDatabase()
            }
        }
    }

    private suspend fun loadLocalDatabase() {
        try {
            val localEntities = productDao.getAllProducts()
            if (localEntities.isNotEmpty()) {
                val localProducts = localEntities.map { entity ->
                    Product(
                        id = entity.id,
                        title = entity.title,
                        price = entity.price,
                        description = entity.description,
                        thumbnail = entity.thumbnail,
                        mine = entity.mine
                    )
                }

                data.value = localProducts.sortedByDescending { it.id }
                status.value = ProductApi.ApiStatus.SUCCESS
            } else {
                status.value = ProductApi.ApiStatus.FAILED
            }
        } catch (localEx: Exception) {
            status.value = ProductApi.ApiStatus.FAILED
        }
    }

    fun saveData(userId: String, title: String, description: String, price: String, bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {

            val highestId = data.value.maxOfOrNull { it.id ?: 0 } ?: 194
            val uniqueId = if (highestId >= 195) highestId + 1 else 195
            val localImagePath = saveBitmapToInternalStorage(bitmap, uniqueId)

            try {
                productDao.insertProducts(
                    listOf(
                        ProductEntity(
                            id = uniqueId,
                            title = title,
                            price = price.toDoubleOrNull() ?: 0.0,
                            description = description,
                            thumbnail = localImagePath,
                            mine = "1"
                        )
                    )
                )

                loadLocalDatabase()
                val response = ProductApi.service.addProduct(
                    title = title.toRequestBody("text/plain".toMediaTypeOrNull()),
                    description = description.toRequestBody("text/plain".toMediaTypeOrNull()),
                    price = price.toRequestBody("text/plain".toMediaTypeOrNull()),
                    image = bitmap.toMultipartBody()
                )

                if (response.isSuccessful) {
                    Log.d("MainViewModel", "Berhasil sinkronisasi produk baru ke server API.")
                } else {
                    Log.d("MainViewModel", "Gagal upload ke server (Kode: ${response.code()}), namun data aman di lokal Room.")
                }

            } catch (e: Exception) {
                Log.d("MainViewModel", "Sedang Offline: Data disimpan di penyimpanan lokal Room saja. (${e.message})")
                loadLocalDatabase()
            }
        }
    }

    fun updateData(id: Int, title: String, description: String, price: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentLocalList = productDao.getAllProducts()
                val existingProduct = currentLocalList.find { it.id == id }
                val oldThumbnailPath = existingProduct?.thumbnail

                val updatedEntity = ProductEntity(
                    id = id,
                    title = title,
                    price = price.toDoubleOrNull() ?: 0.0,
                    description = description,
                    thumbnail = oldThumbnailPath,
                    mine = "1"
                )
                productDao.insertProducts(listOf(updatedEntity))

                loadLocalDatabase()
            } catch (e: Exception) {
                Log.d("MainViewModel", "Update Failure: ${e.message}")
                errorMessage.value = "Error Gagal Update: ${e.message}"
            }
        }
    }

    fun deleteData(userId: String, id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                productDao.deleteProductById(id)

                if (id > 194) {
                    data.value = data.value.filter { it.id != id }
                } else {
                    val response = ProductApi.service.deleteProduct(id)

                    if (response.isSuccessful) {
                        data.value = data.value.filter { it.id != id }
                    } else {
                        throw Exception("Gagal menghapus produk dari server")
                    }
                }
            } catch (e: Exception) {
                Log.d("MainViewModel", "Failure: ${e.message}")
                errorMessage.value = "Error: ${e.message}"
            }
        }
    }
}