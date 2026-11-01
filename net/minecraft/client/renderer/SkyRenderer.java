package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.SkyRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class SkyRenderer implements AutoCloseable {
   private static final Identifier SUN_SPRITE = Identifier.withDefaultNamespace("sun");
   private static final Identifier END_FLASH_SPRITE = Identifier.withDefaultNamespace("end_flash");
   private static final Identifier END_SKY_LOCATION = Identifier.withDefaultNamespace("textures/environment/end_sky.png");
   private static final float SKY_DISC_RADIUS = 512.0F;
   private static final int SKY_VERTICES = 10;
   private static final int STAR_COUNT = 1500;
   private static final float SUN_SIZE = 30.0F;
   private static final float SUN_HEIGHT = 100.0F;
   private static final float MOON_SIZE = 20.0F;
   private static final float MOON_HEIGHT = 100.0F;
   private static final int SUNRISE_STEPS = 16;
   private static final int END_SKY_QUAD_COUNT = 6;
   private static final float END_FLASH_HEIGHT = 100.0F;
   private static final float END_FLASH_SCALE = 60.0F;
   private final TextureAtlas celestialsAtlas;
   private final GpuBuffer starBuffer;
   private final GpuBuffer topSkyBuffer;
   private final GpuBuffer bottomSkyBuffer;
   private final GpuBuffer endSkyBuffer;
   private final GpuBuffer sunBuffer;
   private final GpuBuffer moonBuffer;
   private final GpuBuffer sunriseBuffer;
   private final GpuBuffer endFlashBuffer;
   private final RenderSystem.AutoStorageIndexBuffer quadIndices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
   private final AbstractTexture endSkyTexture;
   private int starIndexCount;

   public SkyRenderer(TextureManager p_455376_, AtlasManager p_455011_) {
      this.celestialsAtlas = p_455011_.getAtlasOrThrow(AtlasIds.CELESTIALS);
      this.starBuffer = this.buildStars();
      this.endSkyBuffer = buildEndSky();
      this.endSkyTexture = this.getTexture(p_455376_, END_SKY_LOCATION);
      this.endFlashBuffer = buildEndFlashQuad(this.celestialsAtlas);
      this.sunBuffer = buildSunQuad(this.celestialsAtlas);
      this.moonBuffer = buildMoonPhases(this.celestialsAtlas);
      this.sunriseBuffer = this.buildSunriseFan();

      try (ByteBufferBuilder bytebufferbuilder = ByteBufferBuilder.exactlySized(10 * DefaultVertexFormat.POSITION.getVertexSize())) {
         BufferBuilder bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
         this.buildSkyDisc(bufferbuilder, 16.0F);

         try (MeshData meshdata = bufferbuilder.buildOrThrow()) {
            this.topSkyBuffer = RenderSystem.getDevice().createBuffer(() -> "Top sky vertex buffer", 32, meshdata.vertexBuffer());
         }

         bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
         this.buildSkyDisc(bufferbuilder, -16.0F);

         try (MeshData meshdata1 = bufferbuilder.buildOrThrow()) {
            this.bottomSkyBuffer = RenderSystem.getDevice().createBuffer(() -> "Bottom sky vertex buffer", 32, meshdata1.vertexBuffer());
         }
      }
   }

   private AbstractTexture getTexture(TextureManager p_455378_, Identifier p_467889_) {
      return p_455378_.getTexture(p_467889_);
   }

   private GpuBuffer buildSunriseFan() {
      int i = 18;
      int j = DefaultVertexFormat.POSITION_COLOR.getVertexSize();

      GpuBuffer gpubuffer;
      try (ByteBufferBuilder bytebufferbuilder = ByteBufferBuilder.exactlySized(18 * j)) {
         BufferBuilder bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
         int k = ARGB.white(1.0F);
         int l = ARGB.white(0.0F);
         bufferbuilder.addVertex(0.0F, 100.0F, 0.0F).setColor(k);

         for (int i1 = 0; i1 <= 16; i1++) {
            float f = i1 * (float) (Math.PI * 2) / 16.0F;
            float f1 = Mth.sin(f);
            float f2 = Mth.cos(f);
            bufferbuilder.addVertex(f1 * 120.0F, f2 * 120.0F, -f2 * 40.0F).setColor(l);
         }

         try (MeshData meshdata = bufferbuilder.buildOrThrow()) {
            gpubuffer = RenderSystem.getDevice().createBuffer(() -> "Sunrise/Sunset fan", 32, meshdata.vertexBuffer());
         }
      }

      return gpubuffer;
   }

   private static GpuBuffer buildSunQuad(TextureAtlas p_455519_) {
      return buildCelestialQuad("Sun quad", p_455519_.getSprite(SUN_SPRITE));
   }

   private static GpuBuffer buildEndFlashQuad(TextureAtlas p_454939_) {
      return buildCelestialQuad("End flash quad", p_454939_.getSprite(END_FLASH_SPRITE));
   }

   private static GpuBuffer buildCelestialQuad(String p_454894_, TextureAtlasSprite p_456200_) {
      VertexFormat vertexformat = DefaultVertexFormat.POSITION_TEX;

      GpuBuffer gpubuffer;
      try (ByteBufferBuilder bytebufferbuilder = ByteBufferBuilder.exactlySized(4 * vertexformat.getVertexSize())) {
         BufferBuilder bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, vertexformat);
         bufferbuilder.addVertex(-1.0F, 0.0F, -1.0F).setUv(p_456200_.getU0(), p_456200_.getV0());
         bufferbuilder.addVertex(1.0F, 0.0F, -1.0F).setUv(p_456200_.getU1(), p_456200_.getV0());
         bufferbuilder.addVertex(1.0F, 0.0F, 1.0F).setUv(p_456200_.getU1(), p_456200_.getV1());
         bufferbuilder.addVertex(-1.0F, 0.0F, 1.0F).setUv(p_456200_.getU0(), p_456200_.getV1());

         try (MeshData meshdata = bufferbuilder.buildOrThrow()) {
            gpubuffer = RenderSystem.getDevice().createBuffer(() -> p_454894_, 32, meshdata.vertexBuffer());
         }
      }

      return gpubuffer;
   }

   private static GpuBuffer buildMoonPhases(TextureAtlas p_455196_) {
      MoonPhase[] amoonphase = MoonPhase.values();
      VertexFormat vertexformat = DefaultVertexFormat.POSITION_TEX;

      GpuBuffer gpubuffer;
      try (ByteBufferBuilder bytebufferbuilder = ByteBufferBuilder.exactlySized(amoonphase.length * 4 * vertexformat.getVertexSize())) {
         BufferBuilder bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, vertexformat);

         for (MoonPhase moonphase : amoonphase) {
            TextureAtlasSprite textureatlassprite = p_455196_.getSprite(Identifier.withDefaultNamespace("moon/" + moonphase.getSerializedName()));
            bufferbuilder.addVertex(-1.0F, 0.0F, -1.0F).setUv(textureatlassprite.getU1(), textureatlassprite.getV1());
            bufferbuilder.addVertex(1.0F, 0.0F, -1.0F).setUv(textureatlassprite.getU0(), textureatlassprite.getV1());
            bufferbuilder.addVertex(1.0F, 0.0F, 1.0F).setUv(textureatlassprite.getU0(), textureatlassprite.getV0());
            bufferbuilder.addVertex(-1.0F, 0.0F, 1.0F).setUv(textureatlassprite.getU1(), textureatlassprite.getV0());
         }

         try (MeshData meshdata = bufferbuilder.buildOrThrow()) {
            gpubuffer = RenderSystem.getDevice().createBuffer(() -> "Moon phases", 32, meshdata.vertexBuffer());
         }
      }

      return gpubuffer;
   }

   private GpuBuffer buildStars() {
      RandomSource randomsource = RandomSource.create(10842L);
      float f = 100.0F;

      GpuBuffer gpubuffer;
      try (ByteBufferBuilder bytebufferbuilder = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION.getVertexSize() * 1500 * 4)) {
         BufferBuilder bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

         for (int i = 0; i < 1500; i++) {
            float f1 = randomsource.nextFloat() * 2.0F - 1.0F;
            float f2 = randomsource.nextFloat() * 2.0F - 1.0F;
            float f3 = randomsource.nextFloat() * 2.0F - 1.0F;
            float f4 = 0.15F + randomsource.nextFloat() * 0.1F;
            float f5 = Mth.lengthSquared(f1, f2, f3);
            if (!(f5 <= 0.010000001F) && !(f5 >= 1.0F)) {
               Vector3f vector3f = new Vector3f(f1, f2, f3).normalize(100.0F);
               float f6 = (float)(randomsource.nextDouble() * (float) Math.PI * 2.0);
               Matrix3f matrix3f = new Matrix3f().rotateTowards(new Vector3f(vector3f).negate(), new Vector3f(0.0F, 1.0F, 0.0F)).rotateZ(-f6);
               bufferbuilder.addVertex(new Vector3f(f4, -f4, 0.0F).mul(matrix3f).add(vector3f));
               bufferbuilder.addVertex(new Vector3f(f4, f4, 0.0F).mul(matrix3f).add(vector3f));
               bufferbuilder.addVertex(new Vector3f(-f4, f4, 0.0F).mul(matrix3f).add(vector3f));
               bufferbuilder.addVertex(new Vector3f(-f4, -f4, 0.0F).mul(matrix3f).add(vector3f));
            }
         }

         try (MeshData meshdata = bufferbuilder.buildOrThrow()) {
            this.starIndexCount = meshdata.drawState().indexCount();
            gpubuffer = RenderSystem.getDevice().createBuffer(() -> "Stars vertex buffer", 40, meshdata.vertexBuffer());
         }
      }

      return gpubuffer;
   }

   private void buildSkyDisc(VertexConsumer p_382865_, float p_361373_) {
      float f = Math.signum(p_361373_) * 512.0F;
      p_382865_.addVertex(0.0F, p_361373_, 0.0F);

      for (int i = -180; i <= 180; i += 45) {
         p_382865_.addVertex(f * Mth.cos(i * (float) (Math.PI / 180.0)), p_361373_, 512.0F * Mth.sin(i * (float) (Math.PI / 180.0)));
      }
   }

   private static GpuBuffer buildEndSky() {
      GpuBuffer gpubuffer;
      try (ByteBufferBuilder bytebufferbuilder = ByteBufferBuilder.exactlySized(24 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize())) {
         BufferBuilder bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

         for (int i = 0; i < 6; i++) {
            Matrix4f matrix4f = new Matrix4f();
            switch (i) {
               case 1:
                  matrix4f.rotationX((float) (Math.PI / 2));
                  break;
               case 2:
                  matrix4f.rotationX((float) (-Math.PI / 2));
                  break;
               case 3:
                  matrix4f.rotationX((float) Math.PI);
                  break;
               case 4:
                  matrix4f.rotationZ((float) (Math.PI / 2));
                  break;
               case 5:
                  matrix4f.rotationZ((float) (-Math.PI / 2));
            }

            bufferbuilder.addVertex(matrix4f, -100.0F, -100.0F, -100.0F).setUv(0.0F, 0.0F).setColor(-14145496);
            bufferbuilder.addVertex(matrix4f, -100.0F, -100.0F, 100.0F).setUv(0.0F, 16.0F).setColor(-14145496);
            bufferbuilder.addVertex(matrix4f, 100.0F, -100.0F, 100.0F).setUv(16.0F, 16.0F).setColor(-14145496);
            bufferbuilder.addVertex(matrix4f, 100.0F, -100.0F, -100.0F).setUv(16.0F, 0.0F).setColor(-14145496);
         }

         try (MeshData meshdata = bufferbuilder.buildOrThrow()) {
            gpubuffer = RenderSystem.getDevice().createBuffer(() -> "End sky vertex buffer", 40, meshdata.vertexBuffer());
         }
      }

      return gpubuffer;
   }

   public void renderSkyDisc(int p_468629_) {
      GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
         .writeTransform(RenderSystem.getModelViewMatrix(), ARGB.vector4fFromARGB32(p_468629_), new Vector3f(), new Matrix4f());
      GpuTextureView gputextureview = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
      GpuTextureView gputextureview1 = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();

      try (RenderPass renderpass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(() -> "Sky disc", gputextureview, OptionalInt.empty(), gputextureview1, OptionalDouble.empty())) {
         renderpass.setPipeline(RenderPipelines.SKY);
         RenderSystem.bindDefaultUniforms(renderpass);
         renderpass.setUniform("DynamicTransforms", gpubufferslice);
         renderpass.setVertexBuffer(0, this.topSkyBuffer);
         renderpass.draw(0, 10);
      }
   }

   public void extractRenderState(ClientLevel p_451440_, float p_451057_, Camera p_456087_, SkyRenderState p_451116_) {
      p_451116_.skybox = p_451440_.dimensionType().skybox();
      if (p_451116_.skybox != DimensionType.Skybox.NONE) {
         if (p_451116_.skybox == DimensionType.Skybox.END) {
            EndFlashState endflashstate = p_451440_.endFlashState();
            if (endflashstate != null) {
               p_451116_.endFlashIntensity = endflashstate.getIntensity(p_451057_);
               p_451116_.endFlashXAngle = endflashstate.getXAngle();
               p_451116_.endFlashYAngle = endflashstate.getYAngle();
            }
         } else {
            EnvironmentAttributeProbe environmentattributeprobe = p_456087_.attributeProbe();
            p_451116_.sunAngle = environmentattributeprobe.getValue(EnvironmentAttributes.SUN_ANGLE, p_451057_) * (float) (Math.PI / 180.0);
            p_451116_.moonAngle = environmentattributeprobe.getValue(EnvironmentAttributes.MOON_ANGLE, p_451057_) * (float) (Math.PI / 180.0);
            p_451116_.starAngle = environmentattributeprobe.getValue(EnvironmentAttributes.STAR_ANGLE, p_451057_) * (float) (Math.PI / 180.0);
            p_451116_.rainBrightness = 1.0F - p_451440_.getRainLevel(p_451057_);
            p_451116_.starBrightness = environmentattributeprobe.getValue(EnvironmentAttributes.STAR_BRIGHTNESS, p_451057_);
            p_451116_.sunriseAndSunsetColor = p_456087_.attributeProbe().getValue(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, p_451057_);
            p_451116_.moonPhase = environmentattributeprobe.getValue(EnvironmentAttributes.MOON_PHASE, p_451057_);
            p_451116_.skyColor = environmentattributeprobe.getValue(EnvironmentAttributes.SKY_COLOR, p_451057_);
            p_451116_.shouldRenderDarkDisc = this.shouldRenderDarkDisc(p_451057_, p_451440_);
         }
      }
   }

   private boolean shouldRenderDarkDisc(float p_451520_, ClientLevel p_451537_) {
      return Minecraft.getInstance().player.getEyePosition(p_451520_).y - p_451537_.getLevelData().getHorizonHeight(p_451537_) < 0.0;
   }

   public void renderDarkDisc() {
      Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
      matrix4fstack.pushMatrix();
      matrix4fstack.translate(0.0F, 12.0F, 0.0F);
      GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
         .writeTransform(matrix4fstack, new Vector4f(0.0F, 0.0F, 0.0F, 1.0F), new Vector3f(), new Matrix4f());
      GpuTextureView gputextureview = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
      GpuTextureView gputextureview1 = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();

      try (RenderPass renderpass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(() -> "Sky dark", gputextureview, OptionalInt.empty(), gputextureview1, OptionalDouble.empty())) {
         renderpass.setPipeline(RenderPipelines.SKY);
         RenderSystem.bindDefaultUniforms(renderpass);
         renderpass.setUniform("DynamicTransforms", gpubufferslice);
         renderpass.setVertexBuffer(0, this.bottomSkyBuffer);
         renderpass.draw(0, 10);
      }

      matrix4fstack.popMatrix();
   }

   public void renderSunMoonAndStars(
      PoseStack p_363513_, float p_362201_, float p_362569_, float p_363542_, MoonPhase p_455415_, float p_468909_, float p_467714_
   ) {
      p_363513_.pushPose();
      p_363513_.mulPose(Axis.YP.rotationDegrees(-90.0F));
      p_363513_.pushPose();
      p_363513_.mulPose(Axis.XP.rotation(p_362201_));
      this.renderSun(p_468909_, p_363513_);
      p_363513_.popPose();
      p_363513_.pushPose();
      p_363513_.mulPose(Axis.XP.rotation(p_362569_));
      this.renderMoon(p_455415_, p_468909_, p_363513_);
      p_363513_.popPose();
      if (p_467714_ > 0.0F) {
         p_363513_.pushPose();
         p_363513_.mulPose(Axis.XP.rotation(p_363542_));
         this.renderStars(p_467714_, p_363513_);
         p_363513_.popPose();
      }

      p_363513_.popPose();
   }

   private void renderSun(float p_362331_, PoseStack p_361665_) {
      Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
      matrix4fstack.pushMatrix();
      matrix4fstack.mul(p_361665_.last().pose());
      matrix4fstack.translate(0.0F, 100.0F, 0.0F);
      matrix4fstack.scale(30.0F, 1.0F, 30.0F);
      GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
         .writeTransform(matrix4fstack, new Vector4f(1.0F, 1.0F, 1.0F, p_362331_), new Vector3f(), new Matrix4f());
      GpuTextureView gputextureview = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
      GpuTextureView gputextureview1 = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();
      GpuBuffer gpubuffer = this.quadIndices.getBuffer(6);

      try (RenderPass renderpass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(() -> "Sky sun", gputextureview, OptionalInt.empty(), gputextureview1, OptionalDouble.empty())) {
         renderpass.setPipeline(RenderPipelines.CELESTIAL);
         RenderSystem.bindDefaultUniforms(renderpass);
         renderpass.setUniform("DynamicTransforms", gpubufferslice);
         renderpass.bindTexture("Sampler0", this.celestialsAtlas.getTextureView(), this.celestialsAtlas.getSampler());
         renderpass.setVertexBuffer(0, this.sunBuffer);
         renderpass.setIndexBuffer(gpubuffer, this.quadIndices.type());
         renderpass.drawIndexed(0, 0, 6, 1);
      }

      matrix4fstack.popMatrix();
   }

   private void renderMoon(MoonPhase p_455407_, float p_362497_, PoseStack p_362676_) {
      int i = p_455407_.index() * 4;
      Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
      matrix4fstack.pushMatrix();
      matrix4fstack.mul(p_362676_.last().pose());
      matrix4fstack.translate(0.0F, 100.0F, 0.0F);
      matrix4fstack.scale(20.0F, 1.0F, 20.0F);
      GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
         .writeTransform(matrix4fstack, new Vector4f(1.0F, 1.0F, 1.0F, p_362497_), new Vector3f(), new Matrix4f());
      GpuTextureView gputextureview = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
      GpuTextureView gputextureview1 = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();
      GpuBuffer gpubuffer = this.quadIndices.getBuffer(6);

      try (RenderPass renderpass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(() -> "Sky moon", gputextureview, OptionalInt.empty(), gputextureview1, OptionalDouble.empty())) {
         renderpass.setPipeline(RenderPipelines.CELESTIAL);
         RenderSystem.bindDefaultUniforms(renderpass);
         renderpass.setUniform("DynamicTransforms", gpubufferslice);
         renderpass.bindTexture("Sampler0", this.celestialsAtlas.getTextureView(), this.celestialsAtlas.getSampler());
         renderpass.setVertexBuffer(0, this.moonBuffer);
         renderpass.setIndexBuffer(gpubuffer, this.quadIndices.type());
         renderpass.drawIndexed(i, 0, 6, 1);
      }

      matrix4fstack.popMatrix();
   }

   private void renderStars(float p_361150_, PoseStack p_365236_) {
      Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
      matrix4fstack.pushMatrix();
      matrix4fstack.mul(p_365236_.last().pose());
      RenderPipeline renderpipeline = RenderPipelines.STARS;
      GpuTextureView gputextureview = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
      GpuTextureView gputextureview1 = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();
      GpuBuffer gpubuffer = this.quadIndices.getBuffer(this.starIndexCount);
      GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
         .writeTransform(matrix4fstack, new Vector4f(p_361150_, p_361150_, p_361150_, p_361150_), new Vector3f(), new Matrix4f());

      try (RenderPass renderpass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(() -> "Stars", gputextureview, OptionalInt.empty(), gputextureview1, OptionalDouble.empty())) {
         renderpass.setPipeline(renderpipeline);
         RenderSystem.bindDefaultUniforms(renderpass);
         renderpass.setUniform("DynamicTransforms", gpubufferslice);
         renderpass.setVertexBuffer(0, this.starBuffer);
         renderpass.setIndexBuffer(gpubuffer, this.quadIndices.type());
         renderpass.drawIndexed(0, 0, this.starIndexCount, 1);
      }

      matrix4fstack.popMatrix();
   }

   public void renderSunriseAndSunset(PoseStack p_362809_, float p_364108_, int p_361766_) {
      float f = ARGB.alphaFloat(p_361766_);
      if (!(f <= 0.001F)) {
         p_362809_.pushPose();
         p_362809_.mulPose(Axis.XP.rotationDegrees(90.0F));
         float f1 = Mth.sin(p_364108_) < 0.0F ? 180.0F : 0.0F;
         p_362809_.mulPose(Axis.ZP.rotationDegrees(f1 + 90.0F));
         Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
         matrix4fstack.pushMatrix();
         matrix4fstack.mul(p_362809_.last().pose());
         matrix4fstack.scale(1.0F, 1.0F, f);
         GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
            .writeTransform(matrix4fstack, ARGB.vector4fFromARGB32(p_361766_), new Vector3f(), new Matrix4f());
         GpuTextureView gputextureview = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
         GpuTextureView gputextureview1 = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();

         try (RenderPass renderpass = RenderSystem.getDevice()
               .createCommandEncoder()
               .createRenderPass(() -> "Sunrise sunset", gputextureview, OptionalInt.empty(), gputextureview1, OptionalDouble.empty())) {
            renderpass.setPipeline(RenderPipelines.SUNRISE_SUNSET);
            RenderSystem.bindDefaultUniforms(renderpass);
            renderpass.setUniform("DynamicTransforms", gpubufferslice);
            renderpass.setVertexBuffer(0, this.sunriseBuffer);
            renderpass.draw(0, 18);
         }

         matrix4fstack.popMatrix();
         p_362809_.popPose();
      }
   }

   public void renderEndSky() {
      RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
      GpuBuffer gpubuffer = rendersystem$autostorageindexbuffer.getBuffer(36);
      GpuTextureView gputextureview = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
      GpuTextureView gputextureview1 = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();
      GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
         .writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());

      try (RenderPass renderpass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(() -> "End sky", gputextureview, OptionalInt.empty(), gputextureview1, OptionalDouble.empty())) {
         renderpass.setPipeline(RenderPipelines.END_SKY);
         RenderSystem.bindDefaultUniforms(renderpass);
         renderpass.setUniform("DynamicTransforms", gpubufferslice);
         renderpass.bindTexture("Sampler0", this.endSkyTexture.getTextureView(), this.endSkyTexture.getSampler());
         renderpass.setVertexBuffer(0, this.endSkyBuffer);
         renderpass.setIndexBuffer(gpubuffer, rendersystem$autostorageindexbuffer.type());
         renderpass.drawIndexed(0, 0, 36, 1);
      }
   }

   public void renderEndFlash(PoseStack p_437341_, float p_437373_, float p_437188_, float p_437384_) {
      p_437341_.mulPose(Axis.YP.rotationDegrees(180.0F - p_437384_));
      p_437341_.mulPose(Axis.XP.rotationDegrees(-90.0F - p_437188_));
      Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
      matrix4fstack.pushMatrix();
      matrix4fstack.mul(p_437341_.last().pose());
      matrix4fstack.translate(0.0F, 100.0F, 0.0F);
      matrix4fstack.scale(60.0F, 1.0F, 60.0F);
      GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
         .writeTransform(matrix4fstack, new Vector4f(p_437373_, p_437373_, p_437373_, p_437373_), new Vector3f(), new Matrix4f());
      GpuTextureView gputextureview = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
      GpuTextureView gputextureview1 = Minecraft.getInstance().getMainRenderTarget().getDepthTextureView();
      GpuBuffer gpubuffer = this.quadIndices.getBuffer(6);

      try (RenderPass renderpass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(() -> "End flash", gputextureview, OptionalInt.empty(), gputextureview1, OptionalDouble.empty())) {
         renderpass.setPipeline(RenderPipelines.CELESTIAL);
         RenderSystem.bindDefaultUniforms(renderpass);
         renderpass.setUniform("DynamicTransforms", gpubufferslice);
         renderpass.bindTexture("Sampler0", this.celestialsAtlas.getTextureView(), this.celestialsAtlas.getSampler());
         renderpass.setVertexBuffer(0, this.endFlashBuffer);
         renderpass.setIndexBuffer(gpubuffer, this.quadIndices.type());
         renderpass.drawIndexed(0, 0, 6, 1);
      }

      matrix4fstack.popMatrix();
   }

   @Override
   public void close() {
      this.sunBuffer.close();
      this.moonBuffer.close();
      this.starBuffer.close();
      this.topSkyBuffer.close();
      this.bottomSkyBuffer.close();
      this.endSkyBuffer.close();
      this.sunriseBuffer.close();
      this.endFlashBuffer.close();
   }
}
