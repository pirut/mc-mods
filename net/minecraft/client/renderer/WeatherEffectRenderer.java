package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.LevelRenderState;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.CustomEnvironmentEffectsRendererManager;
import net.neoforged.neoforge.client.CustomWeatherEffectRenderer;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
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
            float f1 = i - 16;
            float f2 = Mth.length(f, f1);
            this.columnSizeX[i * 32 + j] = -f1 / f2;
            this.columnSizeZ[i * 32 + j] = f / f2;
         }
      }
   }

   public void extractRenderState(Level p_450973_, int p_451472_, float p_451138_, Vec3 p_451294_, WeatherRenderState p_451306_) {
      p_451306_.intensity = p_450973_.getRainLevel(p_451138_);
      if (!(p_451306_.intensity <= 0.0F)) {
         p_451306_.radius = Minecraft.getInstance().options.weatherRadius().get();
         int i = Mth.floor(p_451294_.x);
         int j = Mth.floor(p_451294_.y);
         int k = Mth.floor(p_451294_.z);
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
         RandomSource randomsource = RandomSource.create();

         for (int l = k - p_451306_.radius; l <= k + p_451306_.radius; l++) {
            for (int i1 = i - p_451306_.radius; i1 <= i + p_451306_.radius; i1++) {
               int j1 = p_450973_.getHeight(Heightmap.Types.MOTION_BLOCKING, i1, l);
               int k1 = Math.max(j - p_451306_.radius, j1);
               int l1 = Math.max(j + p_451306_.radius, j1);
               if (l1 - k1 != 0) {
                  Biome.Precipitation biome$precipitation = this.getPrecipitationAt(p_450973_, blockpos$mutableblockpos.set(i1, j, l));
                  if (biome$precipitation != Biome.Precipitation.NONE) {
                     int i2 = i1 * i1 * 3121 + i1 * 45238971 ^ l * l * 418711 + l * 13761;
                     randomsource.setSeed(i2);
                     int j2 = Math.max(j, j1);
                     int k2 = LevelRenderer.getLightColor(p_450973_, blockpos$mutableblockpos.set(i1, j2, l));
                     if (biome$precipitation == Biome.Precipitation.RAIN) {
                        p_451306_.rainColumns.add(this.createRainColumnInstance(randomsource, p_451472_, i1, k1, l1, l, k2, p_451138_));
                     } else if (biome$precipitation == Biome.Precipitation.SNOW) {
                        p_451306_.snowColumns.add(this.createSnowColumnInstance(randomsource, p_451472_, i1, k1, l1, l, k2, p_451138_));
                     }
                  }
               }
            }
         }
      }
   }

   @Deprecated
   public void render(MultiBufferSource p_382792_, Vec3 p_360652_, WeatherRenderState p_451561_) {
      this.render(p_382792_, p_360652_, p_451561_, null);
   }

   public void render(MultiBufferSource p_382792_, Vec3 p_360652_, WeatherRenderState p_451561_, @Nullable LevelRenderState levelRenderState) {
      if (levelRenderState == null
         || levelRenderState.customWeatherEffectRenderer == null
         || !levelRenderState.customWeatherEffectRenderer.renderSnowAndRain(levelRenderState, p_451561_, p_382792_, p_360652_)) {
         if (!p_451561_.rainColumns.isEmpty()) {
            RenderType rendertype = RenderTypes.weather(RAIN_LOCATION, Minecraft.useShaderTransparency());
            this.renderInstances(p_382792_.getBuffer(rendertype), p_451561_.rainColumns, p_360652_, 1.0F, p_451561_.radius, p_451561_.intensity);
         }

         if (!p_451561_.snowColumns.isEmpty()) {
            RenderType rendertype1 = RenderTypes.weather(SNOW_LOCATION, Minecraft.useShaderTransparency());
            this.renderInstances(p_382792_.getBuffer(rendertype1), p_451561_.snowColumns, p_360652_, 0.8F, p_451561_.radius, p_451561_.intensity);
         }
      }
   }

   private WeatherEffectRenderer.ColumnInstance createRainColumnInstance(
      RandomSource p_364494_, int p_361188_, int p_362466_, int p_364844_, int p_361656_, int p_364160_, int p_361622_, float p_363800_
   ) {
      int i = p_361188_ & 131071;
      int j = p_362466_ * p_362466_ * 3121 + p_362466_ * 45238971 + p_364160_ * p_364160_ * 418711 + p_364160_ * 13761 & 0xFF;
      float f = 3.0F + p_364494_.nextFloat();
      float f1 = -(i + j + p_363800_) / 32.0F * f;
      float f2 = f1 % 32.0F;
      return new WeatherEffectRenderer.ColumnInstance(p_362466_, p_364160_, p_364844_, p_361656_, 0.0F, f2, p_361622_);
   }

   private WeatherEffectRenderer.ColumnInstance createSnowColumnInstance(
      RandomSource p_364207_, int p_364249_, int p_360966_, int p_361182_, int p_360486_, int p_364645_, int p_361024_, float p_362542_
   ) {
      float f = p_364249_ + p_362542_;
      float f1 = (float)(p_364207_.nextDouble() + f * 0.01F * (float)p_364207_.nextGaussian());
      float f2 = (float)(p_364207_.nextDouble() + f * (float)p_364207_.nextGaussian() * 0.001F);
      float f3 = -((p_364249_ & 511) + p_362542_) / 512.0F;
      int i = LightTexture.pack((LightTexture.block(p_361024_) * 3 + 15) / 4, (LightTexture.sky(p_361024_) * 3 + 15) / 4);
      return new WeatherEffectRenderer.ColumnInstance(p_360966_, p_364645_, p_361182_, p_360486_, f1, f3 + f2, i);
   }

   private void renderInstances(
      VertexConsumer p_383100_, List<WeatherEffectRenderer.ColumnInstance> p_364429_, Vec3 p_363972_, float p_364489_, int p_361243_, float p_362812_
   ) {
      float f = p_361243_ * p_361243_;

      for (WeatherEffectRenderer.ColumnInstance weathereffectrenderer$columninstance : p_364429_) {
         float f1 = (float)(weathereffectrenderer$columninstance.x + 0.5 - p_363972_.x);
         float f2 = (float)(weathereffectrenderer$columninstance.z + 0.5 - p_363972_.z);
         float f3 = (float)Mth.lengthSquared(f1, f2);
         float f4 = Mth.lerp(Math.min(f3 / f, 1.0F), p_364489_, 0.5F) * p_362812_;
         int i = ARGB.white(f4);
         int j = (weathereffectrenderer$columninstance.z - Mth.floor(p_363972_.z) + 16) * 32
            + weathereffectrenderer$columninstance.x
            - Mth.floor(p_363972_.x)
            + 16;
         float f5 = this.columnSizeX[j] / 2.0F;
         float f6 = this.columnSizeZ[j] / 2.0F;
         float f7 = f1 - f5;
         float f8 = f1 + f5;
         float f9 = (float)(weathereffectrenderer$columninstance.topY - p_363972_.y);
         float f10 = (float)(weathereffectrenderer$columninstance.bottomY - p_363972_.y);
         float f11 = f2 - f6;
         float f12 = f2 + f6;
         float f13 = weathereffectrenderer$columninstance.uOffset + 0.0F;
         float f14 = weathereffectrenderer$columninstance.uOffset + 1.0F;
         float f15 = weathereffectrenderer$columninstance.bottomY * 0.25F + weathereffectrenderer$columninstance.vOffset;
         float f16 = weathereffectrenderer$columninstance.topY * 0.25F + weathereffectrenderer$columninstance.vOffset;
         p_383100_.addVertex(f7, f9, f11).setUv(f13, f15).setColor(i).setLight(weathereffectrenderer$columninstance.lightCoords);
         p_383100_.addVertex(f8, f9, f12).setUv(f14, f15).setColor(i).setLight(weathereffectrenderer$columninstance.lightCoords);
         p_383100_.addVertex(f8, f10, f12).setUv(f14, f16).setColor(i).setLight(weathereffectrenderer$columninstance.lightCoords);
         p_383100_.addVertex(f7, f10, f11).setUv(f13, f16).setColor(i).setLight(weathereffectrenderer$columninstance.lightCoords);
      }
   }

   public void tickRainParticles(ClientLevel p_361823_, Camera p_364990_, int p_361788_, ParticleStatus p_363302_, int p_455026_) {
      CustomWeatherEffectRenderer customWeatherEffectRenderer = CustomEnvironmentEffectsRendererManager.getCustomWeatherEffectRenderer(
         p_361823_, p_364990_.position()
      );
      if (customWeatherEffectRenderer == null || !customWeatherEffectRenderer.tickRain(p_361823_, p_361788_, p_364990_)) {
         float f = p_361823_.getRainLevel(1.0F);
         if (!(f <= 0.0F)) {
            RandomSource randomsource = RandomSource.create(p_361788_ * 312987231L);
            BlockPos blockpos = BlockPos.containing(p_364990_.position());
            BlockPos blockpos1 = null;
            int i = 2 * p_455026_ + 1;
            int j = i * i;
            int k = (int)(0.225F * j * f * f) / (p_363302_ == ParticleStatus.DECREASED ? 2 : 1);

            for (int l = 0; l < k; l++) {
               int i1 = randomsource.nextInt(i) - p_455026_;
               int j1 = randomsource.nextInt(i) - p_455026_;
               BlockPos blockpos2 = p_361823_.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockpos.offset(i1, 0, j1));
               if (blockpos2.getY() > p_361823_.getMinY()
                  && blockpos2.getY() <= blockpos.getY() + 10
                  && blockpos2.getY() >= blockpos.getY() - 10
                  && this.getPrecipitationAt(p_361823_, blockpos2) == Biome.Precipitation.RAIN) {
                  blockpos1 = blockpos2.below();
                  if (p_363302_ == ParticleStatus.MINIMAL) {
                     break;
                  }

                  double d0 = randomsource.nextDouble();
                  double d1 = randomsource.nextDouble();
                  BlockState blockstate = p_361823_.getBlockState(blockpos1);
                  FluidState fluidstate = p_361823_.getFluidState(blockpos1);
                  VoxelShape voxelshape = blockstate.getCollisionShape(p_361823_, blockpos1);
                  double d2 = voxelshape.max(Direction.Axis.Y, d0, d1);
                  double d3 = fluidstate.getHeight(p_361823_, blockpos1);
                  double d4 = Math.max(d2, d3);
                  ParticleOptions particleoptions = !fluidstate.is(FluidTags.LAVA)
                        && !blockstate.is(Blocks.MAGMA_BLOCK)
                        && !CampfireBlock.isLitCampfire(blockstate)
                     ? ParticleTypes.RAIN
                     : ParticleTypes.SMOKE;
                  p_361823_.addParticle(particleoptions, blockpos1.getX() + d0, blockpos1.getY() + d4, blockpos1.getZ() + d1, 0.0, 0.0, 0.0);
               }
            }

            if (blockpos1 != null && randomsource.nextInt(3) < this.rainSoundTime++) {
               this.rainSoundTime = 0;
               if (blockpos1.getY() > blockpos.getY() + 1
                  && p_361823_.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockpos).getY() > Mth.floor((float)blockpos.getY())) {
                  p_361823_.playLocalSound(blockpos1, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.WEATHER, 0.1F, 0.5F, false);
               } else {
                  p_361823_.playLocalSound(blockpos1, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.2F, 1.0F, false);
               }
            }
         }
      }
   }

   private Biome.Precipitation getPrecipitationAt(Level p_362885_, BlockPos p_362817_) {
      if (!p_362885_.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(p_362817_.getX()), SectionPos.blockToSectionCoord(p_362817_.getZ()))) {
         return Biome.Precipitation.NONE;
      } else {
         Biome biome = p_362885_.getBiome(p_362817_).value();
         return biome.getPrecipitationAt(p_362817_, p_362885_.getSeaLevel());
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record ColumnInstance(int x, int z, int bottomY, int topY, float uOffset, float vOffset, int lightCoords) {
   }
}
