package io.github.danielbuchta.kjoin.core


internal fun <Left, Right> JoinPlan<Left, Right>.innerJoin(): List<Pair<Left, Right>> =
    left.indices.flatMap { index -> left[index].associateMany(matchesFor(index)) }
