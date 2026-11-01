package net.minecraft.world.level.chunk;

import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class LevelChunkSection {
   public static final int SECTION_WIDTH = 16;
   public static final int SECTION_HEIGHT = 16;
   public static final int SECTION_SIZE = 4096;
   public static final int BIOME_CONTAINER_BITS = 2;
   private short nonEmptyBlockCount;
   private short tickingBlockCount;
   private short tickingFluidCount;
   private final PalettedContainer<BlockState> states;
   private PalettedContainerRO<Holder<Biome>> biomes;

   private LevelChunkSection(LevelChunkSection section) {
      this.nonEmptyBlockCount = section.nonEmptyBlockCount;
      this.tickingBlockCount = section.tickingBlockCount;
      this.tickingFluidCount = section.tickingFluidCount;
      this.states = section.states.copy();
      this.biomes = section.biomes.copy();
   }

   public LevelChunkSection(PalettedContainer<BlockState> states, PalettedContainerRO<Holder<Biome>> biomes) {
      this.states = states;
      this.biomes = biomes;
      this.recalcBlockCounts();
   }

   public LevelChunkSection(PalettedContainerFactory palettedContainerFactory) {
      this.states = palettedContainerFactory.createForBlockStates();
      this.biomes = palettedContainerFactory.createForBiomes();
   }

   public BlockState getBlockState(int x, int y, int z) {
      return this.states.get(x, y, z);
   }

   public FluidState getFluidState(int x, int y, int z) {
      return this.states.get(x, y, z).getFluidState();
   }

   public void acquire() {
      this.states.acquire();
   }

   public void release() {
      this.states.release();
   }

   public BlockState setBlockState(int x, int y, int z, BlockState state) {
      return this.setBlockState(x, y, z, state, true);
   }

   public BlockState setBlockState(int x, int y, int z, BlockState state, boolean useLocks) {
      BlockState blockState;
      if (useLocks) {
         blockState = this.states.getAndSet(x, y, z, state);
      } else {
         blockState = this.states.getAndSetUnchecked(x, y, z, state);
      }

      FluidState fluidState = blockState.getFluidState();
      FluidState fluidState2 = state.getFluidState();
      if (!blockState.isAir()) {
         this.nonEmptyBlockCount--;
         if (blockState.isRandomlyTicking()) {
            this.tickingBlockCount--;
         }
      }

      if (!fluidState.isEmpty()) {
         this.tickingFluidCount--;
      }

      if (!state.isAir()) {
         this.nonEmptyBlockCount++;
         if (state.isRandomlyTicking()) {
            this.tickingBlockCount++;
         }
      }

      if (!fluidState2.isEmpty()) {
         this.tickingFluidCount++;
      }

      return blockState;
   }

   public boolean hasOnlyAir() {
      return this.nonEmptyBlockCount == 0;
   }

   public boolean isRandomlyTicking() {
      return this.isRandomlyTickingBlocks() || this.isRandomlyTickingFluids();
   }

   public boolean isRandomlyTickingBlocks() {
      return this.tickingBlockCount > 0;
   }

   public boolean isRandomlyTickingFluids() {
      return this.tickingFluidCount > 0;
   }

   public void recalcBlockCounts() {
      class BlockCounter implements PalettedContainer.CountConsumer<BlockState> {
         public int nonEmptyBlockCount;
         public int tickingBlockCount;
         public int tickingFluidCount;

         public void accept(BlockState blockState, int i) {
            FluidState fluidState = blockState.getFluidState();
            if (!blockState.isAir()) {
               this.nonEmptyBlockCount += i;
               if (blockState.isRandomlyTicking()) {
                  this.tickingBlockCount += i;
               }
            }

            if (!fluidState.isEmpty()) {
               this.nonEmptyBlockCount += i;
               if (fluidState.isRandomlyTicking()) {
                  this.tickingFluidCount += i;
               }
            }
         }
      }

      BlockCounter lv = new BlockCounter();
      this.states.count(lv);
      this.nonEmptyBlockCount = (short)lv.nonEmptyBlockCount;
      this.tickingBlockCount = (short)lv.tickingBlockCount;
      this.tickingFluidCount = (short)lv.tickingFluidCount;
   }

   public PalettedContainer<BlockState> getStates() {
      return this.states;
   }

   public PalettedContainerRO<Holder<Biome>> getBiomes() {
      return this.biomes;
   }

   public void read(FriendlyByteBuf buffer) {
      this.nonEmptyBlockCount = buffer.readShort();
      this.states.read(buffer);
      PalettedContainer<Holder<Biome>> palettedContainer = this.biomes.recreate();
      palettedContainer.read(buffer);
      this.biomes = palettedContainer;
   }

   public void readBiomes(FriendlyByteBuf buffer) {
      PalettedContainer<Holder<Biome>> palettedContainer = this.biomes.recreate();
      palettedContainer.read(buffer);
      this.biomes = palettedContainer;
   }

   public void write(FriendlyByteBuf buffer) {
      buffer.writeShort(this.nonEmptyBlockCount);
      this.states.write(buffer);
      this.biomes.write(buffer);
   }

   public int getSerializedSize() {
      return 2 + this.states.getSerializedSize() + this.biomes.getSerializedSize();
   }

   public boolean maybeHas(Predicate<BlockState> predicate) {
      return this.states.maybeHas(predicate);
   }

   public Holder<Biome> getNoiseBiome(int x, int y, int z) {
      return this.biomes.get(x, y, z);
   }

   public void fillBiomesFromNoise(BiomeResolver biomeResolver, Climate.Sampler climateSampler, int x, int y, int z) {
      PalettedContainer<Holder<Biome>> palettedContainer = this.biomes.recreate();
      int i = 4;

      for (int j = 0; j < 4; j++) {
         for (int k = 0; k < 4; k++) {
            for (int l = 0; l < 4; l++) {
               palettedContainer.getAndSetUnchecked(j, k, l, biomeResolver.getNoiseBiome(x + j, y + k, z + l, climateSampler));
            }
         }
      }

      this.biomes = palettedContainer;
   }

   public LevelChunkSection copy() {
      return new LevelChunkSection(this);
   }
}
