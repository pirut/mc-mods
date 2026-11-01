package net.minecraft.data.worldgen;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

public class BastionPieces {
   public static final ResourceKey<StructureTemplatePool> START = Pools.createKey("bastion/starts");

   public static void bootstrap(BootstrapContext<StructureTemplatePool> p_321797_) {
      HolderGetter<StructureProcessorList> $$1 = p_321797_.lookup(Registries.PROCESSOR_LIST);
      Holder<StructureProcessorList> $$2 = $$1.getOrThrow(ProcessorLists.BASTION_GENERIC_DEGRADATION);
      HolderGetter<StructureTemplatePool> $$3 = p_321797_.lookup(Registries.TEMPLATE_POOL);
      Holder<StructureTemplatePool> $$4 = $$3.getOrThrow(Pools.EMPTY);
      p_321797_.register(
         START,
         new StructureTemplatePool(
            $$4,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/units/air_base", $$2), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/air_base", $$2), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/big_air_full", $$2), 1),
               Pair.of(StructurePoolElement.single("bastion/bridge/starting_pieces/entrance_base", $$2), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      BastionHousingUnitsPools.bootstrap(p_321797_);
      BastionHoglinStablePools.bootstrap(p_321797_);
      BastionTreasureRoomPools.bootstrap(p_321797_);
      BastionBridgePools.bootstrap(p_321797_);
      BastionSharedPools.bootstrap(p_321797_);
   }
}
