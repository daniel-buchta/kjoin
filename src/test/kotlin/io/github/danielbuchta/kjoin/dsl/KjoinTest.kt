package io.github.danielbuchta.kjoin.dsl

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import io.github.danielbuchta.kjoin.cardinality.Cardinality.Companion.ONE
import io.github.danielbuchta.kjoin.cardinality.Cardinality.Companion.ZERO_OR_ONE
import io.github.danielbuchta.kjoin.cardinality.CardinalityConstraintViolationException
import io.github.danielbuchta.kjoin.cardinality.CardinalityConstraints
import io.github.danielbuchta.kjoin.cardinality.CardinalityConstraints.Companion.MANY_TO_ONE
import io.github.danielbuchta.kjoin.cardinality.CardinalityConstraints.Companion.ONE_TO_MANY
import io.github.danielbuchta.kjoin.cardinality.CardinalityConstraints.Companion.ONE_TO_ONE
import io.github.danielbuchta.kjoin.test.LeftRow
import io.github.danielbuchta.kjoin.test.RightRow

internal class KjoinTest {

    private val l1: List<LeftRow> = listOf(LeftRow("a", 11), LeftRow("b", 12), LeftRow("c", 13))
    private val l2: List<RightRow> = listOf(RightRow("a", 21), RightRow("b", 22), RightRow("c", 23))

    private val expected = listOf(
        LeftRow("a", 11) to RightRow("a", 21),
        LeftRow("b", 12) to RightRow("b", 22),
        LeftRow("c", 13) to RightRow("c", 23),
    )

    @Test
    fun `sample inner joins`() {
        val ij1 = Kjoin { l1 innerJoin l2 using { key } }
        val ij2 = Kjoin { l1 innerJoin l2 on { left.key == right.key } }
        val ij3 = Kjoin(constraints = ONE to ONE) { l1 innerJoin l2 using { key } }
        val ij4 = Kjoin(constraints = ONE to ONE) { l1 innerJoin l2 on { left.key == right.key } }

        listOf(ij1, ij2, ij3, ij4).forEach { it shouldBe expected }
    }

    @Test
    fun `sample left joins`() {
        val lj1 = Kjoin(constraints = ONE to ONE) { l1 leftJoin l2 on { left.key == right.key } }

        listOf(lj1).forEach { it shouldBe expected }
    }

    @Test
    fun `sample right joins`() {
        val rj1 = Kjoin(constraints = ONE to ONE) { l1 rightJoin l2 on { left.key == right.key } }

        listOf(rj1).forEach { it shouldBe expected }
    }

    @Test
    fun `joins using key should reject a cardinality violation`() {
        val duplicated = l2 + RightRow("a", 24)

        shouldThrowExactly<CardinalityConstraintViolationException> {
            Kjoin(constraints = ONE to ONE) { l1 innerJoin duplicated using { key } }
        }
        shouldThrowExactly<CardinalityConstraintViolationException> {
            Kjoin(constraints = ONE to ONE) { l1 leftJoin duplicated using { key } }
        }
        shouldThrowExactly<CardinalityConstraintViolationException> {
            Kjoin(constraints = ONE to ONE) { l1 rightJoin duplicated using { key } }
        }
        shouldThrowExactly<CardinalityConstraintViolationException> {
            Kjoin(constraints = ONE to ONE) { l1 fullJoin duplicated using { key } }
        }
    }

    @Test
    fun `joins on condition should reject a cardinality violation`() {
        val duplicated = l2 + RightRow("a", 24)

        shouldThrowExactly<CardinalityConstraintViolationException> {
            Kjoin(constraints = ONE to ONE) { l1 innerJoin duplicated on { left.key == right.key } }
        }
        shouldThrowExactly<CardinalityConstraintViolationException> {
            Kjoin(constraints = ONE to ONE) { l1 leftJoin duplicated on { left.key == right.key } }
        }
        shouldThrowExactly<CardinalityConstraintViolationException> {
            Kjoin(constraints = ONE to ONE) { l1 rightJoin duplicated on { left.key == right.key } }
        }
        shouldThrowExactly<CardinalityConstraintViolationException> {
            Kjoin(constraints = ONE to ONE) { l1 fullJoin duplicated on { left.key == right.key } }
        }
    }

