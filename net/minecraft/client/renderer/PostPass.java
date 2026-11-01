package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.IOException;
import java.util.List;
import java.util.function.IntSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class PostPass implements AutoCloseable {
   private final EffectInstance effect;
   public final RenderTarget inTarget;
   public final RenderTarget outTarget;
   private final List<IntSupplier> auxAssets = Lists.newArrayList();
   private final List<String> auxNames = Lists.newArrayList();
   private final List<Integer> auxWidths = Lists.newArrayList();
   private final List<Integer> auxHeights = Lists.newArrayList();
   private Matrix4f shaderOrthoMatrix;
   private final int filterMode;

   public PostPass(ResourceProvider p_331087_, String p_110062_, RenderTarget p_110063_, RenderTarget p_110064_, boolean p_333761_) throws IOException {
      this.effect = new EffectInstance(p_331087_, p_110062_);
      this.inTarget = p_110063_;
      this.outTarget = p_110064_;
      this.filterMode = p_333761_ ? 9729 : 9728;
   }

   @Override
   public void close() {
      this.effect.close();
   }

   public final String getName() {
      return this.effect.getName();
   }

   public void addAuxAsset(String p_110070_, IntSupplier p_110071_, int p_110072_, int p_110073_) {
      this.auxNames.add(this.auxNames.size(), p_110070_);
      this.auxAssets.add(this.auxAssets.size(), p_110071_);
      this.auxWidths.add(this.auxWidths.size(), p_110072_);
      this.auxHeights.add(this.auxHeights.size(), p_110073_);
   }

   public void setOrthoMatrix(Matrix4f p_253811_) {
      this.shaderOrthoMatrix = p_253811_;
   }

   public void process(float p_110066_) {
      this.inTarget.unbindWrite();
      float $$1 = this.outTarget.width;
      float $$2 = this.outTarget.height;
      RenderSystem.viewport(0, 0, (int)$$1, (int)$$2);
      this.effect.setSampler("DiffuseSampler", this.inTarget::getColorTextureId);

      for (int $$3 = 0; $$3 < this.auxAssets.size(); $$3++) {
         this.effect.setSampler(this.auxNames.get($$3), this.auxAssets.get($$3));
         this.effect.safeGetUniform("AuxSize" + $$3).set((float)this.auxWidths.get($$3).intValue(), (float)this.auxHeights.get($$3).intValue());
      }

      this.effect.safeGetUniform("ProjMat").set(this.shaderOrthoMatrix);
      this.effect.safeGetUniform("InSize").set((float)this.inTarget.width, (float)this.inTarget.height);
      this.effect.safeGetUniform("OutSize").set($$1, $$2);
      this.effect.safeGetUniform("Time").set(p_110066_);
      Minecraft $$4 = Minecraft.getInstance();
      this.effect.safeGetUniform("ScreenSize").set((float)$$4.getWindow().getWidth(), (float)$$4.getWindow().getHeight());
      this.effect.apply();
      this.outTarget.clear(Minecraft.ON_OSX);
      this.outTarget.bindWrite(false);
      RenderSystem.depthFunc(519);
      BufferBuilder $$5 = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
      $$5.addVertex(0.0F, 0.0F, 500.0F);
      $$5.addVertex($$1, 0.0F, 500.0F);
      $$5.addVertex($$1, $$2, 500.0F);
      $$5.addVertex(0.0F, $$2, 500.0F);
      BufferUploader.draw($$5.buildOrThrow());
      RenderSystem.depthFunc(515);
      this.effect.clear();
      this.outTarget.unbindWrite();
      this.inTarget.unbindRead();

      for (Object $$6 : this.auxAssets) {
         if ($$6 instanceof RenderTarget) {
            ((RenderTarget)$$6).unbindRead();
         }
      }
   }

   public EffectInstance getEffect() {
      return this.effect;
   }

   public int getFilterMode() {
      return this.filterMode;
   }
}
