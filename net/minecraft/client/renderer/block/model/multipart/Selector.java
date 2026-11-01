package net.minecraft.client.renderer.block.model.multipart;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record Selector(Optional<Condition> condition, BlockStateModel.Unbaked variant) {
   public static final Codec<Selector> CODEC = RecordCodecBuilder.create(
      p_409080_ -> p_409080_.group(
            Condition.CODEC.optionalFieldOf("when").forGetter(Selector::condition), BlockStateModel.Unbaked.CODEC.fieldOf("apply").forGetter(Selector::variant)
         )
         .apply(p_409080_, Selector::new)
   );

   public <O, S extends StateHolder<O, S>> Predicate<S> instantiate(StateDefinition<O, S> p_404970_) {
      return this.condition.<Predicate<S>>map(p_405845_ -> p_405845_.instantiate(p_404970_)).orElse(p_405732_ -> true);
   }
}
