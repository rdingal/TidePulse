package com.example.data.remote

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private val moshi: Moshi = Moshi.Builder().build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    private fun buildRetrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    val noaaMetadataService: NoaaMetadataService by lazy {
        buildRetrofit("https://api.tidesandcurrents.noaa.gov/").create(NoaaMetadataService::class.java)
    }

    val noaaDataService: NoaaDataService by lazy {
        buildRetrofit("https://api.tidesandcurrents.noaa.gov/").create(NoaaDataService::class.java)
    }

    val openMeteoMarineService: OpenMeteoMarineService by lazy {
        buildRetrofit("https://marine-api.open-meteo.com/").create(OpenMeteoMarineService::class.java)
    }

    val openMeteoGeocodingService: OpenMeteoGeocodingService by lazy {
        buildRetrofit("https://geocoding-api.open-meteo.com/").create(OpenMeteoGeocodingService::class.java)
    }

    fun sharedMoshi(): Moshi = moshi
}
