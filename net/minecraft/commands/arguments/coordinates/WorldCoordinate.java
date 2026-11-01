package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;

public record WorldCoordinate(boolean relative, double value) {
   private static final char PREFIX_RELATIVE = '~';
   public static final SimpleCommandExceptionType ERROR_EXPECTED_DOUBLE = new SimpleCommandExceptionType(Component.translatable("argument.pos.missing.double"));
   public static final SimpleCommandExceptionType ERROR_EXPECTED_INT = new SimpleCommandExceptionType(Component.translatable("argument.pos.missing.int"));

   public double get(double coord) {
      return this.relative ? this.value + coord : this.value;
   }

   public static WorldCoordinate parseDouble(StringReader reader, boolean centerCorrect) throws CommandSyntaxException {
      if (reader.canRead() && reader.peek() == '^') {
         throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(reader);
      } else if (!reader.canRead()) {
         throw ERROR_EXPECTED_DOUBLE.createWithContext(reader);
      } else {
         boolean bl = isRelative(reader);
         int i = reader.getCursor();
         double d = reader.canRead() && reader.peek() != ' ' ? reader.readDouble() : 0.0;
         String string = reader.getString().substring(i, reader.getCursor());
         if (bl && string.isEmpty()) {
            return new WorldCoordinate(true, 0.0);
         } else {
            if (!string.contains(".") && !bl && centerCorrect) {
               d += 0.5;
            }

            return new WorldCoordinate(bl, d);
         }
      }
   }

   public static WorldCoordinate parseInt(StringReader reader) throws CommandSyntaxException {
      if (reader.canRead() && reader.peek() == '^') {
         throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(reader);
      } else if (!reader.canRead()) {
         throw ERROR_EXPECTED_INT.createWithContext(reader);
      } else {
         boolean bl = isRelative(reader);
         double d;
         if (reader.canRead() && reader.peek() != ' ') {
            d = bl ? reader.readDouble() : reader.readInt();
         } else {
            d = 0.0;
         }

         return new WorldCoordinate(bl, d);
      }
   }

   public static boolean isRelative(StringReader reader) {
      boolean bl;
      if (reader.peek() == '~') {
         bl = true;
         reader.skip();
      } else {
         bl = false;
      }

      return bl;
   }

   public boolean isRelative() {
      return this.relative;
   }
}
