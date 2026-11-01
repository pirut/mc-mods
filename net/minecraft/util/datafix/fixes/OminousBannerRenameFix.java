package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Optional;

public class OminousBannerRenameFix extends ItemStackTagFix {
   public OminousBannerRenameFix(Schema p_216694_) {
      super(p_216694_, "OminousBannerRenameFix", p_216698_ -> p_216698_.equals("minecraft:white_banner"));
   }

   @Override
   protected <T> Dynamic<T> fixItemStackTag(Dynamic<T> p_216696_) {
      Optional<? extends Dynamic<?>> $$1 = p_216696_.get("display").result();
      if ($$1.isPresent()) {
         Dynamic<?> $$2 = (Dynamic<?>)$$1.get();
         Optional<String> $$3 = $$2.get("Name").asString().result();
         if ($$3.isPresent()) {
            String $$4 = $$3.get();
            $$4 = $$4.replace("\"translate\":\"block.minecraft.illager_banner\"", "\"translate\":\"block.minecraft.ominous_banner\"");
            $$2 = $$2.set("Name", $$2.createString($$4));
         }

         return p_216696_.set("display", $$2);
      } else {
         return p_216696_;
      }
   }
}
