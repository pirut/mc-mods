package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.util.OptionalInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class LightTexture implements AutoCloseable {
   public static final int FULL_BRIGHT = 15728880;
   public static final int FULL_SKY = 15728640;
   public static final int FULL_BLOCK = 240;
   private static final int TEXTURE_SIZE = 16;
   private static final int LIGHTMAP_UBO_SIZE = new Std140SizeCalculator()
      .putFloat()
      .putFloat()
      .putFloat()
      .putFloat()
      .putFloat()
      .putFloat()
      .putFloat()
      .putVec3()
      .putVec3()
      .get();
   private final GpuTexture texture;
   private final GpuTextureView textureView;
   private boolean updateLightTexture;
   private float blockLightRedFlicker;
   private final GameRenderer renderer;
   private final Minecraft minecraft;
   private final MappableRingBuffer ubo;
   private final RandomSource randomSource = RandomSource.create();

   public LightTexture(GameRenderer renderer, Minecraft minecraft) {
      this.renderer = renderer;
      this.minecraft = minecraft;
      GpuDevice gpuDevice = RenderSystem.getDevice();
      this.texture = gpuDevice.createTexture("Light Texture", 12, TextureFormat.RGBA8, 16, 16, 1, 1);
      this.textureView = gpuDevice.createTextureView(this.texture);
      gpuDevice.createCommandEncoder().clearColorTexture(this.texture, -1);
      this.ubo = new MappableRingBuffer(() -> "Lightmap UBO", 130, LIGHTMAP_UBO_SIZE);
   }

   public GpuTextureView getTextureView() {
      return this.textureView;
   }

   @Override
   public void close() {
      this.texture.close();
      this.textureView.close();
      this.ubo.close();
   }

   public void tick() {
      this.blockLightRedFlicker = this.blockLightRedFlicker
         + (this.randomSource.nextFloat() - this.randomSource.nextFloat()) * this.randomSource.nextFloat() * this.randomSource.nextFloat() * 0.1F;
      this.blockLightRedFlicker *= 0.9F;
      this.updateLightTexture = true;
   }

   private float calculateDarknessScale(LivingEntity entity, float gamma, float partialTick) {
      float f = 0.45F * gamma;
      return Math.max(0.0F, Mth.cos((entity.tickCount - partialTick) * (float) Math.PI * 0.025F) * f);
   }

   public void updateLightTexture(float partialTicks) {
      if (this.updateLightTexture) {
         this.updateLightTexture = false;
         ProfilerFiller profilerFiller = Profiler.get();
         profilerFiller.push("lightTex");
         ClientLevel clientLevel = this.minecraft.level;
         if (clientLevel != null) {
            Camera camera = this.minecraft.gameRenderer.getMainCamera();
            int i = camera.attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_COLOR, partialTicks);
            float f = clientLevel.dimensionType().ambientLight();
            float g = camera.attributeProbe().getValue(EnvironmentAttributes.SKY_LIGHT_FACTOR, partialTicks);
            EndFlashState endFlashState = clientLevel.endFlashState();
            Vector3f vector3f;
            if (endFlashState != null) {
               vector3f = new Vector3f(0.99F, 1.12F, 1.0F);
               if (!this.minecraft.options.hideLightningFlash().get()) {
                  float h = endFlashState.getIntensity(partialTicks);
                  if (this.minecraft.gui.getBossOverlay().shouldCreateWorldFog()) {
                     g += h / 3.0F;
                  } else {
                     g += h;
                  }
               }
            } else {
               vector3f = new Vector3f(1.0F, 1.0F, 1.0F);
            }

            float h = this.minecraft.options.darknessEffectScale().get().floatValue();
            float j = this.minecraft.player.getEffectBlendFactor(MobEffects.DARKNESS, partialTicks) * h;
            float k = this.calculateDarknessScale(this.minecraft.player, j, partialTicks) * h;
            float l = this.minecraft.player.getWaterVision();
            float m;
            if (this.minecraft.player.hasEffect(MobEffects.NIGHT_VISION)) {
               m = GameRenderer.getNightVisionScale(this.minecraft.player, partialTicks);
            } else if (l > 0.0F && this.minecraft.player.hasEffect(MobEffects.CONDUIT_POWER)) {
               m = l;
            } else {
               m = 0.0F;
            }

            float n = this.blockLightRedFlicker + 1.5F;
            float o = this.minecraft.options.gamma().get().floatValue();
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

            try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(this.ubo.currentBuffer(), false, true)) {
               Std140Builder.intoBuffer(mappedView.data())
                  .putFloat(f)
                  .putFloat(g)
                  .putFloat(n)
                  .putFloat(m)
                  .putFloat(k)
                  .putFloat(this.renderer.getDarkenWorldAmount(partialTicks))
                  .putFloat(Math.max(0.0F, o - j))
                  .putVec3(ARGB.vector3fFromRGB24(i))
                  .putVec3(vector3f);
            }

            try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "Update light", this.textureView, OptionalInt.empty())) {
               renderPass.setPipeline(RenderPipelines.LIGHTMAP);
               RenderSystem.bindDefaultUniforms(renderPass);
               renderPass.setUniform("LightmapInfo", this.ubo.currentBuffer());
               renderPass.draw(0, 3);
            }

            this.ubo.rotate();
            profilerFiller.pop();
         }
      }
   }

   public static float getBrightness(DimensionType dimensionType, int lightLevel) {
      return getBrightness(dimensionType.ambientLight(), lightLevel);
   }

   public static float getBrightness(float ambientLight, int lightLevel) {
      float f = lightLevel / 15.0F;
      float g = f / (4.0F - 3.0F * f);
      return Mth.lerp(ambientLight, g, 1.0F);
   }

   public static int pack(int blockLight, int skyLight) {
      return blockLight << 4 | skyLight << 20;
   }

   public static int block(int packedLight) {
      return packedLight >>> 4 & 15;
   }

   public static int sky(int packedLight) {
      return packedLight >>> 20 & 15;
   }

   public static int lightCoordsWithEmission(int packedLight, int emission) {
      if (emission == 0) {
         return packedLight;
      } else {
         int i = Math.max(sky(packedLight), emission);
         int j = Math.max(block(packedLight), emission);
         return pack(j, i);
      }
   }
}
