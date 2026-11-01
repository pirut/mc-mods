package net.neoforged.neoforge.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import net.minecraft.DetectedVersion;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.fml.ModList;
import net.neoforged.fml.i18n.FMLTranslations;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion;

public class BrandingControl {
   private static List<String> brandings;
   private static List<String> brandingsNoMC;
   private static List<String> overCopyrightBrandings;

   private static void computeBranding() {
      if (brandings == null) {
         Builder<String> brd = ImmutableList.builder();
         brd.add("Minecraft " + DetectedVersion.BUILT_IN.getName());
         int modCount = ModList.get().size();
         brd.add(FMLTranslations.parseMessage("fml.menu.branding", new Object[]{"NeoForge " + NeoForgeVersion.getVersion(), modCount}));
         brandings = brd.build();
         brandingsNoMC = brandings.subList(1, brandings.size());
      }
   }

   private static List<String> getBrandings(boolean includeMC, boolean reverse) {
      computeBranding();
      if (includeMC) {
         return reverse ? Lists.reverse(brandings) : brandings;
      } else {
         return reverse ? Lists.reverse(brandingsNoMC) : brandingsNoMC;
      }
   }

   private static void computeOverCopyrightBrandings() {
      if (overCopyrightBrandings == null) {
         Builder<String> brd = ImmutableList.builder();
         if (ClientHooks.forgeStatusLine != null) {
            brd.add(ClientHooks.forgeStatusLine);
         }

         overCopyrightBrandings = brd.build();
      }
   }

   public static void forEachLine(boolean includeMC, boolean reverse, BiConsumer<Integer, String> lineConsumer) {
      List<String> brandings = getBrandings(includeMC, reverse);
      IntStream.range(0, brandings.size()).boxed().forEachOrdered(idx -> lineConsumer.accept(idx, brandings.get(idx)));
   }

   public static void forEachAboveCopyrightLine(BiConsumer<Integer, String> lineConsumer) {
      computeOverCopyrightBrandings();
      IntStream.range(0, overCopyrightBrandings.size()).boxed().forEachOrdered(idx -> lineConsumer.accept(idx, overCopyrightBrandings.get(idx)));
   }

   public static String getClientBranding() {
      return "neoforge";
   }

   public static String getServerBranding() {
      return "neoforge";
   }

   public static ResourceManagerReloadListener resourceManagerReloadListener() {
      return BrandingControl::onResourceManagerReload;
   }

   private static void onResourceManagerReload(ResourceManager resourceManager) {
      brandings = null;
      brandingsNoMC = null;
   }
}
