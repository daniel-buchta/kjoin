package io.github.danielbuchta.kjoin.core

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

internal class RightJoinTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("io.github.danielbuchta.kjoin.test.JoinCasesKt#joinCases")
    fun `right join keeps every right row`(case: JoinCase) {
        withClue("using key") {
            Joins.rightJoinUsingKey(case.left, case.right, key = Row::key) shouldBe case.rightOuter
        }
        withClue("on condition") {
            Joins.rightJoinOnCondition(case.left, case.right) { left.key == right.key } shouldBe case.rightOuter
        }
    }

    @Nested
    inner class `right join on condition with custom predicate` {

        @Test
        fun `with value comparison should return filtered matches and unmatched`() {
            val result = Joins.rightJoinOnCondition(sampleLeft, sampleRight) {
                left.key == right.key && left.value < 3
            }

            result shouldBe listOf(
                sampleLeft[0] to sampleRight[0],
                sampleLeft[1] to sampleRight[1],
                null to sampleRight[2],
            )
        }

        @Test
        fun `with always true predicate should return cartesian product ordered by the right input`() {
            val result = Joins.rightJoinOnCondition(sampleLeft, sampleRight) { true }

            result shouldBe sampleRight.flatMap { r -> sampleLeft.map { l -> l to r } }
        }

        @Test
        fun `with always false predicate should return all right rows paired with null`() {
            val result = Joins.rightJoinOnCondition(sampleLeft, sampleRight) { false }

            result shouldBe sampleRight.map { null to it }
        }
    }
}
