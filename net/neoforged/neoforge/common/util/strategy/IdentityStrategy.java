package net.neoforged.neoforge.common.util.strategy;

import it.unimi.dsi.fastutil.Hash.Strategy;

public class IdentityStrategy implements Strategy<Object> {
   public static final Strategy<? super Object> IDENTITY = new IdentityStrategy();

   private IdentityStrategy() {
   }

   public int hashCode(Object o) {
      return System.identityHashCode(o);
   }

   public boolean equals(Object a, Object b) {
      return a == b;
   }
}
