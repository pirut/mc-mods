package net.neoforged.neoforge.client.extensions;

import net.minecraft.client.gui.components.AbstractWidget;

public interface IAbstractWidgetExtension {
   private AbstractWidget self() {
      return (AbstractWidget)this;
   }

   default void onClick(double mouseX, double mouseY, int button) {
      this.self().onClick(mouseX, mouseY);
   }
}
