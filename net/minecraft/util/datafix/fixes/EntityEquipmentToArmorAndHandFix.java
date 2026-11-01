package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Lists;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Dynamic;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class EntityEquipmentToArmorAndHandFix extends DataFix {
   public EntityEquipmentToArmorAndHandFix(Schema p_15417_, boolean p_15418_) {
      super(p_15417_, p_15418_);
   }

   public TypeRewriteRule makeRule() {
      return this.cap(this.getInputSchema().getTypeRaw(References.ITEM_STACK));
   }

   private <IS> TypeRewriteRule cap(Type<IS> p_15427_) {
      Type<Pair<Either<List<IS>, Unit>, Dynamic<?>>> $$1 = DSL.and(DSL.optional(DSL.field("Equipment", DSL.list(p_15427_))), DSL.remainderType());
      Type<Pair<Either<List<IS>, Unit>, Pair<Either<List<IS>, Unit>, Pair<Either<IS, Unit>, Dynamic<?>>>>> $$2 = DSL.and(
         DSL.optional(DSL.field("ArmorItems", DSL.list(p_15427_))),
         DSL.optional(DSL.field("HandItems", DSL.list(p_15427_))),
         DSL.optional(DSL.field("body_armor_item", p_15427_)),
         DSL.remainderType()
      );
      OpticFinder<Pair<Either<List<IS>, Unit>, Dynamic<?>>> $$3 = DSL.typeFinder($$1);
      OpticFinder<List<IS>> $$4 = DSL.fieldFinder("Equipment", DSL.list(p_15427_));
      return this.fixTypeEverywhereTyped(
         "EntityEquipmentToArmorAndHandFix",
         this.getInputSchema().getType(References.ENTITY),
         this.getOutputSchema().getType(References.ENTITY),
         p_326763_ -> {
            Either<List<IS>, Unit> $$5 = Either.right(DSL.unit());
            Either<List<IS>, Unit> $$6 = Either.right(DSL.unit());
            Either<IS, Unit> $$7 = Either.right(DSL.unit());
            Dynamic<?> $$8 = (Dynamic<?>)p_326763_.getOrCreate(DSL.remainderFinder());
            Optional<List<IS>> $$9 = p_326763_.getOptional($$4);
            if ($$9.isPresent()) {
               List<IS> $$10 = $$9.get();
               IS $$11 = (IS)((Pair)p_15427_.read($$8.emptyMap())
                     .result()
                     .orElseThrow(() -> new IllegalStateException("Could not parse newly created empty itemstack.")))
                  .getFirst();
               if (!$$10.isEmpty()) {
                  $$5 = Either.left(Lists.newArrayList(new Object[]{$$10.get(0), $$11}));
               }

               if ($$10.size() > 1) {
                  List<IS> $$12 = Lists.newArrayList(new Object[]{$$11, $$11, $$11, $$11});

                  for (int $$13 = 1; $$13 < Math.min($$10.size(), 5); $$13++) {
                     $$12.set($$13 - 1, $$10.get($$13));
                  }

                  $$6 = Either.left($$12);
               }
            }

            Dynamic<?> $$14 = $$8;
            Optional<? extends Stream<? extends Dynamic<?>>> $$15 = $$8.get("DropChances").asStreamOpt().result();
            if ($$15.isPresent()) {
               Iterator<? extends Dynamic<?>> $$16 = Stream.concat((Stream<? extends Dynamic<?>>)$$15.get(), Stream.generate(() -> $$14.createInt(0)))
                  .iterator();
               float $$17 = $$16.next().asFloat(0.0F);
               if ($$8.get("HandDropChances").result().isEmpty()) {
                  Dynamic<?> $$18 = $$8.createList(Stream.of($$17, 0.0F).map($$8::createFloat));
                  $$8 = $$8.set("HandDropChances", $$18);
               }

               if ($$8.get("ArmorDropChances").result().isEmpty()) {
                  Dynamic<?> $$19 = $$8.createList(
                     Stream.of($$16.next().asFloat(0.0F), $$16.next().asFloat(0.0F), $$16.next().asFloat(0.0F), $$16.next().asFloat(0.0F))
                        .map($$8::createFloat)
                  );
                  $$8 = $$8.set("ArmorDropChances", $$19);
               }

               $$8 = $$8.remove("DropChances");
            }

            return p_326763_.set($$3, $$2, Pair.of($$5, Pair.of($$6, Pair.of($$7, $$8))));
         }
      );
   }
}
