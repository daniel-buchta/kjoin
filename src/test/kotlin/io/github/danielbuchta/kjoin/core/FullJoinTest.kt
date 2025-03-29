package io.github.danielbuchta.kjoin.core

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import io.github.danielbuchta.kjoin.test.JoinCase
import io.github.danielbuchta.kjoin.test.LeftRow
import io.github.danielbuchta.kjoin.test.RightRow
import io.github.danielbuchta.kjoin.test.Row
import io.github.danielbuchta.kjoin.test.sampleLeft
import io.github.danielbuchta.kjoin.test.sampleRight

internal class FullJoinTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("io.github.danielbuchta.kjoin.test.JoinCasesKt#joinCases")
    fun `full join keeps every row from both sides`(case: JoinCase) {
        withClue("using key") {
            Joins.fullJoinUsingKey(case.left, case.right, Row::key) shouldBe case.full
        }
        withClue("on condition") {
            Joins.fullJoinOnCondition(case.left, case.right) { left.key == right.key } shouldBe case.full
        }
    }

    @Nested
    inner class `full join on condition with custom predicate` {

        @Test
        fun `with value comparison should return filtered matches and unmatched`() {
            val result = Joins.fullJoinOnCondition(sampleLeft, sampleRight) {
                left.key == right.key && left.value < 3
            }

            result shouldBe listOf(
                sampleLeft[0] to sampleRight[0],
                sampleLeft[1] to sampleRight[1],
                sampleLeft[2] to null,
                null to sampleRight[2],
            )
        }

        @Test
        fun `with always true predicate should return cartesian product`() {
            val result = Joins.fullJoinOnCondition(sampleLeft, sampleRight) { true }

            result shouldBe sampleLeft.flatMap { l -> sampleRight.map { r -> l to r } }
        }

        @Test
        fun `with always false predicate should return all rows paired with null`() {
            val result = Joins.fullJoinOnCondition(sampleLeft, sampleRight) { false }

            result shouldBe sampleLeft.map<LeftRow, Pair<LeftRow?, RightRow?>> { it to null } +
                sampleRight.map { null to it }
        }
    }

    /**
     * Unmatched right rows are detected by position, not by equality. These tests pin that
     * contract: an equality based implementation would collapse equal-but-distinct rows and
     * silently drop them from the result.
     */
    @Nested
    inner class `full join identifies unmatched right rows by identity` {

        @Test
        fun `keeps an unmatched right row that equals a matched one`() {
            val matched = RightRow("a", 10)
            val equalButUnmatched = RightRow("a", 10)
            val rightRows = listOf(matched, equalButUnmatched)

            val result = Joins.fullJoinOnCondition(listOf(LeftRow("a", 1)), rightRows) { right === matched }

            result shouldBe listOf(
                LeftRow("a", 1) to matched,
                null to equalButUnmatched,
            )
        }

        @Test
        fun `emits one row per occurrence when the same unmatched instance appears twice`() {
            val shared = RightRow("z", 10)

            val result = Joins.fullJoinOnCondition(listOf(LeftRow("a", 1)), listOf(shared, shared)) { false }

            result shouldBe listOf(
                LeftRow("a", 1) to null,
                null to shared,
                null to shared,
            )
        }

        @Test
        fun `does not re-add the same matched instance appearing twice`() {
            val shared = RightRow("a", 10)

            val result = Joins.fullJoinOnCondition(listOf(LeftRow("a", 1)), listOf(shared, shared)) { true }

            result shouldBe listOf(
                LeftRow("a", 1) to shared,
                LeftRow("a", 1) to shared,
            )
        }
    }

    @Nested
    inner class `full join with an empty side` {

        @Test
        fun `empty left returns every right row unmatched`() {
            val expected = sampleRight.map<RightRow, Pair<LeftRow?, RightRow?>> { null to it }

            withClue("using key") {
                Joins.fullJoinUsingKey(emptyList<LeftRow>(), sampleRight, Row::key) shouldBe expected
            }
            withClue("on condition") {
                Joins.fullJoinOnCondition(emptyList<LeftRow>(), sampleRight) {
                    left.key == right.key
                } shouldBe expected
            }
        }

        @Test
        fun `empty right returns every left row unmatched`() {
            val expected = sampleLeft.map<LeftRow, Pair<LeftRow?, RightRow?>> { it to null }

            withClue("using key") {
                Joins.fullJoinUsingKey(sampleLeft, emptyList<RightRow>(), Row::key) shouldBe expected
            }
            withClue("on condition") {
                Joins.fullJoinOnCondition(sampleLeft, emptyList<RightRow>()) {
                    left.key == right.key
                } shouldBe expected
            }
        }
    }
}
