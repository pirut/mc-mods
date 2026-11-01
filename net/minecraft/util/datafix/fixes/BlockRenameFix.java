package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public abstract class BlockRenameFix extends DataFix {
   private final String name;

   public BlockRenameFix(Schema p_14910_, String p_14911_) {
      super(p_14910_, false);
      this.name = p_14911_;
   }

   public TypeRewriteRule makeRule() {
      Type<?> $$0 = this.getInputSchema().getType(References.BLOCK_NAME);
      Type<Pair<String, String>> $$1 = DSL.named(References.BLOCK_NAME.typeName(), NamespacedSchema.namespacedString());
      if (!Objects.equals($$0, $$1)) {
         throw new IllegalStateException("block type is not what was expected.");
      } else {
         TypeRewriteRule $$2 = this.fixTypeEverywhere(this.name + " for block", $$1, p_14923_ -> p_145145_ -> p_145145_.mapSecond(this::renameBlock));
         TypeRewriteRule $$3 = this.fixTypeEverywhereTyped(
            this.name + " for block_state",
            this.getInputSchema().getType(References.BLOCK_STATE),
            p_14913_ -> p_14913_.update(DSL.remainderFinder(), this::fixBlockState)
         );
         TypeRewriteRule $$4 = this.fixTypeEverywhereTyped(
            this.name + " for flat_block_state",
            this.getInputSchema().getType(References.FLAT_BLOCK_STATE),
            p_315924_ -> p_315924_.update(
               DSL.remainderFinder(),
               p_337603_ -> (Dynamic)DataFixUtils.orElse(p_337603_.asString().result().map(this::fixFlatBlockState).map(p_337603_::createString), p_337603_)
            )
         );
         return TypeRewriteRule.seq($$2, new TypeRewriteRule[]{$$3, $$4});
      }
   }

   private Dynamic<?> fixBlockState(Dynamic<?> p_316274_) {
      Optional<String> $$1 = p_316274_.get("Name").asString().result();
      return $$1.isPresent() ? p_316274_.set("Name", p_316274_.createString(this.renameBlock($$1.get()))) : p_316274_;
   }

   private String fixFlatBlockState(String p_316551_) {
      int $$1 = p_316551_.indexOf(91);
      int $$2 = p_316551_.indexOf(123);
      int $$3 = p_316551_.length();
      if ($$1 > 0) {
         $$3 = $$1;
      }

      if ($$2 > 0) {
         $$3 = Math.min($$3, $$2);
      }

      String $$4 = p_316551_.substring(0, $$3);
      String $$5 = this.renameBlock($$4);
      return $$5 + p_316551_.substring($$3);
   }

   protected abstract String renameBlock(String var1);

   public static DataFix create(Schema p_14915_, String p_14916_, final Function<String, String> p_14917_) {
      return new BlockRenameFix(p_14915_, p_14916_) {
         @Override
         protected String renameBlock(String p_316686_) {
            return p_14917_.apply(p_316686_);
         }
      };
   }
}
