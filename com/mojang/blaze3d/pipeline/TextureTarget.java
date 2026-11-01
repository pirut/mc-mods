package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.systems.RenderSystem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TextureTarget extends RenderTarget {
   public TextureTarget(@Nullable String p_405118_, int p_166213_, int p_166214_, boolean p_166215_) {
      this(p_405118_, p_166213_, p_166214_, p_166215_, false);
   }

   public TextureTarget(@Nullable String p_405118_, int p_166213_, int p_166214_, boolean p_166215_, boolean enableStencil) {
      super(p_405118_, p_166215_, enableStencil);
      RenderSystem.assertOnRenderThread();
      this.resize(p_166213_, p_166214_);
   }
}
