package io.github.danielbuchta.kjoin.cardinality

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import io.github.danielbuchta.kjoin.cardinality.Cardinality.Companion.`0-1`
import io.github.danielbuchta.kjoin.cardinality.Cardinality.Companion.`0-N`
import io.github.danielbuchta.kjoin.cardinality.Cardinality.Companion.`1`
import io.github.danielbuchta.kjoin.cardinality.Cardinality.Companion.`1-N`
import io.github.danielbuchta.kjoin.core.JoinPlan

internal class CardinalityConstraintsTest {

    @Test
    fun `positive 0-N to 0-N`() {
        val left = listOf(Item("k1"), Item("k1"), Item("k2"))
        val right = listOf(Item("k1"), Item("k2"), Item("k2"))
        val constraints = `0-N` to `0-N`

        testCheckSuccess(constraints, left, right)
    }

    @Test
    fun `positive 0-N to 0-1`() {
        val left = listOf(Item("k1"), Item("k2"), Item("k3"), Item("k3"))
        val right = listOf(Item("k1"), Item("k3"))
        val constraints = `0-N` to `0-1`

        testCheckSuccess(constraints, left, right)
    }

    @Test
    fun `negative 0-N to 0-1`() {
        val left = listOf(Item("k"), Item("k"), Item("k"))
        val right = listOf(Item("k"), Item("k"))
        val constraints = `0-N` to `0-1`

        testCheckFailure(constraints, left, right)
    }

    @Test
    fun `positive 0-N to 1`() {
        val left = listOf(Item("k"), Item("k"), Item("k"))
        val right = listOf(Item("k"))
        val constraints = `0-N` to `1`

        testCheckSuccess(constraints, left, right)
    }

    @Test
    fun `negative 0-N to 1`() {
        val left = listOf(Item("k"), Item("k"), Item("k"))
        val right = emptyList<Item>()
        val constraints = `0-N` to `1`

        testCheckFailure(constraints, left, right)
    }

    @Test
    fun `negative 0-N to 1 with multiple matches`() {
        val left = listOf(Item("k"), Item("k"), Item("k"))
        val right = listOf(Item("k"), Item("k"))
        val constraints = `0-N` to `1`

        testCheckFailure(constraints, left, right)
    }

    @Test
    fun `positive 0-N to 1-N`() {
        val left = listOf(Item("k"), Item("k"), Item("k"))
        val right = listOf(Item("k"), Item("k"))
        val constraints = `0-N` to `1-N`

        testCheckSuccess(constraints, left, right)
    }

    @Test
    fun `negative 0-N to 1-N`() {
        val left = listOf(Item("k"), Item("k"), Item("k"))
        val right = emptyList<Item>()
        val constraints = `0-N` to `1-N`

        testCheckFailure(constraints, left, right)
    }

    @Test
    fun `positive 0-1 to 0-N`() {
        val left = listOf(Item("k"))
        val right = listOf(Item("k"), Item("k"), Item("k"))
        val constraints = `0-1` to `0-N`

        testCheckSuccess(constraints, left, right)
    }

    @Test
    fun `negative 0-1 to 0-N`() {
        val left = listOf(Item("k"), Item("k"))
        val right = listOf(Item("k"), Item("k"), Item("k"))
        val constraints = `0-1` to `0-N`

        testCheckFailure(constraints, left, right)
    }

    @Test
    fun `positive 0-1 to 0-1`() {
        val left = listOf(Item("k"))
        val right = listOf(Item("k"))
        val constraints = `0-1` to `0-1`

        testCheckSuccess(constraints, left, right)
    }

    @Test
    fun `negative 0-1 to 0-1`() {
        val left = listOf(Item("k"), Item("k"))
        val right = listOf(Item("k"), Item("k"))
        val constraints = `0-1` to `0-1`

        testCheckFailure(constraints, left, right)
    }

    @Test
    fun `positive 0-1 to 1`() {
        val left = listOf(Item("k"))
        val right = listOf(Item("k"))
        val constraints = `0-1` to `1`

        testCheckSuccess(constraints, left, right)
    }

    @Test
    fun `negative 0-1 to 1`() {
        val left = listOf(Item("k"), Item("k"))
        val right = emptyList<Item>()
        val constraints = `0-1` to `1`

        testCheckFailure(constraints, left, right)
    }

    @Test
    fun `negative 0-1 to 1 with multiple matches`() {
        val left = listOf(Item("k"))
        val right = listOf(Item("k"), Item("k"))
        val constraints = `0-1` to `1`

        testCheckFailure(constraints, left, right)
    }

    @Test
    fun `positive 0-1 to 1-N`() {
        val left = listOf(Item("k"))
        val right = listOf(Item("k"), Item("k"))
        val constraints = `0-1` to `1-N`

        testCheckSuccess(constraints, left, right)
    }

