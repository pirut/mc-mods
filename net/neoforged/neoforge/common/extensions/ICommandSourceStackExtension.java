package net.neoforged.neoforge.common.extensions;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Scoreboard;
import org.jspecify.annotations.Nullable;

public interface ICommandSourceStackExtension {
   private CommandSourceStack self() {
      return (CommandSourceStack)this;
   }

   default Scoreboard getScoreboard() {
      return this.self().getServer().getScoreboard();
   }

   @Nullable
   default AdvancementHolder getAdvancement(Identifier id) {
      return this.self().getServer().getAdvancements().get(id);
   }

   default Level getUnsidedLevel() {
      return this.self().getLevel();
   }
}
