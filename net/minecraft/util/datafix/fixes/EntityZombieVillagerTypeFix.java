package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.RandomSource;

public class EntityZombieVillagerTypeFix extends NamedEntityFix {
   private static final int PROFESSION_MAX = 6;

   public EntityZombieVillagerTypeFix(Schema p_15806_, boolean p_15807_) {
      super(p_15806_, p_15807_, "EntityZombieVillagerTypeFix", References.ENTITY, "Zombie");
   }

   public Dynamic<?> fixTag(Dynamic<?> p_15813_) {
      if (p_15813_.get("IsVillager").asBoolean(false)) {
         if (p_15813_.get("ZombieType").result().isEmpty()) {
            int $$1 = this.getVillagerProfession(p_15813_.get("VillagerProfession").asInt(-1));
            if ($$1 == -1) {
               $$1 = this.getVillagerProfession(RandomSource.create().nextInt(6));
            }

            p_15813_ = p_15813_.set("ZombieType", p_15813_.createInt($$1));
         }

         p_15813_ = p_15813_.remove("IsVillager");
      }

      return p_15813_;
   }

   private int getVillagerProfession(int p_15809_) {
      return p_15809_ >= 0 && p_15809_ < 6 ? p_15809_ : -1;
   }

   @Override
   protected Typed<?> fix(Typed<?> p_15811_) {
      return p_15811_.update(DSL.remainderFinder(), this::fixTag);
   }
}
