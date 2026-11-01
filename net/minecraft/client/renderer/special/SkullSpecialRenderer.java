package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.SkullBlock;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class SkullSpecialRenderer implements NoDataSpecialModelRenderer {
   private final SkullModelBase model;
   private final float animation;
   private final RenderType renderType;

   public SkullSpecialRenderer(SkullModelBase model, float animation, RenderType renderType) {
      this.model = model;
      this.animation = animation;
      this.renderType = renderType;
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
      SkullBlockRenderer.submitSkull(null, 180.0F, this.animation, poseStack, nodeCollector, packedLight, this.model, this.renderType, outlineColor, null);
   }

   @Override
   public void getExtents(Consumer<Vector3fc> output) {
      PoseStack poseStack = new PoseStack();
      poseStack.translate(0.5F, 0.0F, 0.5F);
      poseStack.scale(-1.0F, -1.0F, 1.0F);
      SkullModelBase.State state = new SkullModelBase.State();
      state.animationPos = this.animation;
      state.yRot = 180.0F;
      this.model.setupAnim(state);
      this.model.root().getExtentsForGui(poseStack, output);
   }

   @Environment(EnvType.CLIENT)
   public record Unbaked(SkullBlock.Type kind, Optional<Identifier> textureOverride, float animation) implements SpecialModelRenderer.Unbaked {
      public static final MapCodec<SkullSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               SkullBlock.Type.CODEC.fieldOf("kind").forGetter(SkullSpecialRenderer.Unbaked::kind),
               Identifier.CODEC.optionalFieldOf("texture").forGetter(SkullSpecialRenderer.Unbaked::textureOverride),
               Codec.FLOAT.optionalFieldOf("animation", 0.0F).forGetter(SkullSpecialRenderer.Unbaked::animation)
            )
            .apply(instance, SkullSpecialRenderer.Unbaked::new)
      );

      public Unbaked(SkullBlock.Type type) {
         this(type, Optional.empty(), 0.0F);
      }

      @Override
      public MapCodec<SkullSpecialRenderer.Unbaked> type() {
         return MAP_CODEC;
      }

      @Nullable
      @Override
      public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
         SkullModelBase skullModelBase = SkullBlockRenderer.createModel(context.entityModelSet(), this.kind);
         Identifier identifier = this.textureOverride
            .<Identifier>map(identifierx -> identifierx.withPath(string -> "textures/entity/" + string + ".png"))
            .orElse(null);
         if (skullModelBase == null) {
            return null;
         } else {
            RenderType renderType = SkullBlockRenderer.getSkullRenderType(this.kind, identifier);
            return new SkullSpecialRenderer(skullModelBase, this.animation, renderType);
         }
      }
   }
}
