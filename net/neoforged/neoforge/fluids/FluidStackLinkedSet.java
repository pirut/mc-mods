package net.neoforged.neoforge.fluids;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public class FluidStackLinkedSet {
   public static final Strategy<? super FluidStack> TYPE_AND_COMPONENTS = new Strategy<FluidStack>() {
      public int hashCode(@Nullable FluidStack stack) {
         return FluidStack.hashFluidAndComponents(stack);
      }

      public boolean equals(@Nullable FluidStack first, @Nullable FluidStack second) {
         return first == second
            || first != null && second != null && first.isEmpty() == second.isEmpty() && FluidStack.isSameFluidSameComponents(first, second);
      }
   };

   public static Set<FluidStack> createTypeAndComponentsSet() {
      return new ObjectLinkedOpenCustomHashSet(TYPE_AND_COMPONENTS);
   }
}
