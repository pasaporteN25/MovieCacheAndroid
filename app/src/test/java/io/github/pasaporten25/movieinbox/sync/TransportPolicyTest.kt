package io.github.pasaporten25.movieinbox.sync

import org.junit.Test

class TransportPolicyTest {
    @Test(expected = IllegalArgumentException::class)
    fun productionRejectsCleartext() { createRatingRemote("http://192.168.1.20", { "unused" }) }
    @Test(expected = IllegalArgumentException::class)
    fun productionRejectsCredentialsInOrigin() { createRatingRemote("https://user:pass@example.com", { "unused" }) }
}
