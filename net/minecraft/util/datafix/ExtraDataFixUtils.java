package net.minecraft.util.datafix;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.RewriteResult;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.View;
import com.mojang.datafixers.functions.PointFreeRule;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.BitSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Util;

public class ExtraDataFixUtils {
   public static Dynamic<?> fixBlockPos(Dynamic<?> p_326292_) {
      Optional<Number> optional = p_326292_.get("X").asNumber().result();
      Optional<Number> optional1 = p_326292_.get("Y").asNumber().result();
      Optional<Number> optional2 = p_326292_.get("Z").asNumber().result();
      return !optional.isEmpty() && !optional1.isEmpty() && !optional2.isEmpty()
         ? createBlockPos(p_326292_, optional.get().intValue(), optional1.get().intValue(), optional2.get().intValue())
         : p_326292_;
   }

   public static Dynamic<?> fixInlineBlockPos(Dynamic<?> p_401383_, String p_401140_, String p_401018_, String p_401105_, String p_401115_) {
      Optional<Number> optional = p_401383_.get(p_401140_).asNumber().result();
      Optional<Number> optional1 = p_401383_.get(p_401018_).asNumber().result();
      Optional<Number> optional2 = p_401383_.get(p_401105_).asNumber().result();
      return !optional.isEmpty() && !optional1.isEmpty() && !optional2.isEmpty()
         ? p_401383_.remove(p_401140_)
            .remove(p_401018_)
            .remove(p_401105_)
            .set(p_401115_, createBlockPos(p_401383_, optional.get().intValue(), optional1.get().intValue(), optional2.get().intValue()))
         : p_401383_;
   }

   public static Dynamic<?> createBlockPos(Dynamic<?> p_401108_, int p_401194_, int p_401075_, int p_401046_) {
      return p_401108_.createIntList(IntStream.of(p_401194_, p_401075_, p_401046_));
   }

   public static <T, R> Typed<R> cast(Type<R> p_330690_, Typed<T> p_331921_) {
      return new Typed(p_330690_, p_331921_.getOps(), p_331921_.getValue());
   }

   public static <T> Typed<T> cast(Type<T> p_394665_, Object p_394477_, DynamicOps<?> p_394121_) {
      return new Typed(p_394665_, p_394121_, p_394477_);
   }

   public static Type<?> patchSubType(Type<?> p_364210_, Type<?> p_360925_, Type<?> p_365306_) {
      return p_364210_.all(typePatcher(p_360925_, p_365306_), true, false).view().newType();
   }

   private static <A, B> TypeRewriteRule typePatcher(Type<A> p_365338_, Type<B> p_363692_) {
      RewriteResult<A, B> rewriteresult = RewriteResult.create(View.create("Patcher", p_365338_, p_363692_, p_359667_ -> p_359670_ -> {
         throw new UnsupportedOperationException();
      }), new BitSet());
      return TypeRewriteRule.everywhere(TypeRewriteRule.ifSame(p_365338_, rewriteresult), PointFreeRule.nop(), true, true);
   }

   @SafeVarargs
   public static <T> Function<Typed<?>, Typed<?>> chainAllFilters(Function<Typed<?>, Typed<?>>... p_344769_) {
      return p_345927_ -> {
         for (Function<Typed<?>, Typed<?>> function : p_344769_) {
            p_345927_ = function.apply(p_345927_);
         }

         return p_345927_;
      };
   }

   public static Dynamic<?> blockState(String p_361952_, Map<String, String> p_362407_) {
      Dynamic<Tag> dynamic = new Dynamic(NbtOps.INSTANCE, new CompoundTag());
      Dynamic<Tag> dynamic1 = dynamic.set("Name", dynamic.createString(p_361952_));
      if (!p_362407_.isEmpty()) {
         dynamic1 = dynamic1.set(
            "Properties",
            dynamic.createMap(
               p_362407_.entrySet()
                  .stream()
                  .collect(Collectors.toMap(p_359669_ -> dynamic.createString(p_359669_.getKey()), p_359672_ -> dynamic.createString(p_359672_.getValue())))
            )
         );
      }

      return dynamic1;
   }

   public static Dynamic<?> blockState(String p_363485_) {
      return blockState(p_363485_, Map.of());
   }

   public static Dynamic<?> fixStringField(Dynamic<?> p_365145_, String p_361499_, UnaryOperator<String> p_360789_) {
      return p_365145_.update(
         p_361499_, p_359675_ -> (Dynamic)DataFixUtils.orElse(p_359675_.asString().map(p_360789_).map(p_365145_::createString).result(), p_359675_)
      );
   }

   public static String dyeColorIdToName(int p_397819_) {
      return switch (p_397819_) {
         case 1 -> "orange";
         case 2 -> "magenta";
         case 3 -> "light_blue";
         case 4 -> "yellow";
         case 5 -> "lime";
         case 6 -> "pink";
         case 7 -> "gray";
         case 8 -> "light_gray";
         case 9 -> "cyan";
         case 10 -> "purple";
         case 11 -> "blue";
         case 12 -> "brown";
         case 13 -> "green";
         case 14 -> "red";
         case 15 -> "black";
         default -> "white";
      };
   }

   public static <T> Typed<?> readAndSet(Typed<?> p_404652_, OpticFinder<T> p_405690_, Dynamic<?> p_405117_) {
      return p_404652_.set(p_405690_, Util.readTypedOrThrow(p_405690_.type(), p_405117_, true));
   }
}
