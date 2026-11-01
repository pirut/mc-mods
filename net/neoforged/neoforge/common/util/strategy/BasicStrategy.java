package net.neoforged.neoforge.common.util.strategy;

import it.unimi.dsi.fastutil.Hash.Strategy;
import java.util.Objects;

public class BasicStrategy implements Strategy<Object> {
   public static final Strategy<? super Object> BASIC = new BasicStrategy();

   private BasicStrategy() {
   }

   public int hashCode(Object o) {
      return Objects.hashCode(o);
   }

   public boolean equals(Object a, Object b) {
      return Objects.equals(a, b);
   }
}
