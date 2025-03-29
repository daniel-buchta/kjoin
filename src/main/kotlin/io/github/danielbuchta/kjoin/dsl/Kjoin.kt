package io.github.danielbuchta.kjoin.dsl

import io.github.danielbuchta.kjoin.cardinality.Cardinality
import io.github.danielbuchta.kjoin.cardinality.Cardinality.Companion.ZERO_OR_MORE
import io.github.danielbuchta.kjoin.cardinality.CardinalityConstraints
import io.github.danielbuchta.kjoin.core.MatchedPair

/**
 * Entry point for the join DSL:
 *
 * ```
 * Kjoin { employees leftJoin departments using { departmentId } }
 * Kjoin(constraints = ONE to ONE) { employees innerJoin badges on { left.id == right.holderId } }
 * ```
 *
 * Constraints default to `0..*` on both sides, which validates nothing.
 */
public object Kjoin {

    public operator fun <Left, Right, Result> invoke(
        constraints: CardinalityConstraints = CardinalityConstraints(ZERO_OR_MORE, ZERO_OR_MORE),
        join: JoinContext.() -> JoinSpec<Left, Right, Result>
    ): List<Result> = with(JoinContextData(constraints)) {
        join().execute(this)
    }

    /** Convenience overload for `leftCardinality to rightCardinality`. */
    public operator fun <Left, Right, Result> invoke(
        constraints: Pair<Cardinality, Cardinality>,
        join: JoinContext.() -> JoinSpec<Left, Right, Result>
    ): List<Result> = Kjoin(
        constraints = CardinalityConstraints(left = constraints.first, right = constraints.second),
        join = join
    )
}

/**
 * Receiver scope of a [Kjoin] block. `Base` ties both sides to a common supertype so that
 * `using { ... }` can read the join key through it; it carries no constraint for `on { ... }`.
 */
@JoinDsl
public sealed interface JoinContext {

    public infix fun <Base : Any, Left : Base, Right : Base> List<Left>.innerJoin(
        right: List<Right>
    ): InnerJoinDsl.FromStep<Base, Left, Right> = object : InnerJoinDsl.FromStep<Base, Left, Right> {
        override fun <K> using(key: Base.() -> K) = InnerJoin.ByKey(this@innerJoin, right, key)
        override fun on(predicate: MatchedPair<Left, Right>.() -> Boolean) =
            InnerJoin.ByPredicate(this@innerJoin, right, predicate)
    }

    public infix fun <Base : Any, Left : Base, Right : Base> List<Left>.leftJoin(
        right: List<Right>
    ): LeftJoinDsl.FromStep<Base, Left, Right> = object : LeftJoinDsl.FromStep<Base, Left, Right> {
        override fun <K> using(key: Base.() -> K) = LeftJoin.ByKey(this@leftJoin, right, key)
        override fun on(predicate: MatchedPair<Left, Right>.() -> Boolean) =
            LeftJoin.ByPredicate(this@leftJoin, right, predicate)
    }

    public infix fun <Base : Any, Left : Base, Right : Base> List<Left>.rightJoin(
        right: List<Right>
    ): RightJoinDsl.FromStep<Base, Left, Right> = object : RightJoinDsl.FromStep<Base, Left, Right> {
        override fun <K> using(key: Base.() -> K) = RightJoin.ByKey(this@rightJoin, right, key)
        override fun on(predicate: MatchedPair<Left, Right>.() -> Boolean) =
            RightJoin.ByPredicate(this@rightJoin, right, predicate)
    }

    public infix fun <Base : Any, Left : Base, Right : Base> List<Left>.fullJoin(
        right: List<Right>
    ): FullJoinDsl.FromStep<Base, Left, Right> = object : FullJoinDsl.FromStep<Base, Left, Right> {
        override fun <K> using(key: Base.() -> K) = FullJoin.ByKey(this@fullJoin, right, key)
        override fun on(predicate: MatchedPair<Left, Right>.() -> Boolean) =
            FullJoin.ByPredicate(this@fullJoin, right, predicate)
    }
}

internal data class JoinContextData(
    val constraints: CardinalityConstraints,
) : JoinContext

public sealed class JoinSpec<Left, Right, R> {
    internal abstract fun execute(context: JoinContextData): List<R>
}
