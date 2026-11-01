package net.neoforged.neoforge.common.data.internal;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class NeoForgeFluidTagsProvider extends FluidTagsProvider {
   public NeoForgeFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
      super(output, lookupProvider, "neoforge", existingFileHelper);
   }

   @Override
   public void addTags(HolderLookup.Provider lookupProvider) {
      this.tag(Tags.Fluids.WATER).add(Fluids.WATER).add(Fluids.FLOWING_WATER);
      this.tag(Tags.Fluids.LAVA).add(Fluids.LAVA).add(Fluids.FLOWING_LAVA);
      this.tag(Tags.Fluids.MILK).addOptional(NeoForgeMod.MILK.getId()).addOptional(NeoForgeMod.FLOWING_MILK.getId());
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

   private IntrinsicHolderTagsProvider.IntrinsicTagAppender<Fluid> tagWithOptionalLegacy(TagKey<Fluid> tag) {
      IntrinsicHolderTagsProvider.IntrinsicTagAppender<Fluid> tagAppender = this.tag(tag);
      tagAppender.addOptionalTag(ResourceLocation.fromNamespaceAndPath("forge", tag.location().getPath()));
      return tagAppender;
   }
}
