package net.minecraft.world.item;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.neoforged.fml.common.asm.enumextension.ExtensionInfo;
import net.neoforged.fml.common.asm.enumextension.IExtensibleEnum;
import net.neoforged.fml.common.asm.enumextension.IndexedEnum;
import net.neoforged.fml.common.asm.enumextension.NamedEnum;
import net.neoforged.fml.common.asm.enumextension.NetworkedEnum;
import net.neoforged.fml.common.asm.enumextension.NetworkedEnum.NetworkCheck;

@NetworkedEnum(NetworkCheck.BIDIRECTIONAL)
@IndexedEnum
@NamedEnum(1)
public enum Rarity implements StringRepresentable, IExtensibleEnum {
   COMMON(0, "common", ChatFormatting.WHITE),
   UNCOMMON(1, "uncommon", ChatFormatting.YELLOW),
   RARE(2, "rare", ChatFormatting.AQUA),
   EPIC(3, "epic", ChatFormatting.LIGHT_PURPLE);

   public static final Codec<Rarity> CODEC = StringRepresentable.fromValues(Rarity::values);
   public static final IntFunction<Rarity> BY_ID = ByIdMap.continuous(p_335877_ -> p_335877_.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
   public static final StreamCodec<ByteBuf, Rarity> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, p_335484_ -> p_335484_.id);
   private final int id;
   private final String name;
   private final ChatFormatting color;
   private final UnaryOperator<Style> styleModifier;

   private Rarity(int p_336096_, String p_335642_, ChatFormatting p_43028_) {
      this.id = p_336096_;
      this.name = p_335642_;
      this.color = p_43028_;
      this.styleModifier = style -> style.withColor(p_43028_);
   }

   private Rarity(int id, String name, UnaryOperator<Style> styleModifier) {
      this.id = id;
      this.name = name;
      this.color = ChatFormatting.BLACK;
      this.styleModifier = styleModifier;
   }

   @Deprecated
   public ChatFormatting color() {
      return this.color;
   }

   public UnaryOperator<Style> getStyleModifier() {
      return this.styleModifier;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   public static ExtensionInfo getExtensionInfo() {
      return ExtensionInfo.nonExtended(Rarity.class);
   }
}
