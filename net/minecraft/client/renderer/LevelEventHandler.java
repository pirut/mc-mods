package net.minecraft.client.renderer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.core.particles.SculkChargeParticleOptions;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class LevelEventHandler {
   private final Minecraft minecraft;
   private final ClientLevel level;
   private final Map<BlockPos, SoundInstance> playingJukeboxSongs = new HashMap<>();

   public LevelEventHandler(Minecraft minecraft, ClientLevel level) {
      this.minecraft = minecraft;
      this.level = level;
   }

   public void globalLevelEvent(int type, BlockPos pos, int data) {
      switch (type) {
         case 1023:
         case 1028:
         case 1038:
            Camera camera = this.minecraft.gameRenderer.getMainCamera();
            if (camera.isInitialized()) {
               Vec3 vec3 = Vec3.atCenterOf(pos).subtract(camera.position()).normalize();
               Vec3 vec32 = camera.position().add(vec3.scale(2.0));
               if (type == 1023) {
                  this.level.playLocalSound(vec32.x, vec32.y, vec32.z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F, false);
               } else if (type == 1038) {
                  this.level.playLocalSound(vec32.x, vec32.y, vec32.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F, false);
               } else {
                  this.level.playLocalSound(vec32.x, vec32.y, vec32.z, SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 5.0F, 1.0F, false);
               }
            }
      }
   }

   public void levelEvent(int type, BlockPos pos, int data) {
      RandomSource randomSource = this.level.random;
      switch (type) {
         case 1000:
            this.level.playLocalSound(pos, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            break;
         case 1001:
            this.level.playLocalSound(pos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0F, 1.2F, false);
            break;
         case 1002:
            this.level.playLocalSound(pos, SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 1.0F, 1.2F, false);
            break;
         case 1004:
            this.level.playLocalSound(pos, SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.2F, false);
            break;
         case 1009:
            if (data == 0) {
               this.level
                  .playLocalSound(
                     pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (randomSource.nextFloat() - randomSource.nextFloat()) * 0.8F, false
                  );
            } else if (data == 1) {
               this.level
                  .playLocalSound(
                     pos,
                     SoundEvents.GENERIC_EXTINGUISH_FIRE,
                     SoundSource.BLOCKS,
                     0.7F,
                     1.6F + (randomSource.nextFloat() - randomSource.nextFloat()) * 0.4F,
                     false
                  );
            }
            break;
         case 1010:
            this.level.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).get(data).ifPresent(reference -> this.playJukeboxSong(reference, pos));
            break;
         case 1011:
            this.stopJukeboxSongAndNotifyNearby(pos);
            break;
         case 1015:
            this.level
               .playLocalSound(
                  pos, SoundEvents.GHAST_WARN, SoundSource.HOSTILE, 10.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, false
               );
            break;
         case 1016:
            this.level
               .playLocalSound(
                  pos, SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 10.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, false
               );
            break;
         case 1017:
            this.level
               .playLocalSound(
                  pos, SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 10.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, false
               );
            break;
         case 1018:
            this.level
               .playLocalSound(
                  pos, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, false
               );
            break;
         case 1019:
            this.level
               .playLocalSound(
                  pos,
                  SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
                  SoundSource.HOSTILE,
                  2.0F,
                  (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F,
                  false
               );
            break;
         case 1020:
            this.level
               .playLocalSound(
                  pos,
                  SoundEvents.ZOMBIE_ATTACK_IRON_DOOR,
                  SoundSource.HOSTILE,
                  2.0F,
                  (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F,
                  false
               );
            break;
         case 1021:
            this.level
               .playLocalSound(
                  pos,
                  SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                  SoundSource.HOSTILE,
                  2.0F,
                  (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F,
                  false
               );
            break;
         case 1022:
            this.level
               .playLocalSound(
                  pos, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 2.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, false
               );
            break;
         case 1024:
            this.level
               .playLocalSound(
                  pos, SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 2.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, false
               );
            break;
         case 1025:
            this.level
               .playLocalSound(
                  pos, SoundEvents.BAT_TAKEOFF, SoundSource.NEUTRAL, 0.05F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, false
               );
            break;
         case 1026:
            this.level
               .playLocalSound(
                  pos, SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 2.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, false
               );
            break;
         case 1027:
            this.level
               .playLocalSound(
                  pos,
                  SoundEvents.ZOMBIE_VILLAGER_CONVERTED,
                  SoundSource.HOSTILE,
                  2.0F,
                  (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F,
                  false
               );
            break;
         case 1029:
            this.level.playLocalSound(pos, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 1.0F, randomSource.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1030:
            this.level.playLocalSound(pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, randomSource.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1031:
            this.level.playLocalSound(pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1032:
            this.minecraft
               .getSoundManager()
               .play(SimpleSoundInstance.forLocalAmbience(SoundEvents.PORTAL_TRAVEL, randomSource.nextFloat() * 0.4F + 0.8F, 0.25F));
            break;
         case 1033:
            this.level.playLocalSound(pos, SoundEvents.CHORUS_FLOWER_GROW, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            break;
         case 1034:
            this.level.playLocalSound(pos, SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            break;
         case 1035:
            this.level.playLocalSound(pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            break;
         case 1039:
            this.level.playLocalSound(pos, SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE, 0.3F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1040:
            this.level
               .playLocalSound(
                  pos,
                  SoundEvents.ZOMBIE_CONVERTED_TO_DROWNED,
                  SoundSource.HOSTILE,
                  2.0F,
                  (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F,
                  false
               );
            break;
         case 1041:
            this.level
               .playLocalSound(
                  pos,
                  SoundEvents.HUSK_CONVERTED_TO_ZOMBIE,
                  SoundSource.HOSTILE,
                  2.0F,
                  (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F,
                  false
               );
            break;
         case 1042:
            this.level.playLocalSound(pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1043:
            this.level.playLocalSound(pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1044:
            this.level.playLocalSound(pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1045:
            this.level.playLocalSound(pos, SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 1046:
            this.level
               .playLocalSound(
                  pos, SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA_INTO_CAULDRON, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false
               );
            break;
         case 1047:
            this.level
               .playLocalSound(
                  pos, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false
               );
            break;
         case 1048:
            this.level
               .playLocalSound(
                  pos,
                  SoundEvents.SKELETON_CONVERTED_TO_STRAY,
                  SoundSource.HOSTILE,
                  2.0F,
                  (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F,
                  false
               );
            break;
         case 1049:
            this.level.playLocalSound(pos, SoundEvents.CRAFTER_CRAFT, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            break;
         case 1050:
            this.level.playLocalSound(pos, SoundEvents.CRAFTER_FAIL, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            break;
         case 1051:
            this.level
               .playLocalSound(pos, SoundEvents.WIND_CHARGE_THROW, SoundSource.BLOCKS, 0.5F, 0.4F / (this.level.getRandom().nextFloat() * 0.4F + 0.8F), false);
            break;
         case 1500:
            ComposterBlock.handleFill(this.level, pos, data > 0);
            break;
         case 1501:
            this.level
               .playLocalSound(
                  pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (randomSource.nextFloat() - randomSource.nextFloat()) * 0.8F, false
               );

            for (int m = 0; m < 8; m++) {
               this.level
                  .addParticle(
                     ParticleTypes.LARGE_SMOKE, pos.getX() + randomSource.nextDouble(), pos.getY() + 1.2, pos.getZ() + randomSource.nextDouble(), 0.0, 0.0, 0.0
                  );
            }
            break;
         case 1502:
            this.level
               .playLocalSound(
                  pos, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 0.5F, 2.6F + (randomSource.nextFloat() - randomSource.nextFloat()) * 0.8F, false
               );

            for (int m = 0; m < 5; m++) {
               double g = pos.getX() + randomSource.nextDouble() * 0.6 + 0.2;
               double n = pos.getY() + randomSource.nextDouble() * 0.6 + 0.2;
               double o = pos.getZ() + randomSource.nextDouble() * 0.6 + 0.2;
               this.level.addParticle(ParticleTypes.SMOKE, g, n, o, 0.0, 0.0, 0.0);
            }
            break;
         case 1503:
            this.level.playLocalSound(pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 1.0F, false);

            for (int m = 0; m < 16; m++) {
               double g = pos.getX() + (5.0 + randomSource.nextDouble() * 6.0) / 16.0;
               double n = pos.getY() + 0.8125;
               double o = pos.getZ() + (5.0 + randomSource.nextDouble() * 6.0) / 16.0;
               this.level.addParticle(ParticleTypes.SMOKE, g, n, o, 0.0, 0.0, 0.0);
            }
            break;
         case 1504:
            PointedDripstoneBlock.spawnDripParticle(this.level, pos, this.level.getBlockState(pos));
            break;
         case 1505:
            BoneMealItem.addGrowthParticles(this.level, pos, data);
            this.level.playLocalSound(pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            break;
         case 2000:
            this.shootParticles(data, pos, randomSource, ParticleTypes.SMOKE);
            break;
         case 2001:
            BlockState blockState = Block.stateById(data);
            if (!blockState.isAir()) {
               SoundType soundType = blockState.getSoundType();
               this.level
                  .playLocalSound(pos, soundType.getBreakSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F, false);
            }

            this.level.addDestroyBlockEffect(pos, blockState);
            break;
         case 2002:
         case 2007:
            Vec3 vec3 = Vec3.atBottomCenterOf(pos);

            for (int j = 0; j < 8; j++) {
               this.level
                  .addParticle(
                     new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SPLASH_POTION)),
                     vec3.x,
                     vec3.y,
                     vec3.z,
                     randomSource.nextGaussian() * 0.15,
                     randomSource.nextDouble() * 0.2,
                     randomSource.nextGaussian() * 0.15
                  );
            }

            float h = (data >> 16 & 0xFF) / 255.0F;
            float k = (data >> 8 & 0xFF) / 255.0F;
            float l = (data >> 0 & 0xFF) / 255.0F;
            ParticleType<SpellParticleOption> particleType = type == 2007 ? ParticleTypes.INSTANT_EFFECT : ParticleTypes.EFFECT;

            for (int m = 0; m < 100; m++) {
               double g = randomSource.nextDouble() * 4.0;
               double n = randomSource.nextDouble() * Math.PI * 2.0;
               double o = Math.cos(n) * g;
               double p = 0.01 + randomSource.nextDouble() * 0.5;
               double q = Math.sin(n) * g;
               float r = 0.75F + randomSource.nextFloat() * 0.25F;
               SpellParticleOption spellParticleOption = SpellParticleOption.create(particleType, h * r, k * r, l * r, (float)g);
               this.level.addParticle(spellParticleOption, vec3.x + o * 0.1, vec3.y + 0.3, vec3.z + q * 0.1, o, p, q);
            }

            this.level.playLocalSound(pos, SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F, randomSource.nextFloat() * 0.1F + 0.9F, false);
            break;
         case 2003:
            double d = pos.getX() + 0.5;
            double e = pos.getY();
            double f = pos.getZ() + 0.5;

            for (int i = 0; i < 8; i++) {
               this.level
                  .addParticle(
                     new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.ENDER_EYE)),
                     d,
                     e,
                     f,
                     randomSource.nextGaussian() * 0.15,
                     randomSource.nextDouble() * 0.2,
                     randomSource.nextGaussian() * 0.15
                  );
            }

            for (double g = 0.0; g < Math.PI * 2; g += Math.PI / 20) {
               this.level.addParticle(ParticleTypes.PORTAL, d + Math.cos(g) * 5.0, e - 0.4, f + Math.sin(g) * 5.0, Math.cos(g) * -5.0, 0.0, Math.sin(g) * -5.0);
               this.level.addParticle(ParticleTypes.PORTAL, d + Math.cos(g) * 5.0, e - 0.4, f + Math.sin(g) * 5.0, Math.cos(g) * -7.0, 0.0, Math.sin(g) * -7.0);
            }
            break;
         case 2004:
            for (int sx = 0; sx < 20; sx++) {
               double t = pos.getX() + 0.5 + (randomSource.nextDouble() - 0.5) * 2.0;
               double u = pos.getY() + 0.5 + (randomSource.nextDouble() - 0.5) * 2.0;
               double v = pos.getZ() + 0.5 + (randomSource.nextDouble() - 0.5) * 2.0;
               this.level.addParticle(ParticleTypes.SMOKE, t, u, v, 0.0, 0.0, 0.0);
               this.level.addParticle(ParticleTypes.FLAME, t, u, v, 0.0, 0.0, 0.0);
            }
            break;
         case 2006:
            for (int m = 0; m < 200; m++) {
               float ab = randomSource.nextFloat() * 4.0F;
               float ag = randomSource.nextFloat() * (float) (Math.PI * 2);
               double n = Mth.cos(ag) * ab;
               double o = 0.01 + randomSource.nextDouble() * 0.5;
               double p = Mth.sin(ag) * ab;
               this.level
                  .addParticle(
                     PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, ab), pos.getX() + n * 0.1, pos.getY() + 0.3, pos.getZ() + p * 0.1, n, o, p
                  );
            }

            if (data == 1) {
               this.level.playLocalSound(pos, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE, 1.0F, randomSource.nextFloat() * 0.1F + 0.9F, false);
            }
            break;
         case 2008:
            this.level.addParticle(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
            break;
         case 2009:
            for (int m = 0; m < 8; m++) {
               this.level
                  .addParticle(
                     ParticleTypes.CLOUD, pos.getX() + randomSource.nextDouble(), pos.getY() + 1.2, pos.getZ() + randomSource.nextDouble(), 0.0, 0.0, 0.0
                  );
            }
            break;
         case 2010:
            this.shootParticles(data, pos, randomSource, ParticleTypes.WHITE_SMOKE);
            break;
         case 2011:
            ParticleUtils.spawnParticleInBlock(this.level, pos, data, ParticleTypes.HAPPY_VILLAGER);
            break;
         case 2012:
            ParticleUtils.spawnParticleInBlock(this.level, pos, data, ParticleTypes.HAPPY_VILLAGER);
            break;
         case 2013:
            ParticleUtils.spawnSmashAttackParticles(this.level, pos, data);
            break;
         case 3000:
            this.level.addAlwaysVisibleParticle(ParticleTypes.EXPLOSION_EMITTER, true, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
            this.level
               .playLocalSound(
                  pos,
                  SoundEvents.END_GATEWAY_SPAWN,
                  SoundSource.BLOCKS,
                  10.0F,
                  (1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F,
                  false
               );
            break;
         case 3001:
            this.level.playLocalSound(pos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 64.0F, 0.8F + this.level.random.nextFloat() * 0.3F, false);
            break;
         case 3002:
            if (data >= 0 && data < Direction.Axis.VALUES.length) {
               ParticleUtils.spawnParticlesAlongAxis(Direction.Axis.VALUES[data], this.level, pos, 0.125, ParticleTypes.ELECTRIC_SPARK, UniformInt.of(10, 19));
            } else {
               ParticleUtils.spawnParticlesOnBlockFaces(this.level, pos, ParticleTypes.ELECTRIC_SPARK, UniformInt.of(3, 5));
            }
            break;
         case 3003:
            ParticleUtils.spawnParticlesOnBlockFaces(this.level, pos, ParticleTypes.WAX_ON, UniformInt.of(3, 5));
            this.level.playLocalSound(pos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            break;
         case 3004:
            ParticleUtils.spawnParticlesOnBlockFaces(this.level, pos, ParticleTypes.WAX_OFF, UniformInt.of(3, 5));
            break;
         case 3005:
            ParticleUtils.spawnParticlesOnBlockFaces(this.level, pos, ParticleTypes.SCRAPE, UniformInt.of(3, 5));
            break;
         case 3006:
            int s = data >> 6;
            if (s > 0) {
               if (randomSource.nextFloat() < 0.3F + s * 0.1F) {
                  float l = 0.15F + 0.02F * s * s * randomSource.nextFloat();
                  float w = 0.4F + 0.3F * s * randomSource.nextFloat();
                  this.level.playLocalSound(pos, SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.BLOCKS, l, w, false);
               }

               byte b = (byte)(data & 63);
               IntProvider intProvider = UniformInt.of(0, s);
               float x = 0.005F;
               Supplier<Vec3> supplier = () -> new Vec3(
                  Mth.nextDouble(randomSource, -0.005F, 0.005F), Mth.nextDouble(randomSource, -0.005F, 0.005F), Mth.nextDouble(randomSource, -0.005F, 0.005F)
               );
               if (b == 0) {
                  for (Direction direction : Direction.values()) {
                     float y = direction == Direction.DOWN ? (float) Math.PI : 0.0F;
                     double p = direction.getAxis() == Direction.Axis.Y ? 0.65 : 0.57;
                     ParticleUtils.spawnParticlesOnBlockFace(this.level, pos, new SculkChargeParticleOptions(y), intProvider, direction, supplier, p);
                  }
               } else {
                  for (Direction direction2 : MultifaceBlock.unpack(b)) {
                     float z = direction2 == Direction.UP ? (float) Math.PI : 0.0F;
                     double o = 0.35;
                     ParticleUtils.spawnParticlesOnBlockFace(this.level, pos, new SculkChargeParticleOptions(z), intProvider, direction2, supplier, 0.35);
                  }
               }
            } else {
               this.level.playLocalSound(pos, SoundEvents.SCULK_BLOCK_CHARGE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
               boolean bl = this.level.getBlockState(pos).isCollisionShapeFullBlock(this.level, pos);
               int aa = bl ? 40 : 20;
               float x = bl ? 0.45F : 0.25F;
               float ab = 0.07F;

               for (int ac = 0; ac < aa; ac++) {
                  float ad = 2.0F * randomSource.nextFloat() - 1.0F;
                  float z = 2.0F * randomSource.nextFloat() - 1.0F;
                  float ae = 2.0F * randomSource.nextFloat() - 1.0F;
                  this.level
                     .addParticle(
                        ParticleTypes.SCULK_CHARGE_POP,
                        pos.getX() + 0.5 + ad * x,
                        pos.getY() + 0.5 + z * x,
                        pos.getZ() + 0.5 + ae * x,
                        ad * 0.07F,
                        z * 0.07F,
                        ae * 0.07F
                     );
               }
            }
            break;
         case 3007:
            for (int af = 0; af < 10; af++) {
               this.level
                  .addParticle(new ShriekParticleOption(af * 5), pos.getX() + 0.5, pos.getY() + SculkShriekerBlock.TOP_Y, pos.getZ() + 0.5, 0.0, 0.0, 0.0);
            }

            BlockState blockState3 = this.level.getBlockState(pos);
            boolean bl2 = blockState3.hasProperty(BlockStateProperties.WATERLOGGED) && blockState3.getValue(BlockStateProperties.WATERLOGGED);
            if (!bl2) {
               this.level
                  .playLocalSound(
                     pos.getX() + 0.5,
                     pos.getY() + SculkShriekerBlock.TOP_Y,
                     pos.getZ() + 0.5,
                     SoundEvents.SCULK_SHRIEKER_SHRIEK,
                     SoundSource.BLOCKS,
                     2.0F,
                     0.6F + this.level.random.nextFloat() * 0.4F,
                     false
                  );
            }
            break;
         case 3008:
            BlockState blockState2 = Block.stateById(data);
            if (blockState2.getBlock() instanceof BrushableBlock brushableBlock) {
               this.level.playLocalSound(pos, brushableBlock.getBrushCompletedSound(), SoundSource.PLAYERS, 1.0F, 1.0F, false);
            }

            this.level.addDestroyBlockEffect(pos, blockState2);
            break;
         case 3009:
            ParticleUtils.spawnParticlesOnBlockFaces(this.level, pos, ParticleTypes.EGG_CRACK, UniformInt.of(3, 6));
            break;
         case 3011:
            TrialSpawner.addSpawnParticles(this.level, pos, randomSource, TrialSpawner.FlameParticle.decode(data).particleType);
            break;
         case 3012:
            this.level
               .playLocalSound(
                  pos, SoundEvents.TRIAL_SPAWNER_SPAWN_MOB, SoundSource.BLOCKS, 1.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, true
               );
            TrialSpawner.addSpawnParticles(this.level, pos, randomSource, TrialSpawner.FlameParticle.decode(data).particleType);
            break;
         case 3013:
            this.level
               .playLocalSound(
                  pos,
                  SoundEvents.TRIAL_SPAWNER_DETECT_PLAYER,
                  SoundSource.BLOCKS,
                  1.0F,
                  (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F,
                  true
               );
            TrialSpawner.addDetectPlayerParticles(this.level, pos, randomSource, data, ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER);
            break;
         case 3014:
            this.level
               .playLocalSound(
                  pos,
                  SoundEvents.TRIAL_SPAWNER_EJECT_ITEM,
                  SoundSource.BLOCKS,
                  1.0F,
                  (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F,
                  true
               );
            TrialSpawner.addEjectItemParticles(this.level, pos, randomSource);
            break;
         case 3015:
            if (this.level.getBlockEntity(pos) instanceof VaultBlockEntity vaultBlockEntity) {
               VaultBlockEntity.Client.emitActivationParticles(
                  this.level,
                  vaultBlockEntity.getBlockPos(),
                  vaultBlockEntity.getBlockState(),
                  vaultBlockEntity.getSharedData(),
                  data == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.SOUL_FIRE_FLAME
               );
               this.level
                  .playLocalSound(
                     pos, SoundEvents.VAULT_ACTIVATE, SoundSource.BLOCKS, 1.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, true
                  );
            }
            break;
         case 3016:
            VaultBlockEntity.Client.emitDeactivationParticles(this.level, pos, data == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.SOUL_FIRE_FLAME);
            this.level
               .playLocalSound(
                  pos, SoundEvents.VAULT_DEACTIVATE, SoundSource.BLOCKS, 1.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, true
               );
            break;
         case 3017:
            TrialSpawner.addEjectItemParticles(this.level, pos, randomSource);
            break;
         case 3018:
            for (int sx = 0; sx < 10; sx++) {
               double t = randomSource.nextGaussian() * 0.02;
               double u = randomSource.nextGaussian() * 0.02;
               double v = randomSource.nextGaussian() * 0.02;
               this.level
                  .addParticle(
                     ParticleTypes.POOF,
                     pos.getX() + randomSource.nextDouble(),
                     pos.getY() + randomSource.nextDouble(),
                     pos.getZ() + randomSource.nextDouble(),
                     t,
                     u,
                     v
                  );
            }

            this.level
               .playLocalSound(
                  pos, SoundEvents.COBWEB_PLACE, SoundSource.BLOCKS, 1.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F, true
               );
            break;
         case 3019:
            this.level
               .playLocalSound(
                  pos,
                  SoundEvents.TRIAL_SPAWNER_DETECT_PLAYER,
                  SoundSource.BLOCKS,
                  1.0F,
                  (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F,
                  true
               );
            TrialSpawner.addDetectPlayerParticles(this.level, pos, randomSource, data, ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS);
            break;
         case 3020:
            this.level
               .playLocalSound(
                  pos,
                  SoundEvents.TRIAL_SPAWNER_OMINOUS_ACTIVATE,
                  SoundSource.BLOCKS,
                  data == 0 ? 0.3F : 1.0F,
                  (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F,
                  true
               );
            TrialSpawner.addDetectPlayerParticles(this.level, pos, randomSource, 0, ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS);
            TrialSpawner.addBecomeOminousParticles(this.level, pos, randomSource);
            break;
         case 3021:
            this.level
               .playLocalSound(
                  pos,
                  SoundEvents.TRIAL_SPAWNER_SPAWN_ITEM,
                  SoundSource.BLOCKS,
                  1.0F,
                  (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F,
                  true
               );
            TrialSpawner.addSpawnParticles(this.level, pos, randomSource, TrialSpawner.FlameParticle.decode(data).particleType);
      }
   }

   private void shootParticles(int direction, BlockPos pos, RandomSource random, SimpleParticleType particleType) {
      Direction direction2 = Direction.from3DDataValue(direction);
      int i = direction2.getStepX();
      int j = direction2.getStepY();
      int k = direction2.getStepZ();

      for (int l = 0; l < 10; l++) {
         double d = random.nextDouble() * 0.2 + 0.01;
         double e = pos.getX() + i * 0.6 + 0.5 + i * 0.01 + (random.nextDouble() - 0.5) * k * 0.5;
         double f = pos.getY() + j * 0.6 + 0.5 + j * 0.01 + (random.nextDouble() - 0.5) * j * 0.5;
         double g = pos.getZ() + k * 0.6 + 0.5 + k * 0.01 + (random.nextDouble() - 0.5) * i * 0.5;
         double h = i * d + random.nextGaussian() * 0.01;
         double m = j * d + random.nextGaussian() * 0.01;
         double n = k * d + random.nextGaussian() * 0.01;
         this.level.addParticle(particleType, e, f, g, h, m, n);
      }
   }

   private void playJukeboxSong(Holder<JukeboxSong> song, BlockPos pos) {
      this.stopJukeboxSong(pos);
      JukeboxSong jukeboxSong = song.value();
      SoundEvent soundEvent = jukeboxSong.soundEvent().value();
      SoundInstance soundInstance = SimpleSoundInstance.forJukeboxSong(soundEvent, Vec3.atCenterOf(pos));
      this.playingJukeboxSongs.put(pos, soundInstance);
      this.minecraft.getSoundManager().play(soundInstance);
      this.minecraft.gui.setNowPlaying(jukeboxSong.description());
      this.notifyNearbyEntities(this.level, pos, true);
   }

   private void stopJukeboxSong(BlockPos pos) {
      SoundInstance soundInstance = this.playingJukeboxSongs.remove(pos);
      if (soundInstance != null) {
         this.minecraft.getSoundManager().stop(soundInstance);
      }
   }

   private void stopJukeboxSongAndNotifyNearby(BlockPos pos) {
      this.stopJukeboxSong(pos);
      this.notifyNearbyEntities(this.level, pos, false);
   }

   private void notifyNearbyEntities(Level level, BlockPos pos, boolean playing) {
      for (LivingEntity livingEntity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(3.0))) {
         livingEntity.setRecordPlayingNearby(pos, playing);
      }
   }
}
