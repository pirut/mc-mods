package net.minecraft.data.info;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

public class RegistryDumpReport implements DataProvider {
   private final PackOutput output;

   public RegistryDumpReport(PackOutput p_249862_) {
      this.output = p_249862_;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput p_253743_) {
      JsonObject $$1 = new JsonObject();
      BuiltInRegistries.REGISTRY.holders().forEach(p_211088_ -> $$1.add(p_211088_.key().location().toString(), dumpRegistry((Registry<?>)p_211088_.value())));
      Path $$2 = this.output.getOutputFolder(PackOutput.Target.REPORTS).resolve("registries.json");
      return DataProvider.saveStable(p_253743_, $$1, $$2);
   }

   private static <T> JsonElement dumpRegistry(Registry<T> p_124059_) {
      JsonObject $$1 = new JsonObject();
      if (p_124059_ instanceof DefaultedRegistry) {
         ResourceLocation $$2 = ((DefaultedRegistry)p_124059_).getDefaultKey();
         $$1.addProperty("default", $$2.toString());
      }

      int $$3 = BuiltInRegistries.REGISTRY.getId(p_124059_);
      $$1.addProperty("protocol_id", $$3);
      JsonObject $$4 = new JsonObject();
      p_124059_.holders().forEach(p_211092_ -> {
         T $$3x = p_211092_.value();
         int $$4x = p_124059_.getId($$3x);
         JsonObject $$5 = new JsonObject();
         $$5.addProperty("protocol_id", $$4x);
         $$4.add(p_211092_.key().location().toString(), $$5);
      });
      $$1.add("entries", $$4);
      return $$1;
   }

   @Override
   public final String getName() {
      return "Registry Dump";
   }
}
