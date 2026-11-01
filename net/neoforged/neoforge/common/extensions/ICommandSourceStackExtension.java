package net.neoforged.neoforge.common.extensions;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Scoreboard;

public interface ICommandSourceStackExtension {
   private CommandSourceStack self() {
      return (CommandSourceStack)this;
   }

   default Scoreboard getScoreboard() {
      return this.self().getServer().getScoreboard();
   }

   default AdvancementHolder getAdvancement(ResourceLocation id) {
      return this.self().getServer().getAdvancements().get(id);
   }

   default RecipeManager getRecipeManager() {
      return this.self().getServer().getRecipeManager();
   }

   default Level getUnsidedLevel() {
      return this.self().getLevel();
   }
}
