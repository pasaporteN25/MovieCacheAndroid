package io.github.pasaporten25.movieinbox.sync

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class RatingSyncTest {
    private class FakeRemote(var rating: Int? = 2) : RatingRemote {
        var loseResponse = false
        var calls = 0
        override suspend fun readRating(id: String) = rating
        override suspend fun patchRating(id: String, value: Int?, base: Int?): Int? {
            calls++
            if (rating != base) throw RatingConflict()
            rating = value
            if (loseResponse) throw IOException("response lost")
            return rating
        }
    }
    @Test fun confirmedValueAdvancesBothVersions() = runTest {
        val remote = FakeRemote()
        val store = MemoryRatingReplica(RatingReplica("work", 2, 7))
        assertTrue(RatingSyncRepository(remote, store).sync().isSuccess)
        assertEquals(RatingReplica("work", 7, 7), store.snapshot())
        assertEquals(7, remote.rating)
    }
    @Test fun lostResponsePreservesBaseAndRetryConvergesWithoutSecondWrite() = runTest {
        val remote = FakeRemote().apply { loseResponse = true }
        val store = MemoryRatingReplica(RatingReplica("work", 2, 7))
        val repository = RatingSyncRepository(remote, store)
        assertTrue(repository.sync().isFailure)
        assertEquals(RatingReplica("work", 2, 7), store.snapshot())
        remote.loseResponse = false
        assertTrue(repository.sync().isSuccess)
        assertEquals(RatingReplica("work", 7, 7), store.snapshot())
        assertEquals(1, remote.calls)
    }
    @Test fun crossingWriteIsRereadAndMerged() = runTest {
        val remote = object : RatingRemote {
            var rating: Int? = 2
            var first = true
            override suspend fun readRating(id: String) = rating
            override suspend fun patchRating(id: String, value: Int?, base: Int?): Int? {
                if (first) { first = false; rating = 9; throw RatingConflict() }
                assertEquals(rating, base)
                rating = value
                return rating
            }
        }
        val store = MemoryRatingReplica(RatingReplica("work", 2, 7))
        assertTrue(RatingSyncRepository(remote, store).sync().isSuccess)
        assertEquals(RatingReplica("work", 9, 9), store.snapshot())
    }
    @Test fun editDuringRequestRemainsPendingAgainstConfirmedBase() = runTest {
        val store = MemoryRatingReplica(RatingReplica("work", 2, 7))
        val remote = object : RatingRemote {
            override suspend fun readRating(id: String): Int? = 2
            override suspend fun patchRating(id: String, value: Int?, base: Int?): Int? {
                store.edit(10)
                return value
            }
        }
        assertTrue(RatingSyncRepository(remote, store).sync().isSuccess)
        assertEquals(RatingReplica("work", 7, 10), store.snapshot())
    }
}
