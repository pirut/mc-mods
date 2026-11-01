package net.minecraft.world.entity;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

public enum EquipmentSlot implements StringRepresentable {
   MAINHAND(EquipmentSlot.Type.HAND, 0, 0, "mainhand"),
   OFFHAND(EquipmentSlot.Type.HAND, 1, 5, "offhand"),
   FEET(EquipmentSlot.Type.HUMANOID_ARMOR, 0, 1, 1, "feet"),
   LEGS(EquipmentSlot.Type.HUMANOID_ARMOR, 1, 1, 2, "legs"),
   CHEST(EquipmentSlot.Type.HUMANOID_ARMOR, 2, 1, 3, "chest"),
   HEAD(EquipmentSlot.Type.HUMANOID_ARMOR, 3, 1, 4, "head"),
   BODY(EquipmentSlot.Type.ANIMAL_ARMOR, 0, 1, 6, "body"),
   SADDLE(EquipmentSlot.Type.SADDLE, 0, 1, 7, "saddle");

   public static final int NO_COUNT_LIMIT = 0;
   public static final List<EquipmentSlot> VALUES = List.of(values());
   public static final IntFunction<EquipmentSlot> BY_ID = ByIdMap.continuous(p_371874_ -> p_371874_.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
   public static final StringRepresentable.EnumCodec<EquipmentSlot> CODEC = StringRepresentable.fromEnum(EquipmentSlot::values);
   public static final StreamCodec<ByteBuf, EquipmentSlot> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, p_371207_ -> p_371207_.id);
   private final EquipmentSlot.Type type;
   private final int index;
   public final int countLimit;
   private final int id;
   private final String name;

   private EquipmentSlot(EquipmentSlot.Type p_348560_, int p_348539_, int p_348468_, int p_348612_, String p_348519_) {
      this.type = p_348560_;
      this.index = p_348539_;
      this.countLimit = p_348468_;
      this.id = p_348612_;
      this.name = p_348519_;
   }

   private EquipmentSlot(EquipmentSlot.Type p_20739_, int p_20740_, int p_20741_, String p_20742_) {
      this(p_20739_, p_20740_, 0, p_20741_, p_20742_);
   }

   public EquipmentSlot.Type getType() {
      return this.type;
   }

   public int getIndex() {
      return this.index;
   }

   public int getIndex(int p_147069_) {
      return p_147069_ + this.index;
   }

   public ItemStack limit(ItemStack p_348466_) {
      return this.countLimit > 0 ? p_348466_.split(this.countLimit) : p_348466_;
   }

   public int getId() {
      return this.id;
   }

   public int getFilterBit(int p_371931_) {
      return this.id + p_371931_;
   }

   public String getName() {
      return this.name;
   }

   public boolean isArmor() {
      return this.type == EquipmentSlot.Type.HUMANOID_ARMOR || this.type == EquipmentSlot.Type.ANIMAL_ARMOR;
   }

   @Override
   public String getSerializedName() {
      return this.name;
   }

   public boolean canIncreaseExperience() {
      return this.type != EquipmentSlot.Type.SADDLE;
   }

   public static EquipmentSlot byName(String p_20748_) {
      EquipmentSlot equipmentslot = CODEC.byName(p_20748_);
      if (equipmentslot != null) {
         return equipmentslot;
      } else {
         throw new IllegalArgumentException("Invalid slot '" + p_20748_ + "'");
      }
   }

   public static enum Type {
      HAND,
      HUMANOID_ARMOR,
      ANIMAL_ARMOR,
      SADDLE;
   }
}
