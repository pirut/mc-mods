package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.client.resources.model.Material;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ChestRenderState extends BlockEntityRenderState {
   public ChestType type = ChestType.SINGLE;
   public float open;
   public float angle;
   public ChestRenderState.ChestMaterialType material = ChestRenderState.ChestMaterialType.REGULAR;
   @Nullable
   public Material customMaterial;

   @OnlyIn(Dist.CLIENT)
   public static enum ChestMaterialType {
      ENDER_CHEST,
      CHRISTMAS,
      TRAPPED,
      COPPER_UNAFFECTED,
      COPPER_EXPOSED,
      COPPER_WEATHERED,
      COPPER_OXIDIZED,
      REGULAR;
   }
}
