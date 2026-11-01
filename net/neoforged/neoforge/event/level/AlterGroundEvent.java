package net.neoforged.neoforge.event.level;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus.Internal;

public class AlterGroundEvent extends Event {
   private final TreeDecorator.Context ctx;
   private final List<BlockPos> positions;
   private AlterGroundEvent.StateProvider provider;

   @Internal
   public AlterGroundEvent(TreeDecorator.Context ctx, List<BlockPos> positions, AlterGroundEvent.StateProvider provider) {
      this.ctx = ctx;
      this.positions = Collections.unmodifiableList(positions);
      this.provider = provider;
   }

   public TreeDecorator.Context getContext() {
      return this.ctx;
   }

   public List<BlockPos> getPositions() {
      return this.positions;
   }

   public AlterGroundEvent.StateProvider getStateProvider() {
      return this.provider;
   }

   public void setStateProvider(AlterGroundEvent.StateProvider provider) {
      this.provider = Objects.requireNonNull(provider);
   }

   @FunctionalInterface
   public interface StateProvider {
      BlockState getState(RandomSource var1, BlockPos var2);
   }
}
