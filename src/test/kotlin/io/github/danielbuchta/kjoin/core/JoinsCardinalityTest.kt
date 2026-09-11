package io.github.danielbuchta.kjoin.core

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import io.github.danielbuchta.kjoin.cardinality.Cardinality.Companion.ONE
import io.github.danielbuchta.kjoin.cardinality.Cardinality.Companion.ZERO_OR_ONE
import io.github.danielbuchta.kjoin.cardinality.CardinalityConstraintViolationException
import io.github.danielbuchta.kjoin.cardinality.CardinalityConstraints
import io.github.danielbuchta.kjoin.cardinality.CardinalityConstraints.Companion.MANY_TO_ONE
import io.github.danielbuchta.kjoin.cardinality.CardinalityConstraints.Companion.ONE_TO_MANY
import io.github.danielbuchta.kjoin.cardinality.CardinalityConstraints.Companion.ONE_TO_ONE
import io.github.danielbuchta.kjoin.cardinality.CardinalityConstraints.Companion.UNCONSTRAINED
import io.github.danielbuchta.kjoin.cardinality.JoinSide
import io.github.danielbuchta.kjoin.test.LeftRow
import io.github.danielbuchta.kjoin.test.RightRow

/**
 * Cardinality constraints on the non-DSL entry point. The DSL's coverage lives in `dsl/KjoinTest`;
 * what matters here is that every [Joins] function accepts constraints, that omitting them
 * validates nothing, and that the right joins - which validate a transposed plan - keep the
 * constraint's orientation.
 */
internal class JoinsCardinalityTest {

    private val left = listOf(LeftRow("a", 11), LeftRow("b", 12), LeftRow("c", 13))
    private val right = listOf(RightRow("a", 21), RightRow("b", 22), RightRow("c", 23))

    /** Gives left row `a` two right matches. */
    private val duplicatedRight = right + RightRow("a", 24)

    /** Gives right row `a` two left matches. */
    private val duplicatedLeft = left + LeftRow("a", 14)

    @Test
    fun `every join rejects a cardinality violation`() {
        val joins: List<Pair<String, () -> Any>> = listOf(
            "innerJoinUsingKey" to { Joins.innerJoinUsingKey(left, duplicatedRight, ONE_TO_ONE) { key } },
            "leftJoinUsingKey" to { Joins.leftJoinUsingKey(left, duplicatedRight, ONE_TO_ONE) { key } },
            "rightJoinUsingKey" to { Joins.rightJoinUsingKey(left, duplicatedRight, ONE_TO_ONE) { key } },
            "fullJoinUsingKey" to { Joins.fullJoinUsingKey(left, duplicatedRight, ONE_TO_ONE) { key } },
            "innerJoinOnCondition" to { Joins.innerJoinOnCondition(left, duplicatedRight, ONE_TO_ONE) { this.left.key == this.right.key } },
            "leftJoinOnCondition" to { Joins.leftJoinOnCondition(left, duplicatedRight, ONE_TO_ONE) { this.left.key == this.right.key } },
            "rightJoinOnCondition" to { Joins.rightJoinOnCondition(left, duplicatedRight, ONE_TO_ONE) { this.left.key == this.right.key } },
            "fullJoinOnCondition" to { Joins.fullJoinOnCondition(left, duplicatedRight, ONE_TO_ONE) { this.left.key == this.right.key } },
        )

        joins.forEach { (name, join) ->
            withClue(name) {
                val failure = shouldThrowExactly<CardinalityConstraintViolationException>(join)

                // Left row "a" matches two right rows, so it is the left side that offends.
                failure.side shouldBe JoinSide.LEFT
                failure.cardinality shouldBe ONE
            }
        }
    }

    @Test
    fun `omitting the constraints validates nothing`() {
        val joins: List<Pair<String, () -> Any>> = listOf(
            "innerJoinUsingKey" to { Joins.innerJoinUsingKey(left, duplicatedRight) { key } },
            "leftJoinUsingKey" to { Joins.leftJoinUsingKey(left, duplicatedRight) { key } },
            "rightJoinUsingKey" to { Joins.rightJoinUsingKey(left, duplicatedRight) { key } },
            "fullJoinUsingKey" to { Joins.fullJoinUsingKey(left, duplicatedRight) { key } },
            "innerJoinOnCondition" to { Joins.innerJoinOnCondition(left, duplicatedRight) { this.left.key == this.right.key } },
            "leftJoinOnCondition" to { Joins.leftJoinOnCondition(left, duplicatedRight) { this.left.key == this.right.key } },
            "rightJoinOnCondition" to { Joins.rightJoinOnCondition(left, duplicatedRight) { this.left.key == this.right.key } },
            "fullJoinOnCondition" to { Joins.fullJoinOnCondition(left, duplicatedRight) { this.left.key == this.right.key } },
        )

        // The same data that ONE_TO_ONE rejects above must pass when no constraints are given.
        joins.forEach { (name, join) -> withClue(name) { shouldNotThrowAny(join) } }
    }

