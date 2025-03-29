package io.github.danielbuchta.kjoin.core

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import io.github.danielbuchta.kjoin.cardinality.Cardinality.Companion.ONE
import io.github.danielbuchta.kjoin.cardinality.CardinalityConstraintViolationException
import io.github.danielbuchta.kjoin.dsl.Kjoin
import io.github.danielbuchta.kjoin.test.LeftRow
import io.github.danielbuchta.kjoin.test.RightRow

/** Key semantics that the main join matrix does not reach: nullable and non-string keys. */
internal class JoinKeyTest {

    private val left = listOf(LeftRow("a", 1, tag = null), LeftRow("b", 2, tag = "t"))
    private val right = listOf(RightRow("c", 10, tag = null), RightRow("d", 20, tag = "t"))

    @Test
    fun `a null key matches other null keys`() {
        val result = Joins.innerJoinUsingKey(left, right) { tag }

        result shouldBe listOf(
            LeftRow("a", 1, tag = null) to RightRow("c", 10, tag = null),
            LeftRow("b", 2, tag = "t") to RightRow("d", 20, tag = "t"),
        )
    }

    @Test
    fun `a nullable key agrees with the equivalent predicate`() {
        val byKey = Joins.leftJoinUsingKey(left, right) { tag }
        val byPredicate = Joins.leftJoinOnCondition(left, right) { this.left.tag == this.right.tag }

        byKey shouldBe byPredicate
    }

    @Test
    fun `a nullable key is validated like any other`() {
        val result = Kjoin(constraints = ONE to ONE) { left innerJoin right using { tag } }

        result.size shouldBe 2

        val unmatchedNull = left + LeftRow("e", 3, tag = "absent")
        shouldThrowExactly<CardinalityConstraintViolationException> {
            Kjoin(constraints = ONE to ONE) { unmatchedNull innerJoin right using { tag } }
        }
    }

    @Test
    fun `a non-string key works`() {
        val result = Joins.innerJoinUsingKey(left, right) { value % 2 }

        result shouldBe listOf(
            LeftRow("b", 2, tag = "t") to RightRow("c", 10, tag = null),
            LeftRow("b", 2, tag = "t") to RightRow("d", 20, tag = "t"),
        )
    }
}
