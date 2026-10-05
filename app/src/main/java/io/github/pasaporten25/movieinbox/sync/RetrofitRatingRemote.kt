package io.github.pasaporten25.movieinbox.sync

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

@Serializable internal data class RatingPersonalDto(val rating: Int?)
@Serializable internal data class RatingItemDto(val personal: RatingPersonalDto)
@Serializable internal data class RatingBaseDto(val rating: Int?)
@Serializable internal data class RatingPatchDto(val rating: Int?, val base: RatingBaseDto)
@Serializable private data class ErrorEnvelope(val error: ErrorCode)
@Serializable private data class ErrorCode(val code: String)

internal interface RatingService {
    @GET("api/v1/catalog/items/{id}")
    suspend fun read(@Path("id") id: String): RatingItemDto
    @PATCH("api/v1/catalog/items/{id}/personal")
    suspend fun patch(@Path("id") id: String, @Body patch: RatingPatchDto): RatingItemDto
}

internal fun createRatingRemote(
    origin: String,
    token: () -> String?,
    client: OkHttpClient = OkHttpClient(),
    allowLoopbackHttp: Boolean = false,
): RatingRemote {
    val url = origin.toHttpUrl()
    require(url.username.isEmpty() && url.password.isEmpty())
    require(url.encodedPath == "/" && url.query == null && url.fragment == null)
    // Cleartext is limited to the JVM harness; no Android cleartext policy is enabled.
    require(url.isHttps || (allowLoopbackHttp && url.host == "127.0.0.1"))
    val json = Json { ignoreUnknownKeys = true; explicitNulls = true }
    val authenticated = client.newBuilder()
        .followRedirects(false).followSslRedirects(false)
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val bearer = token() ?: throw IOException("Device session unavailable")
            val response = chain.proceed(chain.request().newBuilder()
                .header("Authorization", "Bearer $bearer").build())
            if (response.header("X-Movie-Inbox-Api-Version") != "1") {
                response.close()
                throw IOException("Unsupported device API version")
            }
            response
        }.build()
    val service = Retrofit.Builder().baseUrl(origin.trimEnd('/') + "/")
        .client(authenticated)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build().create(RatingService::class.java)
    return object : RatingRemote {
        override suspend fun readRating(id: String) = service.read(id).personal.rating
        override suspend fun patchRating(id: String, value: Int?, base: Int?): Int? {
            try {
                return service.patch(id, RatingPatchDto(value, RatingBaseDto(base))).personal.rating
            } catch (error: HttpException) {
                val body = error.response()?.errorBody()?.string()
                if (error.code() == 409 && body != null &&
                    runCatching { json.decodeFromString<ErrorEnvelope>(body).error.code }.getOrNull() == "personal_conflict") {
                    throw RatingConflict()
                }
                throw error
            }
        }
    }
}
