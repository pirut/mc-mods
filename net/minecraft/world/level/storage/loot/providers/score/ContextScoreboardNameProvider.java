package net.minecraft.world.level.storage.loot.providers.score;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.scores.ScoreHolder;
import org.jspecify.annotations.Nullable;

public record ContextScoreboardNameProvider(LootContext.EntityTarget target) implements ScoreboardNameProvider {
   public static final MapCodec<ContextScoreboardNameProvider> CODEC = RecordCodecBuilder.mapCodec(
      p_298566_ -> p_298566_.group(LootContext.EntityTarget.CODEC.fieldOf("target").forGetter(ContextScoreboardNameProvider::target))
         .apply(p_298566_, ContextScoreboardNameProvider::new)
   );
   public static final Codec<ContextScoreboardNameProvider> INLINE_CODEC = LootContext.EntityTarget.CODEC
      .xmap(ContextScoreboardNameProvider::new, ContextScoreboardNameProvider::target);

   public static ScoreboardNameProvider forTarget(LootContext.EntityTarget p_165808_) {
      return new ContextScoreboardNameProvider(p_165808_);
   }

   @Override
   public LootScoreProviderType getType() {
      return ScoreboardNameProviders.CONTEXT;
   }

   @Nullable
   @Override
   public ScoreHolder getScoreHolder(LootContext p_313828_) {
      return p_313828_.getOptionalParameter(this.target.contextParam());
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(this.target.contextParam());
   }
}
