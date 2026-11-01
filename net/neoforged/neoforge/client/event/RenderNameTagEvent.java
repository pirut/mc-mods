package net.neoforged.neoforge.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RenderNameTagEvent extends EntityEvent {
   private final Component originalContent;
   private final EntityRenderer<?> entityRenderer;
   private final PoseStack poseStack;
   private final MultiBufferSource multiBufferSource;
   private final int packedLight;
   private final float partialTick;
   private Component content;
   private TriState canRender = TriState.DEFAULT;

   @Internal
   public RenderNameTagEvent(
      Entity entity,
      Component content,
      EntityRenderer<?> entityRenderer,
      PoseStack poseStack,
      MultiBufferSource multiBufferSource,
      int packedLight,
      float partialTick
   ) {
      super(entity);
      this.originalContent = content;
      this.setContent(this.originalContent);
      this.entityRenderer = entityRenderer;
      this.poseStack = poseStack;
      this.multiBufferSource = multiBufferSource;
      this.packedLight = packedLight;
      this.partialTick = partialTick;
   }

   public void setCanRender(TriState canRender) {
      this.canRender = canRender;
   }

   public TriState canRender() {
      return this.canRender;
   }

   public void setContent(Component contents) {
      this.content = contents;
   }

   public Component getContent() {
      return this.content;
   }

   public Component getOriginalContent() {
      return this.originalContent;
   }

   public EntityRenderer<?> getEntityRenderer() {
      return this.entityRenderer;
   }

   public PoseStack getPoseStack() {
      return this.poseStack;
   }

   public MultiBufferSource getMultiBufferSource() {
      return this.multiBufferSource;
   }

   public int getPackedLight() {
      return this.packedLight;
   }

   public float getPartialTick() {
      return this.partialTick;
   }
}
