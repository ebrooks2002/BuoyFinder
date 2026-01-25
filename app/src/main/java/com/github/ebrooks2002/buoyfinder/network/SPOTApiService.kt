/**
 * @author Ethan Brooks
 * Builds a Retrofit client, calls SPOT Server, gets XML Feed, parses data using simpleXML.
 */

package com.github.ebrooks2002.buoyfinder.network

import com.github.ebrooks2002.buoyfinder.model.AssetData
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import retrofit2.http.GET
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.http.Query

private const val FEED_ID = "0r0YXhJmCiRJpmmJiaAdr6Ez6VIhahnMu"

private const val BASE_URL =
    "https://api.findmespot.com/spot-main-web/consumer/rest-api/2.0/public/feed/$FEED_ID/"

private val logger = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY // Contains the full raw XML Response.
}

private val client = OkHttpClient.Builder()
    .addInterceptor(logger)
    .build()

private val retrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .addConverterFactory(SimpleXmlConverterFactory.create())
    .client(client)
    .build()

/**
 * Defines how Retrofit talks to the SPOT web server.
 */
interface SPOTApiService {
    /**
     * Returns an asset data object containing XML feed data.
     */
    @GET("message.xml")
    suspend fun getData(
        @Query("count") count: Int = 50,
        @Query("start") start: Int = 0
    ): AssetData
}

object SPOTApi {
    val retrofitService: SPOTApiService by lazy {
        retrofit.create(SPOTApiService::class.java)
    }
}