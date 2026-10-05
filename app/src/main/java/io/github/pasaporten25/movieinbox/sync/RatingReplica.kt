package io.github.pasaporten25.movieinbox.sync

internal data class RatingReplica(val id: String, val base: Int?, val local: Int?) {
    init {
        require(base == null || base in 0..10)
        require(local == null || local in 0..10)
    }
}

internal fun mergeRating(base: Int?, local: Int?, server: Int?): Int? = when {
    local == base -> server
    server == base || local == server -> local
    else -> listOfNotNull(local, server).maxOrNull()
}

internal interface RatingReplicaStore {
    fun snapshot(): RatingReplica
    fun confirm(sent: RatingReplica, confirmed: Int?)
}
