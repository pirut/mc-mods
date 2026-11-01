package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface OrderedSubmitNodeCollector {
   void submitShadow(PoseStack var1, float var2, List<EntityRenderState.ShadowPiece> var3);

   void submitNameTag(PoseStack var1, @Nullable Vec3 var2, int var3, Component var4, boolean var5, int var6, double var7, CameraRenderState var9);

   void submitText(
      PoseStack var1, float var2, float var3, FormattedCharSequence var4, boolean var5, Font.DisplayMode var6, int var7, int var8, int var9, int var10
   );

   void submitFlame(PoseStack var1, EntityRenderState var2, Quaternionf var3);

   void submitLeash(PoseStack var1, EntityRenderState.LeashState var2);

   <S> void submitModel(
      Model<? super S> var1,
      S var2,
      PoseStack var3,
      RenderType var4,
      int var5,
      int var6,
      int var7,
      @Nullable TextureAtlasSprite var8,
      int var9,
      @Nullable ModelFeatureRenderer.CrumblingOverlay var10
   );

   default <S> void submitModel(
      Model<? super S> p_439327_,
      S p_439516_,
      PoseStack p_438927_,
      RenderType p_459216_,
      int p_439354_,
      int p_439771_,
      int p_440616_,
      @Nullable ModelFeatureRenderer.CrumblingOverlay p_439732_
   ) {
      this.submitModel(p_439327_, p_439516_, p_438927_, p_459216_, p_439354_, p_439771_, -1, null, p_440616_, p_439732_);
   }

   default void submitModelPart(
      ModelPart p_439389_, PoseStack p_438887_, RenderType p_458907_, int p_439289_, int p_440106_, @Nullable TextureAtlasSprite p_439647_
   ) {
      this.submitModelPart(p_439389_, p_438887_, p_458907_, p_439289_, p_440106_, p_439647_, false, false, -1, null, 0);
   }

   default void submitModelPart(
      ModelPart p_438869_,
      PoseStack p_440328_,
      RenderType p_459082_,
      int p_438949_,
      int p_439805_,
      @Nullable TextureAtlasSprite p_440472_,
      int p_458930_,
      @Nullable ModelFeatureRenderer.CrumblingOverlay p_459100_
   ) {
      this.submitModelPart(p_438869_, p_440328_, p_459082_, p_438949_, p_439805_, p_440472_, false, false, p_458930_, p_459100_, 0);
   }

   default void submitModelPart(
      ModelPart p_440506_,
      PoseStack p_439549_,
      RenderType p_459119_,
      int p_439997_,
      int p_440251_,
      @Nullable TextureAtlasSprite p_440420_,
      boolean p_438944_,
      boolean p_440215_
   ) {
      this.submitModelPart(p_440506_, p_439549_, p_459119_, p_439997_, p_440251_, p_440420_, p_438944_, p_440215_, -1, null, 0);
   }

   void submitModelPart(
      ModelPart var1,
      PoseStack var2,
      RenderType var3,
      int var4,
      int var5,
      @Nullable TextureAtlasSprite var6,
      boolean var7,
      boolean var8,
      int var9,
      @Nullable ModelFeatureRenderer.CrumblingOverlay var10,
      int var11
   );

   void submitBlock(PoseStack var1, BlockState var2, int var3, int var4, int var5);

   void submitMovingBlock(PoseStack var1, MovingBlockRenderState var2);

   void submitBlockModel(PoseStack var1, RenderType var2, BlockStateModel var3, float var4, float var5, float var6, int var7, int var8, int var9);

   void submitItem(
      PoseStack var1,
      ItemDisplayContext var2,
      int var3,
      int var4,
      int var5,
      int[] var6,
      List<BakedQuad> var7,
      RenderType var8,
      ItemStackRenderState.FoilType var9
   );

   void submitCustomGeometry(PoseStack var1, RenderType var2, SubmitNodeCollector.CustomGeometryRenderer var3);

   void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer var1);
}
