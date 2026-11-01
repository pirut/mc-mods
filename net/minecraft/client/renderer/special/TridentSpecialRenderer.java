package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.TridentModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public class TridentSpecialRenderer implements NoDataSpecialModelRenderer {
   private final TridentModel model;

   public TridentSpecialRenderer(TridentModel model) {
      this.model = model;
   }

   @Override
   public void submit(
      ItemDisplayContext displayContext,
      PoseStack poseStack,
      SubmitNodeCollector nodeCollector,
      int packedLight,
      int packedOverlay,
      boolean hasFoil,
      int outlineColor
   ) {
      poseStack.pushPose();
      poseStack.scale(1.0F, -1.0F, -1.0F);
      nodeCollector.submitModelPart(
         this.model.root(), poseStack, this.model.renderType(TridentModel.TEXTURE), packedLight, packedOverlay, null, false, hasFoil, -1, null, outlineColor
      );
      poseStack.popPose();
   }

   @Override
   public void getExtents(Consumer<Vector3fc> output) {
      PoseStack poseStack = new PoseStack();
      poseStack.scale(1.0F, -1.0F, -1.0F);
      this.model.root().getExtentsForGui(poseStack, output);
   }

   @Environment(EnvType.CLIENT)
   public record Unbaked() implements SpecialModelRenderer.Unbaked {
      public static final MapCodec<TridentSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(new TridentSpecialRenderer.Unbaked());

      @Override
      public MapCodec<TridentSpecialRenderer.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
         return new TridentSpecialRenderer(new TridentModel(context.entityModelSet().bakeLayer(ModelLayers.TRIDENT)));
      }
   }
}
