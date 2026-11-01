package net.neoforged.neoforge.fluids;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.wrappers.CauldronWrapper;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class CauldronFluidContent {
   public final Block block;
   public final Fluid fluid;
   public final int totalAmount;
   public final int maxLevel;
   @Nullable
   public final IntegerProperty levelProperty;
   private static final Map<Block, CauldronFluidContent> BLOCK_TO_CAULDRON = new IdentityHashMap<>();
   private static final Map<Fluid, CauldronFluidContent> FLUID_TO_CAULDRON = new IdentityHashMap<>();

   public int currentLevel(BlockState state) {
      if (this.fluid == Fluids.EMPTY) {
         return 0;
      } else {
         return this.levelProperty == null ? 1 : state.getValue(this.levelProperty);
      }
   }

   private CauldronFluidContent(Block block, Fluid fluid, int totalAmount, int maxLevel, @Nullable IntegerProperty levelProperty) {
      this.block = block;
      this.fluid = fluid;
      this.totalAmount = totalAmount;
      this.maxLevel = maxLevel;
      this.levelProperty = levelProperty;
   }

   @Nullable
   public static CauldronFluidContent getForBlock(Block block) {
      return BLOCK_TO_CAULDRON.get(block);
   }

   @Nullable
   public static CauldronFluidContent getForFluid(Fluid fluid) {
      return FLUID_TO_CAULDRON.get(fluid);
   }

   @Internal
   public static void init() {
      RegisterCauldronFluidContentEvent registerEvent = new RegisterCauldronFluidContentEvent();
      registerEvent.register(Blocks.CAULDRON, Fluids.EMPTY, 1000, null);
      registerEvent.register(Blocks.WATER_CAULDRON, Fluids.WATER, 1000, LayeredCauldronBlock.LEVEL);
      registerEvent.register(Blocks.LAVA_CAULDRON, Fluids.LAVA, 1000, null);
      ModLoader.postEvent(registerEvent);
   }

   static void register(Block block, Fluid fluid, int totalAmount, @Nullable IntegerProperty levelProperty) {
      if (BLOCK_TO_CAULDRON.get(block) != null) {
         throw new IllegalArgumentException("Duplicate cauldron registration for block %s.".formatted(block));
      } else if (FLUID_TO_CAULDRON.get(fluid) != null) {
         throw new IllegalArgumentException("Duplicate cauldron registration for fluid %s.".formatted(fluid));
      } else if (totalAmount <= 0) {
         throw new IllegalArgumentException("Cauldron total amount %d should be positive.".formatted(totalAmount));
      } else {
         CauldronFluidContent data;
         if (levelProperty == null) {
            data = new CauldronFluidContent(block, fluid, totalAmount, 1, null);
         } else {
            Collection<Integer> levels = levelProperty.getPossibleValues();
            if (levels.isEmpty()) {
               throw new IllegalArgumentException("Cauldron should have at least one possible level.");
            }

            int minLevel = Integer.MAX_VALUE;
            int maxLevel = 0;

            for (int level : levels) {
               minLevel = Math.min(minLevel, level);
               maxLevel = Math.max(maxLevel, level);
            }

            if (minLevel != 1) {
               throw new IllegalStateException("Minimum level should be 1, and maximum level should be >= 1.");
            }

            data = new CauldronFluidContent(block, fluid, totalAmount, maxLevel, levelProperty);
         }

         BLOCK_TO_CAULDRON.put(block, data);
         FLUID_TO_CAULDRON.put(fluid, data);
      }
   }

   @Internal
   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      if (BLOCK_TO_CAULDRON.isEmpty()) {
         throw new IllegalStateException("CauldronFluidContent.init() should have been called before the capability event!");
      } else {
         for (Block block : BLOCK_TO_CAULDRON.keySet()) {
            event.registerBlock(Capabilities.FluidHandler.BLOCK, (level, pos, state, be, context) -> new CauldronWrapper(level, pos), block);
         }
      }
   }
}
