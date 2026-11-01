package net.neoforged.neoforge.common.data.internal;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.Tags;

public final class NeoForgeDamageTypeTagsProvider extends DamageTypeTagsProvider {
   private final Map<Identifier, TagBuilder> vanillaBuilders = Maps.newLinkedHashMap();
   private boolean inVanilla;

   public NeoForgeDamageTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
      super(output, lookupProvider, "neoforge");
   }

   @Override
   protected TagAppender<ResourceKey<DamageType>, DamageType> tag(TagKey<DamageType> tag) {
      return this.inVanilla ? TagAppender.forBuilder(this.vanillaBuilders.computeIfAbsent(tag.location(), location -> TagBuilder.create())) : super.tag(tag);
   }

   @Override
   protected void addTags(HolderLookup.Provider lookupProvider) {
      this.inVanilla = true;
      super.addTags(lookupProvider);
      this.inVanilla = false;
      this.tag(Tags.DamageTypes.IS_POISON).addOptional(NeoForgeMod.POISON_DAMAGE);
      this.tag(DamageTypes.WITHER, Tags.DamageTypes.IS_WITHER);
      this.tag(DamageTypes.WITHER_SKULL, Tags.DamageTypes.IS_WITHER);
      this.tag(DamageTypes.MAGIC, Tags.DamageTypes.IS_MAGIC);
      this.tag(DamageTypes.INDIRECT_MAGIC, Tags.DamageTypes.IS_MAGIC);
      this.tag(DamageTypes.THORNS, Tags.DamageTypes.IS_MAGIC);
      this.tag(DamageTypes.DRAGON_BREATH, Tags.DamageTypes.IS_MAGIC);
      this.tag(Tags.DamageTypes.IS_MAGIC).addTags(new TagKey[]{Tags.DamageTypes.IS_POISON, Tags.DamageTypes.IS_WITHER});
      this.addAsVanilla(DamageTypes.MAGIC).addTags(new TagKey[]{Tags.DamageTypes.IS_POISON});
      this.tag(DamageTypes.IN_FIRE, Tags.DamageTypes.IS_ENVIRONMENT);
      this.tag(DamageTypes.ON_FIRE, Tags.DamageTypes.IS_ENVIRONMENT);
      this.tag(DamageTypes.LAVA, Tags.DamageTypes.IS_ENVIRONMENT);
      this.tag(DamageTypes.HOT_FLOOR, Tags.DamageTypes.IS_ENVIRONMENT);
      this.tag(DamageTypes.DROWN, Tags.DamageTypes.IS_ENVIRONMENT);
      this.tag(DamageTypes.STARVE, Tags.DamageTypes.IS_ENVIRONMENT);
      this.tag(DamageTypes.DRY_OUT, Tags.DamageTypes.IS_ENVIRONMENT);
      this.tag(DamageTypes.FREEZE, Tags.DamageTypes.IS_ENVIRONMENT);
      this.tag(DamageTypes.LIGHTNING_BOLT, Tags.DamageTypes.IS_ENVIRONMENT);
      this.tag(DamageTypes.CACTUS, Tags.DamageTypes.IS_ENVIRONMENT);
      this.tag(DamageTypes.STALAGMITE, Tags.DamageTypes.IS_ENVIRONMENT);
      this.tag(DamageTypes.FALLING_STALACTITE, Tags.DamageTypes.IS_ENVIRONMENT);
      this.tag(DamageTypes.FALLING_BLOCK, Tags.DamageTypes.IS_ENVIRONMENT);
      this.tag(DamageTypes.FALLING_ANVIL, Tags.DamageTypes.IS_ENVIRONMENT);
      this.tag(DamageTypes.CRAMMING, Tags.DamageTypes.IS_ENVIRONMENT);
      this.tag(DamageTypes.FLY_INTO_WALL, Tags.DamageTypes.IS_ENVIRONMENT);
      this.tag(DamageTypes.SWEET_BERRY_BUSH, Tags.DamageTypes.IS_ENVIRONMENT);
      this.tag(DamageTypes.IN_WALL, Tags.DamageTypes.IS_ENVIRONMENT);
      this.tag(DamageTypes.CACTUS, Tags.DamageTypes.IS_PHYSICAL);
      this.tag(DamageTypes.STALAGMITE, Tags.DamageTypes.IS_PHYSICAL);
      this.tag(DamageTypes.FALLING_STALACTITE, Tags.DamageTypes.IS_PHYSICAL);
      this.tag(DamageTypes.FALLING_BLOCK, Tags.DamageTypes.IS_PHYSICAL);
      this.tag(DamageTypes.FALLING_ANVIL, Tags.DamageTypes.IS_PHYSICAL);
      this.tag(DamageTypes.CRAMMING, Tags.DamageTypes.IS_PHYSICAL);
      this.tag(DamageTypes.FLY_INTO_WALL, Tags.DamageTypes.IS_PHYSICAL);
      this.tag(DamageTypes.SWEET_BERRY_BUSH, Tags.DamageTypes.IS_PHYSICAL);
      this.tag(DamageTypes.FALL, Tags.DamageTypes.IS_PHYSICAL);
      this.tag(DamageTypes.STING, Tags.DamageTypes.IS_PHYSICAL);
      this.tag(DamageTypes.MOB_ATTACK, Tags.DamageTypes.IS_PHYSICAL);
      this.tag(DamageTypes.PLAYER_ATTACK, Tags.DamageTypes.IS_PHYSICAL);
      this.tag(DamageTypes.MOB_ATTACK_NO_AGGRO, Tags.DamageTypes.IS_PHYSICAL);
      this.tag(DamageTypes.ARROW, Tags.DamageTypes.IS_PHYSICAL);
      this.tag(DamageTypes.THROWN, Tags.DamageTypes.IS_PHYSICAL);
      this.tag(DamageTypes.TRIDENT, Tags.DamageTypes.IS_PHYSICAL);
      this.tag(DamageTypes.MOB_PROJECTILE, Tags.DamageTypes.IS_PHYSICAL);
      this.tag(DamageTypes.SONIC_BOOM, Tags.DamageTypes.IS_PHYSICAL);
      this.tag(DamageTypes.IN_WALL, Tags.DamageTypes.IS_PHYSICAL);
      this.tag(DamageTypes.GENERIC, Tags.DamageTypes.IS_PHYSICAL);
      this.tag(DamageTypes.GENERIC_KILL, Tags.DamageTypes.IS_TECHNICAL);
      this.tag(DamageTypes.OUTSIDE_BORDER, Tags.DamageTypes.IS_TECHNICAL);
      this.tag(DamageTypes.FELL_OUT_OF_WORLD, Tags.DamageTypes.IS_TECHNICAL);
      this.tag(Tags.DamageTypes.NO_FLINCH);
      this.tagWithOptionalLegacy(Tags.DamageTypes.IS_POISON);
      this.tagWithOptionalLegacy(Tags.DamageTypes.IS_WITHER);
      this.tagWithOptionalLegacy(Tags.DamageTypes.IS_MAGIC);
      this.tagWithOptionalLegacy(Tags.DamageTypes.IS_ENVIRONMENT);
      this.tagWithOptionalLegacy(Tags.DamageTypes.IS_PHYSICAL);
      this.tagWithOptionalLegacy(Tags.DamageTypes.IS_TECHNICAL);
      this.tagWithOptionalLegacy(Tags.DamageTypes.NO_FLINCH);
   }

   private TagAppender<ResourceKey<DamageType>, DamageType> addAsVanilla(ResourceKey<DamageType> entry) {
      final List<TagBuilder> builders = new ArrayList<>();
      this.vanillaBuilders.forEach((location, tagBuilder) -> {
         if (tagBuilder.build().stream().anyMatch(tagEntry -> tagEntry.verifyIfPresent(element -> element.equals(entry.identifier()), tag -> false))) {
            builders.add(this.getOrCreateRawBuilder(TagKey.create(this.registryKey, location)));
         }
      });
      return TagAppender.forBuilder(new TagBuilder() {
         @Override
         public TagBuilder add(TagEntry entry) {
            builders.forEach(builder -> builder.add(entry));
            return super.add(entry);
         }
      });
   }

   @SafeVarargs
   private void tag(ResourceKey<DamageType> type, TagKey<DamageType>... tags) {
      for (TagKey<DamageType> key : tags) {
         this.tag(key).add(type);
      }
   }

   private TagAppender<ResourceKey<DamageType>, DamageType> tagWithOptionalLegacy(TagKey<DamageType> tag) {
      TagAppender<ResourceKey<DamageType>, DamageType> tagAppender = this.tag(tag);
      tagAppender.addOptionalTag(TagKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("forge", tag.location().getPath())));
      return tagAppender;
   }

   @Override
   public String getName() {
      return "NeoForge Damage Type Tags";
   }
}
