package net.minecraft.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class RandomSequences extends SavedData {
   public static final Codec<RandomSequences> CODEC = RecordCodecBuilder.create(
      p_466510_ -> p_466510_.group(
            Codec.INT.fieldOf("salt").forGetter(RandomSequences::salt),
            Codec.BOOL.optionalFieldOf("include_world_seed", true).forGetter(RandomSequences::includeWorldSeed),
            Codec.BOOL.optionalFieldOf("include_sequence_id", true).forGetter(RandomSequences::includeSequenceId),
            Codec.unboundedMap(Identifier.CODEC, RandomSequence.CODEC).fieldOf("sequences").forGetter(p_400906_ -> p_400906_.sequences)
         )
         .apply(p_466510_, RandomSequences::new)
   );
   public static final SavedDataType<RandomSequences> TYPE = new SavedDataType<>(
      "random_sequences", RandomSequences::new, CODEC, DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES
   );
   private int salt;
   private boolean includeWorldSeed = true;
   private boolean includeSequenceId = true;
   private final Map<Identifier, RandomSequence> sequences = new Object2ObjectOpenHashMap();

   public RandomSequences() {
   }

   private RandomSequences(int p_455620_, boolean p_456282_, boolean p_455657_, Map<Identifier, RandomSequence> p_454870_) {
      this.salt = p_455620_;
      this.includeWorldSeed = p_456282_;
      this.includeSequenceId = p_455657_;
      this.sequences.putAll(p_454870_);
   }

   public RandomSource get(Identifier p_468210_, long p_454999_) {
      RandomSource randomsource = this.sequences.computeIfAbsent(p_468210_, p_466509_ -> this.createSequence(p_466509_, p_454999_)).random();
      return new RandomSequences.DirtyMarkingRandomSource(randomsource);
   }

   private RandomSequence createSequence(Identifier p_468223_, long p_456268_) {
      return this.createSequence(p_468223_, p_456268_, this.salt, this.includeWorldSeed, this.includeSequenceId);
   }

   private RandomSequence createSequence(Identifier p_467242_, long p_455488_, int p_296284_, boolean p_296271_, boolean p_295917_) {
      long i = (p_296271_ ? p_455488_ : 0L) ^ p_296284_;
      return new RandomSequence(i, p_295917_ ? Optional.of(p_467242_) : Optional.empty());
   }

   public void forAllSequences(BiConsumer<Identifier, RandomSequence> p_294252_) {
      this.sequences.forEach(p_294252_);
   }

   public void setSeedDefaults(int p_294511_, boolean p_294255_, boolean p_295889_) {
      this.salt = p_294511_;
      this.includeWorldSeed = p_294255_;
      this.includeSequenceId = p_295889_;
   }

   public int clear() {
      int i = this.sequences.size();
      this.sequences.clear();
      return i;
   }

   public void reset(Identifier p_467304_, long p_454957_) {
      this.sequences.put(p_467304_, this.createSequence(p_467304_, p_454957_));
   }

   public void reset(Identifier p_469400_, long p_455894_, int p_468447_, boolean p_469207_, boolean p_468316_) {
      this.sequences.put(p_469400_, this.createSequence(p_469400_, p_455894_, p_468447_, p_469207_, p_468316_));
   }

   private int salt() {
      return this.salt;
   }

   private boolean includeWorldSeed() {
      return this.includeWorldSeed;
   }

   private boolean includeSequenceId() {
      return this.includeSequenceId;
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
            return p_294463_ instanceof RandomSequences.DirtyMarkingRandomSource randomsequences$dirtymarkingrandomsource
               ? this.random.equals(randomsequences$dirtymarkingrandomsource.random)
               : false;
         }
      }
   }
}
