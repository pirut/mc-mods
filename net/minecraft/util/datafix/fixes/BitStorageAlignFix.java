package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List.ListType;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.stream.LongStream;
import net.minecraft.util.Mth;

public class BitStorageAlignFix extends DataFix {
   private static final int BIT_TO_LONG_SHIFT = 6;
   private static final int SECTION_WIDTH = 16;
   private static final int SECTION_HEIGHT = 16;
   private static final int SECTION_SIZE = 4096;
   private static final int HEIGHTMAP_BITS = 9;
   private static final int HEIGHTMAP_SIZE = 256;

   public BitStorageAlignFix(Schema p_14736_) {
      super(p_14736_, false);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.CHUNK);
      Type<?> $$1 = $$0.findFieldType("Level");
      OpticFinder<?> $$2 = DSL.fieldFinder("Level", $$1);
      OpticFinder<?> $$3 = $$2.type().findField("Sections");
      Type<?> $$4 = ((ListType)$$3.type()).getElement();
      OpticFinder<?> $$5 = DSL.typeFinder($$4);
      Type<Pair<String, Dynamic<?>>> $$6 = DSL.named(References.BLOCK_STATE.typeName(), DSL.remainderType());
      OpticFinder<List<Pair<String, Dynamic<?>>>> $$7 = DSL.fieldFinder("Palette", DSL.list($$6));
      return this.fixTypeEverywhereTyped(
         "BitStorageAlignFix",
         $$0,
         this.getOutputSchema().getType(References.CHUNK),
         p_14749_ -> p_14749_.updateTyped($$2, p_145120_ -> this.updateHeightmaps(updateSections($$3, $$5, $$7, p_145120_)))
      );
   }

   private Typed<?> updateHeightmaps(Typed<?> p_14763_) {
      return p_14763_.update(
         DSL.remainderFinder(),
         p_14765_ -> p_14765_.update(
            "Heightmaps", p_145113_ -> p_145113_.updateMapValues(p_145110_ -> p_145110_.mapSecond(p_145123_ -> updateBitStorage(p_14765_, p_145123_, 256, 9)))
         )
      );
   }

   private static Typed<?> updateSections(
      OpticFinder<?> p_14751_, OpticFinder<?> p_14752_, OpticFinder<List<Pair<String, Dynamic<?>>>> p_14753_, Typed<?> p_14754_
   ) {
      return p_14754_.updateTyped(
         p_14751_,
         p_14758_ -> p_14758_.updateTyped(
            p_14752_,
            p_145103_ -> {
               int $$2 = p_145103_.getOptional(p_14753_).map(p_145115_ -> Math.max(4, DataFixUtils.ceillog2(p_145115_.size()))).orElse(0);
               return $$2 != 0 && !Mth.isPowerOfTwo($$2)
                  ? p_145103_.update(
                     DSL.remainderFinder(), p_145100_ -> p_145100_.update("BlockStates", p_145107_ -> updateBitStorage(p_145100_, p_145107_, 4096, $$2))
                  )
                  : p_145103_;
            }
         )
      );
   }

   private static Dynamic<?> updateBitStorage(Dynamic<?> p_14777_, Dynamic<?> p_14778_, int p_14779_, int p_14780_) {
      long[] $$4 = p_14778_.asLongStream().toArray();
      long[] $$5 = addPadding(p_14779_, p_14780_, $$4);
      return p_14777_.createLongList(LongStream.of($$5));
   }

   public static long[] addPadding(int p_14738_, int p_14739_, long[] p_14740_) {
      int $$3 = p_14740_.length;
      if ($$3 == 0) {
         return p_14740_;
      } else {
         long $$4 = (1L << p_14739_) - 1L;
         int $$5 = 64 / p_14739_;
         int $$6 = (p_14738_ + $$5 - 1) / $$5;
         long[] $$7 = new long[$$6];
         int $$8 = 0;
         int $$9 = 0;
         long $$10 = 0L;
         int $$11 = 0;
         long $$12 = p_14740_[0];
         long $$13 = $$3 > 1 ? p_14740_[1] : 0L;

         for (int $$14 = 0; $$14 < p_14738_; $$14++) {
            int $$15 = $$14 * p_14739_;
            int $$16 = $$15 >> 6;
            int $$17 = ($$14 + 1) * p_14739_ - 1 >> 6;
            int $$18 = $$15 ^ $$16 << 6;
            if ($$16 != $$11) {
               $$12 = $$13;
               $$13 = $$16 + 1 < $$3 ? p_14740_[$$16 + 1] : 0L;
               $$11 = $$16;
            }

            long $$19;
            if ($$16 == $$17) {
               $$19 = $$12 >>> $$18 & $$4;
            } else {
               int $$20 = 64 - $$18;
               $$19 = ($$12 >>> $$18 | $$13 << $$20) & $$4;
            }

            int $$22 = $$9 + p_14739_;
            if ($$22 >= 64) {
               $$7[$$8++] = $$10;
               $$10 = $$19;
               $$9 = p_14739_;
            } else {
               $$10 |= $$19 << $$9;
               $$9 = $$22;
            }
         }

         if ($$10 != 0L) {
            $$7[$$8] = $$10;
         }

         return $$7;
      }
   }
}
