package net.minecraft.world.entity.npc.villager;

import com.google.common.collect.ImmutableSet;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

public record VillagerProfession(
   Component name,
   Predicate<Holder<PoiType>> heldJobSite,
   Predicate<Holder<PoiType>> acquirableJobSite,
   ImmutableSet<Item> requestedItems,
   ImmutableSet<Block> secondaryPoi,
   @Nullable SoundEvent workSound
) {
   public static final Predicate<Holder<PoiType>> ALL_ACQUIRABLE_JOBS = p_481582_ -> p_481582_.is(PoiTypeTags.ACQUIRABLE_JOB_SITE);
   public static final ResourceKey<VillagerProfession> NONE = createKey("none");
   public static final ResourceKey<VillagerProfession> ARMORER = createKey("armorer");
   public static final ResourceKey<VillagerProfession> BUTCHER = createKey("butcher");
   public static final ResourceKey<VillagerProfession> CARTOGRAPHER = createKey("cartographer");
   public static final ResourceKey<VillagerProfession> CLERIC = createKey("cleric");
   public static final ResourceKey<VillagerProfession> FARMER = createKey("farmer");
   public static final ResourceKey<VillagerProfession> FISHERMAN = createKey("fisherman");
   public static final ResourceKey<VillagerProfession> FLETCHER = createKey("fletcher");
   public static final ResourceKey<VillagerProfession> LEATHERWORKER = createKey("leatherworker");
   public static final ResourceKey<VillagerProfession> LIBRARIAN = createKey("librarian");
   public static final ResourceKey<VillagerProfession> MASON = createKey("mason");
   public static final ResourceKey<VillagerProfession> NITWIT = createKey("nitwit");
   public static final ResourceKey<VillagerProfession> SHEPHERD = createKey("shepherd");
   public static final ResourceKey<VillagerProfession> TOOLSMITH = createKey("toolsmith");
   public static final ResourceKey<VillagerProfession> WEAPONSMITH = createKey("weaponsmith");

   private static ResourceKey<VillagerProfession> createKey(String p_481847_) {
      return ResourceKey.create(Registries.VILLAGER_PROFESSION, Identifier.withDefaultNamespace(p_481847_));
   }

   private static VillagerProfession register(
      Registry<VillagerProfession> p_479266_, ResourceKey<VillagerProfession> p_478463_, ResourceKey<PoiType> p_481467_, @Nullable SoundEvent p_480290_
   ) {
      return register(p_479266_, p_478463_, p_481848_ -> p_481848_.is(p_481467_), p_481135_ -> p_481135_.is(p_481467_), p_480290_);
   }

   private static VillagerProfession register(
      Registry<VillagerProfession> p_481162_,
      ResourceKey<VillagerProfession> p_480375_,
      Predicate<Holder<PoiType>> p_482192_,
      Predicate<Holder<PoiType>> p_478432_,
      @Nullable SoundEvent p_480799_
   ) {
      return register(p_481162_, p_480375_, p_482192_, p_478432_, ImmutableSet.of(), ImmutableSet.of(), p_480799_);
   }

   private static VillagerProfession register(
      Registry<VillagerProfession> p_480558_,
      ResourceKey<VillagerProfession> p_478990_,
      ResourceKey<PoiType> p_479993_,
      ImmutableSet<Item> p_478498_,
      ImmutableSet<Block> p_482161_,
      @Nullable SoundEvent p_479031_
   ) {
      return register(p_480558_, p_478990_, p_479159_ -> p_479159_.is(p_479993_), p_480352_ -> p_480352_.is(p_479993_), p_478498_, p_482161_, p_479031_);
   }

   private static VillagerProfession register(
      Registry<VillagerProfession> p_481144_,
      ResourceKey<VillagerProfession> p_479833_,
      Predicate<Holder<PoiType>> p_481640_,
      Predicate<Holder<PoiType>> p_477931_,
      ImmutableSet<Item> p_479727_,
      ImmutableSet<Block> p_478425_,
      @Nullable SoundEvent p_478254_
   ) {
      return Registry.register(
         p_481144_,
         p_479833_,
         new VillagerProfession(
            Component.translatable("entity." + p_479833_.identifier().getNamespace() + ".villager." + p_479833_.identifier().getPath()),
            p_481640_,
            p_477931_,
            p_479727_,
            p_478425_,
            p_478254_
         )
      );
   }

   public static VillagerProfession bootstrap(Registry<VillagerProfession> p_481223_) {
      register(p_481223_, NONE, PoiType.NONE, ALL_ACQUIRABLE_JOBS, null);
      register(p_481223_, ARMORER, PoiTypes.ARMORER, SoundEvents.VILLAGER_WORK_ARMORER);
      register(p_481223_, BUTCHER, PoiTypes.BUTCHER, SoundEvents.VILLAGER_WORK_BUTCHER);
      register(p_481223_, CARTOGRAPHER, PoiTypes.CARTOGRAPHER, SoundEvents.VILLAGER_WORK_CARTOGRAPHER);
      register(p_481223_, CLERIC, PoiTypes.CLERIC, SoundEvents.VILLAGER_WORK_CLERIC);
      register(
         p_481223_,
         FARMER,
         PoiTypes.FARMER,
         ImmutableSet.of(Items.WHEAT, Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.BONE_MEAL),
         ImmutableSet.of(Blocks.FARMLAND),
         SoundEvents.VILLAGER_WORK_FARMER
      );
      register(p_481223_, FISHERMAN, PoiTypes.FISHERMAN, SoundEvents.VILLAGER_WORK_FISHERMAN);
      register(p_481223_, FLETCHER, PoiTypes.FLETCHER, SoundEvents.VILLAGER_WORK_FLETCHER);
      register(p_481223_, LEATHERWORKER, PoiTypes.LEATHERWORKER, SoundEvents.VILLAGER_WORK_LEATHERWORKER);
      register(p_481223_, LIBRARIAN, PoiTypes.LIBRARIAN, SoundEvents.VILLAGER_WORK_LIBRARIAN);
      register(p_481223_, MASON, PoiTypes.MASON, SoundEvents.VILLAGER_WORK_MASON);
      register(p_481223_, NITWIT, PoiType.NONE, PoiType.NONE, null);
      register(p_481223_, SHEPHERD, PoiTypes.SHEPHERD, SoundEvents.VILLAGER_WORK_SHEPHERD);
      register(p_481223_, TOOLSMITH, PoiTypes.TOOLSMITH, SoundEvents.VILLAGER_WORK_TOOLSMITH);
      return register(p_481223_, WEAPONSMITH, PoiTypes.WEAPONSMITH, SoundEvents.VILLAGER_WORK_WEAPONSMITH);
   }
}
