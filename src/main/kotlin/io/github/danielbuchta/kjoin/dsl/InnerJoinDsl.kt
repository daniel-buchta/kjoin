package io.github.danielbuchta.kjoin.dsl

import io.github.danielbuchta.kjoin.core.Joins
import io.github.danielbuchta.kjoin.core.MatchedPair

public object InnerJoinDsl {

    @JoinDsl
    public interface FromStep<Base : Any, Left : Base, Right : Base> {
        public infix fun <Key> using(key: Base.() -> Key): InnerJoin.ByKey<Base, Left, Right, Key>
        public infix fun on(
            predicate: MatchedPair<Left, Right>.() -> Boolean
        ): InnerJoin.ByPredicate<Base, Left, Right>
    }
}

/** Keeps only rows that matched on both sides. */
public sealed class InnerJoin<Base : Any, Left : Base, Right : Base> :
    JoinSpec<Left, Right, Pair<Left, Right>>() {

    abstract override fun execute(context: JoinContextData): List<Pair<Left, Right>>

    public class ByKey<Base : Any, Left : Base, Right : Base, Key>(
        public val left: List<Left>,
        public val right: List<Right>,
        public val key: Base.() -> Key
    ) : InnerJoin<Base, Left, Right>() {
        override fun execute(context: JoinContextData): List<Pair<Left, Right>> =
            Joins.innerJoinUsingKey(left, right, context.constraints, key)
    }

    public class ByPredicate<Base : Any, Left : Base, Right : Base>(
        public val left: List<Left>,
        public val right: List<Right>,
        public val predicate: MatchedPair<Left, Right>.() -> Boolean
    ) : InnerJoin<Base, Left, Right>() {
        override fun execute(context: JoinContextData): List<Pair<Left, Right>> =
            Joins.innerJoinOnCondition(left, right, context.constraints, predicate)
    }
}
