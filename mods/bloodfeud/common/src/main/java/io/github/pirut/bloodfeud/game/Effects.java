package io.github.pirut.bloodfeud.game;

import io.github.pirut.bloodfeud.feud.Brute;
import io.github.pirut.bloodfeud.feud.Scar;
import io.github.pirut.bloodfeud.feud.Trait;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/** What traits do in a fight: immunities, weaknesses, strengths and fears. */
public final class Effects {
    private Effects() {
    }

    /** For every hit on the server (through Pirut Lib). True if the hit does nothing at all. */
    public static boolean immune(LivingEntity target, DamageSource source) {
        Live live = Live.current();
        if (live == null) return false;
        Brute b = live.bruteOf(target);
        if (b == null) return false;
        ServerPlayer player = source.getEntity() instanceof ServerPlayer p ? p : null;
        if (b.traits.contains(Trait.ARROWPROOF) && source.is(DamageTypeTags.IS_PROJECTILE)) {
            if (player != null) live.discover(player, b, Trait.ARROWPROOF);
            return true;
        }
        if (b.traits.contains(Trait.FIREPROOF) && source.is(DamageTypeTags.IS_FIRE)) {
            if (player != null) live.discover(player, b, Trait.FIREPROOF);
            target.clearFire();
            return true;
        }
        return false;
    }

