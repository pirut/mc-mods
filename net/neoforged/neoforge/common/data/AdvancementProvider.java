package net.neoforged.neoforge.common.data;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementSubProvider;

public class AdvancementProvider extends net.minecraft.data.advancements.AdvancementProvider {
   public AdvancementProvider(
      PackOutput output,
      CompletableFuture<HolderLookup.Provider> registries,
      ExistingFileHelper existingFileHelper,
      List<AdvancementProvider.AdvancementGenerator> subProviders
   ) {
      super(output, registries, subProviders.stream().map(generator -> generator.toSubProvider(existingFileHelper)).toList());
   }

   public interface AdvancementGenerator {
      void generate(HolderLookup.Provider var1, Consumer<AdvancementHolder> var2, ExistingFileHelper var3);

      default AdvancementSubProvider toSubProvider(ExistingFileHelper existingFileHelper) {
         return (registries, saver) -> this.generate(registries, saver, existingFileHelper);
      }
   }
}
