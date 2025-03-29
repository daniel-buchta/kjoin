# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project aims to follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html) from 1.0.0 onwards.

## [Unreleased]

### Added

- `README.md` with install instructions, the four join kinds, `using` vs `on`, cardinality
  constraints and the ordering/duplicate/eagerness guarantees. Every example is covered by
  `ReadmeExampleTest` so the docs cannot drift from the API.
- `LICENSE` (Apache-2.0), this changelog and a GitHub Actions build across JDK 17, 21 and 25.
- Sources jar and jacoco coverage in the default build; Dokka API-docs jar under `-Prelease`.
- `CardinalityConstraintViolationException` now exposes `side`, `cardinality`, `violatingRowCount`
  and `totalRowCount`, so callers can react without parsing the message.
- `@DslMarker` (`JoinDsl`) on the DSL receiver scopes.
- Tests for previously uncovered behaviour: `Cardinality` range validation, the four
  `CardinalityConstraints` constants, the `CardinalityConstraints` overload of `Kjoin`,
  `on {}`-based cardinality violations, nullable and non-string join keys, full joins with an empty
  side and the identity semantics of unmatched right rows.

### Changed

- **Full joins are no longer quadratic in the unmatched-row count.** Unmatched right rows were found
  by scanning a list of every matched row with reference equality, which cost O(n·m²) when a
  predicate matched only part of the right side. A predicate matching half of a 2400-row right side
  went from 3306 ms to 135 ms.
- The cardinality check and the join now share one match structure (`JoinPlan`), so a key function
  runs exactly once per element and a predicate exactly once per pair. Previously both were
  evaluated twice, and key joins built their index three times.
- The cardinality check no longer materializes the whole cartesian product to compute one count.
- Violation messages name the side, render the cardinality readably and list the offending rows:
  `Cardinality violation: 3 of 3 left rows match a number of right rows outside 1 (row 0 matched 2,
  row 1 matched 2, row 2 matched 0)` — previously
  `Found 2 items from 1st list violating the Cardinality(from=1, to=1) condition`.
- `groupId` is now `org.kotlinjoins`, matching the package name (was `sk.softec`).
- `-Xexplicit-api` raised from `warning` to `strict`.
- Join specs (`InnerJoin.ByKey` and friends) are plain classes rather than `data class`es; their
  generated `equals`/`hashCode`/`copy` were meaningless on lambda-carrying types.
- `Cardinality.check` is now `accepts`; `CardinalityConstraints.check`/`checkByKey` are now a single
  `validate` taking match counts, which decouples the cardinality layer from rows and predicates.
- Test fixtures renamed from `T.kt`/`TB`/`T1`/`T2` to `Rows.kt`/`Row`/`LeftRow`/`RightRow`, and the
  six duplicated `ArgumentsProvider` datasets replaced by one shared `JoinCases` fixture driving
  every parameterized test (1104 lines of join tests down to 542 plus 164 shared).
- `.editorconfig` now uses LF and a 120-character limit (Kotlin's official convention); a
  `.gitattributes` normalises line endings in the repository.

### Removed

- The `infix fun Cardinality.to` overload, which collided with `kotlin.to`: `ONE to ONE` meant
  either `CardinalityConstraints` or `Pair` depending on the importing file's imports, and importing
  it shadowed `kotlin.to` for `Cardinality` receivers. Use `ONE to ONE` (a `Pair`, always), the
  `CardinalityConstraints` constructor or the named `ONE_TO_ONE`/`ONE_TO_MANY`/`MANY_TO_ONE`/
  `MANY_TO_MANY` constants.
- The vacuous `Base` type parameter from every predicate-based signature. It appeared only in the
  bounds `Left : Base, Right : Base` and was inferred as `Any`, so the advertised common-base-type
  guarantee never applied to `on {}` joins. `Base` is retained where the key is read through it.
- The vacuous `Base` type parameter from `JoinSpec` and `Kjoin.invoke`.
- Dead code: `CardinalityConstraints.swap()`, `Cardinality.check(Collection)`, the
  `PositionedList`/`Position` machinery and its English-ordinal helper, and the internal top-level
  join wrappers that duplicated the `Joins` object and forced fully-qualified self-references.
- Unused pom configuration: `exec-maven-plugin` pointing at a non-existent `MainKt`,
  `maven-failsafe-plugin` with no executions, the redundant Maven Central `<repositories>` block,
  and the `kotlin-test-junit5` dependency (tests standardised on JUnit Jupiter + Kotest).

### Open before 1.0.0

- **Projection step.** `CLAUDE.md` previously documented a `select { ... }` step that does not exist.
  The result type parameter on `JoinSpec`/`Kjoin.invoke` can therefore only ever be `Pair<...>`.
  Decide whether to add projection (making the parameter meaningful) or to drop the parameter.
- **`Sequence` support.** All joins are eager. A lazy variant would help large predicate joins.
- **Binary compatibility.** Add `binary-compatibility-validator` (or an equivalent) before the first
  release so ABI breaks are caught between versions.
- **Publishing.** `url`, `<scm>` and `<developers>` in the pom point at a placeholder GitHub
  repository; correct them before publishing. Maven Central additionally requires proving ownership
  of the `io.github.daniel-buchta` coordinate.
