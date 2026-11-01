package net.neoforged.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(
   value = {Dist.CLIENT},
   modid = "neoforge"
)
public final class BlockEntityRenderBoundsDebugRenderer {
   private static final ContextKey<List<BlockEntityRenderBoundsDebugRenderer.BlockEntityRenderBoundsRenderState>> DATA_KEY = new ContextKey<>(
      Identifier.fromNamespaceAndPath("neoforge", "block_entity_render_bounds")
   );
   private static boolean enabled = false;

   @SubscribeEvent
   public static void onExtractLevelRenderState(ExtractLevelRenderStateEvent event) {
      if (enabled) {
         List<BlockEntityRenderBoundsDebugRenderer.BlockEntityRenderBoundsRenderState> renderStates = new ArrayList<>();
         BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
         event.getLevelRenderer().iterateVisibleBlockEntities(be -> {
            BlockEntityRenderer<BlockEntity, ?> renderer = dispatcher.getRenderer(be);
            if (renderer != null) {
               AABB aabb = renderer.getRenderBoundingBox(be);
               if (!aabb.isInfinite()) {
                  BlockPos pos = be.getBlockPos();
                  aabb = aabb.move(-pos.getX(), -pos.getY(), -pos.getZ());
                  renderStates.add(new BlockEntityRenderBoundsDebugRenderer.BlockEntityRenderBoundsRenderState(pos, aabb));
               }
            }
         });
         if (!renderStates.isEmpty()) {
            event.getRenderState().setRenderData(DATA_KEY, renderStates);
         }
      }
   }

   @SubscribeEvent
   public static void onRenderLevelStage(RenderLevelStageEvent.AfterEntities event) {
      if (enabled) {
         List<BlockEntityRenderBoundsDebugRenderer.BlockEntityRenderBoundsRenderState> renderStates = event.getLevelRenderState().getRenderData(DATA_KEY);
         if (renderStates != null) {
            PoseStack poseStack = event.getPoseStack();
            Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
            VertexConsumer consumer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderTypes.lines());

            for (BlockEntityRenderBoundsDebugRenderer.BlockEntityRenderBoundsRenderState be : renderStates) {
               Vec3 offset = Vec3.atLowerCornerOf(be.pos).subtract(camera);
               poseStack.pushPose();
               poseStack.translate(offset.x, offset.y, offset.z);
               ShapeRenderer.renderShape(poseStack, consumer, Shapes.create(be.bounds), 0.0, 0.0, 0.0, -65536, 1.0F);
               poseStack.popPose();
            }
         }
      }
   }

   @SubscribeEvent
   public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)Commands.literal("neoforge")
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("debug_blockentity_renderbounds").requires(Commands.hasPermission(Commands.LEVEL_ADMINS)))
                     .then(Commands.argument("enable", BoolArgumentType.bool()).executes(ctx -> {
                        enabled = BoolArgumentType.getBool(ctx, "enable");
                        return 1;
                     }))
               )
         );
   }

   private BlockEntityRenderBoundsDebugRenderer() {
   }

   private record BlockEntityRenderBoundsRenderState(BlockPos pos, AABB bounds) {
   }
}
