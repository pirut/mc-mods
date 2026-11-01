package net.minecraft.world.inventory;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EquipmentSlot;
import org.jspecify.annotations.Nullable;

public class SlotRanges {
   private static final List<SlotRange> SLOTS = Util.make(new ArrayList<>(), arrayList -> {
      addSingleSlot(arrayList, "contents", 0);
      addSlotRange(arrayList, "container.", 0, 54);
      addSlotRange(arrayList, "hotbar.", 0, 9);
      addSlotRange(arrayList, "inventory.", 9, 27);
      addSlotRange(arrayList, "enderchest.", 200, 27);
      addSlotRange(arrayList, "villager.", 300, 8);
      addSlotRange(arrayList, "horse.", 500, 15);
      int i = EquipmentSlot.MAINHAND.getIndex(98);
      int j = EquipmentSlot.OFFHAND.getIndex(98);
      addSingleSlot(arrayList, "weapon", i);
      addSingleSlot(arrayList, "weapon.mainhand", i);
      addSingleSlot(arrayList, "weapon.offhand", j);
      addSlots(arrayList, "weapon.*", i, j);
      i = EquipmentSlot.HEAD.getIndex(100);
      j = EquipmentSlot.CHEST.getIndex(100);
      int k = EquipmentSlot.LEGS.getIndex(100);
      int l = EquipmentSlot.FEET.getIndex(100);
      int m = EquipmentSlot.BODY.getIndex(105);
      addSingleSlot(arrayList, "armor.head", i);
      addSingleSlot(arrayList, "armor.chest", j);
      addSingleSlot(arrayList, "armor.legs", k);
      addSingleSlot(arrayList, "armor.feet", l);
      addSingleSlot(arrayList, "armor.body", m);
      addSlots(arrayList, "armor.*", i, j, k, l, m);
      addSingleSlot(arrayList, "saddle", EquipmentSlot.SADDLE.getIndex(106));
      addSingleSlot(arrayList, "horse.chest", 499);
      addSingleSlot(arrayList, "player.cursor", 499);
      addSlotRange(arrayList, "player.crafting.", 500, 4);
   });
   public static final Codec<SlotRange> CODEC = StringRepresentable.fromValues(() -> SLOTS.toArray(SlotRange[]::new));
   private static final Function<String, SlotRange> NAME_LOOKUP = StringRepresentable.createNameLookup(SLOTS.toArray(SlotRange[]::new));

   private static SlotRange create(String name, int value) {
      return SlotRange.of(name, IntLists.singleton(value));
   }

   private static SlotRange create(String name, IntList values) {
      return SlotRange.of(name, IntLists.unmodifiable(values));
   }

   private static SlotRange create(String name, int... values) {
      return SlotRange.of(name, IntList.of(values));
   }

   private static void addSingleSlot(List<SlotRange> list, String name, int value) {
      list.add(create(name, value));
   }

   private static void addSlotRange(List<SlotRange> list, String prefix, int startValue, int size) {
      IntList intList = new IntArrayList(size);

      for (int i = 0; i < size; i++) {
         int j = startValue + i;
         list.add(create(prefix + i, j));
         intList.add(j);
      }

      list.add(create(prefix + "*", intList));
   }

   private static void addSlots(List<SlotRange> list, String name, int... values) {
      list.add(create(name, values));
   }

   @Nullable
   public static SlotRange nameToIds(String name) {
      return NAME_LOOKUP.apply(name);
   }

   public static Stream<String> allNames() {
      return SLOTS.stream().map(StringRepresentable::getSerializedName);
   }

   public static Stream<String> singleSlotNames() {
      return SLOTS.stream().filter(slotRange -> slotRange.size() == 1).map(StringRepresentable::getSerializedName);
   }
}
