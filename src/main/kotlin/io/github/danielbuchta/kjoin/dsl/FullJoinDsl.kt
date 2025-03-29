package io.github.danielbuchta.kjoin.dsl

import io.github.danielbuchta.kjoin.core.JoinPlan
import io.github.danielbuchta.kjoin.core.fullJoin
import io.github.danielbuchta.kjoin.core.MatchedPair


public object FullJoinDsl {

    @JoinDsl
    public interface FromStep<Base : Any, Left : Base, Right : Base> {
        public infix fun <Key> using(key: Base.() -> Key): FullJoin.ByKey<Base, Left, Right, Key>
        public infix fun on(
            predicate: MatchedPair<Left, Right>.() -> Boolean
        ): FullJoin.ByPredicate<Base, Left, Right>
    }
}

/**
 * Keeps every row from both sides. Matched rows come first in left-input order, then the unmatched
 * right rows in right-input order.
 */
public sealed class FullJoin<Base : Any, Left : Base, Right : Base> :
    JoinSpec<Left, Right, Pair<Left?, Right?>>() {

    abstract override fun execute(context: JoinContextData): List<Pair<Left?, Right?>>

    public class ByKey<Base : Any, Left : Base, Right : Base, Key>(
        public val left: List<Left>,
        public val right: List<Right>,
        public val key: Base.() -> Key
    ) : FullJoin<Base, Left, Right>() {
        override fun execute(context: JoinContextData): List<Pair<Left?, Right?>> {
            val plan = JoinPlan.byKey<Base, Left, Right, Key>(left, right, key)
            context.constraints.validate(plan.leftMatchCounts(), plan.rightMatchCounts())
            return plan.fullJoin()
        }
    }

    public class ByPredicate<Base : Any, Left : Base, Right : Base>(
        public val left: List<Left>,
        public val right: List<Right>,
        public val predicate: MatchedPair<Left, Right>.() -> Boolean
    ) : FullJoin<Base, Left, Right>() {
        override fun execute(context: JoinContextData): List<Pair<Left?, Right?>> {
            val plan = JoinPlan.byPredicate(left, right, predicate)
            context.constraints.validate(plan.leftMatchCounts(), plan.rightMatchCounts())
            return plan.fullJoin()
        }
    }
}
