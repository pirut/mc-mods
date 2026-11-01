package net.minecraft.client.renderer.item;

import com.google.common.base.Suppliers;
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class MissingItemModel implements ItemModel {
   private final List<BakedQuad> quads;
   private final Supplier<Vector3fc[]> extents;
   private final ModelRenderProperties properties;

   public MissingItemModel(List<BakedQuad> quads, ModelRenderProperties properties) {
      this.quads = quads;
      this.properties = properties;
      this.extents = Suppliers.memoize(() -> BlockModelWrapper.computeExtents(this.quads));
   }

   @Override
   public void update(
      ItemStackRenderState renderState,
      ItemStack stack,
      ItemModelResolver itemModelResolver,
      ItemDisplayContext displayContext,
      @Nullable ClientLevel level,
      @Nullable ItemOwner owner,
      int seed
   ) {
      renderState.appendModelIdentityElement(this);
      ItemStackRenderState.LayerRenderState layerRenderState = renderState.newLayer();
      layerRenderState.setRenderType(Sheets.cutoutBlockSheet());
      this.properties.applyToLayer(layerRenderState, displayContext);
      layerRenderState.setExtents(this.extents);
      layerRenderState.prepareQuadList().addAll(this.quads);
   }
}
