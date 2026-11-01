package net.minecraft.data.info;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryOps;

public class ItemListReport implements DataProvider {
   private final PackOutput output;
   private final CompletableFuture<HolderLookup.Provider> registries;

   public ItemListReport(PackOutput p_331079_, CompletableFuture<HolderLookup.Provider> p_330936_) {
      this.output = p_331079_;
      this.registries = p_330936_;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput p_330682_) {
      Path $$1 = this.output.getOutputFolder(PackOutput.Target.REPORTS).resolve("items.json");
      return this.registries
         .thenCompose(
            p_330267_ -> {
               JsonObject $$3 = new JsonObject();
               RegistryOps<JsonElement> $$4 = p_330267_.createSerializationContext(JsonOps.INSTANCE);
               p_330267_.lookupOrThrow(Registries.ITEM)
                  .listElements()
                  .forEach(
                     p_347011_ -> {
                        JsonObject $$3x = new JsonObject();
                        $$3x.add(
                           "components",
                           (JsonElement)DataComponentMap.CODEC
                              .encodeStart($$4, p_347011_.value().components())
                              .getOrThrow(p_347012_ -> new IllegalStateException("Failed to encode components: " + p_347012_))
                        );
                        $$3.add(p_347011_.getRegisteredName(), $$3x);
                     }
                  );
               return DataProvider.saveStable(p_330682_, $$3, $$1);
            }
         );
   }

   @Override
   public final String getName() {
      return "Item List";
   }
}
