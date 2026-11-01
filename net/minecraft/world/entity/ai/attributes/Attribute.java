package net.minecraft.world.entity.ai.attributes;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.common.extensions.IAttributeExtension;

public class Attribute implements IAttributeExtension {
   public static final Codec<Holder<Attribute>> CODEC = BuiltInRegistries.ATTRIBUTE.holderByNameCodec();
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<Attribute>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.ATTRIBUTE);
   private final double defaultValue;
   private boolean syncable;
   private final String descriptionId;
   private Attribute.Sentiment sentiment = Attribute.Sentiment.POSITIVE;
   protected static final TextColor MERGED_RED = TextColor.fromRgb(16331057);
   protected static final TextColor MERGED_BLUE = TextColor.fromRgb(8026873);
   protected static final TextColor MERGED_GRAY = TextColor.fromRgb(13421772);

   protected Attribute(String p_22080_, double p_22081_) {
      this.defaultValue = p_22081_;
      this.descriptionId = p_22080_;
   }

   public double getDefaultValue() {
      return this.defaultValue;
   }

   public boolean isClientSyncable() {
      return this.syncable;
   }

   public Attribute setSyncable(boolean p_22085_) {
      this.syncable = p_22085_;
      return this;
   }

   public Attribute setSentiment(Attribute.Sentiment p_347714_) {
      this.sentiment = p_347714_;
      return this;
   }

   public double sanitizeValue(double p_22083_) {
      return p_22083_;
   }

   public String getDescriptionId() {
      return this.descriptionId;
   }

   public ChatFormatting getStyle(boolean p_347715_) {
      return this.sentiment.getStyle(p_347715_);
   }

   @Override
   public TextColor getMergedStyle(boolean isPositive) {
      return switch (this.sentiment) {
         case POSITIVE -> isPositive ? MERGED_BLUE : MERGED_RED;
         case NEUTRAL -> MERGED_GRAY;
         case NEGATIVE -> isPositive ? MERGED_RED : MERGED_BLUE;
      };
   }

   public static enum Sentiment {
      POSITIVE,
      NEUTRAL,
      NEGATIVE;

      public ChatFormatting getStyle(boolean p_347500_) {
         return switch (this) {
            case POSITIVE -> p_347500_ ? ChatFormatting.BLUE : ChatFormatting.RED;
            case NEUTRAL -> ChatFormatting.GRAY;
            case NEGATIVE -> p_347500_ ? ChatFormatting.RED : ChatFormatting.BLUE;
         };
      }
   }
}
