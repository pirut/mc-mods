package net.minecraft.world.item;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.criterion.BlockPredicate;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class AdventureModePredicate {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<AdventureModePredicate> CODEC = ExtraCodecs.compactListCodec(
         BlockPredicate.CODEC, ExtraCodecs.nonEmptyList(BlockPredicate.CODEC.listOf())
      )
      .xmap(AdventureModePredicate::new, p_331329_ -> p_331329_.predicates);
   public static final StreamCodec<RegistryFriendlyByteBuf, AdventureModePredicate> STREAM_CODEC = StreamCodec.composite(
      BlockPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()), p_331439_ -> p_331439_.predicates, AdventureModePredicate::new
   );
   public static final Component CAN_BREAK_HEADER = Component.translatable("item.canBreak").withStyle(ChatFormatting.GRAY);
   public static final Component CAN_PLACE_HEADER = Component.translatable("item.canPlace").withStyle(ChatFormatting.GRAY);
   private static final Component UNKNOWN_USE = Component.translatable("item.canUse.unknown").withStyle(ChatFormatting.GRAY);
   private final List<BlockPredicate> predicates;
   @Nullable
   private List<Component> cachedTooltip;
   @Nullable
   private BlockInWorld lastCheckedBlock;
   private boolean lastResult;
   private boolean checksBlockEntity;

   public AdventureModePredicate(List<BlockPredicate> p_331906_) {
      this.predicates = p_331906_;
   }

   private static boolean areSameBlocks(BlockInWorld p_330901_, @Nullable BlockInWorld p_331435_, boolean p_332152_) {
      if (p_331435_ == null || p_330901_.getState() != p_331435_.getState()) {
         return false;
      } else if (!p_332152_) {
         return true;
      } else if (p_330901_.getEntity() == null && p_331435_.getEntity() == null) {
         return true;
      } else if (p_330901_.getEntity() != null && p_331435_.getEntity() != null) {
         boolean flag;
         try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(LOGGER)) {
            RegistryAccess registryaccess = p_330901_.getLevel().registryAccess();
            CompoundTag compoundtag = saveBlockEntity(p_330901_.getEntity(), registryaccess, problemreporter$scopedcollector);
            CompoundTag compoundtag1 = saveBlockEntity(p_331435_.getEntity(), registryaccess, problemreporter$scopedcollector);
            flag = Objects.equals(compoundtag, compoundtag1);
         }

         return flag;
      } else {
         return false;
      }
   }

   private static CompoundTag saveBlockEntity(BlockEntity p_422309_, RegistryAccess p_422663_, ProblemReporter p_422210_) {
      TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(p_422210_.forChild(p_422309_.problemPath()), p_422663_);
      p_422309_.saveWithId(tagvalueoutput);
      return tagvalueoutput.buildResult();
   }

   public boolean test(BlockInWorld p_331911_) {
      if (areSameBlocks(p_331911_, this.lastCheckedBlock, this.checksBlockEntity)) {
         return this.lastResult;
      } else {
         this.lastCheckedBlock = p_331911_;
         this.checksBlockEntity = false;

         for (BlockPredicate blockpredicate : this.predicates) {
            if (blockpredicate.matches(p_331911_)) {
               this.checksBlockEntity = this.checksBlockEntity | blockpredicate.requiresNbt();
               this.lastResult = true;
               return true;
            }
         }

         this.lastResult = false;
         return false;
      }
   }

   private List<Component> tooltip() {
      if (this.cachedTooltip == null) {
         this.cachedTooltip = computeTooltip(this.predicates);
      }

      return this.cachedTooltip;
   }

   public void addToTooltip(Consumer<Component> p_331792_) {
      this.tooltip().forEach(p_331792_);
   }

   private static List<Component> computeTooltip(List<BlockPredicate> p_332164_) {
      for (BlockPredicate blockpredicate : p_332164_) {
         if (blockpredicate.blocks().isEmpty()) {
            return List.of(UNKNOWN_USE);
         }
      }

      return p_332164_.stream()
         .flatMap(p_466632_ -> p_466632_.blocks().orElseThrow().stream())
         .distinct()
         .map(p_331662_ -> p_331662_.value().getName().withStyle(ChatFormatting.DARK_GRAY))
         .toList();
   }

   @Override
   public boolean equals(Object p_330844_) {
      if (this == p_330844_) {
         return true;
      } else {
         return p_330844_ instanceof AdventureModePredicate adventuremodepredicate ? this.predicates.equals(adventuremodepredicate.predicates) : false;
      }
   }

   @Override
   public int hashCode() {
      return this.predicates.hashCode();
   }

   @Override
   public String toString() {
      return "AdventureModePredicate{predicates=" + this.predicates + "}";
   }
}
