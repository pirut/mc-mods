package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.entity.animation.json.AnimationHolder;
import net.neoforged.neoforge.client.entity.animation.json.AnimationLoader;

@OnlyIn(Dist.CLIENT)
public abstract class Model<S> {
   protected final ModelPart root;
   protected final Function<Identifier, RenderType> renderType;
   private final List<ModelPart> allParts;

   public Model(ModelPart p_368583_, Function<Identifier, RenderType> p_103110_) {
      this.root = p_368583_;
      this.renderType = p_103110_;
      this.allParts = p_368583_.getAllParts();
   }

   protected static AnimationHolder getAnimation(Identifier key) {
      return AnimationLoader.INSTANCE.getAnimationHolder(key);
   }

   public final RenderType renderType(Identifier p_469737_) {
      return this.renderType.apply(p_469737_);
   }

   public final void renderToBuffer(PoseStack p_103111_, VertexConsumer p_103112_, int p_103113_, int p_103114_, int p_350308_) {
      this.root().render(p_103111_, p_103112_, p_103113_, p_103114_, p_350308_);
   }

   public final void renderToBuffer(PoseStack p_350804_, VertexConsumer p_350976_, int p_350539_, int p_350374_) {
      this.renderToBuffer(p_350804_, p_350976_, p_350539_, p_350374_, -1);
   }

   public final ModelPart root() {
      return this.root;
   }

   public final List<ModelPart> allParts() {
      return this.allParts;
   }

   public void setupAnim(S p_435637_) {
      this.resetPose();
   }

   public final void resetPose() {
      for (ModelPart modelpart : this.allParts) {
         modelpart.resetPose();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Simple extends Model<Unit> {
      public Simple(ModelPart p_364140_, Function<Identifier, RenderType> p_364753_) {
         super(p_364140_, p_364753_);
      }

      public void setupAnim(Unit p_434895_) {
      }
   }
}
