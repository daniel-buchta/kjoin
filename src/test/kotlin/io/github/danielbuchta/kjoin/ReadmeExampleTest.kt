package io.github.danielbuchta.kjoin

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import io.github.danielbuchta.kjoin.cardinality.Cardinality.Companion.ONE
import io.github.danielbuchta.kjoin.cardinality.CardinalityConstraintViolationException
import io.github.danielbuchta.kjoin.cardinality.CardinalityConstraints.Companion.ONE_TO_MANY
import io.github.danielbuchta.kjoin.cardinality.JoinSide
import io.github.danielbuchta.kjoin.core.Joins
import io.github.danielbuchta.kjoin.dsl.Kjoin

/**
 * Every example in README.md, kept compiling and passing so the documentation cannot drift away
 * from the API.
 */
internal class ReadmeExampleTest {

    private interface Staffed {
        val departmentId: String
    }

    private data class Employee(
        val name: String,
        override val departmentId: String,
        val active: Boolean = true,
    ) : Staffed

    private data class Department(val title: String, override val departmentId: String) : Staffed

    private val employees = listOf(
        Employee("ann", "eng"),
        Employee("bob", "eng", active = false),
        Employee("cid", "ops"),
    )
    private val departments = listOf(
        Department("Engineering", "eng"),
        Department("Operations", "ops"),
    )

    @Test
    fun `the four joins`() {
        Kjoin { employees innerJoin departments using { departmentId } }.size shouldBe 3
        Kjoin { employees leftJoin departments using { departmentId } }.size shouldBe 3
        Kjoin { employees rightJoin departments using { departmentId } }.size shouldBe 3
        Kjoin { employees fullJoin departments using { departmentId } }.size shouldBe 3
    }

    @Test
    fun `using versus on`() {
        val byKey = Kjoin { employees innerJoin departments using { departmentId } }

        val byPredicate = Kjoin {
            employees innerJoin departments on { left.departmentId == right.departmentId && left.active }
        }

        byKey.size shouldBe 3
        byPredicate.map { it.first.name } shouldBe listOf("ann", "cid")
    }

    @Test
    fun `cardinality constraints`() {
        // every department has at least one employee, every employee exactly one department
        val result = Kjoin(constraints = ONE_TO_MANY) {
            departments innerJoin employees using { departmentId }
        }

        result.size shouldBe 3
    }

    @Test
    fun `a violation reports the side, constraint and offending rows`() {
        val duplicated = departments + Department("Engineering (EU)", "eng")

        val failure = shouldThrowExactly<CardinalityConstraintViolationException> {
            Kjoin(constraints = ONE to ONE) { employees innerJoin duplicated using { departmentId } }
        }

        failure.side shouldBe JoinSide.LEFT
        failure.cardinality shouldBe ONE
        failure.message shouldContain "Cardinality violation:"
        failure.message shouldContain "outside 1"
    }

    @Test
    fun `a left join under ONE to ONE cannot produce a null right`() {
        val withUnmatched = employees + Employee("dee", "sales")

        shouldThrowExactly<CardinalityConstraintViolationException> {
            Kjoin(constraints = ONE to ONE) { withUnmatched leftJoin departments using { departmentId } }
        }
    }

    @Test
    fun `without the dsl`() {
        val byKey = Joins.leftJoinUsingKey(employees, departments) { departmentId }
        val byPredicate = Joins.leftJoinOnCondition(employees, departments) {
            left.departmentId == right.departmentId
        }

        byKey shouldBe byPredicate
    }
}
