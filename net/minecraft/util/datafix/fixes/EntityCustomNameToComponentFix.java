package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.LegacyComponentDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class EntityCustomNameToComponentFix extends DataFix {
   public EntityCustomNameToComponentFix(Schema p_15398_) {
      super(p_15398_, true);
   }

   public TypeRewriteRule makeRule() {
      Type<?> type = this.getInputSchema().getType(References.ENTITY);
      Type<?> type1 = this.getOutputSchema().getType(References.ENTITY);
      OpticFinder<String> opticfinder = DSL.fieldFinder("id", NamespacedSchema.namespacedString());
      OpticFinder<String> opticfinder1 = type.findField("CustomName");
      Type<?> type2 = type1.findFieldType("CustomName");
      return this.fixTypeEverywhereTyped(
         "EntityCustomNameToComponentFix", type, type1, p_428677_ -> fixEntity(p_428677_, type1, opticfinder, opticfinder1, type2)
      );
   }

   private static <T> Typed<?> fixEntity(Typed<?> p_394132_, Type<?> p_393629_, OpticFinder<String> p_394022_, OpticFinder<String> p_394085_, Type<T> p_428700_) {
      Optional<String> optional = p_394132_.getOptional(p_394085_);
      if (optional.isEmpty()) {
         return ExtraDataFixUtils.cast(p_393629_, (Typed<T>)p_394132_);
      } else if (optional.get().isEmpty()) {
         return Util.writeAndReadTypedOrThrow(p_394132_, p_393629_, p_428672_ -> p_428672_.remove("CustomName"));
      } else {
         String s = p_394132_.getOptional(p_394022_).orElse("");
         Dynamic<?> dynamic = fixCustomName(p_394132_.getOps(), optional.get(), s);
         return p_394132_.set(p_394085_, Util.readTypedOrThrow(p_428700_, dynamic));
      }
   }

   private static <T> Dynamic<T> fixCustomName(DynamicOps<T> p_393598_, String p_394587_, String p_393526_) {
      return "minecraft:commandblock_minecart".equals(p_393526_)
         ? new Dynamic(p_393598_, p_393598_.createString(p_394587_))
         : LegacyComponentDataFixUtils.createPlainTextComponent(p_393598_, p_394587_);
   }
}
