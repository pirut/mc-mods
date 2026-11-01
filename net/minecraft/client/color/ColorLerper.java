package net.minecraft.client.color;

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ColorLerper {
   public static final DyeColor[] MUSIC_NOTE_COLORS = new DyeColor[]{
      DyeColor.WHITE,
      DyeColor.LIGHT_GRAY,
      DyeColor.LIGHT_BLUE,
      DyeColor.BLUE,
      DyeColor.CYAN,
      DyeColor.GREEN,
      DyeColor.LIME,
      DyeColor.YELLOW,
      DyeColor.ORANGE,
      DyeColor.PINK,
      DyeColor.RED,
      DyeColor.MAGENTA
   };

   public static int getLerpedColor(ColorLerper.Type p_425899_, float p_425551_) {
      int i = Mth.floor(p_425551_);
      int j = i / p_425899_.colorDuration;
      int k = p_425899_.colors.length;
      int l = j % k;
      int i1 = (j + 1) % k;
      float f = (i % p_425899_.colorDuration + Mth.frac(p_425551_)) / p_425899_.colorDuration;
      int j1 = p_425899_.getColor(p_425899_.colors[l]);
      int k1 = p_425899_.getColor(p_425899_.colors[i1]);
      return ARGB.srgbLerp(f, j1, k1);
   }

   static int getModifiedColor(DyeColor p_426206_, float p_425908_) {
      if (p_426206_ == DyeColor.WHITE) {
         return -1644826;
      } else {
         int i = p_426206_.getTextureDiffuseColor();
         return ARGB.color(255, Mth.floor(ARGB.red(i) * p_425908_), Mth.floor(ARGB.green(i) * p_425908_), Mth.floor(ARGB.blue(i) * p_425908_));
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Type {
      SHEEP(25, DyeColor.values(), 0.75F),
      MUSIC_NOTE(30, ColorLerper.MUSIC_NOTE_COLORS, 1.25F);

      final int colorDuration;
      private final Map<DyeColor, Integer> colorByDye;
      final DyeColor[] colors;

      private Type(int p_425636_, DyeColor[] p_425741_, float p_426017_) {
         this.colorDuration = p_425636_;
         this.colorByDye = Maps.newHashMap(
            Arrays.stream(p_425741_)
               .collect(Collectors.toMap(p_426174_ -> (DyeColor)p_426174_, p_426111_ -> ColorLerper.getModifiedColor(p_426111_, p_426017_)))
         );
         this.colors = p_425741_;
      }

      public final int getColor(DyeColor p_425757_) {
         return this.colorByDye.get(p_425757_);
      }
   }
}
