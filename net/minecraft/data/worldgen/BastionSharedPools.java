package net.minecraft.data.worldgen;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class BastionSharedPools {
   public static void bootstrap(BootstrapContext<StructureTemplatePool> p_321501_) {
      HolderGetter<StructureTemplatePool> $$1 = p_321501_.lookup(Registries.TEMPLATE_POOL);
      Holder<StructureTemplatePool> $$2 = $$1.getOrThrow(Pools.EMPTY);
      Pools.register(
         p_321501_,
         "bastion/mobs/piglin",
         new StructureTemplatePool(
            $$2,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/mobs/melee_piglin"), 1),
               Pair.of(StructurePoolElement.single("bastion/mobs/sword_piglin"), 4),
               Pair.of(StructurePoolElement.single("bastion/mobs/crossbow_piglin"), 4),
               Pair.of(StructurePoolElement.single("bastion/mobs/empty"), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_321501_,
         "bastion/mobs/hoglin",
         new StructureTemplatePool(
            $$2,
            ImmutableList.of(Pair.of(StructurePoolElement.single("bastion/mobs/hoglin"), 2), Pair.of(StructurePoolElement.single("bastion/mobs/empty"), 1)),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_321501_,
         "bastion/blocks/gold",
         new StructureTemplatePool(
            $$2,
            ImmutableList.of(Pair.of(StructurePoolElement.single("bastion/blocks/air"), 3), Pair.of(StructurePoolElement.single("bastion/blocks/gold"), 1)),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_321501_,
         "bastion/mobs/piglin_melee",
         new StructureTemplatePool(
            $$2,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/mobs/melee_piglin_always"), 1),
               Pair.of(StructurePoolElement.single("bastion/mobs/melee_piglin"), 5),
               Pair.of(StructurePoolElement.single("bastion/mobs/sword_piglin"), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
   }
}
