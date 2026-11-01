package net.minecraft.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectSortedMaps;
import java.util.HashMap;
import java.util.Map;
import java.util.SequencedMap;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface MultiBufferSource {
   static MultiBufferSource.BufferSource immediate(ByteBufferBuilder p_350664_) {
      return immediateWithBuffers(Object2ObjectSortedMaps.emptyMap(), p_350664_);
   }

   static MultiBufferSource.BufferSource immediateWithBuffers(SequencedMap<RenderType, ByteBufferBuilder> p_352388_, ByteBufferBuilder p_350332_) {
      return new MultiBufferSource.BufferSource(p_350332_, p_352388_);
   }

   VertexConsumer getBuffer(RenderType var1);

   @OnlyIn(Dist.CLIENT)
   public static class BufferSource implements MultiBufferSource {
      protected final ByteBufferBuilder sharedBuffer;
      protected final SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers;
      protected final Map<RenderType, BufferBuilder> startedBuilders = new HashMap<>();
      @Nullable
      protected RenderType lastSharedType;

      protected BufferSource(ByteBufferBuilder p_350964_, SequencedMap<RenderType, ByteBufferBuilder> p_352181_) {
         this.sharedBuffer = p_350964_;
         this.fixedBuffers = p_352181_;
      }

      @Override
      public VertexConsumer getBuffer(RenderType p_109919_) {
         BufferBuilder $$1 = this.startedBuilders.get(p_109919_);
         if ($$1 != null && !p_109919_.canConsolidateConsecutiveGeometry()) {
            this.endBatch(p_109919_, $$1);
            $$1 = null;
         }

         if ($$1 != null) {
            return $$1;
         } else {
            ByteBufferBuilder $$2 = this.fixedBuffers.get(p_109919_);
            if ($$2 != null) {
               $$1 = new BufferBuilder($$2, p_109919_.mode(), p_109919_.format());
            } else {
               if (this.lastSharedType != null) {
                  this.endBatch(this.lastSharedType);
               }

               $$1 = new BufferBuilder(this.sharedBuffer, p_109919_.mode(), p_109919_.format());
               this.lastSharedType = p_109919_;
            }

            this.startedBuilders.put(p_109919_, $$1);
            return $$1;
         }
      }

      public void endLastBatch() {
         if (this.lastSharedType != null) {
            this.endBatch(this.lastSharedType);
            this.lastSharedType = null;
         }
      }

      public void endBatch() {
         this.endLastBatch();

         for (RenderType $$0 : this.fixedBuffers.keySet()) {
            this.endBatch($$0);
         }
      }

      public void endBatch(RenderType p_109913_) {
         BufferBuilder $$1 = this.startedBuilders.remove(p_109913_);
         if ($$1 != null) {
            this.endBatch(p_109913_, $$1);
         }
      }

      private void endBatch(RenderType p_350903_, BufferBuilder p_350797_) {
         MeshData $$2 = p_350797_.build();
         if ($$2 != null) {
            if (p_350903_.sortOnUpload()) {
               ByteBufferBuilder $$3 = this.fixedBuffers.getOrDefault(p_350903_, this.sharedBuffer);
               $$2.sortQuads($$3, RenderSystem.getVertexSorting());
            }

            p_350903_.draw($$2);
         }

         if (p_350903_.equals(this.lastSharedType)) {
            this.lastSharedType = null;
         }
      }
   }
}
