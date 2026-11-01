package net.minecraft.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface ComponentPath {
   static ComponentPath leaf(GuiEventListener component) {
      return new ComponentPath.Leaf(component);
   }

   @Nullable
   static ComponentPath path(ContainerEventHandler component, @Nullable ComponentPath childPath) {
      return childPath == null ? null : new ComponentPath.Path(component, childPath);
   }

   static ComponentPath path(GuiEventListener leafComponent, ContainerEventHandler... ancestorComponents) {
      ComponentPath componentPath = leaf(leafComponent);

      for (ContainerEventHandler containerEventHandler : ancestorComponents) {
         componentPath = path(containerEventHandler, componentPath);
      }

      return componentPath;
   }

   GuiEventListener component();

   void applyFocus(boolean focused);

   @Environment(EnvType.CLIENT)
   public record Leaf(GuiEventListener component) implements ComponentPath {
      @Override
      public void applyFocus(boolean focused) {
         this.component.setFocused(focused);
      }
   }

   @Environment(EnvType.CLIENT)
   public record Path(ContainerEventHandler component, ComponentPath childPath) implements ComponentPath {
      @Override
      public void applyFocus(boolean focused) {
         if (!focused) {
            this.component.setFocused(null);
         } else {
            this.component.setFocused(this.childPath.component());
         }

         this.childPath.applyFocus(focused);
      }
   }
}
