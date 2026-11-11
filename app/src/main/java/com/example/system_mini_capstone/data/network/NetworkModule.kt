package com.example.system_mini_capstone.data.network

import android.content.Context
import com.example.system_mini_capstone.admin.config.SystemConfigManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton network configuration providing Retrofit instances and API services
 * utilizing saved System Config base URL and timeout with robust validation and safe fallback.
 */
object NetworkModule {

    private const val DEFAULT_BASE_URL = "https://api.signlink.app/"

    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    fun getSafeBaseUrl(context: Context?): String {
        if (context == null) return DEFAULT_BASE_URL
        return try {
            val config = SystemConfigManager(context).loadConfig()
            val url = config.apiConfig.baseUrl
            if ((url.startsWith("http://") || url.startsWith("https://")) && url.endsWith("/") && !url.contains(" ")) {
                url
            } else {
                DEFAULT_BASE_URL
            }
        } catch (_: Exception) {
            DEFAULT_BASE_URL
        }
    }

    fun getSafeTimeout(context: Context?): Int {
        if (context == null) return 15
        return try {
            SystemConfigManager(context).loadConfig().apiConfig.timeoutSeconds
        } catch (_: Exception) {
            15
        }
    }

    fun getAuthApiService(context: Context? = null): AuthApiService {
        val baseUrl = getSafeBaseUrl(context)
        val timeout = getSafeTimeout(context)

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(timeout.toLong(), TimeUnit.SECONDS)
            .readTimeout(timeout.toLong(), TimeUnit.SECONDS)
            .writeTimeout(timeout.toLong(), TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(AuthApiService::class.java)
    }

    val authApiService: AuthApiService by lazy {
        getAuthApiService(null)
    }
}
