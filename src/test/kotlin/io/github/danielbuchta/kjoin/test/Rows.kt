package io.github.danielbuchta.kjoin.test

/**
 * Row types shared by every join test.
 *
 * [LeftRow] and [RightRow] are deliberately two distinct types over a common [Row] supertype: that
 * is the shape the library's `Base` type parameter exists to support, and keeping them distinct
 * stops a join from accidentally type-checking with its sides swapped.
 */
public abstract class Row {
    public abstract val key: String
    public abstract val value: Int

    /** A nullable secondary key, so nullable-key joins are testable. */
    public abstract val tag: String?

    /** Compact rendering, so assertion failures read `a/1` rather than `LeftRow(key=a, value=1)`. */
    final override fun toString(): String = "$key/$value"
}

public data class LeftRow(
    override val key: String,
    override val value: Int,
    override val tag: String? = null,
) : Row()

public data class RightRow(
    override val key: String,
    override val value: Int,
    override val tag: String? = null,
) : Row()

/** The three-row inputs used by the handwritten predicate tests. */
public val sampleLeft: List<LeftRow> = listOf(LeftRow("a", 1), LeftRow("b", 2), LeftRow("c", 3))
public val sampleRight: List<RightRow> = listOf(RightRow("a", 10), RightRow("b", 20), RightRow("c", 30))
