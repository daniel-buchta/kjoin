package io.github.danielbuchta.kjoin.dsl

import io.github.danielbuchta.kjoin.core.JoinPlan
import io.github.danielbuchta.kjoin.core.transposedLeftJoin
import io.github.danielbuchta.kjoin.core.MatchedPair

public object RightJoinDsl {

    @JoinDsl
    public interface FromStep<Base : Any, Left : Base, Right : Base> {
        public infix fun <Key> using(key: Base.() -> Key): RightJoin.ByKey<Base, Left, Right, Key>
        public infix fun on(
            predicate: MatchedPair<Left, Right>.() -> Boolean
        ): RightJoin.ByPredicate<Base, Left, Right>
    }
}

/** Keeps every right row, pairing unmatched ones with `null`. Rows are ordered by the right input. */
public sealed class RightJoin<Base : Any, Left : Base, Right : Base> :
    JoinSpec<Left, Right, Pair<Left?, Right>>() {

    abstract override fun execute(context: JoinContextData): List<Pair<Left?, Right>>

    public class ByKey<Base : Any, Left : Base, Right : Base, Key>(
        public val left: List<Left>,
        public val right: List<Right>,
        public val key: Base.() -> Key
    ) : RightJoin<Base, Left, Right>() {
        override fun execute(context: JoinContextData): List<Pair<Left?, Right>> {
            val plan = JoinPlan.byKeyTransposed<Base, Left, Right, Key>(left, right, key)
            // The plan is transposed, so its right side holds this join's left rows.
            context.constraints.validate(plan.rightMatchCounts(), plan.leftMatchCounts())
            return plan.transposedLeftJoin()
        }
    }

    public class ByPredicate<Base : Any, Left : Base, Right : Base>(
        public val left: List<Left>,
        public val right: List<Right>,
        public val predicate: MatchedPair<Left, Right>.() -> Boolean
    ) : RightJoin<Base, Left, Right>() {
        override fun execute(context: JoinContextData): List<Pair<Left?, Right>> {
            val plan = JoinPlan.byPredicateTransposed(left, right, predicate)
            // The plan is transposed, so its right side holds this join's left rows.
            context.constraints.validate(plan.rightMatchCounts(), plan.leftMatchCounts())
            return plan.transposedLeftJoin()
        }
    }
}
