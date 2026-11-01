package net.neoforged.neoforge.fluids;

import com.google.common.collect.UnmodifiableIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.EventHooks;

public final class FluidInteractionRegistry {
   private static final Map<FluidType, List<FluidInteractionRegistry.InteractionInformation>> INTERACTIONS = new HashMap<>();

   public static synchronized void addInteraction(FluidType source, FluidInteractionRegistry.InteractionInformation interaction) {
      INTERACTIONS.computeIfAbsent(source, s -> new ArrayList<>()).add(interaction);
   }

   public static boolean canInteract(Level level, BlockPos pos) {
      FluidState state = level.getFluidState(pos);
      UnmodifiableIterator var3 = LiquidBlock.POSSIBLE_FLOW_DIRECTIONS.iterator();

      while (var3.hasNext()) {
         Direction direction = (Direction)var3.next();
         BlockPos relativePos = pos.relative(direction.getOpposite());

         for (FluidInteractionRegistry.InteractionInformation interaction : INTERACTIONS.getOrDefault(state.getFluidType(), Collections.emptyList())) {
            if (interaction.predicate().test(level, pos, relativePos, state)) {
               interaction.interaction().interact(level, pos, relativePos, state);
               return true;
            }
         }
      }

      return false;
   }

   static {
      addInteraction(
         NeoForgeMod.LAVA_TYPE.value(),
         new FluidInteractionRegistry.InteractionInformation(
            NeoForgeMod.WATER_TYPE.value(), fluidState -> fluidState.isSource() ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState()
         )
      );
      addInteraction(
         NeoForgeMod.LAVA_TYPE.value(),
         new FluidInteractionRegistry.InteractionInformation(
            (level, currentPos, relativePos, currentState) -> level.getBlockState(currentPos.below()).is(Blocks.SOUL_SOIL)
               && level.getBlockState(relativePos).is(Blocks.BLUE_ICE),
            Blocks.BASALT.defaultBlockState()
         )
      );
   }

   @FunctionalInterface
   public interface FluidInteraction {
      void interact(Level var1, BlockPos var2, BlockPos var3, FluidState var4);
   }

   @FunctionalInterface
   public interface HasFluidInteraction {
      boolean test(Level var1, BlockPos var2, BlockPos var3, FluidState var4);
   }

   public record InteractionInformation(FluidInteractionRegistry.HasFluidInteraction predicate, FluidInteractionRegistry.FluidInteraction interaction) {
      public InteractionInformation(FluidType type, BlockState state) {
         this(type, fluidState -> state);
      }

      public InteractionInformation(FluidInteractionRegistry.HasFluidInteraction predicate, BlockState state) {
         this(predicate, fluidState -> state);
      }

      public InteractionInformation(FluidType type, Function<FluidState, BlockState> getState) {
         this((level, currentPos, relativePos, currentState) -> level.getFluidState(relativePos).getFluidType() == type, getState);
      }

      public InteractionInformation(FluidInteractionRegistry.HasFluidInteraction predicate, Function<FluidState, BlockState> getState) {
         this(predicate, (level, currentPos, relativePos, currentState) -> {
            level.setBlockAndUpdate(currentPos, EventHooks.fireFluidPlaceBlockEvent(level, currentPos, currentPos, getState.apply(currentState)));
            level.levelEvent(1501, currentPos, 0);
         });
      }
   }
}
