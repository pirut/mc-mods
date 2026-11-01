package net.neoforged.neoforge.event.entity.living;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.ICancellableEvent;

public class LivingUseTotemEvent extends LivingEvent implements ICancellableEvent {
   private final DamageSource source;
   private final ItemStack totem;
   private final InteractionHand hand;

   public LivingUseTotemEvent(LivingEntity entity, DamageSource source, ItemStack totem, InteractionHand hand) {
      super(entity);
      this.source = source;
      this.totem = totem;
      this.hand = hand;
   }

   public DamageSource getSource() {
      return this.source;
   }

   public ItemStack getTotem() {
      return this.totem;
   }

   public InteractionHand getHandHolding() {
      return this.hand;
   }
}
