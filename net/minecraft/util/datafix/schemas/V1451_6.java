package net.minecraft.util.datafix.schemas;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.types.templates.Hook.HookFunction;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.fixes.References;

public class V1451_6 extends NamespacedSchema {
   public static final String SPECIAL_OBJECTIVE_MARKER = "_special";
   protected static final HookFunction UNPACK_OBJECTIVE_ID = new HookFunction() {
      public <T> T apply(DynamicOps<T> p_181096_, T p_181097_) {
         Dynamic<T> $$2 = new Dynamic(p_181096_, p_181097_);
         return (T)((Dynamic)DataFixUtils.orElse(
               $$2.get("CriteriaName")
                  .asString()
                  .result()
                  .map(p_181094_ -> {
                     int $$1 = p_181094_.indexOf(58);
                     if ($$1 < 0) {
                        return Pair.of("_special", p_181094_);
                     } else {
                        try {
                           ResourceLocation $$2x = ResourceLocation.bySeparator(p_181094_.substring(0, $$1), '.');
                           ResourceLocation $$3 = ResourceLocation.bySeparator(p_181094_.substring($$1 + 1), '.');
                           return Pair.of($$2x.toString(), $$3.toString());
                        } catch (Exception var4) {
                           return Pair.of("_special", p_181094_);
                        }
                     }
                  })
                  .map(
                     p_181092_ -> $$2.set(
                        "CriteriaType",
                        $$2.createMap(
                           ImmutableMap.of(
                              $$2.createString("type"),
                              $$2.createString((String)p_181092_.getFirst()),
                              $$2.createString("id"),
                              $$2.createString((String)p_181092_.getSecond())
                           )
                        )
                     )
                  ),
               $$2
            ))
            .getValue();
      }
   };
   protected static final HookFunction REPACK_OBJECTIVE_ID = new HookFunction() {
      public <T> T apply(DynamicOps<T> p_181105_, T p_181106_) {
         Dynamic<T> $$2 = new Dynamic(p_181105_, p_181106_);
         Optional<Dynamic<T>> $$3 = $$2.get("CriteriaType")
            .get()
            .result()
            .flatMap(
               p_293835_ -> {
                  Optional<String> $$2x = p_293835_.get("type").asString().result();
                  Optional<String> $$3x = p_293835_.get("id").asString().result();
                  if ($$2x.isPresent() && $$3x.isPresent()) {
                     String $$4 = $$2x.get();
                     return $$4.equals("_special")
                        ? Optional.of($$2.createString($$3x.get()))
                        : Optional.of(p_293835_.createString(V1451_6.packNamespacedWithDot($$4) + ":" + V1451_6.packNamespacedWithDot($$3x.get())));
                  } else {
                     return Optional.empty();
                  }
               }
            );
         return (T)((Dynamic)DataFixUtils.orElse($$3.map(p_181101_ -> $$2.set("CriteriaName", p_181101_).remove("CriteriaType")), $$2)).getValue();
      }
   };

   public V1451_6(int p_17532_, Schema p_17533_) {
      super(p_17532_, p_17533_);
   }

   public void registerTypes(Schema p_17540_, Map<String, Supplier<TypeTemplate>> p_17541_, Map<String, Supplier<TypeTemplate>> p_17542_) {
      super.registerTypes(p_17540_, p_17541_, p_17542_);
      Supplier<TypeTemplate> $$3 = () -> DSL.compoundList(References.ITEM_NAME.in(p_17540_), DSL.constType(DSL.intType()));
      p_17540_.registerType(
         false,
         References.STATS,
         () -> DSL.optionalFields(
            "stats",
            DSL.optionalFields(
               new Pair[]{
                  Pair.of("minecraft:mined", DSL.compoundList(References.BLOCK_NAME.in(p_17540_), DSL.constType(DSL.intType()))),
                  Pair.of("minecraft:crafted", $$3.get()),
                  Pair.of("minecraft:used", $$3.get()),
                  Pair.of("minecraft:broken", $$3.get()),
                  Pair.of("minecraft:picked_up", $$3.get()),
                  Pair.of("minecraft:dropped", $$3.get()),
                  Pair.of("minecraft:killed", DSL.compoundList(References.ENTITY_NAME.in(p_17540_), DSL.constType(DSL.intType()))),
                  Pair.of("minecraft:killed_by", DSL.compoundList(References.ENTITY_NAME.in(p_17540_), DSL.constType(DSL.intType()))),
                  Pair.of("minecraft:custom", DSL.compoundList(DSL.constType(namespacedString()), DSL.constType(DSL.intType())))
               }
            )
         )
      );
      Map<String, Supplier<TypeTemplate>> $$4 = createCriterionTypes(p_17540_);
      p_17540_.registerType(
         false,
         References.OBJECTIVE,
         () -> DSL.hook(DSL.optionalFields("CriteriaType", DSL.taggedChoiceLazy("type", DSL.string(), $$4)), UNPACK_OBJECTIVE_ID, REPACK_OBJECTIVE_ID)
      );
   }

   protected static Map<String, Supplier<TypeTemplate>> createCriterionTypes(Schema p_181078_) {
      Supplier<TypeTemplate> $$1 = () -> DSL.optionalFields("id", References.ITEM_NAME.in(p_181078_));
      Supplier<TypeTemplate> $$2 = () -> DSL.optionalFields("id", References.BLOCK_NAME.in(p_181078_));
      Supplier<TypeTemplate> $$3 = () -> DSL.optionalFields("id", References.ENTITY_NAME.in(p_181078_));
      Map<String, Supplier<TypeTemplate>> $$4 = Maps.newHashMap();
      $$4.put("minecraft:mined", $$2);
      $$4.put("minecraft:crafted", $$1);
      $$4.put("minecraft:used", $$1);
      $$4.put("minecraft:broken", $$1);
      $$4.put("minecraft:picked_up", $$1);
      $$4.put("minecraft:dropped", $$1);
      $$4.put("minecraft:killed", $$3);
      $$4.put("minecraft:killed_by", $$3);
      $$4.put("minecraft:custom", () -> DSL.optionalFields("id", DSL.constType(namespacedString())));
      $$4.put("_special", () -> DSL.optionalFields("id", DSL.constType(DSL.string())));
      return $$4;
   }

   public static String packNamespacedWithDot(String p_294923_) {
      ResourceLocation $$1 = ResourceLocation.tryParse(p_294923_);
      return $$1 != null ? $$1.getNamespace() + "." + $$1.getPath() : p_294923_;
   }
}
