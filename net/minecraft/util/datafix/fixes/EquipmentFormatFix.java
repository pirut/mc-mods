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
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class EquipmentFormatFix extends DataFix {
   public EquipmentFormatFix(Schema p_397502_) {
      super(p_397502_, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getTypeRaw(References.ITEM_STACK);
      Type<?> type1 = this.getOutputSchema().getTypeRaw(References.ITEM_STACK);
      OpticFinder<?> opticfinder = type.findField("id");
      return this.fix(type, type1, opticfinder);
   }

   private <ItemStackOld, ItemStackNew> TypeRewriteRule fix(Type<ItemStackOld> p_397976_, Type<ItemStackNew> p_397681_, OpticFinder<?> p_397996_) {
      Type<Pair<String, Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<ItemStackOld, Unit>, Either<ItemStackOld, Unit>>>>>> type = DSL.named(
         References.ENTITY_EQUIPMENT.typeName(),
         DSL.and(
            DSL.optional(DSL.field("ArmorItems", DSL.list(p_397976_))),
            DSL.optional(DSL.field("HandItems", DSL.list(p_397976_))),
            DSL.optional(DSL.field("body_armor_item", p_397976_)),
            DSL.optional(DSL.field("saddle", p_397976_))
         )
      );
      Type<Pair<String, Either<Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Pair<Either<ItemStackNew, Unit>, Dynamic<?>>>>>>>>>, Unit>>> type1 = DSL.named(
         References.ENTITY_EQUIPMENT.typeName(),
         DSL.optional(
            DSL.field(
               "equipment",
               DSL.and(
                  DSL.optional(DSL.field("mainhand", p_397681_)),
                  DSL.optional(DSL.field("offhand", p_397681_)),
                  DSL.optional(DSL.field("feet", p_397681_)),
                  DSL.and(
                     DSL.optional(DSL.field("legs", p_397681_)),
                     DSL.optional(DSL.field("chest", p_397681_)),
                     DSL.optional(DSL.field("head", p_397681_)),
                     DSL.and(DSL.optional(DSL.field("body", p_397681_)), DSL.optional(DSL.field("saddle", p_397681_)), DSL.remainderType())
                  )
               )
            )
         )
      );
      if (!type.equals(this.getInputSchema().getType(References.ENTITY_EQUIPMENT))) {
         throw new IllegalStateException("Input entity_equipment type does not match expected");
      } else if (!type1.equals(this.getOutputSchema().getType(References.ENTITY_EQUIPMENT))) {
         throw new IllegalStateException("Output entity_equipment type does not match expected");
      } else {
         return this.fixTypeEverywhere(
            "EquipmentFormatFix",
            type,
            type1,
            p_397464_ -> {
               Predicate<ItemStackOld> predicate = p_397199_ -> {
                  Typed<ItemStackOld> typed = new Typed(p_397976_, p_397464_, p_397199_);
                  return typed.getOptional(p_397996_).isEmpty();
               };
               return p_397594_ -> {
                  String s = (String)p_397594_.getFirst();
                  Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<ItemStackOld, Unit>, Either<ItemStackOld, Unit>>>> pair = (Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<List<ItemStackOld>, Unit>, Pair<Either<ItemStackOld, Unit>, Either<ItemStackOld, Unit>>>>)p_397594_.getSecond();
                  List<ItemStackOld> list = (List<ItemStackOld>)((Either)pair.getFirst()).map(Function.identity(), p_397225_ -> List.of());
                  List<ItemStackOld> list1 = (List<ItemStackOld>)((Either)((Pair)pair.getSecond()).getFirst()).map(Function.identity(), p_398024_ -> List.of());
                  Either<ItemStackOld, Unit> either = (Either<ItemStackOld, Unit>)((Pair)((Pair)pair.getSecond()).getSecond()).getFirst();
                  Either<ItemStackOld, Unit> either1 = (Either<ItemStackOld, Unit>)((Pair)((Pair)pair.getSecond()).getSecond()).getSecond();
                  Either<ItemStackOld, Unit> either2 = getItemFromList(0, list, predicate);
                  Either<ItemStackOld, Unit> either3 = getItemFromList(1, list, predicate);
                  Either<ItemStackOld, Unit> either4 = getItemFromList(2, list, predicate);
                  Either<ItemStackOld, Unit> either5 = getItemFromList(3, list, predicate);
                  Either<ItemStackOld, Unit> either6 = getItemFromList(0, list1, predicate);
                  Either<ItemStackOld, Unit> either7 = getItemFromList(1, list1, predicate);
                  return areAllEmpty(either, either1, either2, either3, either4, either5, either6, either7)
                     ? Pair.of(s, Either.right(Unit.INSTANCE))
                     : Pair.of(
                        s,
                        Either.left(
                           Pair.of(
                              either6,
                              Pair.of(
                                 either7,
                                 Pair.of(
                                    either2, Pair.of(either3, Pair.of(either4, Pair.of(either5, Pair.of(either, Pair.of(either1, new Dynamic(p_397464_))))))
                                 )
                              )
                           )
                        )
                     );
               };
            }
         );
      }
   }

   @SafeVarargs
   private static boolean areAllEmpty(Either<?, Unit>... p_397988_) {
      for (Either<?, Unit> either : p_397988_) {
         if (either.right().isEmpty()) {
            return false;
         }
      }

      return true;
   }

   private static <ItemStack> Either<ItemStack, Unit> getItemFromList(int p_397085_, List<ItemStack> p_397811_, Predicate<ItemStack> p_396985_) {
      if (p_397085_ >= p_397811_.size()) {
         return Either.right(Unit.INSTANCE);
      } else {
         ItemStack itemstack = p_397811_.get(p_397085_);
         return p_396985_.test(itemstack) ? Either.right(Unit.INSTANCE) : Either.left(itemstack);
      }
   }
}
