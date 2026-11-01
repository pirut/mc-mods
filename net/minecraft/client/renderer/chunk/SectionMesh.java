package net.minecraft.client.renderer.chunk;

import java.util.Collections;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface SectionMesh extends AutoCloseable {
   default boolean isDifferentPointOfView(TranslucencyPointOfView p_427309_) {
      return false;
   }

   default boolean hasRenderableLayers() {
      return false;
   }

   default boolean hasTranslucentGeometry() {
      return false;
   }

   default boolean isEmpty(ChunkSectionLayer p_427270_) {
      return true;
   }

   default List<BlockEntity> getRenderableBlockEntities() {
      return Collections.emptyList();
   }

   boolean facesCanSeeEachother(Direction var1, Direction var2);

   @Nullable
   default SectionBuffers getBuffers(ChunkSectionLayer p_427238_) {
      return null;
   }

   @Override
   default void close() {
   }
}
