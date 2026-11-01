package net.neoforged.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(
   value = {Dist.CLIENT},
   modid = "neoforge"
)
public final class BlockEntityRenderBoundsDebugRenderer {
   private static boolean enabled = false;

   @SubscribeEvent
   public static void onRenderLevelStage(RenderLevelStageEvent event) {
      if (enabled && event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
         LevelRenderer levelRenderer = Minecraft.getInstance().levelRenderer;
         PoseStack poseStack = event.getPoseStack();
         Vec3 camera = event.getCamera().getPosition();
         VertexConsumer consumer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.lines());
         levelRenderer.iterateVisibleBlockEntities(be -> drawRenderBoundingBox(poseStack, consumer, camera, be));
      }
   }

   private static void drawRenderBoundingBox(PoseStack poseStack, VertexConsumer consumer, Vec3 camera, BlockEntity be) {
      BlockEntityRenderer<BlockEntity> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be);
      if (renderer != null) {
         BlockPos pos = be.getBlockPos();
         AABB aabb = renderer.getRenderBoundingBox(be).move(-pos.getX(), -pos.getY(), -pos.getZ());
         Vec3 offset = Vec3.atLowerCornerOf(pos).subtract(camera);
         poseStack.pushPose();
         poseStack.translate(offset.x, offset.y, offset.z);
         LevelRenderer.renderLineBox(poseStack, consumer, aabb, 1.0F, 0.0F, 0.0F, 1.0F);
         poseStack.popPose();
      }
   }

   @SubscribeEvent
   public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)Commands.literal("neoforge")
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("debug_blockentity_renderbounds").requires(src -> src.hasPermission(3)))
                     .then(Commands.argument("enable", BoolArgumentType.bool()).executes(ctx -> {
                        enabled = BoolArgumentType.getBool(ctx, "enable");
                        return 1;
                     }))
               )
         );
   }

   private BlockEntityRenderBoundsDebugRenderer() {
   }
}
