package net.minecraft.client.renderer.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.monster.hoglin.HoglinModel;
import net.minecraft.client.renderer.entity.state.HoglinRenderState;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;

@Environment(EnvType.CLIENT)
public abstract class AbstractHoglinRenderer<T extends Mob & HoglinBase> extends AgeableMobRenderer<T, HoglinRenderState, HoglinModel> {
   public AbstractHoglinRenderer(EntityRendererProvider.Context context, ModelLayerLocation adultModel, ModelLayerLocation babyModel, float shadowRadius) {
      super(context, new HoglinModel(context.bakeLayer(adultModel)), new HoglinModel(context.bakeLayer(babyModel)), shadowRadius);
   }

   public HoglinRenderState createRenderState() {
      return new HoglinRenderState();
   }

   public void extractRenderState(T mob, HoglinRenderState hoglinRenderState, float f) {
      super.extractRenderState(mob, hoglinRenderState, f);
      hoglinRenderState.attackAnimationRemainingTicks = mob.getAttackAnimationRemainingTicks();
   }
}
