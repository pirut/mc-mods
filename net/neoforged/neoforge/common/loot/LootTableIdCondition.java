package net.neoforged.neoforge.common.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

public class LootTableIdCondition implements LootItemCondition {
   public static final MapCodec<LootTableIdCondition> CODEC = RecordCodecBuilder.mapCodec(
      builder -> builder.group(Identifier.CODEC.fieldOf("loot_table_id").forGetter(idCondition -> idCondition.targetLootTableId))
         .apply(builder, LootTableIdCondition::new)
   );
   public static final LootItemConditionType LOOT_TABLE_ID = new LootItemConditionType(CODEC);
   public static final Identifier UNKNOWN_LOOT_TABLE = Identifier.fromNamespaceAndPath("neoforge", "unknown_loot_table");
   private final Identifier targetLootTableId;

   private LootTableIdCondition(Identifier targetLootTableId) {
      this.targetLootTableId = targetLootTableId;
   }

   @Override
   public LootItemConditionType getType() {
      return LOOT_TABLE_ID;
   }

   public boolean test(LootContext lootContext) {
      return lootContext.getQueriedLootTableId().equals(this.targetLootTableId);
   }

   public static LootTableIdCondition.Builder builder(Identifier targetLootTableId) {
      return new LootTableIdCondition.Builder(targetLootTableId);
   }

   public static class Builder implements LootItemCondition.Builder {
      private final Identifier targetLootTableId;

      public Builder(Identifier targetLootTableId) {
         if (targetLootTableId == null) {
            throw new IllegalArgumentException("Target loot table must not be null");
         } else {
            this.targetLootTableId = targetLootTableId;
         }
      }

      @Override
      public LootItemCondition build() {
         return new LootTableIdCondition(this.targetLootTableId);
      }
   }
}
