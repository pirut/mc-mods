package net.neoforged.neoforge.client.event;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

public class AddSectionGeometryEvent extends Event {
   private final List<AddSectionGeometryEvent.AdditionalSectionRenderer> additionalRenderers = new ArrayList<>();
   private final BlockPos sectionOrigin;
   private final Level level;

   public AddSectionGeometryEvent(BlockPos sectionOrigin, Level level) {
      this.sectionOrigin = sectionOrigin;
      this.level = level;
   }

   public void addRenderer(AddSectionGeometryEvent.AdditionalSectionRenderer renderer) {
      this.additionalRenderers.add(renderer);
   }

   public List<AddSectionGeometryEvent.AdditionalSectionRenderer> getAdditionalRenderers() {
      return this.additionalRenderers;
   }

   public BlockPos getSectionOrigin() {
      return this.sectionOrigin;
   }

   public Level getLevel() {
      Preconditions.checkState(Minecraft.getInstance().isSameThread());
      return this.level;
   }

   @FunctionalInterface
   public interface AdditionalSectionRenderer {
      void render(AddSectionGeometryEvent.SectionRenderingContext var1);
   }

   public static final class SectionRenderingContext {
      private final Function<ChunkSectionLayer, VertexConsumer> getOrCreateLayer;
      private final BlockAndTintGetter region;
      private final PoseStack poseStack;

      public SectionRenderingContext(Function<ChunkSectionLayer, VertexConsumer> getOrCreateLayer, BlockAndTintGetter region, PoseStack poseStack) {
         this.getOrCreateLayer = getOrCreateLayer;
         this.region = region;
         this.poseStack = poseStack;
      }

      public VertexConsumer getOrCreateChunkBuffer(ChunkSectionLayer layer) {
         return this.getOrCreateLayer.apply(layer);
      }

      public PoseStack getPoseStack() {
         return this.poseStack;
      }

      public BlockAndTintGetter getRegion() {
         return this.region;
      }
   }
}