    /** For every hit on the server (through Pirut Lib): the damage after traits on either side. */
    public static float damage(LivingEntity target, DamageSource source, float amount) {
        Live live = Live.current();
        if (live == null || amount <= 0) return amount;
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity holder && source.getDirectEntity() == attacker) {
            Relics.onHit(holder.getMainHandItem(), target, holder);
        }
        Brute hurt = live.bruteOf(target);
        if (hurt != null) {
            if (attacker instanceof Player p) live.fought(target, p);
            return hitOnBrute(live, hurt, target, source, attacker, amount);
        }
        if (attacker != null && target instanceof ServerPlayer player) {
            Brute b = live.bruteOf(attacker);
            if (b != null) {
                live.fought(attacker, player);
                return hitByBrute(live, b, (LivingEntity) attacker, player, source, amount);
            }
        }
        return amount;
    }

    private static float hitOnBrute(Live live, Brute b, LivingEntity target, DamageSource source, Entity attacker, float amount) {
        ServerPlayer player = attacker instanceof ServerPlayer p ? p : null;
        Entity direct = source.getDirectEntity();
        boolean melee = player != null && direct == player;
        boolean crit = melee && player.fallDistance > 0 && !player.onGround() && !player.onClimbable()
                && !player.isInWater() && !player.isPassenger() && !player.isSprinting()
                && !player.hasEffect(MobEffects.BLINDNESS);
        if (crit) {
            ItemStack hand = player.getMainHandItem();
            if (hand.is(ItemTags.SWORDS) || hand.is(ItemTags.AXES)) live.lastCrit.put(target.getUUID(), live.gameTime());
        }
        float out = amount;
        if (b.traits.contains(Trait.ARROW_WEAK) && direct instanceof AbstractArrow && direct.getY() >= target.getEyeY() - 0.45) {
            if (player != null) live.discover(player, b, Trait.ARROW_WEAK);
            return Math.max(out, target.getHealth() + 100);
        }
        if (melee && b.traits.contains(Trait.SNEAK_WEAK) && player.isCrouching() && behind(target, player)) {
            live.discover(player, b, Trait.SNEAK_WEAK);
            return Math.max(out, target.getHealth() + 100);
        }
        if (attacker instanceof Wolf && b.traits.contains(Trait.BEAST_WEAK)) out *= 3;
        if (melee && b.traits.contains(Trait.SMITE_WEAK) && smite(player, player.getMainHandItem()) > 0) {
            live.discover(player, b, Trait.SMITE_WEAK);
            out *= 2;
        }
        if (crit && b.traits.contains(Trait.GLASS_JAW)) {
            live.discover(player, b, Trait.GLASS_JAW);
            out *= 2;
        }
        if (b.traits.contains(Trait.SHIELDBEARER) && attacker != null && !behind(target, attacker)
                && !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            if (player != null) live.discover(player, b, Trait.SHIELDBEARER);
            out *= 0.5f;
        }
        if (b.traits.contains(Trait.BLINK) && attacker != null && live.random.nextInt(5) == 0 && target.getHealth() > out) {
            for (int i = 0; i < 8; i++) {
                double x = target.getX() + (live.random.nextDouble() - 0.5) * 12;
                double z = target.getZ() + (live.random.nextDouble() - 0.5) * 12;
                if (target.randomTeleport(x, target.getY() + live.random.nextInt(5) - 2, z, true)) {
                    if (player != null) live.discover(player, b, Trait.BLINK);
                    break;
                }
            }
        }
        // What its scars taught its body.
        if (b.scars.contains(Scar.PIERCED) && source.is(DamageTypeTags.IS_PROJECTILE)) out *= 0.75f;
        if (b.scars.contains(Scar.BLASTED) && source.is(DamageTypeTags.IS_EXPLOSION)) out *= 0.5f;
        if (b.scars.contains(Scar.MAULED) && attacker instanceof Wolf) out *= 0.7f;
        if (b.scars.contains(Scar.DROWNED) && source.is(net.minecraft.world.damagesource.DamageTypes.DROWN)) out = 0;
        return out;
    }

    private static float hitByBrute(Live live, Brute b, LivingEntity brute, ServerPlayer player, DamageSource source, float amount) {
        float out = amount;
        boolean melee = source.getDirectEntity() == brute;
        if (b.traits.contains(Trait.BERSERKER) && brute.getHealth() < brute.getMaxHealth() * 0.4f) {
            live.discover(player, b, Trait.BERSERKER);
            out *= 1.5f;
        }
        if (b.scars.contains(Scar.CUT)) out *= 1.1f; // it fights dirtier since someone opened its face
        if (!melee) return out;
        if (b.scars.contains(Scar.BURNED) && live.random.nextInt(3) == 0) player.igniteForSeconds(2); // it still smolders
        if (b.traits.contains(Trait.VENOM_BLADE)) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0), brute);
            live.discover(player, b, Trait.VENOM_BLADE);
        }
        if (b.traits.contains(Trait.BONEBREAKER)) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), brute);
            live.discover(player, b, Trait.BONEBREAKER);
        }
        if (b.traits.contains(Trait.THIEF) && !player.getMainHandItem().isEmpty() && live.random.nextInt(7) == 0) {
            ItemStack taken = player.getMainHandItem().copy();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            live.discover(player, b, Trait.THIEF);
            live.stolen(b, player, taken);
        }
        return out;
    }

    private static boolean behind(LivingEntity target, Entity from) {
        Vec3 look = Vec3.directionFromRotation(0, target.getYRot());
        Vec3 to = from.position().subtract(target.position()).multiply(1, 0, 1).normalize();
        return look.dot(to) < -0.3;
    }

    private static int smite(ServerPlayer player, ItemStack stack) {
        Holder<Enchantment> smite = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(Enchantments.SMITE).orElse(null);
        return smite == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(smite, stack);
    }

    /** Once a second for each Brute out in the world. */
    static void tick(Live live, Live.Active a, Brute b, Mob mob, ServerLevel level) {
        long gt = live.gameTime();
        if (b.traits.contains(Trait.NIGHT_STALKER)) {
            boolean dark = level.getMaxLocalRawBrightness(mob.blockPosition()) < 8;
            boolean close = level.getNearestPlayer(mob, 6) != null;
            if (dark && !close) mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
            else if (mob.hasEffect(MobEffects.INVISIBILITY)) {
                mob.removeEffect(MobEffects.INVISIBILITY);
                if (close && level.getNearestPlayer(mob, 6) instanceof ServerPlayer p) live.discover(p, b, Trait.NIGHT_STALKER);
            }
        }
        if (gt < a.fleeUntil || !(mob instanceof PathfinderMob pm)) return;
        Vec3 threat = null;
        Trait why = null;
        if (b.traits.contains(Trait.FEARS_FIRE)) {
            for (Player p : level.players()) {
                if (p.distanceTo(mob) < 7 && (holdsFire(p.getMainHandItem()) || holdsFire(p.getOffhandItem()))) {
                    threat = p.position();
                    why = Trait.FEARS_FIRE;
                }
            }
            if (threat == null && mob.isOnFire()) {
                threat = mob.position().add(mob.getLookAngle());
                why = Trait.FEARS_FIRE;
            }
        }
        if (threat == null && b.traits.contains(Trait.FEARS_WOLVES)) {
            for (Wolf w : level.getEntitiesOfClass(Wolf.class, mob.getBoundingBox().inflate(10))) {
                threat = w.position();
                why = Trait.FEARS_WOLVES;
            }
        }
        if (threat == null && b.traits.contains(Trait.FEARS_CATS)) {
            for (LivingEntity c : level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(10),
                    e -> e instanceof Cat || e instanceof Ocelot)) {
                threat = c.position();
                why = Trait.FEARS_CATS;
            }
        }
        if (threat == null && b.traits.contains(Trait.FEARS_BELLS)) {
            BlockPos bell = ringingBell(level, mob.blockPosition());
            if (bell != null) {
                threat = Vec3.atCenterOf(bell);
                why = Trait.FEARS_BELLS;
            }
        }
        if (threat == null) return;
        Vec3 away = DefaultRandomPos.getPosAway(pm, 16, 7, threat);
        if (away == null) return;
        mob.setTarget(null);
        mob.getNavigation().moveTo(away.x, away.y, away.z, 1.5);
        a.fleeUntil = gt + 80;
        if (level.getNearestPlayer(mob, 24) instanceof ServerPlayer p) {
            live.discover(p, b, why);
            if (live.random.nextInt(3) == 0) live.say(p, b, Lines.flee(live, p, b));
        }
    }

    private static final DustParticleOptions BLOOD = new DustParticleOptions(new org.joml.Vector3f(0.45f, 0.02f, 0.02f), 0.9f);

    /** Scars you can see: it smokes, it drips, it bleeds. */
    static void scars(Brute b, Mob mob, ServerLevel level) {
        if (b.scars.isEmpty() || mob.isInvisible() || level.getNearestPlayer(mob, 32) == null) return;
        double h = mob.getBbHeight(), w = mob.getBbWidth() * 0.4;
        for (Scar s : b.scars) {
            ParticleOptions particle;
            int count;
            double y;
            switch (s) {
                case BURNED -> {
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, mob.getX(), mob.getY() + h * 0.7, mob.getZ(), 1, w, h * 0.15, w, 0.01);
                    particle = ParticleTypes.SMOKE; count = 4; y = 0.6;
                }
                case BLASTED -> { particle = ParticleTypes.ASH; count = 12; y = 0.5; }
                case DROWNED -> { particle = ParticleTypes.FALLING_WATER; count = 3; y = 0.8; }
                case CUT -> { particle = BLOOD; count = 2; y = 0.85; }
                case MAULED -> { particle = BLOOD; count = 3; y = 0.45; }
                case PIERCED -> { particle = ParticleTypes.CRIT; count = 1; y = 0.6; }
                default -> { continue; } // a broken leg shows in how slowly it walks
            }
            level.sendParticles(particle, mob.getX(), mob.getY() + h * y, mob.getZ(), count, w, h * 0.2, w, 0.0);
        }
    }

    private static boolean holdsFire(ItemStack s) {
        return s.is(Items.TORCH) || s.is(Items.SOUL_TORCH) || s.is(Items.LANTERN) || s.is(Items.SOUL_LANTERN)
                || s.is(Items.FLINT_AND_STEEL) || s.is(Items.FIRE_CHARGE) || s.is(Items.LAVA_BUCKET)
                || s.is(Items.CAMPFIRE) || s.is(Items.SOUL_CAMPFIRE);
    }

    private static BlockPos ringingBell(ServerLevel level, BlockPos at) {
        int cx = at.getX() >> 4, cz = at.getZ() >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!level.hasChunk(cx + dx, cz + dz)) continue;
                LevelChunk chunk = level.getChunk(cx + dx, cz + dz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof BellBlockEntity bell && bell.shaking && be.getBlockPos().distSqr(at) < 24 * 24) return be.getBlockPos();
                }
            }
        }
        return null;
    }
}
