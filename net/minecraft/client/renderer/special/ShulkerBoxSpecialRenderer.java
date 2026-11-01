package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public class ShulkerBoxSpecialRenderer implements NoDataSpecialModelRenderer {
   private final ShulkerBoxRenderer shulkerBoxRenderer;
   private final float openness;
   private final Direction orientation;
   private final Material material;

   public ShulkerBoxSpecialRenderer(ShulkerBoxRenderer shulkerBoxRenderer, float openness, Direction orientation, Material material) {
      this.shulkerBoxRenderer = shulkerBoxRenderer;
      this.openness = openness;
      this.orientation = orientation;
      this.material = material;
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
      this.shulkerBoxRenderer.submit(poseStack, nodeCollector, packedLight, packedOverlay, this.orientation, this.openness, null, this.material, outlineColor);
   }

   @Override
   public void getExtents(Consumer<Vector3fc> output) {
      this.shulkerBoxRenderer.getExtents(this.orientation, this.openness, output);
   }

   @Environment(EnvType.CLIENT)
   public record Unbaked(Identifier texture, float openness, Direction orientation) implements SpecialModelRenderer.Unbaked {
      public static final MapCodec<ShulkerBoxSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               Identifier.CODEC.fieldOf("texture").forGetter(ShulkerBoxSpecialRenderer.Unbaked::texture),
               Codec.FLOAT.optionalFieldOf("openness", 0.0F).forGetter(ShulkerBoxSpecialRenderer.Unbaked::openness),
               Direction.CODEC.optionalFieldOf("orientation", Direction.UP).forGetter(ShulkerBoxSpecialRenderer.Unbaked::orientation)
            )
            .apply(instance, ShulkerBoxSpecialRenderer.Unbaked::new)
      );

      public Unbaked() {
         this(Identifier.withDefaultNamespace("shulker"), 0.0F, Direction.UP);
      }

      public Unbaked(DyeColor color) {
         this(Sheets.colorToShulkerMaterial(color), 0.0F, Direction.UP);
      }

      @Override
      public MapCodec<ShulkerBoxSpecialRenderer.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
         return new ShulkerBoxSpecialRenderer(new ShulkerBoxRenderer(context), this.openness, this.orientation, Sheets.SHULKER_MAPPER.apply(this.texture));
      }
   }
}
