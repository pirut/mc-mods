package net.minecraft.world.item.equipment.trim;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;

public class TrimPatterns {
   public static final ResourceKey<TrimPattern> SENTRY = registryKey("sentry");
   public static final ResourceKey<TrimPattern> DUNE = registryKey("dune");
   public static final ResourceKey<TrimPattern> COAST = registryKey("coast");
   public static final ResourceKey<TrimPattern> WILD = registryKey("wild");
   public static final ResourceKey<TrimPattern> WARD = registryKey("ward");
   public static final ResourceKey<TrimPattern> EYE = registryKey("eye");
   public static final ResourceKey<TrimPattern> VEX = registryKey("vex");
   public static final ResourceKey<TrimPattern> TIDE = registryKey("tide");
   public static final ResourceKey<TrimPattern> SNOUT = registryKey("snout");
   public static final ResourceKey<TrimPattern> RIB = registryKey("rib");
   public static final ResourceKey<TrimPattern> SPIRE = registryKey("spire");
   public static final ResourceKey<TrimPattern> WAYFINDER = registryKey("wayfinder");
   public static final ResourceKey<TrimPattern> SHAPER = registryKey("shaper");
   public static final ResourceKey<TrimPattern> SILENCE = registryKey("silence");
   public static final ResourceKey<TrimPattern> RAISER = registryKey("raiser");
   public static final ResourceKey<TrimPattern> HOST = registryKey("host");
   public static final ResourceKey<TrimPattern> FLOW = registryKey("flow");
   public static final ResourceKey<TrimPattern> BOLT = registryKey("bolt");

   public static void bootstrap(BootstrapContext<TrimPattern> p_371366_) {
      register(p_371366_, SENTRY);
      register(p_371366_, DUNE);
      register(p_371366_, COAST);
      register(p_371366_, WILD);
      register(p_371366_, WARD);
      register(p_371366_, EYE);
      register(p_371366_, VEX);
      register(p_371366_, TIDE);
      register(p_371366_, SNOUT);
      register(p_371366_, RIB);
      register(p_371366_, SPIRE);
      register(p_371366_, WAYFINDER);
      register(p_371366_, SHAPER);
      register(p_371366_, SILENCE);
      register(p_371366_, RAISER);
      register(p_371366_, HOST);
      register(p_371366_, FLOW);
      register(p_371366_, BOLT);
   }

   public static void register(BootstrapContext<TrimPattern> p_371430_, ResourceKey<TrimPattern> p_371696_) {
      TrimPattern trimpattern = new TrimPattern(
         defaultAssetId(p_371696_), Component.translatable(Util.makeDescriptionId("trim_pattern", p_371696_.identifier())), false
      );
      p_371430_.register(p_371696_, trimpattern);
   }

   private static ResourceKey<TrimPattern> registryKey(String p_371622_) {
      return ResourceKey.create(Registries.TRIM_PATTERN, Identifier.withDefaultNamespace(p_371622_));
   }

   public static Identifier defaultAssetId(ResourceKey<TrimPattern> p_399743_) {
      return p_399743_.identifier();
   }
}
