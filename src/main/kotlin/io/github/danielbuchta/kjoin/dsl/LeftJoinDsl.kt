package io.github.danielbuchta.kjoin.dsl

import io.github.danielbuchta.kjoin.core.JoinPlan
import io.github.danielbuchta.kjoin.core.leftJoin
import io.github.danielbuchta.kjoin.core.MatchedPair

public object LeftJoinDsl {

    @JoinDsl
    public interface FromStep<Base : Any, Left : Base, Right : Base> {
        public infix fun <Key> using(key: Base.() -> Key): LeftJoin.ByKey<Base, Left, Right, Key>
        public infix fun on(
            predicate: MatchedPair<Left, Right>.() -> Boolean
        ): LeftJoin.ByPredicate<Base, Left, Right>
    }
}

/** Keeps every left row, pairing unmatched ones with `null`. */
public sealed class LeftJoin<Base : Any, Left : Base, Right : Base> :
    JoinSpec<Left, Right, Pair<Left, Right?>>() {

    abstract override fun execute(context: JoinContextData): List<Pair<Left, Right?>>

    public class ByKey<Base : Any, Left : Base, Right : Base, Key>(
        public val left: List<Left>,
        public val right: List<Right>,
        public val key: Base.() -> Key
    ) : LeftJoin<Base, Left, Right>() {
        override fun execute(context: JoinContextData): List<Pair<Left, Right?>> {
            val plan = JoinPlan.byKey<Base, Left, Right, Key>(left, right, key)
            context.constraints.validate(plan.leftMatchCounts(), plan.rightMatchCounts())
            return plan.leftJoin()
        }
    }

    public class ByPredicate<Base : Any, Left : Base, Right : Base>(
        public val left: List<Left>,
        public val right: List<Right>,
        public val predicate: MatchedPair<Left, Right>.() -> Boolean
    ) : LeftJoin<Base, Left, Right>() {
        override fun execute(context: JoinContextData): List<Pair<Left, Right?>> {
            val plan = JoinPlan.byPredicate(left, right, predicate)
            context.constraints.validate(plan.leftMatchCounts(), plan.rightMatchCounts())
            return plan.leftJoin()
        }
    }
}
