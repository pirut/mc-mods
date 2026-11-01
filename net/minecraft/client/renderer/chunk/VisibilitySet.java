package net.minecraft.client.renderer.chunk;

import java.util.BitSet;
import java.util.Set;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VisibilitySet {
   private static final int FACINGS = Direction.values().length;
   private final BitSet data = new BitSet(FACINGS * FACINGS);

   public void add(Set<Direction> p_112991_) {
      for (Direction $$1 : p_112991_) {
         for (Direction $$2 : p_112991_) {
            this.set($$1, $$2, true);
         }
      }
   }

   public void set(Direction p_112987_, Direction p_112988_, boolean p_112989_) {
      this.data.set(p_112987_.ordinal() + p_112988_.ordinal() * FACINGS, p_112989_);
      this.data.set(p_112988_.ordinal() + p_112987_.ordinal() * FACINGS, p_112989_);
   }

   public void setAll(boolean p_112993_) {
      this.data.set(0, this.data.size(), p_112993_);
   }

   public boolean visibilityBetween(Direction p_112984_, Direction p_112985_) {
      return this.data.get(p_112984_.ordinal() + p_112985_.ordinal() * FACINGS);
   }

   @Override
   public String toString() {
      StringBuilder $$0 = new StringBuilder();
      $$0.append(' ');

      for (Direction $$1 : Direction.values()) {
         $$0.append(' ').append($$1.toString().toUpperCase().charAt(0));
      }

      $$0.append('\n');

      for (Direction $$2 : Direction.values()) {
         $$0.append($$2.toString().toUpperCase().charAt(0));

         for (Direction $$3 : Direction.values()) {
            if ($$2 == $$3) {
               $$0.append("  ");
            } else {
               boolean $$4 = this.visibilityBetween($$2, $$3);
               $$0.append(' ').append((char)($$4 ? 'Y' : 'n'));
            }
         }

         $$0.append('\n');
      }

      return $$0.toString();
   }
}
