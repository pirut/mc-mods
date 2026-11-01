package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.ARBVertexAttribBinding;
import org.lwjgl.opengl.GLCapabilities;

@Environment(EnvType.CLIENT)
public abstract class VertexArrayCache {
   public static VertexArrayCache create(GLCapabilities capabilities, GlDebugLabel debugLabel, Set<String> enabledExtensions) {
      if (capabilities.GL_ARB_vertex_attrib_binding && GlDevice.USE_GL_ARB_vertex_attrib_binding) {
         enabledExtensions.add("GL_ARB_vertex_attrib_binding");
         return new VertexArrayCache.Separate(debugLabel);
      } else {
         return new VertexArrayCache.Emulated(debugLabel);
      }
   }

   public abstract void bindVertexArray(VertexFormat format, @Nullable GlBuffer buffer);

   @Environment(EnvType.CLIENT)
   static class Emulated extends VertexArrayCache {
      private final Map<VertexFormat, VertexArrayCache.VertexArray> cache = new HashMap<>();
      private final GlDebugLabel debugLabels;

      public Emulated(GlDebugLabel debugLabels) {
         this.debugLabels = debugLabels;
      }

      @Override
      public void bindVertexArray(VertexFormat format, @Nullable GlBuffer buffer) {
         VertexArrayCache.VertexArray vertexArray = this.cache.get(format);
         if (vertexArray == null) {
            int i = GlStateManager._glGenVertexArrays();
            GlStateManager._glBindVertexArray(i);
            if (buffer != null) {
               GlStateManager._glBindBuffer(34962, buffer.handle);
               setupCombinedAttributes(format, true);
            }

            VertexArrayCache.VertexArray vertexArray2 = new VertexArrayCache.VertexArray(i, format, buffer);
            this.debugLabels.applyLabel(vertexArray2);
            this.cache.put(format, vertexArray2);
         } else {
            GlStateManager._glBindVertexArray(vertexArray.id);
            if (buffer != null && vertexArray.lastVertexBuffer != buffer) {
               GlStateManager._glBindBuffer(34962, buffer.handle);
               vertexArray.lastVertexBuffer = buffer;
               setupCombinedAttributes(format, false);
            }
         }
      }

      private static void setupCombinedAttributes(VertexFormat vertexFormat, boolean enabled) {
         int i = vertexFormat.getVertexSize();
         List<VertexFormatElement> list = vertexFormat.getElements();

         for (int j = 0; j < list.size(); j++) {
            VertexFormatElement vertexFormatElement = list.get(j);
            if (enabled) {
               GlStateManager._enableVertexAttribArray(j);
            }

            switch (vertexFormatElement.usage()) {
               case POSITION:
               case GENERIC:
               case UV:
                  if (vertexFormatElement.type() == VertexFormatElement.Type.FLOAT) {
                     GlStateManager._vertexAttribPointer(
                        j, vertexFormatElement.count(), GlConst.toGl(vertexFormatElement.type()), false, i, vertexFormat.getOffset(vertexFormatElement)
                     );
                  } else {
                     GlStateManager._vertexAttribIPointer(
                        j, vertexFormatElement.count(), GlConst.toGl(vertexFormatElement.type()), i, vertexFormat.getOffset(vertexFormatElement)
                     );
                  }
                  break;
               case NORMAL:
               case COLOR:
                  GlStateManager._vertexAttribPointer(
                     j, vertexFormatElement.count(), GlConst.toGl(vertexFormatElement.type()), true, i, vertexFormat.getOffset(vertexFormatElement)
                  );
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   static class Separate extends VertexArrayCache {
      private final Map<VertexFormat, VertexArrayCache.VertexArray> cache = new HashMap<>();
      private final GlDebugLabel debugLabels;
      private final boolean needsMesaWorkaround;

      public Separate(GlDebugLabel debugLabels) {
         this.debugLabels = debugLabels;
         if ("Mesa".equals(GlStateManager._getString(7936))) {
            String string = GlStateManager._getString(7938);
            this.needsMesaWorkaround = string.contains("25.0.0") || string.contains("25.0.1") || string.contains("25.0.2");
         } else {
            this.needsMesaWorkaround = false;
         }
      }

      @Override
      public void bindVertexArray(VertexFormat format, @Nullable GlBuffer buffer) {
         VertexArrayCache.VertexArray vertexArray = this.cache.get(format);
         if (vertexArray != null) {
            GlStateManager._glBindVertexArray(vertexArray.id);
            if (buffer != null && vertexArray.lastVertexBuffer != buffer) {
               if (this.needsMesaWorkaround && vertexArray.lastVertexBuffer != null && vertexArray.lastVertexBuffer.handle == buffer.handle) {
                  ARBVertexAttribBinding.glBindVertexBuffer(0, 0, 0L, 0);
               }

               ARBVertexAttribBinding.glBindVertexBuffer(0, buffer.handle, 0L, format.getVertexSize());
               vertexArray.lastVertexBuffer = buffer;
            }
         } else {
            int i = GlStateManager._glGenVertexArrays();
            GlStateManager._glBindVertexArray(i);
            if (buffer != null) {
               List<VertexFormatElement> list = format.getElements();

               for (int j = 0; j < list.size(); j++) {
                  VertexFormatElement vertexFormatElement = list.get(j);
                  GlStateManager._enableVertexAttribArray(j);
                  switch (vertexFormatElement.usage()) {
                     case POSITION:
                     case GENERIC:
                     case UV:
                        if (vertexFormatElement.type() == VertexFormatElement.Type.FLOAT) {
                           ARBVertexAttribBinding.glVertexAttribFormat(
                              j, vertexFormatElement.count(), GlConst.toGl(vertexFormatElement.type()), false, format.getOffset(vertexFormatElement)
                           );
                        } else {
                           ARBVertexAttribBinding.glVertexAttribIFormat(
                              j, vertexFormatElement.count(), GlConst.toGl(vertexFormatElement.type()), format.getOffset(vertexFormatElement)
                           );
                        }
                        break;
                     case NORMAL:
                     case COLOR:
                        ARBVertexAttribBinding.glVertexAttribFormat(
                           j, vertexFormatElement.count(), GlConst.toGl(vertexFormatElement.type()), true, format.getOffset(vertexFormatElement)
                        );
                  }

                  ARBVertexAttribBinding.glVertexAttribBinding(j, 0);
               }
            }

            if (buffer != null) {
               ARBVertexAttribBinding.glBindVertexBuffer(0, buffer.handle, 0L, format.getVertexSize());
            }

            VertexArrayCache.VertexArray vertexArray2 = new VertexArrayCache.VertexArray(i, format, buffer);
            this.debugLabels.applyLabel(vertexArray2);
            this.cache.put(format, vertexArray2);
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public static class VertexArray {
      final int id;
      final VertexFormat format;
      @Nullable
      GlBuffer lastVertexBuffer;

      VertexArray(int id, VertexFormat format, @Nullable GlBuffer lastVertexBuffer) {
         this.id = id;
         this.format = format;
         this.lastVertexBuffer = lastVertexBuffer;
      }
   }
}
