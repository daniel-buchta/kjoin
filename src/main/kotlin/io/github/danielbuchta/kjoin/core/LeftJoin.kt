package io.github.danielbuchta.kjoin.core


internal fun <Left, Right> JoinPlan<Left, Right>.leftJoin(): List<Pair<Left, Right?>> =
    left.indices.flatMap { index -> left[index].associateManyOrNull(matchesFor(index)) }
