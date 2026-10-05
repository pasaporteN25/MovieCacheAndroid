package io.github.pasaporten25.movieinbox.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class RatingMergeTest {
    @Test fun unchangedLocalTakesServer() = assertEquals(3, mergeRating(8, 8, 3))
    @Test fun unchangedServerTakesLocalIncludingClear() = assertEquals(null, mergeRating(8, null, 8))
    @Test fun concurrentChangesTakeHighest() = assertEquals(9, mergeRating(2, 7, 9))
    @Test fun convergedChangesStayConverged() = assertEquals(7, mergeRating(2, 7, 7))
    @Test fun concurrentClearPreservesOtherScore() = assertEquals(5, mergeRating(8, null, 5))
}
