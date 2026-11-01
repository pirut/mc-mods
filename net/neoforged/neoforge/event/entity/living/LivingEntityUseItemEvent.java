package net.neoforged.neoforge.event.entity.living;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.ICancellableEvent;

public class LivingEntityUseItemEvent extends LivingEvent {
   private final ItemStack item;
   private final InteractionHand hand;
   private int duration;

   private LivingEntityUseItemEvent(LivingEntity entity, ItemStack item, int duration) {
      this(entity, item, entity.getUsedItemHand(), duration);
   }

   private LivingEntityUseItemEvent(LivingEntity entity, ItemStack item, InteractionHand hand, int duration) {
      super(entity);
      this.item = item;
      this.hand = hand;
      this.setDuration(duration);
   }

   public ItemStack getItem() {
      return this.item;
   }

   public int getDuration() {
      return this.duration;
   }

   public void setDuration(int duration) {
      this.duration = duration;
   }

   public InteractionHand getHand() {
      return this.hand;
   }

   public static class Finish extends LivingEntityUseItemEvent {
      private ItemStack result;

      public Finish(LivingEntity entity, ItemStack item, int duration, ItemStack result) {
         super(entity, item, duration);
         this.setResultStack(result);
      }

      public ItemStack getResultStack() {
         return this.result;
      }

      public void setResultStack(ItemStack result) {
         this.result = result;
      }
   }

   public static class Start extends LivingEntityUseItemEvent implements ICancellableEvent {
      @Deprecated(
         since = "1.21.5",
         forRemoval = true
      )
      public Start(LivingEntity entity, ItemStack item, int duration) {
         super(entity, item, entity.getItemInHand(InteractionHand.MAIN_HAND) == item ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, duration);
      }

      public Start(LivingEntity entity, ItemStack item, InteractionHand hand, int duration) {
         super(entity, item, hand, duration);
      }
   }

   public static class Stop extends LivingEntityUseItemEvent implements ICancellableEvent {
      public Stop(LivingEntity entity, ItemStack item, int duration) {
         super(entity, item, duration);
      }
   }

   public static class Tick extends LivingEntityUseItemEvent implements ICancellableEvent {
      public Tick(LivingEntity entity, ItemStack item, int duration) {
         super(entity, item, duration);
      }
   }
}
