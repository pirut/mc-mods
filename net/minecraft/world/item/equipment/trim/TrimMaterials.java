package net.minecraft.world.item.equipment.trim;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ProvidesTrimMaterial;

public class TrimMaterials {
   public static final ResourceKey<TrimMaterial> QUARTZ = registryKey("quartz");
   public static final ResourceKey<TrimMaterial> IRON = registryKey("iron");
   public static final ResourceKey<TrimMaterial> NETHERITE = registryKey("netherite");
   public static final ResourceKey<TrimMaterial> REDSTONE = registryKey("redstone");
   public static final ResourceKey<TrimMaterial> COPPER = registryKey("copper");
   public static final ResourceKey<TrimMaterial> GOLD = registryKey("gold");
   public static final ResourceKey<TrimMaterial> EMERALD = registryKey("emerald");
   public static final ResourceKey<TrimMaterial> DIAMOND = registryKey("diamond");
   public static final ResourceKey<TrimMaterial> LAPIS = registryKey("lapis");
   public static final ResourceKey<TrimMaterial> AMETHYST = registryKey("amethyst");
   public static final ResourceKey<TrimMaterial> RESIN = registryKey("resin");

   public static void bootstrap(BootstrapContext<TrimMaterial> p_371627_) {
      register(p_371627_, QUARTZ, Style.EMPTY.withColor(14931140), MaterialAssetGroup.QUARTZ);
      register(p_371627_, IRON, Style.EMPTY.withColor(15527148), MaterialAssetGroup.IRON);
      register(p_371627_, NETHERITE, Style.EMPTY.withColor(6445145), MaterialAssetGroup.NETHERITE);
      register(p_371627_, REDSTONE, Style.EMPTY.withColor(9901575), MaterialAssetGroup.REDSTONE);
      register(p_371627_, COPPER, Style.EMPTY.withColor(11823181), MaterialAssetGroup.COPPER);
      register(p_371627_, GOLD, Style.EMPTY.withColor(14594349), MaterialAssetGroup.GOLD);
      register(p_371627_, EMERALD, Style.EMPTY.withColor(1155126), MaterialAssetGroup.EMERALD);
      register(p_371627_, DIAMOND, Style.EMPTY.withColor(7269586), MaterialAssetGroup.DIAMOND);
      register(p_371627_, LAPIS, Style.EMPTY.withColor(4288151), MaterialAssetGroup.LAPIS);
      register(p_371627_, AMETHYST, Style.EMPTY.withColor(10116294), MaterialAssetGroup.AMETHYST);
      register(p_371627_, RESIN, Style.EMPTY.withColor(16545810), MaterialAssetGroup.RESIN);
   }

   public static Optional<Holder<TrimMaterial>> getFromIngredient(HolderLookup.Provider p_371665_, ItemStack p_371700_) {
      ProvidesTrimMaterial providestrimmaterial = p_371700_.get(DataComponents.PROVIDES_TRIM_MATERIAL);
      return providestrimmaterial != null ? providestrimmaterial.unwrap(p_371665_) : Optional.empty();
   }

   private static void register(BootstrapContext<TrimMaterial> p_371763_, ResourceKey<TrimMaterial> p_371867_, Style p_371730_, MaterialAssetGroup p_399962_) {
      Component component = Component.translatable(Util.makeDescriptionId("trim_material", p_371867_.identifier())).withStyle(p_371730_);
      p_371763_.register(p_371867_, new TrimMaterial(p_399962_, component));
   }

   private static ResourceKey<TrimMaterial> registryKey(String p_371483_) {
      return ResourceKey.create(Registries.TRIM_MATERIAL, Identifier.withDefaultNamespace(p_371483_));
   }
}
