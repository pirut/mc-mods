package net.neoforged.neoforge.client.gui;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

@Internal
public class PictureInPictureRendererPool<T extends PictureInPictureRenderState> implements AutoCloseable {
   private final PictureInPictureRendererRegistration<T> factory;
   private final MultiBufferSource.BufferSource buffers;
   private Object2ObjectMap<T, PictureInPictureRenderer<T>> renderersLastFrame = new Object2ObjectOpenHashMap();
   private Object2ObjectMap<T, PictureInPictureRenderer<T>> renderersThisFrame = new Object2ObjectOpenHashMap();

   public PictureInPictureRendererPool(PictureInPictureRendererRegistration<T> factory, MultiBufferSource.BufferSource buffers) {
      this.factory = factory;
      this.buffers = buffers;
   }

   @Nullable
   public PictureInPictureRenderer<T> get(T state, int guiScale, boolean firstPass) {
      int width = (state.x1() - state.x0()) * guiScale;
      int height = (state.y1() - state.y0()) * guiScale;
      if (firstPass) {
         PictureInPictureRenderer<T> renderer = (PictureInPictureRenderer<T>)this.renderersLastFrame.get(state);
         if (renderer != null && renderer.canBeReusedFor(state, width, height)) {
            this.renderersLastFrame.remove(state);
            this.renderersThisFrame.put(state, renderer);
            return renderer;
         } else {
            return null;
         }
      } else {
         ObjectIterator<PictureInPictureRenderer<T>> it = this.renderersLastFrame.values().iterator();

         while (it.hasNext()) {
            PictureInPictureRenderer<T> renderer = (PictureInPictureRenderer<T>)it.next();
            if (renderer.canBeReusedFor(state, width, height)) {
               it.remove();
               this.renderersThisFrame.put(state, renderer);
               return renderer;
            }
         }

         PictureInPictureRenderer<T> renderer = this.factory.factory().apply(this.buffers);
         this.renderersThisFrame.put(state, renderer);
         return renderer;
      }
   }

   public void clearUnusedRenderers() {
      this.renderersLastFrame.values().forEach(PictureInPictureRenderer::close);
      this.renderersLastFrame.clear();
      Object2ObjectMap<T, PictureInPictureRenderer<T>> tmp = this.renderersLastFrame;
      this.renderersLastFrame = this.renderersThisFrame;
      this.renderersThisFrame = tmp;
   }

   @Override
   public void close() {
      this.renderersThisFrame.values().forEach(PictureInPictureRenderer::close);
      this.renderersLastFrame.values().forEach(PictureInPictureRenderer::close);
   }

   public static Map<Class<? extends PictureInPictureRenderState>, PictureInPictureRendererPool<?>> createPools(
      MultiBufferSource.BufferSource bufferSource, List<PictureInPictureRendererRegistration<?>> pipRendererFactories
   ) {
      Builder<Class<? extends PictureInPictureRenderState>, PictureInPictureRendererPool<?>> builder = ImmutableMap.builder();

      for (PictureInPictureRendererRegistration<?> factory : pipRendererFactories) {
         builder.put(factory.stateClass(), new PictureInPictureRendererPool<>(factory, bufferSource));
      }

      return builder.buildOrThrow();
   }
}
