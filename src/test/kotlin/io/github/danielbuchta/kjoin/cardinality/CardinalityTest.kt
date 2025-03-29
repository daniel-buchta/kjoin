package io.github.danielbuchta.kjoin.cardinality

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class CardinalityTest {

    @Nested
    inner class `rejects invalid ranges` {

        @Test
        fun `from greater than to`() {
            shouldThrowExactly<IllegalArgumentException> { Cardinality(2, 1) }
        }

        @Test
        fun `negative from with an upper bound`() {
            shouldThrowExactly<IllegalArgumentException> { Cardinality(-1, 5) }
        }

        @Test
        fun `negative from without an upper bound`() {
            shouldThrowExactly<IllegalArgumentException> { Cardinality(-1, null) }
        }

        @Test
        fun `negative exact value`() {
            shouldThrowExactly<IllegalArgumentException> { Cardinality(-1) }
        }
    }

    @Nested
    inner class `accepts valid ranges` {

        @Test
        fun `exact value sets both bounds`() {
            Cardinality(3) shouldBe Cardinality(3, 3)
        }

        @Test
        fun `zero is a valid lower bound`() {
            Cardinality(0, 0) shouldBe Cardinality(0)
        }
    }

    @Nested
    inner class `named constants match their range` {

        @Test
        fun `zero or more is unbounded from zero`() {
            Cardinality.ZERO_OR_MORE shouldBe Cardinality(0, null)
        }

        @Test
        fun `zero or one`() {
            Cardinality.ZERO_OR_ONE shouldBe Cardinality(0, 1)
        }

        @Test
        fun `one is exact`() {
            Cardinality.ONE shouldBe Cardinality(1, 1)
        }

        @Test
        fun `one or more is unbounded from one`() {
            Cardinality.ONE_OR_MORE shouldBe Cardinality(1, null)
        }

        @Test
        fun `backticked aliases are the same values as the named constants`() {
            Cardinality.`0-N` shouldBe Cardinality.ZERO_OR_MORE
            Cardinality.`0-1` shouldBe Cardinality.ZERO_OR_ONE
            Cardinality.`1` shouldBe Cardinality.ONE
            Cardinality.`1-N` shouldBe Cardinality.ONE_OR_MORE
        }
    }
}
