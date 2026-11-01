package net.minecraft.world.entity.animal.feline;

import java.util.List;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.variant.MoonBrightnessCheck;
import net.minecraft.world.entity.variant.PriorityProvider;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;
import net.minecraft.world.entity.variant.StructureCheck;
import net.minecraft.world.level.levelgen.structure.Structure;

public interface CatVariants {
   ResourceKey<CatVariant> TABBY = createKey("tabby");
   ResourceKey<CatVariant> BLACK = createKey("black");
   ResourceKey<CatVariant> RED = createKey("red");
   ResourceKey<CatVariant> SIAMESE = createKey("siamese");
   ResourceKey<CatVariant> BRITISH_SHORTHAIR = createKey("british_shorthair");
   ResourceKey<CatVariant> CALICO = createKey("calico");
   ResourceKey<CatVariant> PERSIAN = createKey("persian");
   ResourceKey<CatVariant> RAGDOLL = createKey("ragdoll");
   ResourceKey<CatVariant> WHITE = createKey("white");
   ResourceKey<CatVariant> JELLIE = createKey("jellie");
   ResourceKey<CatVariant> ALL_BLACK = createKey("all_black");

   private static ResourceKey<CatVariant> createKey(String p_478741_) {
      return ResourceKey.create(Registries.CAT_VARIANT, Identifier.withDefaultNamespace(p_478741_));
   }

   static void bootstrap(BootstrapContext<CatVariant> p_479432_) {
      HolderGetter<Structure> holdergetter = p_479432_.lookup(Registries.STRUCTURE);
      registerForAnyConditions(p_479432_, TABBY, "entity/cat/tabby");
      registerForAnyConditions(p_479432_, BLACK, "entity/cat/black");
      registerForAnyConditions(p_479432_, RED, "entity/cat/red");
      registerForAnyConditions(p_479432_, SIAMESE, "entity/cat/siamese");
      registerForAnyConditions(p_479432_, BRITISH_SHORTHAIR, "entity/cat/british_shorthair");
      registerForAnyConditions(p_479432_, CALICO, "entity/cat/calico");
      registerForAnyConditions(p_479432_, PERSIAN, "entity/cat/persian");
      registerForAnyConditions(p_479432_, RAGDOLL, "entity/cat/ragdoll");
      registerForAnyConditions(p_479432_, WHITE, "entity/cat/white");
      registerForAnyConditions(p_479432_, JELLIE, "entity/cat/jellie");
      register(
         p_479432_,
         ALL_BLACK,
         "entity/cat/all_black",
         new SpawnPrioritySelectors(
            List.of(
               new PriorityProvider.Selector<>(new StructureCheck(holdergetter.getOrThrow(StructureTags.CATS_SPAWN_AS_BLACK)), 1),
               new PriorityProvider.Selector<>(new MoonBrightnessCheck(MinMaxBounds.Doubles.atLeast(0.9)), 0)
            )
         )
      );
   }

   private static void registerForAnyConditions(BootstrapContext<CatVariant> p_482200_, ResourceKey<CatVariant> p_480949_, String p_480332_) {
      register(p_482200_, p_480949_, p_480332_, SpawnPrioritySelectors.fallback(0));
   }

   private static void register(BootstrapContext<CatVariant> p_479860_, ResourceKey<CatVariant> p_480166_, String p_480483_, SpawnPrioritySelectors p_478967_) {
      p_479860_.register(p_480166_, new CatVariant(new ClientAsset.ResourceTexture(Identifier.withDefaultNamespace(p_480483_)), p_478967_));
   }
}
