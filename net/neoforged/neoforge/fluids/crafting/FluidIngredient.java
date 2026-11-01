package net.neoforged.neoforge.fluids.crafting;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackLinkedSet;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

public abstract class FluidIngredient implements Predicate<FluidStack> {
   private static final MapCodec<FluidIngredient> SINGLE_OR_TAG_CODEC = MapCodec.recursive("FluidIngredient.SINGLE_OR_TAG_CODEC", self -> singleOrTagCodec());
   public static final MapCodec<FluidIngredient> MAP_CODEC_NONEMPTY = makeMapCodec();
   private static final Codec<FluidIngredient> MAP_CODEC_CODEC = MAP_CODEC_NONEMPTY.codec();
   public static final Codec<List<FluidIngredient>> LIST_CODEC = MAP_CODEC_CODEC.listOf();
   public static final Codec<List<FluidIngredient>> LIST_CODEC_NON_EMPTY = LIST_CODEC.validate(
      list -> list.isEmpty() ? DataResult.error(() -> "Fluid ingredient cannot be empty, at least one item must be defined") : DataResult.success(list)
   );
   public static final Codec<FluidIngredient> CODEC = codec(true);
   public static final Codec<FluidIngredient> CODEC_NON_EMPTY = codec(false);
   public static final StreamCodec<RegistryFriendlyByteBuf, FluidIngredient> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, FluidIngredient>() {
      private static final StreamCodec<RegistryFriendlyByteBuf, FluidIngredient> DISPATCH_CODEC = ByteBufCodecs.registry(
            NeoForgeRegistries.Keys.FLUID_INGREDIENT_TYPES
         )
         .dispatch(FluidIngredient::getType, FluidIngredientType::streamCodec);
      private static final StreamCodec<RegistryFriendlyByteBuf, List<FluidStack>> FLUID_LIST_CODEC = FluidStack.STREAM_CODEC
         .apply(ByteBufCodecs.collection(NonNullList::createWithCapacity));

      public void encode(RegistryFriendlyByteBuf buf, FluidIngredient ingredient) {
         if (ingredient.isSimple()) {
            FLUID_LIST_CODEC.encode(buf, Arrays.asList(ingredient.getStacks()));
         } else {
            buf.writeVarInt(-1);
            DISPATCH_CODEC.encode(buf, ingredient);
         }
      }

      public FluidIngredient decode(RegistryFriendlyByteBuf buf) {
         int size = buf.readVarInt();
         return size == -1
            ? DISPATCH_CODEC.decode(buf)
            : CompoundFluidIngredient.of(Stream.<FluidStack>generate(() -> FluidStack.STREAM_CODEC.decode(buf)).limit(size).map(FluidIngredient::single));
      }
   };
   @Nullable
   private FluidStack[] stacks;

   public final FluidStack[] getStacks() {
      if (this.stacks == null) {
         this.stacks = this.generateStacks().collect(Collectors.toCollection(FluidStackLinkedSet::createTypeAndComponentsSet)).toArray(FluidStack[]::new);
      }

      return this.stacks;
   }

   public abstract boolean test(FluidStack var1);

   protected abstract Stream<FluidStack> generateStacks();

   public abstract boolean isSimple();

   public abstract FluidIngredientType<?> getType();

   public final boolean isEmpty() {
      return this == empty();
   }

   public final boolean hasNoFluids() {
      return this.getStacks().length == 0;
   }

   @Override
   public abstract int hashCode();

   @Override
   public abstract boolean equals(Object var1);

   public static FluidIngredient empty() {
      return EmptyFluidIngredient.INSTANCE;
   }

   public static FluidIngredient of() {
      return empty();
   }

   public static FluidIngredient of(FluidStack... fluids) {
      return of(Arrays.stream(fluids).map(FluidStack::getFluid));
   }

   public static FluidIngredient of(Fluid... fluids) {
      return of(Arrays.stream(fluids));
   }

   private static FluidIngredient of(Stream<Fluid> fluids) {
      return CompoundFluidIngredient.of(fluids.map(FluidIngredient::single));
   }

   public static FluidIngredient single(FluidStack stack) {
      return single(stack.getFluid());
   }

   public static FluidIngredient single(Fluid fluid) {
      return single(fluid.builtInRegistryHolder());
   }

   public static FluidIngredient single(Holder<Fluid> holder) {
      return new SingleFluidIngredient(holder);
   }

   public static FluidIngredient tag(TagKey<Fluid> tag) {
      return new TagFluidIngredient(tag);
   }

   private static MapCodec<FluidIngredient> singleOrTagCodec() {
      return NeoForgeExtraCodecs.xor(SingleFluidIngredient.CODEC, TagFluidIngredient.CODEC)
         .xmap(either -> (FluidIngredient)either.map(id -> id, id -> id), ingredient -> {
            if (ingredient instanceof SingleFluidIngredient fluid) {
               return Either.left(fluid);
            } else if (ingredient instanceof TagFluidIngredient tag) {
               return Either.right(tag);
            } else {
               throw new IllegalStateException("Basic fluid ingredient should be either a fluid or a tag!");
            }
         });
   }

   private static MapCodec<FluidIngredient> makeMapCodec() {
      return NeoForgeExtraCodecs.dispatchMapOrElse(
            NeoForgeRegistries.FLUID_INGREDIENT_TYPES.byNameCodec(), FluidIngredient::getType, FluidIngredientType::codec, SINGLE_OR_TAG_CODEC
         )
         .xmap(
            either -> (FluidIngredient)either.map(id -> id, id -> id),
            ingredient -> !(ingredient instanceof SingleFluidIngredient) && !(ingredient instanceof TagFluidIngredient)
               ? Either.left(ingredient)
               : Either.right(ingredient)
         )
         .validate(
            ingredient -> ingredient.isEmpty()
               ? DataResult.error(() -> "Cannot serialize empty fluid ingredient using the map codec")
               : DataResult.success(ingredient)
         );
   }

   private static Codec<FluidIngredient> codec(boolean allowEmpty) {
      Codec<List<FluidIngredient>> listCodec = Codec.lazyInitialized(() -> allowEmpty ? LIST_CODEC : LIST_CODEC_NON_EMPTY);
      return Codec.either(listCodec, MAP_CODEC_CODEC).xmap(either -> (FluidIngredient)either.map(CompoundFluidIngredient::of, i -> i), ingredient -> {
         if (ingredient instanceof CompoundFluidIngredient compound) {
            return Either.left(compound.children());
         } else {
            return ingredient.isEmpty() ? Either.left(List.of()) : Either.right(ingredient);
         }
      });
   }
}
