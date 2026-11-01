package net.minecraft.world.entity.animal.wolf;

import net.minecraft.core.ClientAsset;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.variant.BiomeCheck;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public class WolfVariants {
   public static final ResourceKey<WolfVariant> PALE = createKey("pale");
   public static final ResourceKey<WolfVariant> SPOTTED = createKey("spotted");
   public static final ResourceKey<WolfVariant> SNOWY = createKey("snowy");
   public static final ResourceKey<WolfVariant> BLACK = createKey("black");
   public static final ResourceKey<WolfVariant> ASHEN = createKey("ashen");
   public static final ResourceKey<WolfVariant> RUSTY = createKey("rusty");
   public static final ResourceKey<WolfVariant> WOODS = createKey("woods");
   public static final ResourceKey<WolfVariant> CHESTNUT = createKey("chestnut");
   public static final ResourceKey<WolfVariant> STRIPED = createKey("striped");
   public static final ResourceKey<WolfVariant> DEFAULT = PALE;

   private static ResourceKey<WolfVariant> createKey(String p_406224_) {
      return ResourceKey.create(Registries.WOLF_VARIANT, Identifier.withDefaultNamespace(p_406224_));
   }

   private static void register(BootstrapContext<WolfVariant> p_406316_, ResourceKey<WolfVariant> p_406242_, String p_406301_, ResourceKey<Biome> p_406284_) {
      register(p_406316_, p_406242_, p_406301_, highPrioBiome(HolderSet.direct(p_406316_.lookup(Registries.BIOME).getOrThrow(p_406284_))));
   }

   private static void register(BootstrapContext<WolfVariant> p_406329_, ResourceKey<WolfVariant> p_406365_, String p_406220_, TagKey<Biome> p_406376_) {
      register(p_406329_, p_406365_, p_406220_, highPrioBiome(p_406329_.lookup(Registries.BIOME).getOrThrow(p_406376_)));
   }

   private static SpawnPrioritySelectors highPrioBiome(HolderSet<Biome> p_406327_) {
      return SpawnPrioritySelectors.single(new BiomeCheck(p_406327_), 1);
   }

   private static void register(BootstrapContext<WolfVariant> p_406335_, ResourceKey<WolfVariant> p_406253_, String p_406319_, SpawnPrioritySelectors p_406264_) {
      Identifier identifier = Identifier.withDefaultNamespace("entity/wolf/" + p_406319_);
      Identifier identifier1 = Identifier.withDefaultNamespace("entity/wolf/" + p_406319_ + "_tame");
      Identifier identifier2 = Identifier.withDefaultNamespace("entity/wolf/" + p_406319_ + "_angry");
      p_406335_.register(
         p_406253_,
         new WolfVariant(
            new WolfVariant.AssetInfo(
               new ClientAsset.ResourceTexture(identifier), new ClientAsset.ResourceTexture(identifier1), new ClientAsset.ResourceTexture(identifier2)
            ),
            p_406264_
         )
      );
   }

   public static void bootstrap(BootstrapContext<WolfVariant> p_406256_) {
      register(p_406256_, PALE, "wolf", SpawnPrioritySelectors.fallback(0));
      register(p_406256_, SPOTTED, "wolf_spotted", BiomeTags.IS_SAVANNA);
      register(p_406256_, SNOWY, "wolf_snowy", Biomes.GROVE);
      register(p_406256_, BLACK, "wolf_black", Biomes.OLD_GROWTH_PINE_TAIGA);
      register(p_406256_, ASHEN, "wolf_ashen", Biomes.SNOWY_TAIGA);
      register(p_406256_, RUSTY, "wolf_rusty", BiomeTags.IS_JUNGLE);
      register(p_406256_, WOODS, "wolf_woods", Biomes.FOREST);
      register(p_406256_, CHESTNUT, "wolf_chestnut", Biomes.OLD_GROWTH_SPRUCE_TAIGA);
      register(p_406256_, STRIPED, "wolf_striped", BiomeTags.IS_BADLANDS);
   }
}
