package io.github.danielbuchta.kjoin.core

/** Pairs this row with each of [other]; yields nothing when [other] is empty. */
internal fun <Left, Right> Left.associateMany(other: List<Right>): List<Pair<Left, Right>> =
    other.map { this to it }

/** Pairs this row with each of [other], or with `null` when [other] is empty - the outer-join tail. */
internal fun <Left, Right> Left.associateManyOrNull(other: List<Right>): List<Pair<Left, Right?>> =
    when {
        other.isEmpty() -> listOf(this to null)
        else -> other.map { this to it }
    }
