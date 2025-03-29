# KJoin

SQL-style joins over in-memory Kotlin collections, with a type-safe DSL and optional cardinality
constraints.

```kotlin
Kjoin { employees leftJoin departments using { departmentId } }
// List<Pair<Employee, Department?>>
```

The point of the library is not the join itself — `groupBy` plus `flatMap` gets you there — but the
things that are tedious to redo each time: consistent outer-join semantics across all four join
kinds, and a declared cardinality that fails loudly when the data does not match your assumption.

## Install

```xml
<dependency>
    <groupId>io.github.daniel-buchta</groupId>
    <artifactId>kjoin</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

Requires Kotlin 2.2+ and JVM 17+.

## The four joins

Both sides must share a supertype that exposes the join key:

```kotlin
interface Staffed { val departmentId: String }

data class Employee(
    val name: String,
    override val departmentId: String,
    val active: Boolean = true,
) : Staffed

data class Department(val title: String, override val departmentId: String) : Staffed
```

| Join        | Result                       | Keeps                                    |
|-------------|------------------------------|------------------------------------------|
| `innerJoin` | `List<Pair<L, R>>`           | only rows matched on both sides          |
| `leftJoin`  | `List<Pair<L, R?>>`          | every left row                           |
| `rightJoin` | `List<Pair<L?, R>>`          | every right row                          |
| `fullJoin`  | `List<Pair<L?, R?>>`         | every row from both sides                |

```kotlin
Kjoin { employees innerJoin departments using { departmentId } }
Kjoin { employees leftJoin  departments using { departmentId } }
Kjoin { employees rightJoin departments using { departmentId } }
Kjoin { employees fullJoin  departments using { departmentId } }
```

## `using` vs `on`

`using { key }` is an equality join. Both sides are indexed by the key once, so it costs
**O(n + m)** plus the size of the result.

```kotlin
Kjoin { employees innerJoin departments using { departmentId } }
```

`on { predicate }` takes an arbitrary condition. The predicate receives a `MatchedPair` as its
receiver, so you can write `left`/`right` directly. A predicate cannot be indexed, so every pair is
evaluated: **O(n * m)**.

```kotlin
Kjoin {
    employees innerJoin departments on { left.departmentId == right.departmentId && left.active }
}
```

Prefer `using` whenever the condition is key equality.

> **Watch out:** inside `on { ... }`, `left` and `right` are members of the `MatchedPair` receiver.
> If a local variable in scope is also called `left` or `right`, the local wins and the code will
> not compile (or worse, compiles against the wrong thing). Qualify with `this.left` / `this.right`
> when that happens.

## Cardinality constraints

Constraints declare what you expect the data to look like, and throw
`CardinalityConstraintViolationException` when it does not. They are read as `left to right`:

- **`left`** bounds how many *left* rows each *right* row may match
- **`right`** bounds how many *right* rows each *left* row may match

```kotlin
import io.github.danielbuchta.kjoin.cardinality.Cardinality.Companion.ONE
import io.github.danielbuchta.kjoin.cardinality.CardinalityConstraints.Companion.ONE_TO_MANY

// every employee has exactly one department, and every department exactly one employee
Kjoin(constraints = ONE to ONE) { employees innerJoin departments using { departmentId } }

// every department has at least one employee, every employee exactly one department
Kjoin(constraints = ONE_TO_MANY) { departments innerJoin employees using { departmentId } }
```

Available cardinalities: `ZERO_OR_MORE` (`0..*`), `ZERO_OR_ONE` (`0..1`), `ONE` (`1`),
`ONE_OR_MORE` (`1..*`), plus the Kotlin-only shorthands `` `0-N` ``, `` `0-1` ``, `` `1` ``,
`` `1-N` ``. Named pairs are also provided: `ONE_TO_ONE`, `ONE_TO_MANY`, `MANY_TO_ONE`,
`MANY_TO_MANY`.

The default is `0..*` on both sides, which validates nothing.

A violation reports which side failed, the constraint, and the offending rows:

```
Cardinality violation: 3 of 3 left rows match a number of right rows outside 1
(row 0 matched 2, row 1 matched 2, row 2 matched 0)
```

The exception also exposes `side`, `cardinality`, `violatingRowCount` and `totalRowCount` so you can
react programmatically instead of parsing the message.

Note that constraints interact with outer joins: a `leftJoin` under `ONE to ONE` can never produce a
null right, because an unmatched left row violates the constraint before the result is built. Use
`ONE to ZERO_OR_ONE` if you want the outer behaviour *and* a uniqueness guarantee.

## Guarantees

- **Ordering.** Rows follow the left input. `rightJoin` follows the right input. A `fullJoin` emits
  matched rows in left-input order, then unmatched right rows in right-input order.
- **Duplicates.** Rows are treated as distinct by position, not by value. Two equal-but-distinct
  unmatched rows both appear in the result; the same instance appearing twice appears twice.
- **Eagerness.** Every join returns a fully materialised `List`. A predicate join over two 10,000-row
  lists evaluates 100,000,000 pairs — use `using` where you can.

## Without the DSL

`Joins` exposes the same operations as plain functions:

```kotlin
import io.github.danielbuchta.kjoin.core.Joins

Joins.leftJoinUsingKey(employees, departments) { departmentId }
Joins.leftJoinOnCondition(employees, departments) { left.departmentId == right.departmentId }
```

These do not validate cardinality — that is a DSL-level concern.

## Kotlin only

The API relies on receiver lambdas, `operator invoke`, and backticked identifiers. It is not
intended to be called from Java.

## Building

```bash
mvn verify                # compile, test, coverage report
mvn -Prelease verify      # additionally builds the Dokka API-docs jar (needs JDK 17 or 21)
```

## License

[Apache License 2.0](LICENSE).
