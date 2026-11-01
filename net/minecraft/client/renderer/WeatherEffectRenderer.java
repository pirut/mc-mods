package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.WeatherRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

@Environment(EnvType.CLIENT)
public class WeatherEffectRenderer {
   private static final float RAIN_PARTICLES_PER_BLOCK = 0.225F;
   private static final int RAIN_RADIUS = 10;
   private static final Identifier RAIN_LOCATION = Identifier.withDefaultNamespace("textures/environment/rain.png");
   private static final Identifier SNOW_LOCATION = Identifier.withDefaultNamespace("textures/environment/snow.png");
   private static final int RAIN_TABLE_SIZE = 32;
   private static final int HALF_RAIN_TABLE_SIZE = 16;
   private int rainSoundTime;
   private final float[] columnSizeX = new float[1024];
   private final float[] columnSizeZ = new float[1024];

   public WeatherEffectRenderer() {
      for (int i = 0; i < 32; i++) {
         for (int j = 0; j < 32; j++) {
            float f = j - 16;
            float g = i - 16;
            float h = Mth.length(f, g);
            this.columnSizeX[i * 32 + j] = -g / h;
            this.columnSizeZ[i * 32 + j] = f / h;
         }
      }
   }

   public void extractRenderState(Level level, int ticks, float partialTick, Vec3 cameraPosition, WeatherRenderState reusedState) {
      reusedState.intensity = level.getRainLevel(partialTick);
      if (!(reusedState.intensity <= 0.0F)) {
         reusedState.radius = Minecraft.getInstance().options.weatherRadius().get();
         int i = Mth.floor(cameraPosition.x);
         int j = Mth.floor(cameraPosition.y);
         int k = Mth.floor(cameraPosition.z);
         BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
         RandomSource randomSource = RandomSource.create();

         for (int l = k - reusedState.radius; l <= k + reusedState.radius; l++) {
            for (int m = i - reusedState.radius; m <= i + reusedState.radius; m++) {
               int n = level.getHeight(Heightmap.Types.MOTION_BLOCKING, m, l);
               int o = Math.max(j - reusedState.radius, n);
               int p = Math.max(j + reusedState.radius, n);
               if (p - o != 0) {
                  Biome.Precipitation precipitation = this.getPrecipitationAt(level, mutableBlockPos.set(m, j, l));
                  if (precipitation != Biome.Precipitation.NONE) {
                     int q = m * m * 3121 + m * 45238971 ^ l * l * 418711 + l * 13761;
                     randomSource.setSeed(q);
                     int r = Math.max(j, n);
                     int s = LevelRenderer.getLightColor(level, mutableBlockPos.set(m, r, l));
                     if (precipitation == Biome.Precipitation.RAIN) {
                        reusedState.rainColumns.add(this.createRainColumnInstance(randomSource, ticks, m, o, p, l, s, partialTick));
                     } else if (precipitation == Biome.Precipitation.SNOW) {
                        reusedState.snowColumns.add(this.createSnowColumnInstance(randomSource, ticks, m, o, p, l, s, partialTick));
                     }
                  }
               }
            }
         }
      }
   }

   public void render(MultiBufferSource bufferSource, Vec3 cameraPosition, WeatherRenderState renderState) {
      if (!renderState.rainColumns.isEmpty()) {
         RenderType renderType = RenderTypes.weather(RAIN_LOCATION, Minecraft.useShaderTransparency());
         this.renderInstances(bufferSource.getBuffer(renderType), renderState.rainColumns, cameraPosition, 1.0F, renderState.radius, renderState.intensity);
      }

      if (!renderState.snowColumns.isEmpty()) {
         RenderType renderType = RenderTypes.weather(SNOW_LOCATION, Minecraft.useShaderTransparency());
         this.renderInstances(bufferSource.getBuffer(renderType), renderState.snowColumns, cameraPosition, 0.8F, renderState.radius, renderState.intensity);
      }
   }

   private WeatherEffectRenderer.ColumnInstance createRainColumnInstance(
      RandomSource random, int ticks, int x, int bottomY, int topY, int z, int lightCoords, float partialTick
   ) {
      int i = ticks & 131071;
      int j = x * x * 3121 + x * 45238971 + z * z * 418711 + z * 13761 & 0xFF;
      float f = 3.0F + random.nextFloat();
      float g = -(i + j + partialTick) / 32.0F * f;
      float h = g % 32.0F;
      return new WeatherEffectRenderer.ColumnInstance(x, z, bottomY, topY, 0.0F, h, lightCoords);
   }

   private WeatherEffectRenderer.ColumnInstance createSnowColumnInstance(
      RandomSource random, int ticks, int x, int bottomY, int topY, int z, int lightCoords, float partialTick
   ) {
      float f = ticks + partialTick;
      float g = (float)(random.nextDouble() + f * 0.01F * (float)random.nextGaussian());
      float h = (float)(random.nextDouble() + f * (float)random.nextGaussian() * 0.001F);
      float i = -((ticks & 511) + partialTick) / 512.0F;
      int j = LightTexture.pack((LightTexture.block(lightCoords) * 3 + 15) / 4, (LightTexture.sky(lightCoords) * 3 + 15) / 4);
      return new WeatherEffectRenderer.ColumnInstance(x, z, bottomY, topY, g, i + h, j);
   }

