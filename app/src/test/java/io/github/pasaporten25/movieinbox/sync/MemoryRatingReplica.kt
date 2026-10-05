package io.github.pasaporten25.movieinbox.sync

internal class MemoryRatingReplica(private var value: RatingReplica) : RatingReplicaStore {
    @Synchronized override fun snapshot() = value
    @Synchronized fun edit(rating: Int?) { value = value.copy(local = rating) }
    @Synchronized override fun confirm(sent: RatingReplica, confirmed: Int?) {
        value = value.copy(base = confirmed, local = if (value.local == sent.local) confirmed else value.local)
    }
}
