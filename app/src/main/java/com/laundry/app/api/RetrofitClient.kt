package com.laundry.app.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val DEFAULT_BASE_URL = "https://front-back-node-laundry-system-production.up.railway.app/"

    private var currentToken: String? = null
    private var customBaseUrl: String = DEFAULT_BASE_URL

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = okhttp3.Interceptor { chain ->
        val originalRequest = chain.request()
        val token = currentToken
        
        val newRequest = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .build()
        } else {
            originalRequest
        }
        chain.proceed(newRequest)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private var retrofitInstance: Retrofit? = null

    fun setToken(token: String?) {
        currentToken = token
    }

    fun setBaseUrl(url: String) {
        val formattedUrl = if (url.endsWith("/")) url else "$url/"
        if (customBaseUrl != formattedUrl) {
            customBaseUrl = formattedUrl
            retrofitInstance = null
        }
    }

    fun getBaseUrl(): String {
        return customBaseUrl
    }

    private fun getRetrofit(): Retrofit {
        if (retrofitInstance == null) {
            retrofitInstance = Retrofit.Builder()
                .baseUrl(customBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofitInstance!!
    }

    val apiService: ApiService
        get() = getRetrofit().create(ApiService::class.java)
}
