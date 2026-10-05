package io.github.pasaporten25.movieinbox.sync

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

internal class RatingSyncRepository(
    private val remote: RatingRemote,
    private val store: RatingReplicaStore,
) {
    private val mutex = Mutex()

    suspend fun sync(): Result<Unit> = mutex.withLock {
        try {
            val sent = store.snapshot()
            repeat(3) {
                val server = remote.readRating(sent.id)
                val merged = mergeRating(sent.base, sent.local, server)
                try {
                    val confirmed = if (merged == server) server
                        else remote.patchRating(sent.id, merged, server)
                    store.confirm(sent, confirmed)
                    return@withLock Result.success(Unit)
                } catch (_: RatingConflict) {
                    // A crossed edit requires a fresh read, never an unconditional write.
                }
            }
            Result.failure(SyncError.ConcurrentChanges())
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            Result.failure(SyncError.Network())
        } catch (error: HttpException) {
            Result.failure(SyncError.Server(error.code()))
        }
    }
}
