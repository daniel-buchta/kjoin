package io.github.danielbuchta.kjoin.core

import io.github.danielbuchta.kjoin.cardinality.CardinalityConstraints

/**
 * Entry point for core (non-DSL) join operations.
 *
 * All joins return a fully materialised list and preserve input order: rows follow the left input,
 * except the right joins, which follow the right input, and the unmatched right rows of a full
 * join, which are appended in right-input order.
 *
 * Key-based joins index each side once and cost O(n + m) plus the size of the result. Predicate-based
 * joins evaluate the predicate for every pair and therefore cost O(n * m) - the predicate cannot be indexed.
 *
 * `Base` ties both sides to a common supertype and is only meaningful for the key-based joins,
 * where the key is read through it. The predicate-based joins impose no relationship between the
 * two sides.
 *
 * Every join takes optional [CardinalityConstraints]. They default to
 * [CardinalityConstraints.UNCONSTRAINED] (`0..*` on both sides), which validates nothing; otherwise
 * a violation throws `CardinalityConstraintViolationException` before the result is built. The
 * `leftCardinality to rightCardinality` shorthand is DSL-only - pass a `CardinalityConstraints`
 * value or one of its named constants here.
 */
public object Joins {

    public fun <Base : Any, Left : Base, Right : Base, Key> innerJoinUsingKey(
        left: List<Left>,
        right: List<Right>,
        constraints: CardinalityConstraints = CardinalityConstraints.UNCONSTRAINED,
        key: Base.() -> Key,
    ): List<Pair<Left, Right>> = JoinPlan.byKey(left, right, key).validated(constraints).innerJoin()

    public fun <Left, Right> innerJoinOnCondition(
        left: List<Left>,
        right: List<Right>,
        constraints: CardinalityConstraints = CardinalityConstraints.UNCONSTRAINED,
        predicate: MatchedPair<Left, Right>.() -> Boolean,
    ): List<Pair<Left, Right>> = JoinPlan.byPredicate(left, right, predicate).validated(constraints).innerJoin()

    public fun <Base : Any, Left : Base, Right : Base, Key> leftJoinUsingKey(
        left: List<Left>,
        right: List<Right>,
        constraints: CardinalityConstraints = CardinalityConstraints.UNCONSTRAINED,
        key: Base.() -> Key,
    ): List<Pair<Left, Right?>> = JoinPlan.byKey(left, right, key).validated(constraints).leftJoin()

    public fun <Left, Right> leftJoinOnCondition(
        left: List<Left>,
        right: List<Right>,
        constraints: CardinalityConstraints = CardinalityConstraints.UNCONSTRAINED,
        predicate: MatchedPair<Left, Right>.() -> Boolean,
    ): List<Pair<Left, Right?>> = JoinPlan.byPredicate(left, right, predicate).validated(constraints).leftJoin()

    public fun <Base : Any, Left : Base, Right : Base, Key> rightJoinUsingKey(
        left: List<Left>,
        right: List<Right>,
        constraints: CardinalityConstraints = CardinalityConstraints.UNCONSTRAINED,
        key: Base.() -> Key,
    ): List<Pair<Left?, Right>> =
        JoinPlan.byKeyTransposed(left, right, key).validatedTransposed(constraints).transposedLeftJoin()

    public fun <Left, Right> rightJoinOnCondition(
        left: List<Left>,
        right: List<Right>,
        constraints: CardinalityConstraints = CardinalityConstraints.UNCONSTRAINED,
        predicate: MatchedPair<Left, Right>.() -> Boolean,
    ): List<Pair<Left?, Right>> =
        JoinPlan.byPredicateTransposed(left, right, predicate).validatedTransposed(constraints).transposedLeftJoin()

    public fun <Base : Any, Left : Base, Right : Base, Key> fullJoinUsingKey(
        left: List<Left>,
        right: List<Right>,
        constraints: CardinalityConstraints = CardinalityConstraints.UNCONSTRAINED,
        key: Base.() -> Key,
    ): List<Pair<Left?, Right?>> = JoinPlan.byKey(left, right, key).validated(constraints).fullJoin()

    public fun <Left, Right> fullJoinOnCondition(
        left: List<Left>,
        right: List<Right>,
        constraints: CardinalityConstraints = CardinalityConstraints.UNCONSTRAINED,
        predicate: MatchedPair<Left, Right>.() -> Boolean,
    ): List<Pair<Left?, Right?>> = JoinPlan.byPredicate(left, right, predicate).validated(constraints).fullJoin()
}

private fun <Left, Right> JoinPlan<Left, Right>.validated(constraints: CardinalityConstraints) = apply {
    constraints.validate(leftMatchCounts(), rightMatchCounts())
}

/** A transposed plan holds the join's sides swapped, so its counts are passed swapped too. */
private fun <Left, Right> JoinPlan<Left, Right>.validatedTransposed(constraints: CardinalityConstraints) = apply {
    constraints.validate(rightMatchCounts(), leftMatchCounts())
}
