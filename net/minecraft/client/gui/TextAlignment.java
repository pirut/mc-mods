package net.minecraft.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.FormattedCharSequence;

@Environment(EnvType.CLIENT)
public enum TextAlignment {
   LEFT {
      @Override
      public int calculateLeft(int x, int width) {
         return x;
      }

      @Override
      public int calculateLeft(int x, Font font, FormattedCharSequence text) {
         return x;
      }
   },
   CENTER {
      @Override
      public int calculateLeft(int x, int width) {
         return x - width / 2;
      }
   },
   RIGHT {
      @Override
      public int calculateLeft(int x, int width) {
         return x - width;
      }
   };

   public abstract int calculateLeft(int x, int width);

   public int calculateLeft(int x, Font font, FormattedCharSequence text) {
      return this.calculateLeft(x, font.width(text));
   }
}
