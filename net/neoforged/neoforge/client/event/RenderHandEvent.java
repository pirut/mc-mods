package net.neoforged.neoforge.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RenderHandEvent extends Event implements ICancellableEvent {
   private final InteractionHand hand;
   private final PoseStack poseStack;
   private final SubmitNodeCollector submitNodeCollector;
   private final int packedLight;
   private final float partialTick;
   private final float interpolatedPitch;
   private final float swingProgress;
   private final float equipProgress;
   private final ItemStack stack;

   @Internal
   public RenderHandEvent(
      InteractionHand hand,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      int packedLight,
      float partialTick,
      float interpolatedPitch,
      float swingProgress,
      float equipProgress,
      ItemStack stack
   ) {
      this.hand = hand;
      this.poseStack = poseStack;
      this.submitNodeCollector = submitNodeCollector;
      this.packedLight = packedLight;
      this.partialTick = partialTick;
      this.interpolatedPitch = interpolatedPitch;
      this.swingProgress = swingProgress;
      this.equipProgress = equipProgress;
      this.stack = stack;
   }

   public InteractionHand getHand() {
      return this.hand;
   }

   public PoseStack getPoseStack() {
      return this.poseStack;
   }

   public SubmitNodeCollector getSubmitNodeCollector() {
      return this.submitNodeCollector;
   }

   public int getPackedLight() {
      return this.packedLight;
   }

   public float getPartialTick() {
      return this.partialTick;
   }

   public float getInterpolatedPitch() {
      return this.interpolatedPitch;
   }

   public float getSwingProgress() {
      return this.swingProgress;
   }

   public float getEquipProgress() {
      return this.equipProgress;
   }

   public ItemStack getItemStack() {
      return this.stack;
   }
}