    @Test
    fun `negative 0-1 to 1-N`() {
        val left = listOf(Item("k"), Item("k"))
        val right = emptyList<Item>()
        val constraints = `0-1` to `1-N`

        testCheckFailure(constraints, left, right)
    }

    @Test
    fun `positive 1 to 0-N`() {
        val left = listOf(Item("k"))
        val right = listOf(Item("k"), Item("k"), Item("k"))
        val constraints = `1` to `0-N`

        testCheckSuccess(constraints, left, right)
    }

    @Test
    fun `negative 1 to 0-N`() {
        val left = emptyList<Item>()
        val right = listOf(Item("k"), Item("k"), Item("k"))
        val constraints = `1` to `0-N`

        testCheckFailure(constraints, left, right)
    }

    @Test
    fun `positive 1 to 0-1`() {
        val left = listOf(Item("k"))
        val right = listOf(Item("k"))
        val constraints = `1` to `0-1`

        testCheckSuccess(constraints, left, right)
    }

    @Test
    fun `negative 1 to 0-1`() {
        val left = emptyList<Item>()
        val right = listOf(Item("k"), Item("k"))
        val constraints = `1` to `0-1`

        testCheckFailure(constraints, left, right)
    }

    @Test
    fun `negative 1 to 0-1 with multiple matches`() {
        val left = listOf(Item("k"), Item("k"))
        val right = listOf(Item("k"))
        val constraints = `1` to `0-1`

        testCheckFailure(constraints, left, right)
    }

    @Test
    fun `positive 1 to 1`() {
        val left = listOf(Item("k"))
        val right = listOf(Item("k"))
        val constraints = `1` to `1`

        testCheckSuccess(constraints, left, right)
    }

    @Test
    fun `negative 1 to 1`() {
        val left = listOf(Item("k"))
        val right = emptyList<Item>()
        val constraints = `1` to `1`

        testCheckFailure(constraints, left, right)
    }

    @Test
    fun `negative 1 to 1 with multiple matches`() {
        val left = listOf(Item("k"))
        val right = listOf(Item("k"), Item("k"))
        val constraints = `1` to `1`

        testCheckFailure(constraints, left, right)
    }

    @Test
    fun `positive 1 to 1-N`() {
        val left = listOf(Item("k"))
        val right = listOf(Item("k"), Item("k"))
        val constraints = `1` to `1-N`

        testCheckSuccess(constraints, left, right)
    }

    @Test
    fun `negative 1 to 1-N`() {
        val left = listOf(Item("k"))
        val right = emptyList<Item>()
        val constraints = `1` to `1-N`

        testCheckFailure(constraints, left, right)
    }

    @Test
    fun `negative 1 to 1-N with multiple matches`() {
        val left = listOf(Item("k"), Item("k"))
        val right = listOf(Item("k"))
        val constraints = `1` to `1-N`

        testCheckFailure(constraints, left, right)
    }

    @Test
    fun `positive 1-N to 0-N`() {
        val left = listOf(Item("k"), Item("k"))
        val right = listOf(Item("k"), Item("k"), Item("k"))
        val constraints = `1-N` to `0-N`

        testCheckSuccess(constraints, left, right)
    }

    @Test
    fun `negative 1-N to 0-N`() {
        val left = emptyList<Item>()
        val right = listOf(Item("k"), Item("k"), Item("k"))
        val constraints = `1-N` to `0-N`

        testCheckFailure(constraints, left, right)
    }

    @Test
    fun `positive 1-N to 0-1`() {
        val left = listOf(Item("k"), Item("k"))
        val right = listOf(Item("k"))
        val constraints = `1-N` to `0-1`

        testCheckSuccess(constraints, left, right)
    }

    @Test
    fun `negative 1-N to 0-1`() {
        val left = emptyList<Item>()
        val right = listOf(Item("k"), Item("k"))
        val constraints = `1-N` to `0-1`

        testCheckFailure(constraints, left, right)
    }

    @Test
    fun `positive 1-N to 1`() {
        val left = listOf(Item("k"), Item("k"))
        val right = listOf(Item("k"))
        val constraints = `1-N` to `1`

        testCheckSuccess(constraints, left, right)
    }

    @Test
    fun `negative 1-N to 1`() {
        val left = emptyList<Item>()
        val right = listOf(Item("k"))
        val constraints = `1-N` to `1`

        testCheckFailure(constraints, left, right)
    }

    @Test
    fun `negative 1-N to 1 with multiple matches`() {
        val left = listOf(Item("k"))
        val right = listOf(Item("k"), Item("k"))
        val constraints = `1-N` to `1`

        testCheckFailure(constraints, left, right)
    }

    @Test
    fun `positive 1-N to 1-N`() {
        val left = listOf(Item("k"), Item("k"))
        val right = listOf(Item("k"), Item("k"))
        val constraints = `1-N` to `1-N`

        testCheckSuccess(constraints, left, right)
    }

