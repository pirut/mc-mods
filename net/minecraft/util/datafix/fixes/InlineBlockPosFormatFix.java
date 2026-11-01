package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class InlineBlockPosFormatFix extends DataFix {
   public InlineBlockPosFormatFix(Schema p_404647_) {
      super(p_404647_, false);
   }

   public TypeRewriteRule makeRule() {
      OpticFinder<?> opticfinder = this.entityFinder("minecraft:vex");
      OpticFinder<?> opticfinder1 = this.entityFinder("minecraft:phantom");
      OpticFinder<?> opticfinder2 = this.entityFinder("minecraft:turtle");
      List<OpticFinder<?>> list = List.of(
         this.entityFinder("minecraft:item_frame"),
         this.entityFinder("minecraft:glow_item_frame"),
         this.entityFinder("minecraft:painting"),
         this.entityFinder("minecraft:leash_knot")
      );
      return TypeRewriteRule.seq(
         this.fixTypeEverywhereTyped(
            "InlineBlockPosFormatFix - player",
            this.getInputSchema().getType(References.PLAYER),
            p_404865_ -> p_404865_.update(DSL.remainderFinder(), this::fixPlayer)
         ),
         this.fixTypeEverywhereTyped(
            "InlineBlockPosFormatFix - entity",
            this.getInputSchema().getType(References.ENTITY),
            p_404996_ -> {
               p_404996_ = p_404996_.update(DSL.remainderFinder(), this::fixLivingEntity)
                  .updateTyped(opticfinder, p_405778_ -> p_405778_.update(DSL.remainderFinder(), this::fixVex))
                  .updateTyped(opticfinder1, p_405644_ -> p_405644_.update(DSL.remainderFinder(), this::fixPhantom))
                  .updateTyped(opticfinder2, p_405664_ -> p_405664_.update(DSL.remainderFinder(), this::fixTurtle));

               for (OpticFinder<?> opticfinder3 : list) {
                  p_404996_ = p_404996_.updateTyped(opticfinder3, p_404887_ -> p_404887_.update(DSL.remainderFinder(), this::fixBlockAttached));
               }

               return p_404996_;
            }
         )
      );
   }

   private OpticFinder<?> entityFinder(String p_404811_) {
      return DSL.namedChoice(p_404811_, this.getInputSchema().getChoiceType(References.ENTITY, p_404811_));
   }

   private Dynamic<?> fixPlayer(Dynamic<?> p_405309_) {
      p_405309_ = this.fixLivingEntity(p_405309_);
      Optional<Number> optional = p_405309_.get("SpawnX").asNumber().result();
      Optional<Number> optional1 = p_405309_.get("SpawnY").asNumber().result();
      Optional<Number> optional2 = p_405309_.get("SpawnZ").asNumber().result();
      if (optional.isPresent() && optional1.isPresent() && optional2.isPresent()) {
         Dynamic<?> dynamic = p_405309_.createMap(
            Map.of(
               p_405309_.createString("pos"),
               ExtraDataFixUtils.createBlockPos(p_405309_, optional.get().intValue(), optional1.get().intValue(), optional2.get().intValue())
            )
         );
         dynamic = Dynamic.copyField(p_405309_, "SpawnAngle", dynamic, "angle");
         dynamic = Dynamic.copyField(p_405309_, "SpawnDimension", dynamic, "dimension");
         dynamic = Dynamic.copyField(p_405309_, "SpawnForced", dynamic, "forced");
         p_405309_ = p_405309_.remove("SpawnX").remove("SpawnY").remove("SpawnZ").remove("SpawnAngle").remove("SpawnDimension").remove("SpawnForced");
         p_405309_ = p_405309_.set("respawn", dynamic);
      }

      Optional<? extends Dynamic<?>> optional3 = p_405309_.get("enteredNetherPosition").result();
      if (optional3.isPresent()) {
         p_405309_ = p_405309_.remove("enteredNetherPosition")
            .set(
               "entered_nether_pos",
               p_405309_.createList(
                  Stream.of(
                     p_405309_.createDouble(optional3.get().get("x").asDouble(0.0)),
                     p_405309_.createDouble(optional3.get().get("y").asDouble(0.0)),
                     p_405309_.createDouble(optional3.get().get("z").asDouble(0.0))
                  )
               )
            );
      }

      return p_405309_;
   }

   private Dynamic<?> fixLivingEntity(Dynamic<?> p_405023_) {
      return ExtraDataFixUtils.fixInlineBlockPos(p_405023_, "SleepingX", "SleepingY", "SleepingZ", "sleeping_pos");
   }

   private Dynamic<?> fixVex(Dynamic<?> p_404948_) {
      return ExtraDataFixUtils.fixInlineBlockPos(p_404948_.renameField("LifeTicks", "life_ticks"), "BoundX", "BoundY", "BoundZ", "bound_pos");
   }

   private Dynamic<?> fixPhantom(Dynamic<?> p_404710_) {
      return ExtraDataFixUtils.fixInlineBlockPos(p_404710_.renameField("Size", "size"), "AX", "AY", "AZ", "anchor_pos");
   }

   private Dynamic<?> fixTurtle(Dynamic<?> p_404724_) {
      p_404724_ = p_404724_.remove("TravelPosX").remove("TravelPosY").remove("TravelPosZ");
      p_404724_ = ExtraDataFixUtils.fixInlineBlockPos(p_404724_, "HomePosX", "HomePosY", "HomePosZ", "home_pos");
      return p_404724_.renameField("HasEgg", "has_egg");
   }

   private Dynamic<?> fixBlockAttached(Dynamic<?> p_404684_) {
      return ExtraDataFixUtils.fixInlineBlockPos(p_404684_, "TileX", "TileY", "TileZ", "block_pos");
   }
}
