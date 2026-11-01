package net.minecraft.core.cauldron;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;

public interface CauldronInteraction {
   Map<String, CauldronInteraction.InteractionMap> INTERACTIONS = new Object2ObjectArrayMap();
   Codec<CauldronInteraction.InteractionMap> CODEC = Codec.stringResolver(CauldronInteraction.InteractionMap::name, INTERACTIONS::get);
   CauldronInteraction.InteractionMap EMPTY = newInteractionMap("empty");
   CauldronInteraction.InteractionMap WATER = newInteractionMap("water");
   CauldronInteraction.InteractionMap LAVA = newInteractionMap("lava");
   CauldronInteraction.InteractionMap POWDER_SNOW = newInteractionMap("powder_snow");

   static CauldronInteraction.InteractionMap newInteractionMap(String p_304841_) {
      Object2ObjectOpenHashMap<Item, CauldronInteraction> object2objectopenhashmap = new Object2ObjectOpenHashMap();
      object2objectopenhashmap.defaultReturnValue(
         (CauldronInteraction)(p_359359_, p_359360_, p_359361_, p_359362_, p_359363_, p_359364_) -> InteractionResult.TRY_WITH_EMPTY_HAND
      );
      CauldronInteraction.InteractionMap cauldroninteraction$interactionmap = new CauldronInteraction.InteractionMap(p_304841_, object2objectopenhashmap);
      INTERACTIONS.put(p_304841_, cauldroninteraction$interactionmap);
      return cauldroninteraction$interactionmap;
   }

   InteractionResult interact(BlockState var1, Level var2, BlockPos var3, Player var4, InteractionHand var5, ItemStack var6);

