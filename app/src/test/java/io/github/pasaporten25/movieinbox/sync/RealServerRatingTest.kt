package io.github.pasaporten25.movieinbox.sync

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test

class RealServerRatingTest {
    @Test fun downloadEditUploadRereadAndCrossedWrite() = runTest {
        val origin = System.getProperty("movieInboxTestOrigin")
        assumeNotNull(origin)
        val json = Json { ignoreUnknownKeys = true }
        val client = OkHttpClient()
        val loginRequest = Request.Builder().url("$origin/api/v1/auth/login")
            .post("""{"username":"sync-test","password":"synthetic-long-password","device_name":"Kotlin harness"}""".toRequestBody("application/json".toMediaType())).build()
        val token = client.newCall(loginRequest).execute().use {
            assertEquals(201, it.code)
            json.parseToJsonElement(it.body!!.string()).jsonObject.getValue("access_token").jsonPrimitive.content
        }
        var discardNextPatch = false
        val transport = client.newBuilder().addInterceptor { chain ->
            val response = chain.proceed(chain.request())
            if (discardNextPatch && chain.request().method == "PATCH" && response.isSuccessful) {
                discardNextPatch = false
                response.close()
                throw java.io.IOException("Synthetic lost response")
            }
            response
        }.build()
        val remote = createRatingRemote(origin!!, { token }, transport, allowLoopbackHttp = true)
        val request = Request.Builder().url("$origin/api/v1/catalog/items")
            .header("Authorization", "Bearer $token").build()
        val id = client.newCall(request).execute().use {
            assertEquals(200, it.code)
            json.parseToJsonElement(it.body!!.string()).jsonObject.getValue("items").jsonArray[0]
                .jsonObject.getValue("id").jsonPrimitive.content
        }
        val initial = remote.readRating(id)
        val store = MemoryRatingReplica(RatingReplica(id, initial, 7))
        assertTrue(RatingSyncRepository(remote, store).sync().isSuccess)
        assertEquals(7, remote.readRating(id))
        assertEquals(RatingReplica(id, 7, 7), store.snapshot())
        remote.patchRating(id, 9, 7)
        try {
            remote.patchRating(id, 8, 7)
            fail("Stale base must be rejected")
        } catch (_: RatingConflict) { }
        store.edit(8)
        assertTrue(RatingSyncRepository(remote, store).sync().isSuccess)
        assertEquals(RatingReplica(id, 9, 9), store.snapshot())
        assertEquals(9, remote.readRating(id))
        store.edit(10)
        discardNextPatch = true
        assertTrue(RatingSyncRepository(remote, store).sync().isFailure)
        assertEquals(RatingReplica(id, 9, 10), store.snapshot())
        assertEquals(10, remote.readRating(id))
        assertTrue(RatingSyncRepository(remote, store).sync().isSuccess)
        assertEquals(RatingReplica(id, 10, 10), store.snapshot())
    }
}
