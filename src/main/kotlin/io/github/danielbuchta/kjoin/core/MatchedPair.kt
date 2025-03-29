package io.github.danielbuchta.kjoin.core

/**
 * The candidate pair handed to an `on { ... }` predicate as its receiver, so a condition can be
 * written as `left.departmentId == right.id`.
 *
 * One instance is allocated per evaluated pair, which is why predicate joins cost more than key-based ones on large inputs.
 */
public data class MatchedPair<Left, Right>(val left: Left, val right: Right) {
    internal fun swap() = MatchedPair(right, left)
}
