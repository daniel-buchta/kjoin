package io.github.danielbuchta.kjoin.dsl

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import io.github.danielbuchta.kjoin.test.JoinCase
import io.github.danielbuchta.kjoin.test.Row
import io.github.danielbuchta.kjoin.test.sampleLeft
import io.github.danielbuchta.kjoin.test.sampleRight

internal class InnerJoinDslTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("io.github.danielbuchta.kjoin.test.JoinCasesKt#joinCases")
    fun `inner join dsl agrees with the core join`(case: JoinCase) {
        withClue("using") {
            Kjoin { case.left innerJoin case.right using Row::key } shouldBe case.inner
        }
        withClue("on") {
            Kjoin { case.left innerJoin case.right on { left.key == right.key } } shouldBe case.inner
        }
    }

    @Nested
    inner class `inner join dsl on condition with custom predicate` {

        @Test
        fun `with value comparison should return filtered matches`() {
            val result = Kjoin {
                sampleLeft innerJoin sampleRight on { left.key == right.key && left.value < 3 }
            }

            result shouldBe listOf(
                sampleLeft[0] to sampleRight[0],
                sampleLeft[1] to sampleRight[1],
            )
        }

        @Test
        fun `with always true predicate should return cartesian product`() {
            val result = Kjoin { sampleLeft innerJoin sampleRight on { true } }

            result shouldBe sampleLeft.flatMap { l -> sampleRight.map { r -> l to r } }
        }

        @Test
        fun `with always false predicate should return empty list`() {
            val result = Kjoin { sampleLeft innerJoin sampleRight on { false } }

            result shouldBe emptyList()
        }
    }
}
