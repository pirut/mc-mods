package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.function.Function;

public class ChunkRenamesFix extends DataFix {
   public ChunkRenamesFix(Schema p_185100_) {
      super(p_185100_, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.CHUNK);
      OpticFinder<?> $$1 = $$0.findField("Level");
      OpticFinder<?> $$2 = $$1.type().findField("Structures");
      Type<?> $$3 = this.getOutputSchema().getType(References.CHUNK);
      Type<?> $$4 = $$3.findFieldType("structures");
      return this.fixTypeEverywhereTyped("Chunk Renames; purge Level-tag", $$0, $$3, p_199427_ -> {
         Typed<?> $$4x = p_199427_.getTyped($$1);
         Typed<?> $$5 = appendChunkName($$4x);
         $$5 = $$5.set(DSL.remainderFinder(), mergeRemainders(p_199427_, (Dynamic)$$4x.get(DSL.remainderFinder())));
         $$5 = renameField($$5, "TileEntities", "block_entities");
         $$5 = renameField($$5, "TileTicks", "block_ticks");
         $$5 = renameField($$5, "Entities", "entities");
         $$5 = renameField($$5, "Sections", "sections");
         $$5 = $$5.updateTyped($$2, $$4, p_185128_ -> renameField(p_185128_, "Starts", "starts"));
         $$5 = renameField($$5, "Structures", "structures");
         return $$5.update(DSL.remainderFinder(), p_199429_ -> p_199429_.remove("Level"));
      });
   }

   private static Typed<?> renameField(Typed<?> p_185112_, String p_185113_, String p_185114_) {
      return renameFieldHelper(p_185112_, p_185113_, p_185114_, p_185112_.getType().findFieldType(p_185113_))
         .update(DSL.remainderFinder(), p_199439_ -> p_199439_.remove(p_185113_));
   }

   private static <A> Typed<?> renameFieldHelper(Typed<?> p_185116_, String p_185117_, String p_185118_, Type<A> p_185119_) {
      Type<Either<A, Unit>> $$4 = DSL.optional(DSL.field(p_185117_, p_185119_));
      Type<Either<A, Unit>> $$5 = DSL.optional(DSL.field(p_185118_, p_185119_));
      return p_185116_.update($$4.finder(), $$5, Function.identity());
   }

   private static <A> Typed<Pair<String, A>> appendChunkName(Typed<A> p_185107_) {
      return new Typed(DSL.named("chunk", p_185107_.getType()), p_185107_.getOps(), Pair.of("chunk", p_185107_.getValue()));
   }

   private static <T> Dynamic<T> mergeRemainders(Typed<?> p_185109_, Dynamic<T> p_185110_) {
      DynamicOps<T> $$2 = p_185110_.getOps();
      Dynamic<T> $$3 = ((Dynamic)p_185109_.get(DSL.remainderFinder())).convert($$2);
      DataResult<T> $$4 = $$2.getMap(p_185110_.getValue()).flatMap(p_199433_ -> $$2.mergeToMap($$3.getValue(), p_199433_));
      return $$4.result().map(p_199436_ -> new Dynamic($$2, p_199436_)).orElse(p_185110_);
   }
}
