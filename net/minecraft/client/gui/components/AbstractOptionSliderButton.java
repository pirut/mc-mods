package net.minecraft.client.gui.components;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Options;
import net.minecraft.network.chat.CommonComponents;

@Environment(EnvType.CLIENT)
public abstract class AbstractOptionSliderButton extends AbstractSliderButton {
   protected final Options options;

   protected AbstractOptionSliderButton(Options options, int x, int y, int width, int height, double value) {
      super(x, y, width, height, CommonComponents.EMPTY, value);
      this.options = options;
   }
}
