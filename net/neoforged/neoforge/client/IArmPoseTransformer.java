package net.neoforged.neoforge.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.HumanoidArm;

@FunctionalInterface
public interface IArmPoseTransformer {
   void applyTransform(HumanoidModel<?> var1, HumanoidRenderState var2, HumanoidArm var3);
}
