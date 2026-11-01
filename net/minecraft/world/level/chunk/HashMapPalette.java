package net.minecraft.world.level.chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.util.CrudeIncrementalIntIdentityHashBiMap;

public class HashMapPalette<T> implements Palette<T> {
   private final IdMap<T> registry;
   private final CrudeIncrementalIntIdentityHashBiMap<T> values;
   private final PaletteResize<T> resizeHandler;
   private final int bits;

   public HashMapPalette(IdMap<T> p_187908_, int p_187909_, PaletteResize<T> p_187910_, List<T> p_187911_) {
      this(p_187908_, p_187909_, p_187910_);
      p_187911_.forEach(this.values::add);
   }

   public HashMapPalette(IdMap<T> p_187904_, int p_187905_, PaletteResize<T> p_187906_) {
      this(p_187904_, p_187905_, p_187906_, CrudeIncrementalIntIdentityHashBiMap.create(1 << p_187905_));
   }

   private HashMapPalette(IdMap<T> p_199915_, int p_199916_, PaletteResize<T> p_199917_, CrudeIncrementalIntIdentityHashBiMap<T> p_199918_) {
      this.registry = p_199915_;
      this.bits = p_199916_;
      this.resizeHandler = p_199917_;
      this.values = p_199918_;
   }

   public static <A> Palette<A> create(int p_187913_, IdMap<A> p_187914_, PaletteResize<A> p_187915_, List<A> p_187916_) {
      return new HashMapPalette<>(p_187914_, p_187913_, p_187915_, p_187916_);
   }

   @Override
   public int idFor(T p_62673_) {
      int $$1 = this.values.getId(p_62673_);
      if ($$1 == -1) {
         $$1 = this.values.add(p_62673_);
         if ($$1 >= 1 << this.bits) {
            $$1 = this.resizeHandler.onResize(this.bits + 1, p_62673_);
         }
      }

      return $$1;
   }

   @Override
   public boolean maybeHas(Predicate<T> p_62675_) {
      for (int $$1 = 0; $$1 < this.getSize(); $$1++) {
         if (p_62675_.test(this.values.byId($$1))) {
            return true;
         }
      }

      return false;
   }

   @Override
   public T valueFor(int p_62671_) {
      T $$1 = this.values.byId(p_62671_);
      if ($$1 == null) {
         throw new MissingPaletteEntryException(p_62671_);
      } else {
         return $$1;
      }
   }

   @Override
   public void read(FriendlyByteBuf p_62679_) {
      this.values.clear();
      int $$1 = p_62679_.readVarInt();

      for (int $$2 = 0; $$2 < $$1; $$2++) {
         this.values.add(this.registry.byIdOrThrow(p_62679_.readVarInt()));
      }
   }

   @Override
   public void write(FriendlyByteBuf p_62684_) {
      int $$1 = this.getSize();
      p_62684_.writeVarInt($$1);

      for (int $$2 = 0; $$2 < $$1; $$2++) {
         p_62684_.writeVarInt(this.registry.getId(this.values.byId($$2)));
      }
   }

   @Override
   public int getSerializedSize() {
      int $$0 = VarInt.getByteSize(this.getSize());

      for (int $$1 = 0; $$1 < this.getSize(); $$1++) {
         $$0 += VarInt.getByteSize(this.registry.getId(this.values.byId($$1)));
      }

      return $$0;
   }

   public List<T> getEntries() {
      ArrayList<T> $$0 = new ArrayList<>();
      this.values.iterator().forEachRemaining($$0::add);
      return $$0;
   }

   @Override
   public int getSize() {
      return this.values.size();
   }

   @Override
   public Palette<T> copy() {
      return new HashMapPalette<>(this.registry, this.bits, this.resizeHandler, this.values.copy());
   }
}
