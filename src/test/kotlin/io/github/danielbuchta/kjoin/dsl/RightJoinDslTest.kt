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

internal class RightJoinDslTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("io.github.danielbuchta.kjoin.test.JoinCasesKt#joinCases")
    fun `right join dsl agrees with the core join`(case: JoinCase) {
        withClue("using") {
            Kjoin { case.left rightJoin case.right using Row::key } shouldBe case.rightOuter
        }
        withClue("on") {
            Kjoin { case.left rightJoin case.right on { left.key == right.key } } shouldBe case.rightOuter
        }
    }

    @Nested
    inner class `right join dsl on condition with custom predicate` {

        @Test
        fun `with value comparison should return filtered matches and unmatched`() {
            val result = Kjoin {
                sampleLeft rightJoin sampleRight on { left.key == right.key && left.value < 3 }
            }

            result shouldBe listOf(
                sampleLeft[0] to sampleRight[0],
                sampleLeft[1] to sampleRight[1],
                null to sampleRight[2],
            )
        }

        @Test
        fun `with always true predicate should return cartesian product ordered by the right input`() {
            val result = Kjoin { sampleLeft rightJoin sampleRight on { true } }

            result shouldBe sampleRight.flatMap { r -> sampleLeft.map { l -> l to r } }
        }

        @Test
        fun `with always false predicate should return all right rows paired with null`() {
            val result = Kjoin { sampleLeft rightJoin sampleRight on { false } }

            result shouldBe sampleRight.map { null to it }
        }
    }
}
