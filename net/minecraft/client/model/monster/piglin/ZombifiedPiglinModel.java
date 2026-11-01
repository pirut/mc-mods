package net.minecraft.client.model.monster.piglin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ZombifiedPiglinRenderState;

@Environment(EnvType.CLIENT)
public class ZombifiedPiglinModel extends AbstractPiglinModel<ZombifiedPiglinRenderState> {
   public ZombifiedPiglinModel(ModelPart modelPart) {
      super(modelPart);
   }

   public void setupAnim(ZombifiedPiglinRenderState zombifiedPiglinRenderState) {
      super.setupAnim(zombifiedPiglinRenderState);
      AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, zombifiedPiglinRenderState.isAggressive, zombifiedPiglinRenderState);
   }

   @Override
   public void setAllVisible(boolean visible) {
      super.setAllVisible(visible);
      this.leftSleeve.visible = visible;
      this.rightSleeve.visible = visible;
      this.leftPants.visible = visible;
      this.rightPants.visible = visible;
      this.jacket.visible = visible;
   }
}
