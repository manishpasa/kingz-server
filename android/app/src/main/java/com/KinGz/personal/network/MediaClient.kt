package com.KinGz.personal.network

import android.content.Context
import okhttp3.OkHttpClient

object MediaClient {

    fun create(context: Context): OkHttpClient {
        val preferences = AuthPreferences(context.applicationContext)

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = preferences.getToken()

                val request = chain.request()
                    .newBuilder()
                    .apply {
                        if (!token.isNullOrBlank()) {
                            header("Authorization", "Bearer $token")
                        }
                    }
                    .build()

                chain.proceed(request)
            }
            .build()
    }
}