    @Test
    fun `the default is UNCONSTRAINED`() {
        Joins.innerJoinUsingKey(left, duplicatedRight, UNCONSTRAINED) { key } shouldBe
            Joins.innerJoinUsingKey(left, duplicatedRight) { key }
    }

    /**
     * `ONE_TO_MANY` and `MANY_TO_ONE` accept opposite shapes of data, so a join that mixed up the
     * two sides of the constraint - as a right join validating a transposed plan easily could -
     * would flip both of these assertions.
     */
    @Test
    fun `constraints keep their orientation in every join`() {
        withClue("several left rows per right row is MANY_TO_ONE, not ONE_TO_MANY") {
            shouldNotThrowAny { Joins.innerJoinUsingKey(duplicatedLeft, right, MANY_TO_ONE) { key } }
            shouldNotThrowAny { Joins.leftJoinUsingKey(duplicatedLeft, right, MANY_TO_ONE) { key } }
            shouldNotThrowAny { Joins.rightJoinUsingKey(duplicatedLeft, right, MANY_TO_ONE) { key } }
            shouldNotThrowAny { Joins.fullJoinUsingKey(duplicatedLeft, right, MANY_TO_ONE) { key } }

            listOf<() -> Any>(
                { Joins.innerJoinUsingKey(duplicatedLeft, right, ONE_TO_MANY) { key } },
                { Joins.leftJoinUsingKey(duplicatedLeft, right, ONE_TO_MANY) { key } },
                { Joins.rightJoinUsingKey(duplicatedLeft, right, ONE_TO_MANY) { key } },
                { Joins.fullJoinUsingKey(duplicatedLeft, right, ONE_TO_MANY) { key } },
            ).forEach { shouldThrowExactly<CardinalityConstraintViolationException>(it).side shouldBe JoinSide.RIGHT }
        }

        withClue("several right rows per left row is ONE_TO_MANY, not MANY_TO_ONE") {
            shouldNotThrowAny { Joins.innerJoinUsingKey(left, duplicatedRight, ONE_TO_MANY) { key } }
            shouldNotThrowAny { Joins.leftJoinUsingKey(left, duplicatedRight, ONE_TO_MANY) { key } }
            shouldNotThrowAny { Joins.rightJoinUsingKey(left, duplicatedRight, ONE_TO_MANY) { key } }
            shouldNotThrowAny { Joins.fullJoinUsingKey(left, duplicatedRight, ONE_TO_MANY) { key } }

            listOf<() -> Any>(
                { Joins.innerJoinUsingKey(left, duplicatedRight, MANY_TO_ONE) { key } },
                { Joins.leftJoinUsingKey(left, duplicatedRight, MANY_TO_ONE) { key } },
                { Joins.rightJoinUsingKey(left, duplicatedRight, MANY_TO_ONE) { key } },
                { Joins.fullJoinUsingKey(left, duplicatedRight, MANY_TO_ONE) { key } },
            ).forEach { shouldThrowExactly<CardinalityConstraintViolationException>(it).side shouldBe JoinSide.LEFT }
        }
    }

    @Test
    fun `a validated right join still returns the right join result`() {
        val byKey = Joins.rightJoinUsingKey(duplicatedLeft, right, MANY_TO_ONE) { key }
        val byPredicate = Joins.rightJoinOnCondition(duplicatedLeft, right, MANY_TO_ONE) { this.left.key == this.right.key }

        byKey shouldBe listOf(
            LeftRow("a", 11) to RightRow("a", 21),
            LeftRow("a", 14) to RightRow("a", 21),
            LeftRow("b", 12) to RightRow("b", 22),
            LeftRow("c", 13) to RightRow("c", 23),
        )
        byPredicate shouldBe byKey
    }

    /**
     * Validation reads the plan, not the result, so an unmatched left row is still emitted with a
     * null right when the constraints allow it.
     */
    @Test
    fun `a left join under ONE to ZERO_OR_ONE keeps its null right`() {
        val leftWithUnmatched = left + LeftRow("d", 14)
        val constraints = CardinalityConstraints(ONE, ZERO_OR_ONE)

        val result = Joins.leftJoinUsingKey(leftWithUnmatched, right, constraints) { key }

        result shouldBe listOf(
            LeftRow("a", 11) to RightRow("a", 21),
            LeftRow("b", 12) to RightRow("b", 22),
            LeftRow("c", 13) to RightRow("c", 23),
            LeftRow("d", 14) to null,
        )
    }
}
