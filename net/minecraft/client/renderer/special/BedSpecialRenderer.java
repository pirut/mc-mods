package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BedRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3fc;

@Environment(EnvType.CLIENT)
public class BedSpecialRenderer implements NoDataSpecialModelRenderer {
   private final BedRenderer bedRenderer;
   private final Material material;

   public BedSpecialRenderer(BedRenderer bedRenderer, Material material) {
      this.bedRenderer = bedRenderer;
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
      this.bedRenderer.submitSpecial(poseStack, nodeCollector, packedLight, packedOverlay, this.material, outlineColor);
   }

   @Override
   public void getExtents(Consumer<Vector3fc> output) {
      this.bedRenderer.getExtents(output);
   }

   @Environment(EnvType.CLIENT)
   public record Unbaked(Identifier texture) implements SpecialModelRenderer.Unbaked {
      public static final MapCodec<BedSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(Identifier.CODEC.fieldOf("texture").forGetter(BedSpecialRenderer.Unbaked::texture))
            .apply(instance, BedSpecialRenderer.Unbaked::new)
      );

      public Unbaked(DyeColor color) {
         this(Sheets.colorToResourceMaterial(color));
      }

      @Override
      public MapCodec<BedSpecialRenderer.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
         return new BedSpecialRenderer(new BedRenderer(context), Sheets.BED_MAPPER.apply(this.texture));
      }
   }
}
