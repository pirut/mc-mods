package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.network.FriendlyByteBuf;

public class IntegerArgumentInfo implements ArgumentTypeInfo<IntegerArgumentType, IntegerArgumentInfo.Template> {
   public void serializeToNetwork(IntegerArgumentInfo.Template p_235551_, FriendlyByteBuf p_235552_) {
      boolean $$2 = p_235551_.min != Integer.MIN_VALUE;
      boolean $$3 = p_235551_.max != Integer.MAX_VALUE;
      p_235552_.writeByte(ArgumentUtils.createNumberFlags($$2, $$3));
      if ($$2) {
         p_235552_.writeInt(p_235551_.min);
      }

      if ($$3) {
         p_235552_.writeInt(p_235551_.max);
      }
   }

   public IntegerArgumentInfo.Template deserializeFromNetwork(FriendlyByteBuf p_235554_) {
      byte $$1 = p_235554_.readByte();
      int $$2 = ArgumentUtils.numberHasMin($$1) ? p_235554_.readInt() : Integer.MIN_VALUE;
      int $$3 = ArgumentUtils.numberHasMax($$1) ? p_235554_.readInt() : Integer.MAX_VALUE;
      return new IntegerArgumentInfo.Template($$2, $$3);
   }

   public void serializeToJson(IntegerArgumentInfo.Template p_235548_, JsonObject p_235549_) {
      if (p_235548_.min != Integer.MIN_VALUE) {
         p_235549_.addProperty("min", p_235548_.min);
      }

      if (p_235548_.max != Integer.MAX_VALUE) {
         p_235549_.addProperty("max", p_235548_.max);
      }
   }

   public IntegerArgumentInfo.Template unpack(IntegerArgumentType p_235540_) {
      return new IntegerArgumentInfo.Template(p_235540_.getMinimum(), p_235540_.getMaximum());
   }

   public final class Template implements ArgumentTypeInfo.Template<IntegerArgumentType> {
      final int min;
      final int max;

      Template(int p_235562_, int p_235563_) {
         this.min = p_235562_;
         this.max = p_235563_;
      }

      public IntegerArgumentType instantiate(CommandBuildContext p_235566_) {
         return IntegerArgumentType.integer(this.min, this.max);
      }

      @Override
      public ArgumentTypeInfo<IntegerArgumentType, ?> type() {
         return IntegerArgumentInfo.this;
      }
   }
}
