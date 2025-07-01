package com.example.prototipopasseios.controller

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Altere a BASE_URL para a URL da sua API
    private const val BASE_URL = "http://192.168.0.89:8080/"

    // 1) Cria um interceptor de logging
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        // Todas as requisições/respostas serão logadas com esta tag
        Log.d("API-LOG", message)
    }.apply {
        // BODY para registrar cabeçalhos e corpo completo
        level = HttpLoggingInterceptor.Level.BODY
    }

    // 2) Constrói o OkHttpClient que usa o interceptor
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    // 3) Cria o Retrofit usando esse cliente
    val pessoaApiService: PessoaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            // IMPORTANTE: passa o httpClient com logging
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PessoaApiService::class.java)
    }
}
