package net.neoforged.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.commands.Commands;
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
public final class ParticleBoundsDebugRenderer {
   private static boolean enabled = false;

   @SubscribeEvent
   public static void onRenderLevelStage(RenderLevelStageEvent event) {
      if (enabled && event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
         Vec3 camPos = event.getCamera().getPosition();
         PoseStack poseStack = event.getPoseStack();
         poseStack.pushPose();
         VertexConsumer consumer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.lines());
         Minecraft.getInstance().particleEngine.iterateParticles(particle -> {
            AABB bb = particle.getRenderBoundingBox(event.getPartialTick().getGameTimeDeltaPartialTick(false));
            if (!bb.isInfinite() && event.getFrustum().isVisible(bb)) {
               poseStack.pushPose();
               Vec3 offset = particle.getPos().subtract(camPos);
               poseStack.translate(offset.x, offset.y, offset.z);
               bb = bb.move(-particle.getPos().x, -particle.getPos().y, -particle.getPos().z);
               LevelRenderer.renderLineBox(poseStack, consumer, bb, 1.0F, 0.0F, 0.0F, 1.0F);
               poseStack.popPose();
            }
         });
         Minecraft.getInstance().renderBuffers().bufferSource().endBatch(RenderType.lines());
         poseStack.popPose();
      }
   }

   @SubscribeEvent
   public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)Commands.literal("neoforge")
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("debug_particle_renderbounds").requires(src -> src.hasPermission(3)))
                     .then(Commands.argument("enable", BoolArgumentType.bool()).executes(ctx -> {
                        enabled = BoolArgumentType.getBool(ctx, "enable");
                        return 1;
                     }))
               )
         );
   }

   private ParticleBoundsDebugRenderer() {
   }
}
