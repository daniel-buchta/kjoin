package io.github.danielbuchta.kjoin.cardinality


/**
 * The cardinality a join must satisfy, read as `left to right`.
 *
 * [left] bounds how many left rows each right row may match, and [right] bounds how many right rows
 * each left row may match. So `ONE to ONE_OR_MORE` means "each right row belongs to exactly one left
 * row, and each left row has at least one right row".
 *
 * A side of `0..*` constrains nothing and is skipped entirely.
 */
public data class CardinalityConstraints(
    val left: Cardinality,
    val right: Cardinality
) {

    /**
     * Validates both directions of a join from its match counts.
     *
     * The counts come from the join's own match structure, so validation costs one pass per side
     * and never re-evaluates the join's key function or predicate.
     *
     * @param rightRowsPerLeftRow how many right rows each left row matches, in left-row order
     * @param leftRowsPerRightRow how many left rows each right row matches, in right-row order
     */
    internal fun validate(rightRowsPerLeftRow: List<Int>, leftRowsPerRightRow: List<Int>) {
        if (!right.isAny()) {
            validateSide(rightRowsPerLeftRow, right, countedSide = JoinSide.LEFT, matchedSide = JoinSide.RIGHT)
        }
        if (!left.isAny()) {
            validateSide(leftRowsPerRightRow, left, countedSide = JoinSide.RIGHT, matchedSide = JoinSide.LEFT)
        }
    }

    public companion object {
        /**
         * 1 to 1
         */
        public val ONE_TO_ONE: CardinalityConstraints =
            CardinalityConstraints(Cardinality.ONE, Cardinality.ONE)

        /**
         * 1 to 1..*
         */
        public val ONE_TO_MANY: CardinalityConstraints =
            CardinalityConstraints(Cardinality.ONE, Cardinality.ONE_OR_MORE)

        /**
         * 1..* to 1
         */
        public val MANY_TO_ONE: CardinalityConstraints =
            CardinalityConstraints(Cardinality.ONE_OR_MORE, Cardinality.ONE)

        /**
         * 1..* to 1..*
         */
        public val MANY_TO_MANY: CardinalityConstraints =
            CardinalityConstraints(Cardinality.ONE_OR_MORE, Cardinality.ONE_OR_MORE)
    }
}
