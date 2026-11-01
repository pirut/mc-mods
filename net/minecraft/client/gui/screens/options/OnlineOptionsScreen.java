package net.minecraft.client.gui.screens.options;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Optionull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OnlineOptionsScreen extends OptionsSubScreen {
   private static final Component TITLE = Component.translatable("options.online.title");
   @Nullable
   private OptionInstance<Unit> difficultyDisplay;

   public OnlineOptionsScreen(Screen p_345316_, Options p_346369_) {
      super(p_345316_, p_346369_, TITLE);
   }

   @Override
   protected void init() {
      super.init();
      if (this.difficultyDisplay != null) {
         AbstractWidget $$0 = this.list.findOption(this.difficultyDisplay);
         if ($$0 != null) {
            $$0.active = false;
         }
      }
   }

   private OptionInstance<?>[] options(Options p_346292_, Minecraft p_344882_) {
      List<OptionInstance<?>> $$2 = new ArrayList<>();
      $$2.add(p_346292_.realmsNotifications());
      $$2.add(p_346292_.allowServerListing());
      OptionInstance<Unit> $$3 = Optionull.map(
         p_344882_.level,
         p_346363_ -> {
            Difficulty $$1 = p_346363_.getDifficulty();
            return new OptionInstance<>(
               "options.difficulty.online",
               OptionInstance.noTooltip(),
               (p_345525_, p_345135_) -> $$1.getDisplayName(),
               new OptionInstance.Enum<>(List.of(Unit.INSTANCE), Codec.EMPTY.codec()),
               Unit.INSTANCE,
               p_346107_ -> {}
            );
         }
      );
      if ($$3 != null) {
         this.difficultyDisplay = $$3;
         $$2.add($$3);
      }

      return $$2.toArray(new OptionInstance[0]);
   }

   @Override
   protected void addOptions() {
      this.list.addSmall(this.options(this.options, this.minecraft));
   }
}
