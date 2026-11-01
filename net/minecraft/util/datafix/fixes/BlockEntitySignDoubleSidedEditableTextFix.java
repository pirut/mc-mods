package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Streams;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;

public class BlockEntitySignDoubleSidedEditableTextFix extends NamedEntityWriteReadFix {
   public static final List<String> FIELDS_TO_DROP = List.of(
      "Text1", "Text2", "Text3", "Text4", "FilteredText1", "FilteredText2", "FilteredText3", "FilteredText4", "Color", "GlowingText"
   );
   public static final String FILTERED_CORRECT = "_filtered_correct";
   private static final String DEFAULT_COLOR = "black";

   public BlockEntitySignDoubleSidedEditableTextFix(Schema p_277789_, String p_278061_, String p_277403_) {
      super(p_277789_, true, p_278061_, References.BLOCK_ENTITY, p_277403_);
   }

   @Override
   protected <T> Dynamic<T> fix(Dynamic<T> p_394516_) {
      p_394516_ = p_394516_.set("front_text", fixFrontTextTag(p_394516_))
         .set("back_text", createDefaultText(p_394516_))
         .set("is_waxed", p_394516_.createBoolean(false))
         .set("_filtered_correct", p_394516_.createBoolean(true));

      for (String s : FIELDS_TO_DROP) {
         p_394516_ = p_394516_.remove(s);
      }

      return p_394516_;
   }

   private static <T> Dynamic<T> fixFrontTextTag(Dynamic<T> p_295403_) {
      Dynamic<T> dynamic = LegacyComponentDataFixUtils.createEmptyComponent(p_295403_.getOps());
      List<Dynamic<T>> list = getLines(p_295403_, "Text").map(p_294721_ -> p_294721_.orElse(dynamic)).toList();
      Dynamic<T> dynamic1 = p_295403_.emptyMap()
         .set("messages", p_295403_.createList(list.stream()))
         .set("color", p_295403_.get("Color").result().orElse(p_295403_.createString("black")))
         .set("has_glowing_text", p_295403_.get("GlowingText").result().orElse(p_295403_.createBoolean(false)));
      List<Optional<Dynamic<T>>> list1 = getLines(p_295403_, "FilteredText").toList();
      if (list1.stream().anyMatch(Optional::isPresent)) {
         dynamic1 = dynamic1.set("filtered_messages", p_295403_.createList(Streams.mapWithIndex(list1.stream(), (p_295046_, p_294135_) -> {
            Dynamic<T> dynamic2 = list.get((int)p_294135_);
            return p_295046_.orElse(dynamic2);
         })));
      }

      return dynamic1;
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
      Dynamic<T> dynamic = LegacyComponentDataFixUtils.createEmptyComponent(p_294420_.getOps());
      return p_294420_.createList(Stream.of(dynamic, dynamic, dynamic, dynamic));
   }
}
