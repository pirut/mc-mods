package net.minecraft.client.gui.render.state.pip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record GuiBannerResultRenderState(
   BannerFlagModel flag,
   DyeColor baseColor,
   BannerPatternLayers resultBannerPatterns,
   int x0,
   int y0,
   int x1,
   int y1,
   @Nullable ScreenRectangle scissorArea,
   @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
   public GuiBannerResultRenderState(
      BannerFlagModel flag, DyeColor baseColor, BannerPatternLayers resultBannerPatterns, int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea
   ) {
      this(flag, baseColor, resultBannerPatterns, x0, y0, x1, y1, scissorArea, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
   }

   @Override
   public float scale() {
      return 16.0F;
   }
}
