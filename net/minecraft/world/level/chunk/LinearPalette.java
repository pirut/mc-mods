package net.minecraft.world.level.chunk;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import org.apache.commons.lang3.Validate;

public class LinearPalette<T> implements Palette<T> {
   private final IdMap<T> registry;
   private final T[] values;
   private final PaletteResize<T> resizeHandler;
   private final int bits;
   private int size;

   private LinearPalette(IdMap<T> p_188015_, int p_188016_, PaletteResize<T> p_188017_, List<T> p_188018_) {
      this.registry = p_188015_;
      this.values = (T[])(new Object[1 << p_188016_]);
      this.bits = p_188016_;
      this.resizeHandler = p_188017_;
      Validate.isTrue(
         p_188018_.size() <= this.values.length,
         "Can't initialize LinearPalette of size %d with %d entries",
         new Object[]{this.values.length, p_188018_.size()}
      );

      for (int $$4 = 0; $$4 < p_188018_.size(); $$4++) {
         this.values[$$4] = p_188018_.get($$4);
      }

      this.size = p_188018_.size();
   }

   private LinearPalette(IdMap<T> p_199921_, T[] p_199922_, PaletteResize<T> p_199923_, int p_199924_, int p_199925_) {
      this.registry = p_199921_;
      this.values = p_199922_;
      this.resizeHandler = p_199923_;
      this.bits = p_199924_;
      this.size = p_199925_;
   }

   public static <A> Palette<A> create(int p_188020_, IdMap<A> p_188021_, PaletteResize<A> p_188022_, List<A> p_188023_) {
      return new LinearPalette<>(p_188021_, p_188020_, p_188022_, p_188023_);
   }

   @Override
   public int idFor(T p_63040_) {
      for (int $$1 = 0; $$1 < this.size; $$1++) {
         if (this.values[$$1] == p_63040_) {
            return $$1;
         }
      }

      int $$2 = this.size;
      if ($$2 < this.values.length) {
         this.values[$$2] = p_63040_;
         this.size++;
         return $$2;
      } else {
         return this.resizeHandler.onResize(this.bits + 1, p_63040_);
      }
   }

   @Override
   public boolean maybeHas(Predicate<T> p_63042_) {
      for (int $$1 = 0; $$1 < this.size; $$1++) {
         if (p_63042_.test(this.values[$$1])) {
            return true;
         }
      }

      return false;
   }

   @Override
   public T valueFor(int p_63038_) {
      if (p_63038_ >= 0 && p_63038_ < this.size) {
         return this.values[p_63038_];
      } else {
         throw new MissingPaletteEntryException(p_63038_);
      }
   }

   @Override
   public void read(FriendlyByteBuf p_63046_) {
      this.size = p_63046_.readVarInt();

      for (int $$1 = 0; $$1 < this.size; $$1++) {
         this.values[$$1] = this.registry.byIdOrThrow(p_63046_.readVarInt());
      }
   }

   @Override
   public void write(FriendlyByteBuf p_63049_) {
      p_63049_.writeVarInt(this.size);

      for (int $$1 = 0; $$1 < this.size; $$1++) {
         p_63049_.writeVarInt(this.registry.getId(this.values[$$1]));
      }
   }

   @Override
   public int getSerializedSize() {
      int $$0 = VarInt.getByteSize(this.getSize());

      for (int $$1 = 0; $$1 < this.getSize(); $$1++) {
         $$0 += VarInt.getByteSize(this.registry.getId(this.values[$$1]));
      }

      return $$0;
   }

   @Override
   public int getSize() {
      return this.size;
   }

   @Override
   public Palette<T> copy() {
      return new LinearPalette<>(this.registry, (T[])((Object[])this.values.clone()), this.resizeHandler, this.bits, this.size);
   }
}
