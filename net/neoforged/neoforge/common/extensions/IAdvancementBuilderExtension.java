package net.neoforged.neoforge.common.extensions;

import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;

public interface IAdvancementBuilderExtension {
   private Advancement.Builder self() {
      return (Advancement.Builder)this;
   }

   default AdvancementHolder save(Consumer<AdvancementHolder> saver, Identifier id) {
      AdvancementHolder advancementholder = this.self().build(id);
      saver.accept(advancementholder);
      return advancementholder;
   }
}
