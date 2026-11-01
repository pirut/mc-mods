package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetCustomModelDataFunction extends LootItemConditionalFunction {
   private static final Codec<NumberProvider> COLOR_PROVIDER_CODEC = Codec.withAlternative(
      NumberProviders.CODEC, ExtraCodecs.RGB_COLOR_CODEC, ConstantValue::new
   );
   public static final MapCodec<SetCustomModelDataFunction> CODEC = RecordCodecBuilder.mapCodec(
      p_386432_ -> commonFields(p_386432_)
         .and(
            p_386432_.group(
               ListOperation.StandAlone.codec(NumberProviders.CODEC, Integer.MAX_VALUE).optionalFieldOf("floats").forGetter(p_386438_ -> p_386438_.floats),
               ListOperation.StandAlone.codec(Codec.BOOL, Integer.MAX_VALUE).optionalFieldOf("flags").forGetter(p_386429_ -> p_386429_.flags),
               ListOperation.StandAlone.codec(Codec.STRING, Integer.MAX_VALUE).optionalFieldOf("strings").forGetter(p_386435_ -> p_386435_.strings),
               ListOperation.StandAlone.codec(COLOR_PROVIDER_CODEC, Integer.MAX_VALUE).optionalFieldOf("colors").forGetter(p_386434_ -> p_386434_.colors)
            )
         )
         .apply(p_386432_, SetCustomModelDataFunction::new)
   );
   private final Optional<ListOperation.StandAlone<NumberProvider>> floats;
   private final Optional<ListOperation.StandAlone<Boolean>> flags;
   private final Optional<ListOperation.StandAlone<String>> strings;
   private final Optional<ListOperation.StandAlone<NumberProvider>> colors;

   public SetCustomModelDataFunction(
      List<LootItemCondition> p_340822_,
      Optional<ListOperation.StandAlone<NumberProvider>> p_387465_,
      Optional<ListOperation.StandAlone<Boolean>> p_387009_,
      Optional<ListOperation.StandAlone<String>> p_388312_,
      Optional<ListOperation.StandAlone<NumberProvider>> p_386869_
   ) {
      super(p_340822_);
      this.floats = p_387465_;
      this.flags = p_387009_;
      this.strings = p_388312_;
      this.colors = p_386869_;
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Stream.concat(this.floats.stream(), this.colors.stream())
         .flatMap(p_386433_ -> p_386433_.value().stream())
         .flatMap(p_460611_ -> p_460611_.getReferencedContextParams().stream())
         .collect(Collectors.toSet());
   }

   @Override
   public LootItemFunctionType<SetCustomModelDataFunction> getType() {
      return LootItemFunctions.SET_CUSTOM_MODEL_DATA;
   }

   private static <T> List<T> apply(Optional<ListOperation.StandAlone<T>> p_387231_, List<T> p_386572_) {
      return p_387231_.<List<T>>map(p_386428_ -> p_386428_.apply(p_386572_)).orElse(p_386572_);
   }

   private static <T, E> List<E> apply(Optional<ListOperation.StandAlone<T>> p_388615_, List<E> p_387094_, Function<T, E> p_387821_) {
      return p_388615_.<List<E>>map(p_386442_ -> {
         List<E> list = p_386442_.value().stream().map(p_387821_).toList();
         return p_386442_.operation().apply(p_387094_, list);
      }).orElse(p_387094_);
   }

   @Override
   public ItemStack run(ItemStack p_341195_, LootContext p_341335_) {
      CustomModelData custommodeldata = p_341195_.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY);
      p_341195_.set(
         DataComponents.CUSTOM_MODEL_DATA,
         new CustomModelData(
            apply(this.floats, custommodeldata.floats(), p_386431_ -> p_386431_.getFloat(p_341335_)),
            apply(this.flags, custommodeldata.flags()),
            apply(this.strings, custommodeldata.strings()),
            apply(this.colors, custommodeldata.colors(), p_386437_ -> p_386437_.getInt(p_341335_))
         )
      );
      return p_341195_;
   }
}