    @Test
    fun `negative 1-N to 1-N`() {
        val left = listOf(Item("k"))
        val right = emptyList<Item>()
        val constraints = `1-N` to `1-N`

        testCheckFailure(constraints, left, right)
    }

    @Test
    fun `a violation names the side, the constraint and the offending rows`() {
        val left = listOf(Item("k1"), Item("k1"), Item("k2"))
        val right = listOf(Item("k1"), Item("k1"))

        val failure = shouldThrowExactly<CardinalityConstraintViolationException> {
            validateByKey(`0-N` to `1`, left, right)
        }

        failure.side shouldBe JoinSide.LEFT
        failure.cardinality shouldBe `1`
        failure.violatingRowCount shouldBe 3
        failure.totalRowCount shouldBe 3
        failure.message shouldBe
            "Cardinality violation: 3 of 3 left rows match a number of right rows outside 1 " +
            "(row 0 matched 2, row 1 matched 2, row 2 matched 0)"
    }

    @Test
    fun `a violation on the right side is reported as such`() {
        val left = listOf(Item("k"), Item("k"))
        val right = listOf(Item("k"))

        val failure = shouldThrowExactly<CardinalityConstraintViolationException> {
            validateByKey(`1` to `0-N`, left, right)
        }

        failure.side shouldBe JoinSide.RIGHT
        failure.violatingRowCount shouldBe 1
        failure.totalRowCount shouldBe 1
        failure.message shouldBe
            "Cardinality violation: 1 of 1 right rows match a number of left rows outside 1 " +
            "(row 0 matched 2)"
    }

    @Test
    fun `a violation lists at most three offending rows`() {
        val left = List(4) { Item("k") }
        val right = listOf(Item("k"), Item("k"))

        val failure = shouldThrowExactly<CardinalityConstraintViolationException> {
            validateByKey(`0-N` to `1`, left, right)
        }

        failure.violatingRowCount shouldBe 4
        failure.message shouldBe
            "Cardinality violation: 4 of 4 left rows match a number of right rows outside 1 " +
            "(row 0 matched 2, row 1 matched 2, row 2 matched 2, ...)"
    }

    @Test
    fun `key based validation reports the same violation as the predicate based one`() {
        val left = listOf(Item("k1"), Item("k1"), Item("k2"))
        val right = listOf(Item("k1"), Item("k1"))
        val constraints = `0-N` to `1`

        val fromPredicate = shouldThrowExactly<CardinalityConstraintViolationException> {
            validateByPredicate(constraints, left, right)
        }
        val fromKey = shouldThrowExactly<CardinalityConstraintViolationException> {
            validateByKey(constraints, left, right)
        }

        fromKey.message shouldBe fromPredicate.message
    }

    @Test
    fun `key based validation evaluates the key function exactly once per element`() {
        val left = listOf(Item("k"), Item("k"))
        val right = listOf(Item("k"), Item("k"))
        var calls = 0
        val countingKey: Item.() -> String = { calls++; key }

        val plan = JoinPlan.byKey<Item, Item, Item, String>(left, right, countingKey)
        CardinalityConstraints(`1-N`, `1-N`).validate(plan.leftMatchCounts(), plan.rightMatchCounts())

        // The plan indexes each side once and validation reads counts out of it, so the key
        // function runs once per element for the check and the subsequent join combined.
        calls shouldBe left.size + right.size
    }

    private fun testCheckSuccess(
        constraints: Pair<Cardinality, Cardinality>,
        left: List<Item>,
        right: List<Item>
    ) {
        shouldNotThrowAny { validateByPredicate(constraints, left, right) }
        shouldNotThrowAny { validateByKey(constraints, left, right) }
    }

    private fun testCheckFailure(
        constraints: Pair<Cardinality, Cardinality>,
        left: List<Item>,
        right: List<Item>
    ) {
        shouldThrowExactly<CardinalityConstraintViolationException> {
            validateByPredicate(constraints, left, right)
        }
        shouldThrowExactly<CardinalityConstraintViolationException> {
            validateByKey(constraints, left, right)
        }
    }

    private fun validateByPredicate(
        constraints: Pair<Cardinality, Cardinality>,
        left: List<Item>,
        right: List<Item>
    ) {
        val plan = JoinPlan.byPredicate(left, right) { this.left.key == this.right.key }
        constraints.asConstraints().validate(plan.leftMatchCounts(), plan.rightMatchCounts())
    }

    private fun validateByKey(
        constraints: Pair<Cardinality, Cardinality>,
        left: List<Item>,
        right: List<Item>
    ) {
        val plan = JoinPlan.byKey<Item, Item, Item, String>(left, right) { key }
        constraints.asConstraints().validate(plan.leftMatchCounts(), plan.rightMatchCounts())
    }

    private fun Pair<Cardinality, Cardinality>.asConstraints() = CardinalityConstraints(first, second)

    private data class Item(val key: String)
}
