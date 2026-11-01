package net.minecraft.world.entity;

import io.netty.buffer.ByteBuf;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public enum Relative {
   X(0),
   Y(1),
   Z(2),
   Y_ROT(3),
   X_ROT(4),
   DELTA_X(5),
   DELTA_Y(6),
   DELTA_Z(7),
   ROTATE_DELTA(8);

   public static final Set<Relative> ALL = Set.of(values());
   public static final Set<Relative> ROTATION = Set.of(X_ROT, Y_ROT);
   public static final Set<Relative> DELTA = Set.of(DELTA_X, DELTA_Y, DELTA_Z, ROTATE_DELTA);
   public static final StreamCodec<ByteBuf, Set<Relative>> SET_STREAM_CODEC = ByteBufCodecs.INT.map(Relative::unpack, Relative::pack);
   private final int bit;

   @SafeVarargs
   public static Set<Relative> union(Set<Relative>... p_371821_) {
      HashSet<Relative> hashset = new HashSet<>();

      for (Set<Relative> set : p_371821_) {
         hashset.addAll(set);
      }

      return hashset;
   }

   public static Set<Relative> rotation(boolean p_436779_, boolean p_436783_) {
      Set<Relative> set = EnumSet.noneOf(Relative.class);
      if (p_436779_) {
         set.add(Y_ROT);
      }

      if (p_436783_) {
         set.add(X_ROT);
      }

      return set;
   }

   public static Set<Relative> position(boolean p_436742_, boolean p_436759_, boolean p_436761_) {
      Set<Relative> set = EnumSet.noneOf(Relative.class);
      if (p_436742_) {
         set.add(X);
      }

      if (p_436759_) {
         set.add(Y);
      }

      if (p_436761_) {
         set.add(Z);
      }

      return set;
   }

   public static Set<Relative> direction(boolean p_436582_, boolean p_436667_, boolean p_436747_) {
      Set<Relative> set = EnumSet.noneOf(Relative.class);
      if (p_436582_) {
         set.add(DELTA_X);
      }

      if (p_436667_) {
         set.add(DELTA_Y);
      }

      if (p_436747_) {
         set.add(DELTA_Z);
      }

      return set;
   }

   private Relative(int p_371238_) {
      this.bit = p_371238_;
   }

   private int getMask() {
      return 1 << this.bit;
   }

   private boolean isSet(int p_371275_) {
      return (p_371275_ & this.getMask()) == this.getMask();
   }

   public static Set<Relative> unpack(int p_371843_) {
      Set<Relative> set = EnumSet.noneOf(Relative.class);

      for (Relative relative : values()) {
         if (relative.isSet(p_371843_)) {
            set.add(relative);
         }
      }

      return set;
   }

   public static int pack(Set<Relative> p_371953_) {
      int i = 0;

      for (Relative relative : p_371953_) {
         i |= relative.getMask();
      }

      return i;
   }
}
