package com.moefactory.bettermiuiexpress.repository

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.moefactory.bettermiuiexpress.api.CaiNiaoApi
import com.moefactory.bettermiuiexpress.api.KuaiDi100Api
import com.moefactory.bettermiuiexpress.base.converter.CaiNiaoRequestDataConverterFactory
import com.moefactory.bettermiuiexpress.base.converter.KuaiDi100RequestDataConverterFactory
import com.moefactory.bettermiuiexpress.base.cookiejar.MemoryCookieJar
import com.moefactory.bettermiuiexpress.base.interceptor.CaiNiaoRequestInterceptor
import com.moefactory.bettermiuiexpress.base.interceptor.KuaiDi100Interceptor
import com.moefactory.bettermiuiexpress.model.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

object ExpressActualRepository {

    val jsonParser by lazy {
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .addInterceptor(CaiNiaoRequestInterceptor())
            .addInterceptor(KuaiDi100Interceptor())
            .cookieJar(MemoryCookieJar())
            .build()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://poll.kuaidi100.com/")
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(jsonParser.asConverterFactory("application/json".toMediaType()))
            .addConverterFactory(CaiNiaoRequestDataConverterFactory.create())
            .addConverterFactory(KuaiDi100RequestDataConverterFactory.create())
            .client(okHttpClient)
            .build()
    }

    private val kuaiDi100Api by lazy { retrofit.create(KuaiDi100Api::class.java) }

    private val caiNiaoApi by lazy { retrofit.create(CaiNiaoApi::class.java) }

    /**** KuaiDi100 Begin ****/

    suspend fun queryCompanyActual(mailNumber: String, secretKey: String): List<KuaiDi100Company> {
        val response = kuaiDi100Api.queryExpressCompany(secretKey, mailNumber)
        when {
            // Normal
            response.startsWith("[") -> {
                return jsonParser.decodeFromString(response)
            }
            // Error
            response.startsWith("{") -> {
                val message = jsonParser.parseToJsonElement(response)
                    .jsonObject["message"]?.jsonPrimitive?.content
                throw Exception(message)
            }
            // Exception
            else -> throw Exception("Unexpected response: $response")
        }
    }

    suspend fun queryExpressDetailsFromKuaiDi100Actual(
        companyCode: String,
        mailNumber: String,
        phoneNumber: String?,
        secretKey: String,
        customer: String
    ): BaseKuaiDi100Response {
        // Shunfeng and Fengwang need phone number
        val data = if (companyCode == "shunfeng" || companyCode == "fengwang") {
            KuaiDi100RequestParam(companyCode, mailNumber, phoneNumber)
        } else {
            KuaiDi100RequestParam(companyCode, mailNumber)
        }

        return kuaiDi100Api.queryPackage(data, customer, secretKey)
    }

    /****  KuaiDi100 End  ****/

    /****  CaiNiao Begin  ****/

    suspend fun queryExpressDetailsFromCaiNiaoActual(mailNumber: String): CaiNiaoExpressDetailsResult? {
        val tokenResponse = caiNiaoApi.getToken()
        val tokenField = tokenResponse.token ?: throw IllegalArgumentException("Missing token in response")
        val token = tokenField.split("_")[0]

        val detailsResponse = caiNiaoApi.queryExpressDetails(CaiNiaoRequestData(mailNumber), token)

        return detailsResponse.data.results?.get(0)
    }
}