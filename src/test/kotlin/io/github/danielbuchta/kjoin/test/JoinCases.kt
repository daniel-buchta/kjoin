package io.github.danielbuchta.kjoin.test

/**
 * One join input pair together with the expected result of all four join kinds.
 *
 * Keeping the four expectations side by side is the point: it makes the differences between the
 * join kinds visible in one place, and it means a new input case is added once rather than six
 * times.
 */
public class JoinCase(
    private val name: String,
    public val left: List<LeftRow>,
    public val right: List<RightRow>,
    public val inner: List<Pair<LeftRow, RightRow>>,
    public val leftOuter: List<Pair<LeftRow, RightRow?>>,
    public val rightOuter: List<Pair<LeftRow?, RightRow>>,
    public val full: List<Pair<LeftRow?, RightRow?>>,
) {
    override fun toString(): String = name
}

/** Feeds the `@MethodSource` of every parameterized join test. */
public fun joinCases(): List<JoinCase> = listOf(
    uniqueMatchingRows(),
    extraRowsOnBothSides(),
    duplicatesOnBothSides(),
    emptyInputs(),
    noMatchingRows(),
    duplicatesAndExtraRows(),
)

private fun uniqueMatchingRows(): JoinCase {
    val matched = listOf(
        LeftRow("a", 1) to RightRow("a", 10),
        LeftRow("b", 2) to RightRow("b", 20),
        LeftRow("c", 3) to RightRow("c", 30),
    )
    return JoinCase(
        name = "unique matching rows",
        left = listOf(LeftRow("a", 1), LeftRow("b", 2), LeftRow("c", 3)),
        right = listOf(RightRow("a", 10), RightRow("b", 20), RightRow("c", 30)),
        inner = matched,
        leftOuter = matched,
        rightOuter = matched,
        full = matched,
    )
}

private fun extraRowsOnBothSides(): JoinCase {
    val matched = listOf(
        LeftRow("a", 1) to RightRow("a", 10),
        LeftRow("b", 2) to RightRow("b", 20),
        LeftRow("c", 3) to RightRow("c", 30),
    )
    val unmatchedLeft = LeftRow("only-left", 4)
    val unmatchedRight = RightRow("only-right", 40)
    return JoinCase(
        name = "extra rows on both sides",
        left = listOf(LeftRow("a", 1), LeftRow("b", 2), LeftRow("c", 3), unmatchedLeft),
        right = listOf(RightRow("a", 10), RightRow("b", 20), RightRow("c", 30), unmatchedRight),
        inner = matched,
        leftOuter = matched + (unmatchedLeft to null),
        rightOuter = matched + (null to unmatchedRight),
        full = matched + (unmatchedLeft to null) + (null to unmatchedRight),
    )
}

private fun duplicatesOnBothSides(): JoinCase {
    val matched = listOf(
        LeftRow("a", 1) to RightRow("a", 10),
        LeftRow("a", 1) to RightRow("a", 11),
        LeftRow("b", 2) to RightRow("b", 20),
        LeftRow("c", 3) to RightRow("c", 30),
        LeftRow("c", 4) to RightRow("c", 30),
    )
    return JoinCase(
        name = "duplicates on both sides",
        left = listOf(LeftRow("a", 1), LeftRow("b", 2), LeftRow("c", 3), LeftRow("c", 4)),
        right = listOf(RightRow("a", 10), RightRow("a", 11), RightRow("b", 20), RightRow("c", 30)),
        inner = matched,
        leftOuter = matched,
        rightOuter = matched,
        full = matched,
    )
}

private fun emptyInputs(): JoinCase = JoinCase(
    name = "empty inputs",
    left = emptyList(),
    right = emptyList(),
    inner = emptyList(),
    leftOuter = emptyList(),
    rightOuter = emptyList(),
    full = emptyList(),
)

private fun noMatchingRows(): JoinCase {
    val left = listOf(LeftRow("a", 1), LeftRow("b", 2))
    val right = listOf(RightRow("c", 20), RightRow("d", 30))
    return JoinCase(
        name = "no matching rows",
        left = left,
        right = right,
        inner = emptyList(),
        leftOuter = left.map { it to null },
        rightOuter = right.map { null to it },
        full = left.map<LeftRow, Pair<LeftRow?, RightRow?>> { it to null } + right.map { null to it },
    )
}

private fun duplicatesAndExtraRows(): JoinCase {
    val matched = listOf(
        LeftRow("a", 1) to RightRow("a", 10),
        LeftRow("a", 1) to RightRow("a", 11),
        LeftRow("b", 2) to RightRow("b", 20),
        LeftRow("c", 3) to RightRow("c", 30),
        LeftRow("c", 4) to RightRow("c", 30),
    )
    val unmatchedLeft = listOf(LeftRow("d", 5), LeftRow("d", 6))
    val unmatchedRight = listOf(RightRow("e", 50), RightRow("e", 51))
    return JoinCase(
        name = "duplicates and extra rows",
        left = listOf(LeftRow("a", 1), LeftRow("b", 2), LeftRow("c", 3), LeftRow("c", 4)) + unmatchedLeft,
        right = listOf(RightRow("a", 10), RightRow("a", 11), RightRow("b", 20), RightRow("c", 30)) + unmatchedRight,
        inner = matched,
        leftOuter = matched + unmatchedLeft.map { it to null },
        rightOuter = matched + unmatchedRight.map { null to it },
        full = matched.map<Pair<LeftRow, RightRow>, Pair<LeftRow?, RightRow?>> { it } +
            unmatchedLeft.map { it to null } +
            unmatchedRight.map { null to it },
    )
}
