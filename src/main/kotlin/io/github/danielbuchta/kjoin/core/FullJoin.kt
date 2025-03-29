package io.github.danielbuchta.kjoin.core


internal fun <Left, Right> JoinPlan<Left, Right>.fullJoin(): List<Pair<Left?, Right?>> {
    val matched: List<Pair<Left?, Right?>> = leftJoin()
    val unmatched: List<Pair<Left?, Right?>> = unmatchedRight().map { null to it }
    return matched + unmatched
}
