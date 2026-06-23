package com.rheivalseptian8600.assesment3.model

import com.google.gson.annotations.SerializedName

data class ProductResponse(
    @SerializedName("products") val products: List<Product>
)

data class Product(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("title") val title: String,
    @SerializedName("price") val price: Double,
    @SerializedName("description") val description: String,

    @SerializedName("thumbnail") val thumbnail: Any? = null,

    @SerializedName("mine") val mine: String? = "0"
)