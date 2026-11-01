package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.stream.Stream;
import net.minecraft.Util;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class WorldGenSettingsHeightAndBiomeFix extends DataFix {
   private static final String NAME = "WorldGenSettingsHeightAndBiomeFix";
   public static final String WAS_PREVIOUSLY_INCREASED_KEY = "has_increased_height_already";

   public WorldGenSettingsHeightAndBiomeFix(Schema p_185174_) {
      super(p_185174_, true);
   }

   protected TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.WORLD_GEN_SETTINGS);
      OpticFinder<?> $$1 = $$0.findField("dimensions");
      Type<?> $$2 = this.getOutputSchema().getType(References.WORLD_GEN_SETTINGS);
      Type<?> $$3 = $$2.findFieldType("dimensions");
      return this.fixTypeEverywhereTyped(
         "WorldGenSettingsHeightAndBiomeFix",
         $$0,
         $$2,
         p_311573_ -> {
            OptionalDynamic<?> $$3x = ((Dynamic)p_311573_.get(DSL.remainderFinder())).get("has_increased_height_already");
            boolean $$4 = $$3x.result().isEmpty();
            boolean $$5 = $$3x.asBoolean(true);
            return p_311573_.update(DSL.remainderFinder(), p_185205_ -> p_185205_.remove("has_increased_height_already"))
               .updateTyped(
                  $$1,
                  $$3,
                  p_311577_ -> Util.writeAndReadTypedOrThrow(
                     p_311577_,
                     $$3,
                     p_311580_ -> p_311580_.update(
                        "minecraft:overworld",
                        p_185194_ -> p_185194_.update(
                           "generator",
                           p_185201_ -> {
                              String $$3xx = p_185201_.get("type").asString("");
                              if ("minecraft:noise".equals($$3xx)) {
                                 MutableBoolean $$4x = new MutableBoolean();
                                 p_185201_ = p_185201_.update(
                                    "biome_source",
                                    p_185185_ -> {
                                       String $$3xxx = p_185185_.get("type").asString("");
                                       if ("minecraft:vanilla_layered".equals($$3xxx) || $$4 && "minecraft:multi_noise".equals($$3xxx)) {
                                          if (p_185185_.get("large_biomes").asBoolean(false)) {
                                             $$4x.setTrue();
                                          }

                                          return p_185185_.createMap(
                                             ImmutableMap.of(
                                                p_185185_.createString("preset"),
                                                p_185185_.createString("minecraft:overworld"),
                                                p_185185_.createString("type"),
                                                p_185185_.createString("minecraft:multi_noise")
                                             )
                                          );
                                       } else {
                                          return p_185185_;
                                       }
                                    }
                                 );
                                 return $$4x.booleanValue()
                                    ? p_185201_.update(
                                       "settings",
                                       p_185203_ -> "minecraft:overworld".equals(p_185203_.asString(""))
                                          ? p_185203_.createString("minecraft:large_biomes")
                                          : p_185203_
                                    )
                                    : p_185201_;
                              } else if ("minecraft:flat".equals($$3xx)) {
                                 return $$5
                                    ? p_185201_
                                    : p_185201_.update("settings", p_185197_ -> p_185197_.update("layers", WorldGenSettingsHeightAndBiomeFix::updateLayers));
                              } else {
                                 return p_185201_;
                              }
                           }
                        )
                     )
                  )
               );
         }
      );
   }

   private static Dynamic<?> updateLayers(Dynamic<?> p_185181_) {
      Dynamic<?> $$1 = p_185181_.createMap(
         ImmutableMap.of(p_185181_.createString("height"), p_185181_.createInt(64), p_185181_.createString("block"), p_185181_.createString("minecraft:air"))
      );
      return p_185181_.createList(Stream.concat(Stream.of($$1), p_185181_.asStream()));
   }
}
