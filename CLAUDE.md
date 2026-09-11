# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Test Commands

```bash
# Compile the project
mvn compile

# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=KjoinTest

# Compile, test and produce the coverage report
mvn verify

# Additionally build the Dokka API-docs jar (needs JDK 17 or 21, not 25)
mvn -Prelease verify
```

`-Xexplicit-api=strict` is on for main sources: every public declaration needs an explicit
visibility and return type, or the build fails.

## Changelog

Any change under `src/main/kotlin/**` gets a `CHANGELOG.md` entry in the same change, not a follow-up.

- **Where.** A bullet under `## Unreleased`, in the matching Keep a Changelog category — `Added`, `Changed`,
  `Deprecated`, `Removed`, `Fixed`, `Security`. Add the `### Category` heading if it is not there yet, and
  keep the categories in that order. Never edit a released version's section.
- **Voice.** One bullet describing the user-visible effect, not the implementation:
  `Add cardinality constraints to non-DSL join API.` A second sentence is fine when the change carries a
  guarantee worth stating, as the 1.0.0 entry does.
- **When to skip.** Test-only changes, doc-only changes (`README.md`, `CLAUDE.md`) and build/CI changes
  (`pom.xml`, `.github/workflows/**`) need no entry.
- **Release flow.** At release, `## Unreleased` is renamed to `## [x.y.z] - YYYY-MM-DD` and a fresh empty
  `## Unreleased` goes back on top. The version bump itself is not a changelog entry.

## Project Architecture

A Kotlin library for SQL-style joins on in-memory collections, with a type-safe DSL and optional
cardinality constraints. Three layers, each in its own package.

### 1. DSL layer — `src/main/kotlin/io/github/danielbuchta/kjoin/dsl/`

- `Kjoin.kt` — the `Kjoin { }` entry point, the `JoinContext` receiver scope (which supplies the
  `innerJoin`/`leftJoin`/`rightJoin`/`fullJoin` infix functions as default implementations), and the
  `JoinSpec` base class.
- `InnerJoinDsl.kt`, `LeftJoinDsl.kt`, `RightJoinDsl.kt`, `FullJoinDsl.kt` — one `FromStep` interface
  and one sealed spec per join kind, each with a `ByKey` and a `ByPredicate` variant.
- `JoinDsl.kt` — the `@DslMarker` annotation.

A spec's `execute` is a one-line delegation to the matching `Joins` function, passing the context's
constraints through. The DSL adds the fluent syntax and the constraint defaults; it holds no join
logic of its own.

Usage: `Kjoin { left innerJoin right using { key } }` or `... on { left.k == right.k }`.

### 2. Core layer — `src/main/kotlin/io/github/danielbuchta/kjoin/core/`

- `JoinPlan.kt` — **the central abstraction.** Built once per join, it holds the per-left-row match
  lists and the per-right-row match counts. Both the cardinality check and the join read from it, so
  a key function runs exactly once per element and a predicate exactly once per pair. Right-side
  counts are kept **by position**, which is what lets a full join find unmatched right rows without
  comparing rows to each other.
- `InnerJoin.kt`, `LeftJoin.kt`, `RightJoin.kt`, `FullJoin.kt` — one extension function each,
  turning a plan into a result. Right joins use a *transposed* plan (`byKeyTransposed` /
  `byPredicateTransposed`) plus `transposedLeftJoin`.
- `Joins.kt` — the public non-DSL entry point, and the single implementation of every join: build a
  plan, validate it, run the join. Each function takes optional `CardinalityConstraints`, defaulting
  to `UNCONSTRAINED`. The private `validated` / `validatedTransposed` helpers are the only place the
  constraint orientation is decided. The `left to right` pair shorthand is DSL-only.
- `MatchedPair.kt` — the receiver handed to `on { }` predicates.
- `AssociateMany.kt` — pairing helpers; `associateManyOrNull` supplies the outer-join `null`.

### 3. Cardinality layer — `src/main/kotlin/io/github/danielbuchta/kjoin/cardinality/`

- `Cardinality.kt` — a range (`from`, optional `to`) with the named constants and their backticked
  Kotlin-only aliases.
- `CardinalityConstraints.kt` — a `left`/`right` pair. `validate` takes **match counts, not rows**,
  which keeps this layer independent of the core layer's types.
- `Checks.kt` — `JoinSide`, the violation exception (with structured fields), and `validateSide`.

## Key Design Points

- **Constraint orientation.** `left to right`: `left` bounds how many left rows each right row may
  match; `right` bounds how many right rows each left row may match. A side of `0..*` is skipped.
- **Right joins are transposed left joins.** When wiring a right join, the plan's sides are swapped,
  so the two count arguments passed to `validate` must be swapped too. This is the easiest thing to
  get wrong in this codebase; it now lives only in `Joins.kt`'s `validatedTransposed`, and
  `JoinsCardinalityTest.constraints keep their orientation in every join` is what catches a swap —
  the DSL tests do not.
- **`Base` is only meaningful for `using`.** It exists so the key can be read through a common
  supertype. Predicate-based signatures deliberately do not declare it — it would be inferred as
  `Any` and guarantee nothing.
- **Ordering is part of the contract.** Left input order, except right joins (right input order) and
  a full join's unmatched right rows (appended in right input order). Tests depend on it.
- **Rows are distinct by position, not by value.** Two equal-but-distinct unmatched rows both appear.
- **Everything is eager.** Predicate joins are O(n·m) with no lazy path.

## Tests

- `src/test/kotlin/io/github/danielbuchta/kjoin/test/Rows.kt` — `Row`/`LeftRow`/`RightRow` fixtures and the shared
  `sampleLeft`/`sampleRight` inputs. `LeftRow` and `RightRow` are deliberately distinct types over a
  common base, mirroring what `Base` exists for.
- `src/test/kotlin/io/github/danielbuchta/kjoin/test/JoinCases.kt` — one dataset holding all six input cases with
  the expected result of all four join kinds. Fed to every parameterized join test via
  `@MethodSource("io.github.danielbuchta.kjoin.test.JoinCasesKt#joinCases")`. **Add a new input case here once,
  not in eight files.**
- `ReadmeExampleTest` covers every snippet in `README.md`; update it when the README changes.

Note that surefire reports parameterized invocations under the `@Nested` class line and shows
`Tests run: 0` for the outer class. The tests do run — check the surefire XML if in doubt.

## Code Style

- Use comments sparingly. Only comment complex code.
- Kotlin official style, LF line endings, 120-character lines (see `.editorconfig`).
