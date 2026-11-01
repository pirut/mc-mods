package net.minecraft.client.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.SharedConstants;
import net.minecraft.SuppressForbidden;
import net.minecraft.client.ClientBootstrap;
import net.minecraft.client.data.models.EquipmentAssetProvider;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.WaypointStyleProvider;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.client.resources.IndexedAssetSource;
import net.minecraft.data.DataGenerator;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.data.loading.DatagenModLoader;

@OnlyIn(Dist.CLIENT)
public class Main {
   @DontObfuscate
   @SuppressForbidden(
      reason = "System.out needed before bootstrap"
   )
   public static void main(String[] p_388033_) throws IOException {
      SharedConstants.tryDetectVersion();
      OptionParser optionparser = new OptionParser();
      OptionSpec<Void> optionspec = optionparser.accepts("help", "Show the help menu").forHelp();
      OptionSpec<Void> optionspec1 = optionparser.accepts("client", "Include client generators");
      OptionSpec<Void> optionspec2 = optionparser.accepts("all", "Include all generators");
      OptionSpec<String> optionspec3 = optionparser.accepts("output", "Output folder").withRequiredArg().defaultsTo("generated", new String[0]);
      OptionSpec<String> existing = optionparser.accepts("existing", "Existing resource packs that generated resources can reference").withRequiredArg();
      OptionSpec<String> mod = optionparser.accepts("mod", "A modid to dump").withRequiredArg().withValuesSeparatedBy(",");
      OptionSpec<Void> flat = optionparser.accepts("flat", "Do not append modid prefix to output directory when generating for multiple mods");
      OptionSpec<String> assetIndex = optionparser.accepts("assetIndex").withRequiredArg();
      OptionSpec<File> assetsDir = optionparser.accepts("assetsDir").withRequiredArg().ofType(File.class);
      OptionSpec<Void> validateSpec = optionparser.accepts("validate", "Validate inputs");
      OptionSet optionset = optionparser.parse(p_388033_);
      if (!optionset.has(optionspec) && optionset.hasOptions()) {
         Path path = Paths.get((String)optionspec3.value(optionset));
         boolean flag = optionset.has(optionspec2);
         boolean flag1 = flag || optionset.has(optionspec1);
         Collection<Path> existingPacks = optionset.valuesOf(existing).stream().map(x$0 -> Paths.get(x$0)).toList();
         Set<String> mods = new HashSet<>(optionset.valuesOf(mod));
         boolean isFlat = mods.isEmpty() || optionset.has(flat);
         boolean validate = optionset.has(validateSpec);
         DataGenerator datagenerator = new DataGenerator(isFlat ? path : path.resolve("minecraft"), SharedConstants.getCurrentVersion(), true);
         if (mods.contains("minecraft") || mods.isEmpty()) {
            addClientProviders(datagenerator, flag1);
            Util.shutdownExecutors();
         }

         File assetsDirValue = (File)optionset.valueOf(assetsDir);
         String assetIndexValue = (String)optionset.valueOf(assetIndex);
         DatagenModLoader.begin(mods, path, List.of(), existingPacks, false, false, validate, isFlat, () -> {
            ClientBootstrap.bootstrap();
            ClientHooks.initClientRegistries();
         }, GatherDataEvent.Client::new, datagenerator, packConsumer -> {
            if (assetsDirValue != null && assetIndexValue != null) {
               packConsumer.accept(ClientPackSource.createVanillaPackSource(IndexedAssetSource.createIndexFs(assetsDirValue.toPath(), assetIndexValue)));
            }
         });
      } else {
         optionparser.printHelpOn(System.out);
      }
   }

   public static void addClientProviders(DataGenerator p_388709_, boolean p_388751_) {
      DataGenerator.PackGenerator datagenerator$packgenerator = p_388709_.getVanillaPack(p_388751_);
      datagenerator$packgenerator.addProvider(ModelProvider::new);
      datagenerator$packgenerator.addProvider(EquipmentAssetProvider::new);
      datagenerator$packgenerator.addProvider(WaypointStyleProvider::new);
      datagenerator$packgenerator.addProvider(AtlasProvider::new);
   }
}
