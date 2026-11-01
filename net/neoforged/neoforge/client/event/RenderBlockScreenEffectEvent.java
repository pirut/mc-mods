package net.neoforged.neoforge.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RenderBlockScreenEffectEvent extends Event implements ICancellableEvent {
   private final Player player;
   private final PoseStack poseStack;
   private final RenderBlockScreenEffectEvent.OverlayType overlayType;
   private final BlockState blockState;
   private final BlockPos blockPos;
   private final MaterialSet materials;
   private final MultiBufferSource bufferSource;

   @Internal
   public RenderBlockScreenEffectEvent(
      Player player,
      PoseStack poseStack,
      RenderBlockScreenEffectEvent.OverlayType type,
      BlockState block,
      BlockPos blockPos,
      MaterialSet materials,
      MultiBufferSource bufferSource
   ) {
      this.player = player;
      this.poseStack = poseStack;
      this.overlayType = type;
      this.blockState = block;
      this.blockPos = blockPos;
      this.materials = materials;
      this.bufferSource = bufferSource;
   }

   public Player getPlayer() {
      return this.player;
   }

   public PoseStack getPoseStack() {
      return this.poseStack;
   }

   public RenderBlockScreenEffectEvent.OverlayType getOverlayType() {
      return this.overlayType;
   }

   public BlockState getBlockState() {
      return this.blockState;
   }

   public BlockPos getBlockPos() {
      return this.blockPos;
   }

   public MaterialSet getMaterials() {
      return this.materials;
   }

   public MultiBufferSource getBufferSource() {
      return this.bufferSource;
   }

   public static enum OverlayType {
      FIRE,
      BLOCK,
      WATER;
   }
}
