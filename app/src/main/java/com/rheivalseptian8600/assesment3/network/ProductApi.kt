package com.rheivalseptian8600.assesment3.network

import com.rheivalseptian8600.assesment3.model.Product
import com.rheivalseptian8600.assesment3.model.ProductResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

private const val BASE_URL = "https://dummyjson.com/"

private val retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl(BASE_URL)
    .build()

interface ProductApiService {
    @GET("products")
    suspend fun getProducts(): ProductResponse

    @DELETE("products/{id}")
    suspend fun deleteProduct(@Path("id") id: Int): Response<Unit>

    @Multipart
    @POST("products/add")
    suspend fun addProduct(
        @Part("title") title: RequestBody,
        @Part("price") price: RequestBody,
        @Part("description") description: RequestBody,
        @Part image: MultipartBody.Part?
    ): Response<Product>
}

object ProductApi {
    enum class ApiStatus { LOADING, SUCCESS, FAILED }

    val service: ProductApiService by lazy {
        retrofit.create(ProductApiService::class.java)
    }
}