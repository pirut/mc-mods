package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class BlockEntityFurnaceBurnTimeFix extends NamedEntityFix {
   public BlockEntityFurnaceBurnTimeFix(Schema p_390603_, String p_390605_) {
      super(p_390603_, false, "BlockEntityFurnaceBurnTimeFix" + p_390605_, References.BLOCK_ENTITY, p_390605_);
   }

   public Dynamic<?> fixBurnTime(Dynamic<?> p_390609_) {
      p_390609_ = p_390609_.renameField("CookTime", "cooking_time_spent");
      p_390609_ = p_390609_.renameField("CookTimeTotal", "cooking_total_time");
      p_390609_ = p_390609_.renameField("BurnTime", "lit_time_remaining");
      return p_390609_.setFieldIfPresent("lit_total_time", p_390609_.get("lit_time_remaining").result());
   }

   @Override
   protected Typed<?> fix(Typed<?> p_390602_) {
      return p_390602_.update(DSL.remainderFinder(), this::fixBurnTime);
   }
}
