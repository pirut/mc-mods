package net.neoforged.neoforge.common.util;

import com.mojang.logging.LogUtils;
import java.lang.ref.WeakReference;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class BlockSnapshot {
   private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("neoforge.debugBlockSnapshot", "false"));
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ResourceKey<Level> dim;
   private final BlockPos pos;
   private final int flags;
   private final BlockState state;
   @Nullable
   private final CompoundTag nbt;
   private WeakReference<LevelAccessor> level;
   @Nullable
   private String toString = null;

   private BlockSnapshot(ResourceKey<Level> dim, LevelAccessor level, BlockPos pos, BlockState state, @Nullable CompoundTag nbt, int flags) {
      this.dim = dim;
      this.pos = pos.immutable();
      this.state = state;
      this.flags = flags;
      this.nbt = nbt;
      this.level = new WeakReference<>(level);
      if (DEBUG) {
         LOGGER.debug("Created " + this.toString());
      }
   }

   public static BlockSnapshot create(ResourceKey<Level> dim, LevelAccessor level, BlockPos pos, int flag) {
      return new BlockSnapshot(dim, level, pos, level.getBlockState(pos), getBlockEntityTag(level, pos), flag);
   }

   public static BlockSnapshot create(ResourceKey<Level> dim, LevelAccessor level, BlockPos pos) {
      return create(dim, level, pos, 3);
   }

   public ResourceKey<Level> getDimension() {
      return this.dim;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public int getFlags() {
      return this.flags;
   }

   @Nullable
   public CompoundTag getTag() {
      return this.nbt;
   }

   public BlockState getState() {
      return this.state;
   }

   @Nullable
   public LevelAccessor getLevel() {
      LevelAccessor level = this.level.get();
      if (level == null) {
         level = ServerLifecycleHooks.getCurrentServer().getLevel(this.dim);
         this.level = new WeakReference<>(level);
      }

      return level;
   }

   public BlockState getCurrentState() {
      LevelAccessor level = this.getLevel();
      return level == null ? Blocks.AIR.defaultBlockState() : level.getBlockState(this.pos);
   }

   @Nullable
   public BlockEntity recreateBlockEntity(HolderLookup.Provider provider) {
      return this.getTag() != null ? BlockEntity.loadStatic(this.getPos(), this.getState(), this.getTag(), provider) : null;
   }

   public boolean restoreToLocation(LevelAccessor level, BlockPos pos, int flags) {
      BlockState replaced = this.getState();
      if (!level.setBlock(pos, replaced, flags)) {
         return false;
      } else {
         if (level instanceof Level realLevel) {
            BlockState current = this.getCurrentState();
            realLevel.sendBlockUpdated(pos, current, replaced, flags);
         }

         this.restoreBlockEntity(level, pos);
         if (DEBUG) {
            LOGGER.debug("Restored " + this.toString());
         }

         return true;
      }
   }

   public boolean restore(int flags) {
      return this.restoreToLocation(this.getLevel(), this.getPos(), flags);
   }

   public boolean restore() {
      return this.restore(this.getFlags());
   }

   public boolean restoreBlockEntity(LevelAccessor level, BlockPos pos) {
      BlockEntity be = null;
      if (this.getTag() != null) {
         be = level.getBlockEntity(pos);
         if (be != null) {
            boolean var5;
            try (ProblemReporter.ScopedCollector problems = new ProblemReporter.ScopedCollector(be.problemPath(), LOGGER)) {
               be.loadWithComponents(TagValueInput.create(problems, level.registryAccess(), this.getTag()));
               be.setChanged();
               var5 = true;
            }

            return var5;
         }
      }

      return false;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else if (obj != null && this.getClass() == obj.getClass()) {
         BlockSnapshot other = (BlockSnapshot)obj;
         return this.dim.equals(other.dim)
            && this.pos.equals(other.pos)
            && this.state == other.state
            && this.flags == other.flags
            && Objects.equals(this.nbt, other.nbt);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int hash = 7;
      hash = 73 * hash + this.dim.hashCode();
      hash = 73 * hash + this.pos.hashCode();
      hash = 73 * hash + this.state.hashCode();
      hash = 73 * hash + this.flags;
      return 73 * hash + Objects.hashCode(this.getTag());
   }

   @Override
   public String toString() {
      if (this.toString == null) {
         this.toString = "BlockSnapshot[Level:"
            + this.dim.identifier()
            + ",Pos: "
            + this.pos
            + ",State: "
            + this.state
            + ",Flags: "
            + this.flags
            + ",NBT: "
            + (this.nbt == null ? "null" : this.nbt.toString())
            + "]";
      }

      return this.toString;
   }

   @Nullable
   private static CompoundTag getBlockEntityTag(LevelAccessor level, BlockPos pos) {
      BlockEntity be = level.getBlockEntity(pos);
      return be == null ? null : be.saveWithFullMetadata(level.registryAccess());
   }
}
