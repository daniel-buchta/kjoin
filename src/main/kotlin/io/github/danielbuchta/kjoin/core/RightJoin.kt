package io.github.danielbuchta.kjoin.core


/**
 * A left join over a transposed plan (see [JoinPlan.byKeyTransposed]), with the pairs put back into
 * the caller's left-to-right order. Rows come out ordered by the join's right side, as SQL's
 * RIGHT JOIN does.
 */
internal fun <A, B> JoinPlan<A, B>.transposedLeftJoin(): List<Pair<B?, A>> =
    leftJoin().map { (a, b) -> b to a }