   static void bootStrap() {
      Map<Item, CauldronInteraction> map = EMPTY.map();
      addDefaultInteractions(map);
      map.put(Items.POTION, (p_465918_, p_465919_, p_465920_, p_465921_, p_465922_, p_465923_) -> {
         PotionContents potioncontents = p_465923_.get(DataComponents.POTION_CONTENTS);
         if (potioncontents != null && potioncontents.is(Potions.WATER)) {
            if (!p_465919_.isClientSide()) {
               Item item = p_465923_.getItem();
               p_465921_.setItemInHand(p_465922_, ItemUtils.createFilledResult(p_465923_, p_465921_, new ItemStack(Items.GLASS_BOTTLE)));
               p_465921_.awardStat(Stats.USE_CAULDRON);
               p_465921_.awardStat(Stats.ITEM_USED.get(item));
               p_465919_.setBlockAndUpdate(p_465920_, Blocks.WATER_CAULDRON.defaultBlockState());
               p_465919_.playSound(null, p_465920_, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
               p_465919_.gameEvent(null, GameEvent.FLUID_PLACE, p_465920_);
            }

            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
         }
      });
      Map<Item, CauldronInteraction> map1 = WATER.map();
      addDefaultInteractions(map1);
      map1.put(
         Items.BUCKET,
         (p_359353_, p_359354_, p_359355_, p_359356_, p_359357_, p_359358_) -> fillBucket(
            p_359353_,
            p_359354_,
            p_359355_,
            p_359356_,
            p_359357_,
            p_359358_,
            new ItemStack(Items.WATER_BUCKET),
            p_175660_ -> p_175660_.getValue(LayeredCauldronBlock.LEVEL) == 3,
            SoundEvents.BUCKET_FILL
         )
      );
      map1.put(Items.GLASS_BOTTLE, (p_465930_, p_465931_, p_465932_, p_465933_, p_465934_, p_465935_) -> {
         if (!p_465931_.isClientSide()) {
            Item item = p_465935_.getItem();
            p_465933_.setItemInHand(p_465934_, ItemUtils.createFilledResult(p_465935_, p_465933_, PotionContents.createItemStack(Items.POTION, Potions.WATER)));
            p_465933_.awardStat(Stats.USE_CAULDRON);
            p_465933_.awardStat(Stats.ITEM_USED.get(item));
            LayeredCauldronBlock.lowerFillLevel(p_465930_, p_465931_, p_465932_);
            p_465931_.playSound(null, p_465932_, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            p_465931_.gameEvent(null, GameEvent.FLUID_PICKUP, p_465932_);
         }

         return InteractionResult.SUCCESS;
      });
      map1.put(Items.POTION, (p_465924_, p_465925_, p_465926_, p_465927_, p_465928_, p_465929_) -> {
         if (p_465924_.getValue(LayeredCauldronBlock.LEVEL) == 3) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
         } else {
            PotionContents potioncontents = p_465929_.get(DataComponents.POTION_CONTENTS);
            if (potioncontents != null && potioncontents.is(Potions.WATER)) {
               if (!p_465925_.isClientSide()) {
                  p_465927_.setItemInHand(p_465928_, ItemUtils.createFilledResult(p_465929_, p_465927_, new ItemStack(Items.GLASS_BOTTLE)));
                  p_465927_.awardStat(Stats.USE_CAULDRON);
                  p_465927_.awardStat(Stats.ITEM_USED.get(p_465929_.getItem()));
                  p_465925_.setBlockAndUpdate(p_465926_, p_465924_.cycle(LayeredCauldronBlock.LEVEL));
                  p_465925_.playSound(null, p_465926_, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                  p_465925_.gameEvent(null, GameEvent.FLUID_PLACE, p_465926_);
               }

               return InteractionResult.SUCCESS;
            } else {
               return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
         }
      });
      map1.put(Items.LEATHER_BOOTS, CauldronInteraction::dyedItemIteration);
      map1.put(Items.LEATHER_LEGGINGS, CauldronInteraction::dyedItemIteration);
      map1.put(Items.LEATHER_CHESTPLATE, CauldronInteraction::dyedItemIteration);
      map1.put(Items.LEATHER_HELMET, CauldronInteraction::dyedItemIteration);
      map1.put(Items.LEATHER_HORSE_ARMOR, CauldronInteraction::dyedItemIteration);
      map1.put(Items.WOLF_ARMOR, CauldronInteraction::dyedItemIteration);
      map1.put(Items.WHITE_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.GRAY_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.BLACK_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.BLUE_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.BROWN_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.CYAN_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.GREEN_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.LIGHT_BLUE_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.LIGHT_GRAY_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.LIME_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.MAGENTA_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.ORANGE_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.PINK_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.PURPLE_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.RED_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.YELLOW_BANNER, CauldronInteraction::bannerInteraction);
      map1.put(Items.WHITE_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.GRAY_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.BLACK_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.BLUE_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.BROWN_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.CYAN_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.GREEN_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.LIGHT_BLUE_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.LIGHT_GRAY_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.LIME_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.MAGENTA_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.ORANGE_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.PINK_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.PURPLE_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.RED_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      map1.put(Items.YELLOW_SHULKER_BOX, CauldronInteraction::shulkerBoxInteraction);
      Map<Item, CauldronInteraction> map2 = LAVA.map();
      map2.put(
         Items.BUCKET,
         (p_359341_, p_359342_, p_359343_, p_359344_, p_359345_, p_359346_) -> fillBucket(
            p_359341_, p_359342_, p_359343_, p_359344_, p_359345_, p_359346_, new ItemStack(Items.LAVA_BUCKET), p_175651_ -> true, SoundEvents.BUCKET_FILL_LAVA
         )
      );
      addDefaultInteractions(map2);
      Map<Item, CauldronInteraction> map3 = POWDER_SNOW.map();
      map3.put(
         Items.BUCKET,
         (p_359347_, p_359348_, p_359349_, p_359350_, p_359351_, p_359352_) -> fillBucket(
            p_359347_,
            p_359348_,
            p_359349_,
            p_359350_,
            p_359351_,
            p_359352_,
            new ItemStack(Items.POWDER_SNOW_BUCKET),
            p_175627_ -> p_175627_.getValue(LayeredCauldronBlock.LEVEL) == 3,
            SoundEvents.BUCKET_FILL_POWDER_SNOW
         )
      );
      addDefaultInteractions(map3);
   }

   static void addDefaultInteractions(Map<Item, CauldronInteraction> p_175648_) {
      p_175648_.put(Items.LAVA_BUCKET, CauldronInteraction::fillLavaInteraction);
      p_175648_.put(Items.WATER_BUCKET, CauldronInteraction::fillWaterInteraction);
      p_175648_.put(Items.POWDER_SNOW_BUCKET, CauldronInteraction::fillPowderSnowInteraction);
   }

   static InteractionResult fillBucket(
      BlockState p_175636_,
      Level p_175637_,
      BlockPos p_175638_,
      Player p_175639_,
      InteractionHand p_175640_,
      ItemStack p_175641_,
      ItemStack p_175642_,
      Predicate<BlockState> p_175643_,
      SoundEvent p_175644_
   ) {
      if (!p_175643_.test(p_175636_)) {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      } else {
         if (!p_175637_.isClientSide()) {
            Item item = p_175641_.getItem();
            p_175639_.setItemInHand(p_175640_, ItemUtils.createFilledResult(p_175641_, p_175639_, p_175642_));
            p_175639_.awardStat(Stats.USE_CAULDRON);
            p_175639_.awardStat(Stats.ITEM_USED.get(item));
            p_175637_.setBlockAndUpdate(p_175638_, Blocks.CAULDRON.defaultBlockState());
            p_175637_.playSound(null, p_175638_, p_175644_, SoundSource.BLOCKS, 1.0F, 1.0F);
            p_175637_.gameEvent(null, GameEvent.FLUID_PICKUP, p_175638_);
         }

         return InteractionResult.SUCCESS;
      }
   }

   static InteractionResult emptyBucket(
      Level p_175619_, BlockPos p_175620_, Player p_175621_, InteractionHand p_175622_, ItemStack p_175623_, BlockState p_175624_, SoundEvent p_175625_
   ) {
      if (!p_175619_.isClientSide()) {
         Item item = p_175623_.getItem();
         p_175621_.setItemInHand(p_175622_, ItemUtils.createFilledResult(p_175623_, p_175621_, new ItemStack(Items.BUCKET)));
         p_175621_.awardStat(Stats.FILL_CAULDRON);
         p_175621_.awardStat(Stats.ITEM_USED.get(item));
         p_175619_.setBlockAndUpdate(p_175620_, p_175624_);
         p_175619_.playSound(null, p_175620_, p_175625_, SoundSource.BLOCKS, 1.0F, 1.0F);
         p_175619_.gameEvent(null, GameEvent.FLUID_PLACE, p_175620_);
      }

      return InteractionResult.SUCCESS;
   }

   private static InteractionResult fillWaterInteraction(
      BlockState p_362955_, Level p_363378_, BlockPos p_363149_, Player p_364905_, InteractionHand p_361406_, ItemStack p_361413_
   ) {
      return emptyBucket(
         p_363378_,
         p_363149_,
         p_364905_,
         p_361406_,
         p_361413_,
         Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3),
         SoundEvents.BUCKET_EMPTY
      );
   }

   private static InteractionResult fillLavaInteraction(
      BlockState p_360382_, Level p_365170_, BlockPos p_361603_, Player p_365069_, InteractionHand p_360292_, ItemStack p_364835_
   ) {
      return (InteractionResult)(isUnderWater(p_365170_, p_361603_)
         ? InteractionResult.CONSUME
         : emptyBucket(p_365170_, p_361603_, p_365069_, p_360292_, p_364835_, Blocks.LAVA_CAULDRON.defaultBlockState(), SoundEvents.BUCKET_EMPTY_LAVA));
   }

   private static InteractionResult fillPowderSnowInteraction(
      BlockState p_361682_, Level p_362979_, BlockPos p_361996_, Player p_361273_, InteractionHand p_361726_, ItemStack p_361338_
   ) {
      return (InteractionResult)(isUnderWater(p_362979_, p_361996_)
         ? InteractionResult.CONSUME
         : emptyBucket(
            p_362979_,
            p_361996_,
            p_361273_,
            p_361726_,
            p_361338_,
            Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3),
            SoundEvents.BUCKET_EMPTY_POWDER_SNOW
         ));
   }

   private static InteractionResult shulkerBoxInteraction(
      BlockState p_360793_, Level p_365272_, BlockPos p_361319_, Player p_365441_, InteractionHand p_361085_, ItemStack p_365174_
   ) {
      Block block = Block.byItem(p_365174_.getItem());
      if (!(block instanceof ShulkerBoxBlock)) {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      } else {
         if (!p_365272_.isClientSide()) {
            ItemStack itemstack = p_365174_.transmuteCopy(Blocks.SHULKER_BOX, 1);
            p_365441_.setItemInHand(p_361085_, ItemUtils.createFilledResult(p_365174_, p_365441_, itemstack, false));
            p_365441_.awardStat(Stats.CLEAN_SHULKER_BOX);
            LayeredCauldronBlock.lowerFillLevel(p_360793_, p_365272_, p_361319_);
         }

         return InteractionResult.SUCCESS;
      }
   }

   private static InteractionResult bannerInteraction(
      BlockState p_365039_, Level p_360792_, BlockPos p_364354_, Player p_362948_, InteractionHand p_363589_, ItemStack p_365256_
   ) {
      BannerPatternLayers bannerpatternlayers = p_365256_.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
      if (bannerpatternlayers.layers().isEmpty()) {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      } else {
         if (!p_360792_.isClientSide()) {
            ItemStack itemstack = p_365256_.copyWithCount(1);
            itemstack.set(DataComponents.BANNER_PATTERNS, bannerpatternlayers.removeLast());
            p_362948_.setItemInHand(p_363589_, ItemUtils.createFilledResult(p_365256_, p_362948_, itemstack, false));
            p_362948_.awardStat(Stats.CLEAN_BANNER);
            LayeredCauldronBlock.lowerFillLevel(p_365039_, p_360792_, p_364354_);
         }

         return InteractionResult.SUCCESS;
      }
   }

   private static InteractionResult dyedItemIteration(
      BlockState p_364488_, Level p_363832_, BlockPos p_363503_, Player p_362213_, InteractionHand p_360757_, ItemStack p_360363_
   ) {
      if (!p_360363_.is(ItemTags.DYEABLE)) {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      } else if (!p_360363_.has(DataComponents.DYED_COLOR)) {
         return InteractionResult.TRY_WITH_EMPTY_HAND;
      } else {
         if (!p_363832_.isClientSide()) {
            p_360363_.remove(DataComponents.DYED_COLOR);
            p_362213_.awardStat(Stats.CLEAN_ARMOR);
            LayeredCauldronBlock.lowerFillLevel(p_364488_, p_363832_, p_363503_);
         }

         return InteractionResult.SUCCESS;
      }
   }

   private static boolean isUnderWater(Level p_364930_, BlockPos p_363494_) {
      FluidState fluidstate = p_364930_.getFluidState(p_363494_.above());
      return fluidstate.is(FluidTags.WATER);
   }

   public record InteractionMap(String name, Map<Item, CauldronInteraction> map) {
   }
}
