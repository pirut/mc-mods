package net.minecraft.util.datafix.fixes;

import com.google.common.collect.Streams;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;

public class DropInvalidSignDataFix extends DataFix {
   private final String entityName;

   public DropInvalidSignDataFix(Schema p_296288_, String p_294804_) {
      super(p_296288_, false);
      this.entityName = p_294804_;
   }

   private <T> Dynamic<T> fix(Dynamic<T> p_295232_) {
      p_295232_ = p_295232_.update("front_text", DropInvalidSignDataFix::fixText);
      p_295232_ = p_295232_.update("back_text", DropInvalidSignDataFix::fixText);

      for (String s : BlockEntitySignDoubleSidedEditableTextFix.FIELDS_TO_DROP) {
         p_295232_ = p_295232_.remove(s);
      }

      return p_295232_;
   }

   private static <T> Dynamic<T> fixText(Dynamic<T> p_296074_) {
      Optional<Stream<Dynamic<T>>> optional = p_296074_.get("filtered_messages").asStreamOpt().result();
      if (optional.isEmpty()) {
         return p_296074_;
      } else {
         Dynamic<T> dynamic = LegacyComponentDataFixUtils.createEmptyComponent(p_296074_.getOps());
         List<Dynamic<T>> list = p_296074_.get("messages").asStreamOpt().result().orElse(Stream.of()).toList();
         List<Dynamic<T>> list1 = Streams.mapWithIndex(optional.get(), (p_294909_, p_296017_) -> {
            Dynamic<T> dynamic1 = p_296017_ < list.size() ? list.get((int)p_296017_) : dynamic;
            return p_294909_.equals(dynamic) ? dynamic1 : p_294909_;
         }).toList();
         return list1.equals(list) ? p_296074_.remove("filtered_messages") : p_296074_.set("filtered_messages", p_296074_.createList(list1.stream()));
      }
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.BLOCK_ENTITY);
      Type<?> type1 = this.getInputSchema().getChoiceType(References.BLOCK_ENTITY, this.entityName);
      OpticFinder<?> opticfinder = DSL.namedChoice(this.entityName, type1);
      return this.fixTypeEverywhereTyped(
         "DropInvalidSignDataFix for " + this.entityName,
         type,
         p_392821_ -> p_392821_.updateTyped(
            opticfinder,
            type1,
            p_466458_ -> {
               boolean flag = ((Dynamic)p_466458_.get(DSL.remainderFinder())).get("_filtered_correct").asBoolean(false);
               return flag
                  ? p_466458_.update(DSL.remainderFinder(), p_392816_ -> p_392816_.remove("_filtered_correct"))
                  : Util.writeAndReadTypedOrThrow(p_466458_, type1, this::fix);
            }
         )
      );
   }
}
