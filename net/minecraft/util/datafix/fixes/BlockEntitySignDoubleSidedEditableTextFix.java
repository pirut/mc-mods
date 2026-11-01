package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Streams;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.datafix.ComponentDataFixUtils;

public class BlockEntitySignDoubleSidedEditableTextFix extends NamedEntityFix {
   public static final String FILTERED_CORRECT = "_filtered_correct";
   private static final String DEFAULT_COLOR = "black";

   public BlockEntitySignDoubleSidedEditableTextFix(Schema p_277789_, String p_278061_, String p_277403_) {
      super(p_277789_, false, p_278061_, References.BLOCK_ENTITY, p_277403_);
   }

   private static <T> Dynamic<T> fixTag(Dynamic<T> p_278110_) {
      return p_278110_.set("front_text", fixFrontTextTag(p_278110_))
         .set("back_text", createDefaultText(p_278110_))
         .set("is_waxed", p_278110_.createBoolean(false));
   }

   private static <T> Dynamic<T> fixFrontTextTag(Dynamic<T> p_295403_) {
      Dynamic<T> $$1 = ComponentDataFixUtils.createEmptyComponent(p_295403_.getOps());
      List<Dynamic<T>> $$2 = getLines(p_295403_, "Text").map(p_294721_ -> p_294721_.orElse($$1)).toList();
      Dynamic<T> $$3 = p_295403_.emptyMap()
         .set("messages", p_295403_.createList($$2.stream()))
         .set("color", p_295403_.get("Color").result().orElse(p_295403_.createString("black")))
         .set("has_glowing_text", p_295403_.get("GlowingText").result().orElse(p_295403_.createBoolean(false)))
         .set("_filtered_correct", p_295403_.createBoolean(true));
      List<Optional<Dynamic<T>>> $$4 = getLines(p_295403_, "FilteredText").toList();
      if ($$4.stream().anyMatch(Optional::isPresent)) {
         $$3 = $$3.set("filtered_messages", p_295403_.createList(Streams.mapWithIndex($$4.stream(), (p_295046_, p_294135_) -> {
            Dynamic<T> $$3x = $$2.get((int)p_294135_);
            return p_295046_.orElse($$3x);
         })));
      }

      return $$3;
   }

   private static <T> Stream<Optional<Dynamic<T>>> getLines(Dynamic<T> p_295400_, String p_294843_) {
      return Stream.of(
         p_295400_.get(p_294843_ + "1").result(),
         p_295400_.get(p_294843_ + "2").result(),
         p_295400_.get(p_294843_ + "3").result(),
         p_295400_.get(p_294843_ + "4").result()
      );
   }

   private static <T> Dynamic<T> createDefaultText(Dynamic<T> p_294259_) {
      return p_294259_.emptyMap()
         .set("messages", createEmptyLines(p_294259_))
         .set("color", p_294259_.createString("black"))
         .set("has_glowing_text", p_294259_.createBoolean(false));
   }

   private static <T> Dynamic<T> createEmptyLines(Dynamic<T> p_294420_) {
      Dynamic<T> $$1 = ComponentDataFixUtils.createEmptyComponent(p_294420_.getOps());
      return p_294420_.createList(Stream.of($$1, $$1, $$1, $$1));
   }

   @Override
   protected Typed<?> fix(Typed<?> p_277962_) {
      return p_277962_.update(DSL.remainderFinder(), BlockEntitySignDoubleSidedEditableTextFix::fixTag);
   }
}
