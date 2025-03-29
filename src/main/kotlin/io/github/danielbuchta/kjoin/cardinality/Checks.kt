package io.github.danielbuchta.kjoin.cardinality

/** Which side of a join a cardinality constraint or violation refers to. */
public enum class JoinSide {
    LEFT,
    RIGHT;

    override fun toString(): String = name.lowercase()
}

/**
 * Thrown when a join's actual match counts fall outside its declared cardinality.
 *
 * The properties describe the violation so callers can react to it without parsing [message].
 *
 * @property side the side whose rows violated the constraint
 * @property cardinality the constraint that was not met
 * @property violatingRowCount how many rows of [side] violated it
 * @property totalRowCount how many rows [side] had in total
 */
public class CardinalityConstraintViolationException internal constructor(
    public val side: JoinSide,
    public val cardinality: Cardinality,
    public val violatingRowCount: Int,
    public val totalRowCount: Int,
    override val message: String,
) : RuntimeException(message)

private const val MAX_REPORTED_OFFENDERS = 3

/**
 * Throws if any row's match count falls outside [cardinality].
 *
 * @param matchCounts one entry per row of [countedSide], holding how many [matchedSide] rows it matched
 */
internal fun validateSide(
    matchCounts: List<Int>,
    cardinality: Cardinality,
    countedSide: JoinSide,
    matchedSide: JoinSide,
) {
    val offenders = matchCounts.withIndex().filter { !cardinality.accepts(it.value) }
    if (offenders.isEmpty()) return

    val sample = offenders.take(MAX_REPORTED_OFFENDERS)
        .joinToString { "row ${it.index} matched ${it.value}" }
    val elision = if (offenders.size > MAX_REPORTED_OFFENDERS) ", ..." else ""

    throw CardinalityConstraintViolationException(
        side = countedSide,
        cardinality = cardinality,
        violatingRowCount = offenders.size,
        totalRowCount = matchCounts.size,
        message = "Cardinality violation: ${offenders.size} of ${matchCounts.size} $countedSide rows " +
            "match a number of $matchedSide rows outside $cardinality ($sample$elision)",
    )
}
