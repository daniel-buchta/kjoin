# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project aims to follow
[Semantic Versioning](https://semver.org/spec/v2.0.0.html) from 1.0.0 onwards.

## Unreleased

### Changed

- Add cardinality constraints to non-DSL join API.

## [1.0.0] - 2026-08-28

### Added

- KJoin DSL including 4 types of join, `using` vs `on`, cardinality constraints and the ordering/duplicate/eagerness
  guarantees. Every example is covered by `ReadmeExampleTest` so the docs cannot drift from the API.
