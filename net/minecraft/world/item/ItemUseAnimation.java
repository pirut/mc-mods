package net.minecraft.world.item;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
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
public enum ItemUseAnimation implements StringRepresentable, IExtensibleEnum {
   NONE(0, "none"),
   EAT(1, "eat", true),
   DRINK(2, "drink", true),
   BLOCK(3, "block"),
   BOW(4, "bow"),
   TRIDENT(5, "trident"),
   CROSSBOW(6, "crossbow"),
   SPYGLASS(7, "spyglass"),
   TOOT_HORN(8, "toot_horn"),
   BRUSH(9, "brush"),
   BUNDLE(10, "bundle"),
   SPEAR(11, "spear", true);

   private static final IntFunction<ItemUseAnimation> BY_ID = ByIdMap.continuous(ItemUseAnimation::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
   public static final Codec<ItemUseAnimation> CODEC = StringRepresentable.fromEnum(ItemUseAnimation::values);
   public static final StreamCodec<ByteBuf, ItemUseAnimation> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, ItemUseAnimation::getId);
   private final int id;
   private final String name;
   private final boolean customArmTransform;

   private ItemUseAnimation(int p_366747_, String p_366776_) {
      this(p_366747_, p_366776_, false);
   }

   private ItemUseAnimation(int p_455287_, String p_455065_, boolean p_455058_) {
      this.id = p_455287_;
      this.name = p_455065_;
      this.customArmTransform = p_455058_;
   }

   public int getId() {
      return this.id;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   public static ExtensionInfo getExtensionInfo() {
      return ExtensionInfo.nonExtended(ItemUseAnimation.class);
   }

   public boolean hasCustomArmTransform() {
      return this.customArmTransform;
   }
}
