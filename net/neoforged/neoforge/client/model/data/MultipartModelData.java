package net.neoforged.neoforge.client.model.data;

import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class MultipartModelData {
   private static final ModelProperty<Map<BakedModel, ModelData>> PROPERTY = new ModelProperty<>();

   public static ModelData resolve(ModelData modelData, BakedModel model) {
      Map<BakedModel, ModelData> multipartData = modelData.get(PROPERTY);
      if (multipartData == null) {
         return modelData;
      } else {
         ModelData partData = multipartData.get(model);
         return partData != null ? partData : modelData;
      }
   }

   public static ModelData create(
      List<Pair<Predicate<BlockState>, BakedModel>> selectors, BitSet bitset, BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData tileModelData
   ) {
      Map<BakedModel, ModelData> dataMap = null;

      for (int i = 0; i < bitset.length(); i++) {
         if (bitset.get(i)) {
            BakedModel model = (BakedModel)selectors.get(i).getRight();
            ModelData data = model.getModelData(level, pos, state, tileModelData);
            if (data != tileModelData) {
               if (dataMap == null) {
                  dataMap = new IdentityHashMap<>();
               }

               dataMap.put(model, data);
            }
         }
      }

      return dataMap == null ? tileModelData : tileModelData.derive().with(PROPERTY, dataMap).build();
   }
}