    /**
     * `ONE to ONE` forbids a left row without a match, so a left join under it can never produce a
     * null right. Relaxing the right side to `ZERO_OR_ONE` is what makes the outer join observable.
     */
    @Test
    fun `left join under ONE to ONE rejects an unmatched left row rather than emitting a null right`() {
        val leftWithExtra = l1 + LeftRow("d", 14)

        shouldThrowExactly<CardinalityConstraintViolationException> {
            Kjoin(constraints = ONE to ONE) { leftWithExtra leftJoin l2 using { key } }
        }
        shouldThrowExactly<CardinalityConstraintViolationException> {
            Kjoin(constraints = ONE to ONE) { leftWithExtra leftJoin l2 on { left.key == right.key } }
        }
    }

    @Test
    fun `left join under ONE to ZERO_OR_ONE allows a null right`() {
        val leftWithExtra = l1 + LeftRow("d", 14)

        val result = Kjoin(constraints = ONE to ZERO_OR_ONE) { leftWithExtra leftJoin l2 using { key } }

        result shouldBe expected + (LeftRow("d", 14) to null)
    }

    @Test
    fun `a nested Kjoin uses its own constraints, not the enclosing ones`() {
        val duplicated = l2 + RightRow("a", 24)

        val result = Kjoin(constraints = ONE to ONE) {
            // The inner block defaults to 0..*; the duplicate right row would violate the
            // enclosing ONE to ONE if constraints leaked inwards.
            Kjoin { l1 innerJoin duplicated using { key } }.size shouldBe 4
            l1 innerJoin l2 using { key }
        }

        result shouldBe expected
    }

    @Test
    fun `constraints may be given as a CardinalityConstraints value`() {
        val viaConstructor = Kjoin(constraints = CardinalityConstraints(ONE, ONE)) {
            l1 innerJoin l2 using { key }
        }
        val viaConstant = Kjoin(constraints = ONE_TO_ONE) { l1 innerJoin l2 using { key } }

        listOf(viaConstructor, viaConstant).forEach { it shouldBe expected }
    }

    @Test
    fun `the CardinalityConstraints overload enforces constraints too`() {
        val duplicated = l2 + RightRow("a", 24)

        shouldThrowExactly<CardinalityConstraintViolationException> {
            Kjoin(constraints = ONE_TO_ONE) { l1 innerJoin duplicated using { key } }
        }
    }

    @Test
    fun `ONE_TO_MANY permits several right rows per left row but no unmatched left row`() {
        val duplicated = l2 + RightRow("a", 24)

        val result = Kjoin(constraints = ONE_TO_MANY) { l1 innerJoin duplicated using { key } }

        result shouldBe listOf(
            LeftRow("a", 11) to RightRow("a", 21),
            LeftRow("a", 11) to RightRow("a", 24),
            LeftRow("b", 12) to RightRow("b", 22),
            LeftRow("c", 13) to RightRow("c", 23),
        )
        shouldThrowExactly<CardinalityConstraintViolationException> {
            Kjoin(constraints = ONE_TO_MANY) { (l1 + LeftRow("d", 14)) innerJoin l2 using { key } }
        }
    }

    @Test
    fun `MANY_TO_ONE permits several left rows per right row but not the reverse`() {
        val duplicatedLeft = l1 + LeftRow("a", 14)

        val result = Kjoin(constraints = MANY_TO_ONE) { duplicatedLeft innerJoin l2 using { key } }

        result shouldBe expected + (LeftRow("a", 14) to RightRow("a", 21))
        shouldThrowExactly<CardinalityConstraintViolationException> {
            Kjoin(constraints = MANY_TO_ONE) { l1 innerJoin (l2 + RightRow("a", 24)) using { key } }
        }
    }
}
