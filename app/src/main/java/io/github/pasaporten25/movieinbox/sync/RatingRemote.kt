package io.github.pasaporten25.movieinbox.sync

internal interface RatingRemote {
    suspend fun readRating(id: String): Int?
    suspend fun patchRating(id: String, value: Int?, base: Int?): Int?
}

internal class RatingConflict : Exception()

internal sealed class SyncError : Exception() {
    class Network : SyncError()
    class Server(val status: Int) : SyncError()
    class ConcurrentChanges : SyncError()
}
