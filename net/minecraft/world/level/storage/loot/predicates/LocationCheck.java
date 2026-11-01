package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public record LocationCheck(Optional<LocationPredicate> predicate, BlockPos offset) implements LootItemCondition {
   private static final MapCodec<BlockPos> OFFSET_CODEC = RecordCodecBuilder.mapCodec(
      p_338168_ -> p_338168_.group(
            Codec.INT.optionalFieldOf("offsetX", 0).forGetter(Vec3i::getX),
            Codec.INT.optionalFieldOf("offsetY", 0).forGetter(Vec3i::getY),
            Codec.INT.optionalFieldOf("offsetZ", 0).forGetter(Vec3i::getZ)
         )
         .apply(p_338168_, BlockPos::new)
   );
   public static final MapCodec<LocationCheck> CODEC = RecordCodecBuilder.mapCodec(
      p_338169_ -> p_338169_.group(
            LocationPredicate.CODEC.optionalFieldOf("predicate").forGetter(LocationCheck::predicate), OFFSET_CODEC.forGetter(LocationCheck::offset)
         )
         .apply(p_338169_, LocationCheck::new)
   );

   @Override
   public LootItemConditionType getType() {
      return LootItemConditions.LOCATION_CHECK;
   }

   public boolean test(LootContext p_81731_) {
      Vec3 $$1 = p_81731_.getParamOrNull(LootContextParams.ORIGIN);
      return $$1 != null
         && (
            this.predicate.isEmpty()
               || this.predicate.get().matches(p_81731_.getLevel(), $$1.x() + this.offset.getX(), $$1.y() + this.offset.getY(), $$1.z() + this.offset.getZ())
         );
   }

   @Override
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return Set.of(LootContextParams.ORIGIN);
   }

   public static LootItemCondition.Builder checkLocation(LocationPredicate.Builder p_81726_) {
      return () -> new LocationCheck(Optional.of(p_81726_.build()), BlockPos.ZERO);
   }

   public static LootItemCondition.Builder checkLocation(LocationPredicate.Builder p_81728_, BlockPos p_81729_) {
      return () -> new LocationCheck(Optional.of(p_81728_.build()), p_81729_);
   }
}
