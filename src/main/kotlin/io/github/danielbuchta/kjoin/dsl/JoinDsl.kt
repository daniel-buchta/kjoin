package io.github.danielbuchta.kjoin.dsl

/**
 * Scope marker for the join DSL. Prevents an outer [JoinContext] from being reached implicitly
 * inside a nested [Kjoin] block, where it would silently join the wrong lists.
 */
@DslMarker
public annotation class JoinDsl
