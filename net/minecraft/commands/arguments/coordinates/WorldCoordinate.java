package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;

public class WorldCoordinate {
   private static final char PREFIX_RELATIVE = '~';
   public static final SimpleCommandExceptionType ERROR_EXPECTED_DOUBLE = new SimpleCommandExceptionType(Component.translatable("argument.pos.missing.double"));
   public static final SimpleCommandExceptionType ERROR_EXPECTED_INT = new SimpleCommandExceptionType(Component.translatable("argument.pos.missing.int"));
   private final boolean relative;
   private final double value;

   public WorldCoordinate(boolean p_120864_, double p_120865_) {
      this.relative = p_120864_;
      this.value = p_120865_;
   }

   public double get(double p_120868_) {
      return this.relative ? this.value + p_120868_ : this.value;
   }

   public static WorldCoordinate parseDouble(StringReader p_120872_, boolean p_120873_) throws CommandSyntaxException {
      if (p_120872_.canRead() && p_120872_.peek() == '^') {
         throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(p_120872_);
      } else if (!p_120872_.canRead()) {
         throw ERROR_EXPECTED_DOUBLE.createWithContext(p_120872_);
      } else {
         boolean $$2 = isRelative(p_120872_);
         int $$3 = p_120872_.getCursor();
         double $$4 = p_120872_.canRead() && p_120872_.peek() != ' ' ? p_120872_.readDouble() : 0.0;
         String $$5 = p_120872_.getString().substring($$3, p_120872_.getCursor());
         if ($$2 && $$5.isEmpty()) {
            return new WorldCoordinate(true, 0.0);
         } else {
            if (!$$5.contains(".") && !$$2 && p_120873_) {
               $$4 += 0.5;
            }

            return new WorldCoordinate($$2, $$4);
         }
      }
   }

   public static WorldCoordinate parseInt(StringReader p_120870_) throws CommandSyntaxException {
      if (p_120870_.canRead() && p_120870_.peek() == '^') {
         throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(p_120870_);
      } else if (!p_120870_.canRead()) {
         throw ERROR_EXPECTED_INT.createWithContext(p_120870_);
      } else {
         boolean $$1 = isRelative(p_120870_);
         double $$2;
         if (p_120870_.canRead() && p_120870_.peek() != ' ') {
            $$2 = $$1 ? p_120870_.readDouble() : p_120870_.readInt();
         } else {
            $$2 = 0.0;
         }

         return new WorldCoordinate($$1, $$2);
      }
   }

   public static boolean isRelative(StringReader p_120875_) {
      boolean $$1;
      if (p_120875_.peek() == '~') {
         $$1 = true;
         p_120875_.skip();
      } else {
         $$1 = false;
      }

      return $$1;
   }

   @Override
   public boolean equals(Object p_120877_) {
      if (this == p_120877_) {
         return true;
      } else if (!(p_120877_ instanceof WorldCoordinate $$1)) {
         return false;
      } else {
         return this.relative != $$1.relative ? false : Double.compare($$1.value, this.value) == 0;
      }
   }

   @Override
   public int hashCode() {
      int $$0 = this.relative ? 1 : 0;
      long $$1 = Double.doubleToLongBits(this.value);
      return 31 * $$0 + (int)($$1 ^ $$1 >>> 32);
   }

   public boolean isRelative() {
      return this.relative;
   }
}
