package net.minecraft.world.level.gamerules;

public interface GameRuleTypeVisitor {
   default <T> void visit(GameRule<T> p_461081_) {
   }

   default void visitBoolean(GameRule<Boolean> p_460848_) {
   }

   default void visitInteger(GameRule<Integer> p_461240_) {
   }
}
