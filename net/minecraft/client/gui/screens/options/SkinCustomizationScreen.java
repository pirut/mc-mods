package net.minecraft.client.gui.screens.options;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkinCustomizationScreen extends OptionsSubScreen {
   private static final Component TITLE = Component.translatable("options.skinCustomisation.title");

   public SkinCustomizationScreen(Screen p_346103_, Options p_345751_) {
      super(p_346103_, p_345751_, TITLE);
   }

   @Override
   protected void addOptions() {
      List<AbstractWidget> $$0 = new ArrayList<>();

      for (PlayerModelPart $$1 : PlayerModelPart.values()) {
         $$0.add(
            CycleButton.onOffBuilder(this.options.isModelPartEnabled($$1))
               .create($$1.getName(), (p_344949_, p_344903_) -> this.options.toggleModelPart($$1, p_344903_))
         );
      }

      $$0.add(this.options.mainHand().createButton(this.options));
      this.list.addSmall($$0);
   }
}
