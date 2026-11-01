package net.minecraft.client.resources.model;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModelGroupCollector {
   static final int SINGLETON_MODEL_GROUP = -1;
   private static final int INVISIBLE_MODEL_GROUP = 0;

   public static Object2IntMap<BlockState> build(BlockColors p_364277_, BlockStateModelLoader.LoadedModels p_360427_) {
      Map<Block, List<Property<?>>> map = new HashMap<>();
      Map<ModelGroupCollector.GroupKey, Set<BlockState>> map1 = new HashMap<>();
      p_360427_.models().forEach((p_409114_, p_409115_) -> {
         List<Property<?>> list = map.computeIfAbsent(p_409114_.getBlock(), p_362091_ -> List.copyOf(p_364277_.getColoringProperties(p_362091_)));
         ModelGroupCollector.GroupKey modelgroupcollector$groupkey = ModelGroupCollector.GroupKey.create(p_409114_, p_409115_, list);
         map1.computeIfAbsent(modelgroupcollector$groupkey, p_361541_ -> Sets.newIdentityHashSet()).add(p_409114_);
      });
      int i = 1;
      Object2IntMap<BlockState> object2intmap = new Object2IntOpenHashMap();
      object2intmap.defaultReturnValue(-1);

      for (Set<BlockState> set : map1.values()) {
         Iterator<BlockState> iterator = set.iterator();

         while (iterator.hasNext()) {
            BlockState blockstate = iterator.next();
            if (blockstate.getRenderShape() != RenderShape.MODEL) {
               iterator.remove();
               object2intmap.put(blockstate, 0);
            }
         }

         if (set.size() > 1) {
            int j = i++;
            set.forEach(p_365109_ -> object2intmap.put(p_365109_, j));
         }
      }

      return object2intmap;
   }

   @OnlyIn(Dist.CLIENT)
   record GroupKey(Object equalityGroup, List<Object> coloringValues) {
      public static ModelGroupCollector.GroupKey create(BlockState p_361701_, BlockStateModel.UnbakedRoot p_409921_, List<Property<?>> p_362442_) {
         List<Object> list = getColoringValues(p_361701_, p_362442_);
         Object object = p_409921_.visualEqualityGroup(p_361701_);
         return new ModelGroupCollector.GroupKey(object, list);
      }

      private static List<Object> getColoringValues(BlockState p_362069_, List<Property<?>> p_363650_) {
         Object[] aobject = new Object[p_363650_.size()];

         for (int i = 0; i < p_363650_.size(); i++) {
            aobject[i] = p_362069_.getValue(p_363650_.get(i));
         }

         return List.of(aobject);
      }
   }
}
