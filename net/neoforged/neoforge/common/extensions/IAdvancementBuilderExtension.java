package net.neoforged.neoforge.common.extensions;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public interface IAdvancementBuilderExtension {
   private Advancement.Builder self() {
      return (Advancement.Builder)this;
   }

   default AdvancementHolder save(Consumer<AdvancementHolder> saver, ResourceLocation id, ExistingFileHelper fileHelper) {
      AdvancementHolder advancementholder = this.self().build(id);
      Optional<ResourceLocation> parent = advancementholder.value().parent();
      if (parent.isPresent() && !fileHelper.exists(parent.get(), PackType.SERVER_DATA, ".json", "advancement")) {
         throw new IllegalStateException("The parent: '%s' of advancement '%s', has not been saved yet!".formatted(parent.orElseThrow(), id));
      } else {
         saver.accept(advancementholder);
         fileHelper.trackGenerated(id, PackType.SERVER_DATA, ".json", "advancement");
         return advancementholder;
      }
   }
}
