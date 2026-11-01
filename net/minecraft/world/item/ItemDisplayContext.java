package net.minecraft.world.item;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.neoforged.fml.common.asm.enumextension.ExtensionInfo;
import net.neoforged.fml.common.asm.enumextension.IExtensibleEnum;
import net.neoforged.fml.common.asm.enumextension.IndexedEnum;
import net.neoforged.fml.common.asm.enumextension.NamedEnum;
import net.neoforged.fml.common.asm.enumextension.NetworkedEnum;
import net.neoforged.fml.common.asm.enumextension.ReservedConstructor;
import net.neoforged.fml.common.asm.enumextension.NetworkedEnum.NetworkCheck;
import org.jetbrains.annotations.Nullable;

@NetworkedEnum(NetworkCheck.CLIENTBOUND)
@IndexedEnum
@NamedEnum(1)
public enum ItemDisplayContext implements StringRepresentable, IExtensibleEnum {
   NONE(0, "none"),
   THIRD_PERSON_LEFT_HAND(1, "thirdperson_lefthand"),
   THIRD_PERSON_RIGHT_HAND(2, "thirdperson_righthand"),
   FIRST_PERSON_LEFT_HAND(3, "firstperson_lefthand"),
   FIRST_PERSON_RIGHT_HAND(4, "firstperson_righthand"),
   HEAD(5, "head"),
   GUI(6, "gui"),
   GROUND(7, "ground"),
   FIXED(8, "fixed");

   public static final Codec<ItemDisplayContext> CODEC = StringRepresentable.fromEnum(ItemDisplayContext::values);
   public static final IntFunction<ItemDisplayContext> BY_ID = ByIdMap.continuous(ItemDisplayContext::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
   private final byte id;
   private final String name;
   private final boolean isModded;
   private final Supplier<ItemDisplayContext> fallback;

   @ReservedConstructor
   private ItemDisplayContext(int p_270624_, String p_270851_) {
      this.name = p_270851_;
      this.id = (byte)p_270624_;
      this.isModded = false;
      this.fallback = () -> null;
   }

   private ItemDisplayContext(int id, String name, @Nullable String fallbackName) {
      this.id = (byte)id;
      this.name = name;
      this.isModded = true;
      this.fallback = (Supplier<ItemDisplayContext>)(fallbackName == null ? () -> null : Suppliers.memoize(() -> valueOf(fallbackName)));
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   public byte getId() {
      return this.id;
   }

   public boolean firstPerson() {
      return this == FIRST_PERSON_LEFT_HAND || this == FIRST_PERSON_RIGHT_HAND;
   }

   public boolean isModded() {
      return this.isModded;
   }

   @Nullable
   public ItemDisplayContext fallback() {
      return this.fallback.get();
   }

   public static ExtensionInfo getExtensionInfo() {
      return ExtensionInfo.nonExtended(ItemDisplayContext.class);
   }
}
