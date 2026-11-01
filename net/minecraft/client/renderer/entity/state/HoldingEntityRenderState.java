package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HoldingEntityRenderState extends LivingEntityRenderState {
   public final ItemStackRenderState heldItem = new ItemStackRenderState();

   public static void extractHoldingEntityRenderState(LivingEntity p_387852_, HoldingEntityRenderState p_386946_, ItemModelResolver p_386944_) {
      p_386944_.updateForLiving(p_386946_.heldItem, p_387852_.getMainHandItem(), ItemDisplayContext.GROUND, p_387852_);
   }
}