   private void renderInstances(
      VertexConsumer buffer, List<WeatherEffectRenderer.ColumnInstance> columnInstances, Vec3 cameraPosition, float amount, int radius, float rainLevel
   ) {
      float f = radius * radius;

      for (WeatherEffectRenderer.ColumnInstance columnInstance : columnInstances) {
         float g = (float)(columnInstance.x + 0.5 - cameraPosition.x);
         float h = (float)(columnInstance.z + 0.5 - cameraPosition.z);
         float i = (float)Mth.lengthSquared(g, h);
         float j = Mth.lerp(Math.min(i / f, 1.0F), amount, 0.5F) * rainLevel;
         int k = ARGB.white(j);
         int l = (columnInstance.z - Mth.floor(cameraPosition.z) + 16) * 32 + columnInstance.x - Mth.floor(cameraPosition.x) + 16;
         float m = this.columnSizeX[l] / 2.0F;
         float n = this.columnSizeZ[l] / 2.0F;
         float o = g - m;
         float p = g + m;
         float q = (float)(columnInstance.topY - cameraPosition.y);
         float r = (float)(columnInstance.bottomY - cameraPosition.y);
         float s = h - n;
         float t = h + n;
         float u = columnInstance.uOffset + 0.0F;
         float v = columnInstance.uOffset + 1.0F;
         float w = columnInstance.bottomY * 0.25F + columnInstance.vOffset;
         float x = columnInstance.topY * 0.25F + columnInstance.vOffset;
         buffer.addVertex(o, q, s).setUv(u, w).setColor(k).setLight(columnInstance.lightCoords);
         buffer.addVertex(p, q, t).setUv(v, w).setColor(k).setLight(columnInstance.lightCoords);
         buffer.addVertex(p, r, t).setUv(v, x).setColor(k).setLight(columnInstance.lightCoords);
         buffer.addVertex(o, r, s).setUv(u, x).setColor(k).setLight(columnInstance.lightCoords);
      }
   }

   public void tickRainParticles(ClientLevel level, Camera camera, int ticks, ParticleStatus particleStatus, int weatherRadius) {
      float f = level.getRainLevel(1.0F);
      if (!(f <= 0.0F)) {
         RandomSource randomSource = RandomSource.create(ticks * 312987231L);
         BlockPos blockPos = BlockPos.containing(camera.position());
         BlockPos blockPos2 = null;
         int i = 2 * weatherRadius + 1;
         int j = i * i;
         int k = (int)(0.225F * j * f * f) / (particleStatus == ParticleStatus.DECREASED ? 2 : 1);

         for (int l = 0; l < k; l++) {
            int m = randomSource.nextInt(i) - weatherRadius;
            int n = randomSource.nextInt(i) - weatherRadius;
            BlockPos blockPos3 = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockPos.offset(m, 0, n));
            if (blockPos3.getY() > level.getMinY()
               && blockPos3.getY() <= blockPos.getY() + 10
               && blockPos3.getY() >= blockPos.getY() - 10
               && this.getPrecipitationAt(level, blockPos3) == Biome.Precipitation.RAIN) {
               blockPos2 = blockPos3.below();
               if (particleStatus == ParticleStatus.MINIMAL) {
                  break;
               }

               double d = randomSource.nextDouble();
               double e = randomSource.nextDouble();
               BlockState blockState = level.getBlockState(blockPos2);
               FluidState fluidState = level.getFluidState(blockPos2);
               VoxelShape voxelShape = blockState.getCollisionShape(level, blockPos2);
               double g = voxelShape.max(Direction.Axis.Y, d, e);
               double h = fluidState.getHeight(level, blockPos2);
               double o = Math.max(g, h);
               ParticleOptions particleOptions = !fluidState.is(FluidTags.LAVA)
                     && !blockState.is(Blocks.MAGMA_BLOCK)
                     && !CampfireBlock.isLitCampfire(blockState)
                  ? ParticleTypes.RAIN
                  : ParticleTypes.SMOKE;
               level.addParticle(particleOptions, blockPos2.getX() + d, blockPos2.getY() + o, blockPos2.getZ() + e, 0.0, 0.0, 0.0);
            }
         }

         if (blockPos2 != null && randomSource.nextInt(3) < this.rainSoundTime++) {
            this.rainSoundTime = 0;
            if (blockPos2.getY() > blockPos.getY() + 1
               && level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockPos).getY() > Mth.floor((float)blockPos.getY())) {
               level.playLocalSound(blockPos2, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.WEATHER, 0.1F, 0.5F, false);
            } else {
               level.playLocalSound(blockPos2, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.2F, 1.0F, false);
            }
         }
      }
   }

   private Biome.Precipitation getPrecipitationAt(Level level, BlockPos pos) {
      if (!level.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()))) {
         return Biome.Precipitation.NONE;
      } else {
         Biome biome = level.getBiome(pos).value();
         return biome.getPrecipitationAt(pos, level.getSeaLevel());
      }
   }

   @Environment(EnvType.CLIENT)
   public record ColumnInstance(int x, int z, int bottomY, int topY, float uOffset, float vOffset, int lightCoords) {
   }
}
