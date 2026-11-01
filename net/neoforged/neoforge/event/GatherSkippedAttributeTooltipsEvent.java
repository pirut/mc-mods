package net.neoforged.neoforge.event;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;
import org.jspecify.annotations.Nullable;

public class GatherSkippedAttributeTooltipsEvent extends Event {
   protected final ItemStack stack;
   protected final AttributeTooltipContext ctx;
   @Nullable
   private Set<Identifier> skippedIds = null;
   @Nullable
   private Set<EquipmentSlotGroup> skippedGroups = null;
   private boolean skipAll = false;

   public GatherSkippedAttributeTooltipsEvent(ItemStack stack, AttributeTooltipContext ctx) {
      this.stack = stack;
      this.ctx = ctx;
   }

   public AttributeTooltipContext getContext() {
      return this.ctx;
   }

   public ItemStack getStack() {
      return this.stack;
   }

   public void skipId(Identifier id) {
      this.getSkippedIds().add(id);
   }

   public void skipGroup(EquipmentSlotGroup group) {
      this.getSkippedGroups().add(group);
   }

   public boolean isSkipped(Identifier id) {
      return this.skipAll || this.skippedIds != null && this.skippedIds.contains(id);
   }

   public boolean isSkipped(EquipmentSlotGroup group) {
      return this.skipAll || this.skippedGroups != null && this.skippedGroups.contains(group);
   }

   public void setSkipAll(boolean skip) {
      this.skipAll = skip;
   }

   public boolean isSkippingAll() {
      return this.skipAll;
   }

   protected Set<Identifier> getSkippedIds() {
      if (this.skippedIds == null) {
         this.skippedIds = new HashSet<>();
      }

      return this.skippedIds;
   }

   protected Set<EquipmentSlotGroup> getSkippedGroups() {
      if (this.skippedGroups == null) {
         this.skippedGroups = EnumSet.noneOf(EquipmentSlotGroup.class);
      }

      return this.skippedGroups;
   }
}
