package net.neoforged.neoforge.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RenderArmEvent extends Event implements ICancellableEvent {
   private final PoseStack poseStack;
   private final SubmitNodeCollector submitNodeCollector;
   private final int packedLight;
   private final AbstractClientPlayer player;
   private final HumanoidArm arm;

   @Internal
   public RenderArmEvent(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight, AbstractClientPlayer player, HumanoidArm arm) {
      this.poseStack = poseStack;
      this.submitNodeCollector = submitNodeCollector;
      this.packedLight = packedLight;
      this.player = player;
      this.arm = arm;
   }

   public HumanoidArm getArm() {
      return this.arm;
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

   public AbstractClientPlayer getPlayer() {
      return this.player;
   }
}
