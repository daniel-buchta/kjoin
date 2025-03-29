package io.github.danielbuchta.kjoin.cardinality

/**
 * How many rows on one side of a join may match a single row on the other side, as an inclusive
 * range with an optional upper bound.
 *
 * Renders as `1`, `0..1`, `1..*` or `2..5`.
 *
 * @property from minimum number of matches, inclusive; must not be negative
 * @property to maximum number of matches, inclusive, or `null` for no upper bound
 * @throws IllegalArgumentException if [from] is negative, or greater than [to]
 */
public data class Cardinality(val from: Int, val to: Int?) {

    init {
        require(from >= 0) { "Cardinality lower bound must not be negative, but was $from" }
        require(to == null || from <= to) {
            "Cardinality lower bound must not exceed its upper bound, but was $from..$to"
        }
    }

    /** An exact cardinality: both bounds are [value]. */
    public constructor(value: Int) : this(value, value)

    internal fun accepts(matchCount: Int): Boolean =
        (from <= matchCount) && (to == null || matchCount <= to)

    /** True when this permits any number of matches (`0..*`) and so constrains nothing. */
    internal fun isAny(): Boolean = from == 0 && to == null

    override fun toString(): String = when {
        to == null -> "$from..*"
        from == to -> "$from"
        else -> "$from..$to"
    }

    public companion object {
        public val ZERO_OR_MORE: Cardinality = Cardinality(0, null)
        public val ZERO_OR_ONE: Cardinality = Cardinality(0, 1)
        public val ONE: Cardinality = Cardinality(1)
        public val ONE_OR_MORE: Cardinality = Cardinality(1, null)

        /**
         * Shorthand aliases for the constants above. Usable from Kotlin only - the backticked names
         * are not valid Java identifiers.
         */
        public val `0-N`: Cardinality = ZERO_OR_MORE
        public val `0-1`: Cardinality = ZERO_OR_ONE
        public val `1`: Cardinality = ONE
        public val `1-N`: Cardinality = ONE_OR_MORE
    }
}
