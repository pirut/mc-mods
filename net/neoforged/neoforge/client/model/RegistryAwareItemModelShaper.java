package net.neoforged.neoforge.client.model;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class RegistryAwareItemModelShaper extends ItemModelShaper {
   private final Map<Item, ModelResourceLocation> locations = Maps.newIdentityHashMap();
   private final Map<Item, BakedModel> models = Maps.newIdentityHashMap();

   public RegistryAwareItemModelShaper(ModelManager manager) {
      super(manager);
   }

   @Nullable
   @Override
   public BakedModel getItemModel(Item item) {
      return this.models.get(item);
   }

   @Override
   public void register(Item item, ModelResourceLocation location) {
      this.locations.put(item, location);
      this.models.put(item, this.getModelManager().getModel(location));
   }

   @Override
   public void rebuildCache() {
      ModelManager manager = this.getModelManager();

      for (Entry<Item, ModelResourceLocation> e : this.locations.entrySet()) {
         this.models.put(e.getKey(), manager.getModel(e.getValue()));
      }
   }

   public ModelResourceLocation getLocation(ItemStack stack) {
      ModelResourceLocation location = this.locations.get(stack.getItem());
      return location == null ? ModelBakery.MISSING_MODEL_VARIANT : location;
   }
}
