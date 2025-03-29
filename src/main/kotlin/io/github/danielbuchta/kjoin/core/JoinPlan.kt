package io.github.danielbuchta.kjoin.core

/**
 * The match structure shared by a join and its cardinality check.
 *
 * Building it evaluates the key function exactly once per element, and the predicate exactly once
 * per pair. The check and the join then both read from the plan instead of walking the input again.
 *
 * Right-side match counts are kept by position rather than by value. That is what lets a full join
 * recognize unmatched right rows without comparing rows to each other: two equal but distinct rows
 * are tracked independently, and a row appearing twice is reported twice.
 */
internal class JoinPlan<Left, Right> private constructor(
    val left: List<Left>,
    val right: List<Right>,
    private val matchesByLeftIndex: List<List<Right>>,
    private val matchCountByRightIndex: IntArray,
) {

    fun matchesFor(leftIndex: Int): List<Right> = matchesByLeftIndex[leftIndex]

    /** How many right rows each left row matches, in the order of [left]. */
    fun leftMatchCounts(): List<Int> = matchesByLeftIndex.map { it.size }

    /** How many left rows each right row matches, in the order of [right]. */
    fun rightMatchCounts(): List<Int> = matchCountByRightIndex.asList()

    fun unmatchedRight(): List<Right> =
        right.filterIndexed { index, _ -> matchCountByRightIndex[index] == 0 }

    companion object {

        fun <Left, Right> byPredicate(
            left: List<Left>,
            right: List<Right>,
            predicate: MatchedPair<Left, Right>.() -> Boolean,
        ): JoinPlan<Left, Right> {
            val matches = ArrayList<List<Right>>(left.size)
            val rightCounts = IntArray(right.size)

            for (leftRow in left) {
                val matched = ArrayList<Right>()
                right.forEachIndexed { rightIndex, rightRow ->
                    if (predicate(MatchedPair(leftRow, rightRow))) {
                        matched.add(rightRow)
                        rightCounts[rightIndex]++
                    }
                }
                matches.add(matched)
            }

            return JoinPlan(left, right, matches, rightCounts)
        }

        fun <Base : Any, Left : Base, Right : Base, Key> byKey(
            left: List<Left>,
            right: List<Right>,
            key: Base.() -> Key,
        ): JoinPlan<Left, Right> {
            val leftKeys = left.map { it.key() }
            val rightKeys = right.map { it.key() }

            val rightRowsByKey = HashMap<Key, MutableList<Right>>()
            rightKeys.forEachIndexed { index, rightKey ->
                rightRowsByKey.getOrPut(rightKey) { ArrayList() }.add(right[index])
            }

            val leftRowCountByKey = HashMap<Key, Int>()
            leftKeys.forEach { leftKey -> leftRowCountByKey[leftKey] = (leftRowCountByKey[leftKey] ?: 0) + 1 }

            return JoinPlan(
                left = left,
                right = right,
                matchesByLeftIndex = leftKeys.map { leftKey -> rightRowsByKey[leftKey] ?: emptyList() },
                matchCountByRightIndex = IntArray(right.size) { index -> leftRowCountByKey[rightKeys[index]] ?: 0 },
            )
        }

        /**
         * A plan whose sides are exchanged so that a left join over it yields right-join
         * semantics. See [transposedLeftJoin].
         */
        fun <Base : Any, Left : Base, Right : Base, Key> byKeyTransposed(
            left: List<Left>,
            right: List<Right>,
            key: Base.() -> Key,
        ): JoinPlan<Right, Left> = byKey(left = right, right = left, key = key)

        /** Predicate counterpart of [byKeyTransposed]; the predicate is fed swapped pairs. */
        fun <Left, Right> byPredicateTransposed(
            left: List<Left>,
            right: List<Right>,
            predicate: MatchedPair<Left, Right>.() -> Boolean,
        ): JoinPlan<Right, Left> = byPredicate(right, left) { swap().predicate() }
    }
}
