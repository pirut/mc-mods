package net.neoforged.neoforge.common.data.internal;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.Tags;

public final class NeoForgeFluidTagsProvider extends FluidTagsProvider {
   public NeoForgeFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
      super(output, lookupProvider, "neoforge");
   }

   @Override
   public void addTags(HolderLookup.Provider lookupProvider) {
      this.tag(Tags.Fluids.WATER).add(Fluids.WATER).add(Fluids.FLOWING_WATER);
      this.tag(Tags.Fluids.LAVA).add(Fluids.LAVA).add(Fluids.FLOWING_LAVA);
      this.tag(Tags.Fluids.MILK).addOptional(NeoForgeMod.MILK.get()).addOptional(NeoForgeMod.FLOWING_MILK.get());
      this.tag(Tags.Fluids.GASEOUS);
      this.tag(Tags.Fluids.HONEY);
      this.tag(Tags.Fluids.EXPERIENCE);
      this.tag(Tags.Fluids.POTION);
      this.tag(Tags.Fluids.SUSPICIOUS_STEW);
      this.tag(Tags.Fluids.MUSHROOM_STEW);
      this.tag(Tags.Fluids.RABBIT_STEW);
      this.tag(Tags.Fluids.BEETROOT_SOUP);
      this.tag(Tags.Fluids.HIDDEN_FROM_RECIPE_VIEWERS);
      this.tagWithOptionalLegacy(Tags.Fluids.MILK);
      this.tagWithOptionalLegacy(Tags.Fluids.GASEOUS);
      this.tagWithOptionalLegacy(Tags.Fluids.HONEY);
      this.tagWithOptionalLegacy(Tags.Fluids.POTION);
      this.tagWithOptionalLegacy(Tags.Fluids.SUSPICIOUS_STEW);
      this.tagWithOptionalLegacy(Tags.Fluids.MUSHROOM_STEW);
      this.tagWithOptionalLegacy(Tags.Fluids.RABBIT_STEW);
      this.tagWithOptionalLegacy(Tags.Fluids.BEETROOT_SOUP);
   }

   private TagAppender<Fluid, Fluid> tagWithOptionalLegacy(TagKey<Fluid> tag) {
      TagAppender<Fluid, Fluid> tagAppender = this.tag(tag);
      tagAppender.addOptionalTag(TagKey.create(Registries.FLUID, Identifier.fromNamespaceAndPath("forge", tag.location().getPath())));
      return tagAppender;
   }
}
