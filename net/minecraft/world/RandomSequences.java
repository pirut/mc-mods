package net.minecraft.world;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

public class RandomSequences extends SavedData {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final long worldSeed;
   private int salt;
   private boolean includeWorldSeed = true;
   private boolean includeSequenceId = true;
   private final Map<ResourceLocation, RandomSequence> sequences = new Object2ObjectOpenHashMap();

   public static SavedData.Factory<RandomSequences> factory(long p_294688_) {
      return new SavedData.Factory<>(
         () -> new RandomSequences(p_294688_), (p_293846_, p_324262_) -> load(p_294688_, p_293846_), DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
      );
   }

   public RandomSequences(long p_287622_) {
      this.worldSeed = p_287622_;
   }

   public RandomSource get(ResourceLocation p_287751_) {
      RandomSource $$1 = this.sequences.computeIfAbsent(p_287751_, this::createSequence).random();
      return new RandomSequences.DirtyMarkingRandomSource($$1);
   }

   private RandomSequence createSequence(ResourceLocation p_295796_) {
      return this.createSequence(p_295796_, this.salt, this.includeWorldSeed, this.includeSequenceId);
   }

   private RandomSequence createSequence(ResourceLocation p_295614_, int p_296284_, boolean p_296271_, boolean p_295917_) {
      long $$4 = (p_296271_ ? this.worldSeed : 0L) ^ p_296284_;
      return new RandomSequence($$4, p_295917_ ? Optional.of(p_295614_) : Optional.empty());
   }

   public void forAllSequences(BiConsumer<ResourceLocation, RandomSequence> p_294252_) {
      this.sequences.forEach(p_294252_);
   }

   public void setSeedDefaults(int p_294511_, boolean p_294255_, boolean p_295889_) {
      this.salt = p_294511_;
      this.includeWorldSeed = p_294255_;
      this.includeSequenceId = p_295889_;
   }

   @Override
   public CompoundTag save(CompoundTag p_287658_, HolderLookup.Provider p_323500_) {
      p_287658_.putInt("salt", this.salt);
      p_287658_.putBoolean("include_world_seed", this.includeWorldSeed);
      p_287658_.putBoolean("include_sequence_id", this.includeSequenceId);
      CompoundTag $$2 = new CompoundTag();
      this.sequences
         .forEach(
            (p_337697_, p_337698_) -> $$2.put(p_337697_.toString(), (Tag)RandomSequence.CODEC.encodeStart(NbtOps.INSTANCE, p_337698_).result().orElseThrow())
         );
      p_287658_.put("sequences", $$2);
      return p_287658_;
   }

   private static boolean getBooleanWithDefault(CompoundTag p_296109_, String p_295934_, boolean p_295516_) {
      return p_296109_.contains(p_295934_, 1) ? p_296109_.getBoolean(p_295934_) : p_295516_;
   }

   public static RandomSequences load(long p_287756_, CompoundTag p_287587_) {
      RandomSequences $$2 = new RandomSequences(p_287756_);
      $$2.setSeedDefaults(
         p_287587_.getInt("salt"), getBooleanWithDefault(p_287587_, "include_world_seed", true), getBooleanWithDefault(p_287587_, "include_sequence_id", true)
      );
      CompoundTag $$3 = p_287587_.getCompound("sequences");

      for (String $$5 : $$3.getAllKeys()) {
         try {
            RandomSequence $$6 = (RandomSequence)((Pair)RandomSequence.CODEC.decode(NbtOps.INSTANCE, $$3.get($$5)).result().get()).getFirst();
            $$2.sequences.put(ResourceLocation.parse($$5), $$6);
         } catch (Exception var9) {
            LOGGER.error("Failed to load random sequence {}", $$5, var9);
         }
      }

      return $$2;
   }

   public int clear() {
      int $$0 = this.sequences.size();
      this.sequences.clear();
      return $$0;
   }

   public void reset(ResourceLocation p_296099_) {
      this.sequences.put(p_296099_, this.createSequence(p_296099_));
   }

   public void reset(ResourceLocation p_294608_, int p_295700_, boolean p_296000_, boolean p_294735_) {
      this.sequences.put(p_294608_, this.createSequence(p_294608_, p_295700_, p_296000_, p_294735_));
   }

   class DirtyMarkingRandomSource implements RandomSource {
      private final RandomSource random;

      DirtyMarkingRandomSource(RandomSource p_295768_) {
         this.random = p_295768_;
      }

      @Override
      public RandomSource fork() {
         RandomSequences.this.setDirty();
         return this.random.fork();
      }

      @Override
      public PositionalRandomFactory forkPositional() {
         RandomSequences.this.setDirty();
         return this.random.forkPositional();
      }

      @Override
      public void setSeed(long p_295551_) {
         RandomSequences.this.setDirty();
         this.random.setSeed(p_295551_);
      }

      @Override
      public int nextInt() {
         RandomSequences.this.setDirty();
         return this.random.nextInt();
      }

      @Override
      public int nextInt(int p_294632_) {
         RandomSequences.this.setDirty();
         return this.random.nextInt(p_294632_);
      }

      @Override
      public long nextLong() {
         RandomSequences.this.setDirty();
         return this.random.nextLong();
      }

      @Override
      public boolean nextBoolean() {
         RandomSequences.this.setDirty();
         return this.random.nextBoolean();
      }

      @Override
      public float nextFloat() {
         RandomSequences.this.setDirty();
         return this.random.nextFloat();
      }

      @Override
      public double nextDouble() {
         RandomSequences.this.setDirty();
         return this.random.nextDouble();
      }

      @Override
      public double nextGaussian() {
         RandomSequences.this.setDirty();
         return this.random.nextGaussian();
      }

      @Override
      public boolean equals(Object p_294463_) {
         if (this == p_294463_) {
            return true;
         } else {
            return p_294463_ instanceof RandomSequences.DirtyMarkingRandomSource $$1 ? this.random.equals($$1.random) : false;
         }
      }
   }
}
