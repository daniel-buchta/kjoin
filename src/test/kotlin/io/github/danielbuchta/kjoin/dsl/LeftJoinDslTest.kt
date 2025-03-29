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

internal class LeftJoinDslTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("io.github.danielbuchta.kjoin.test.JoinCasesKt#joinCases")
    fun `left join dsl agrees with the core join`(case: JoinCase) {
        withClue("using") {
            Kjoin { case.left leftJoin case.right using Row::key } shouldBe case.leftOuter
        }
        withClue("on") {
            Kjoin { case.left leftJoin case.right on { left.key == right.key } } shouldBe case.leftOuter
        }
    }

    @Nested
    inner class `left join dsl on condition with custom predicate` {

        @Test
        fun `with value comparison should return filtered matches and unmatched`() {
            val result = Kjoin {
                sampleLeft leftJoin sampleRight on { left.key == right.key && left.value < 3 }
            }

            result shouldBe listOf(
                sampleLeft[0] to sampleRight[0],
                sampleLeft[1] to sampleRight[1],
                sampleLeft[2] to null,
            )
        }

        @Test
        fun `with always true predicate should return cartesian product`() {
            val result = Kjoin { sampleLeft leftJoin sampleRight on { true } }

            result shouldBe sampleLeft.flatMap { l -> sampleRight.map { r -> l to r } }
        }

        @Test
        fun `with always false predicate should return all left rows paired with null`() {
            val result = Kjoin { sampleLeft leftJoin sampleRight on { false } }

            result shouldBe sampleLeft.map { it to null }
        }
    }
}
