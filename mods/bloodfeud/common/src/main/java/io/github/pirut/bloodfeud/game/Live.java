package io.github.pirut.bloodfeud.game;

import io.github.pirut.bloodfeud.BloodFeud;
import io.github.pirut.bloodfeud.Config;
import io.github.pirut.bloodfeud.feud.Brute;
import io.github.pirut.bloodfeud.feud.DeathCause;
import io.github.pirut.bloodfeud.feud.Family;
import io.github.pirut.bloodfeud.feud.Feud;
import io.github.pirut.bloodfeud.feud.Memory;
import io.github.pirut.bloodfeud.feud.News;
import io.github.pirut.bloodfeud.feud.Pack;
import io.github.pirut.bloodfeud.feud.PlaceNames;
import io.github.pirut.bloodfeud.feud.Rank;
import io.github.pirut.bloodfeud.feud.Relation;
import io.github.pirut.bloodfeud.feud.Trait;
import io.github.pirut.lib.Spots;
import io.github.pirut.lib.Titles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Holder;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * The Feud while the server runs: which Brutes are out in the world as real mobs, their boss bars, the mobs that
 * are about to rise, and the checks that bring Brutes and packs to players. One per server.
 */
public final class Live {
    public static final String TAG_BRUTE = "bloodfeud.brute";
    public static final String TAG_GRUNT = "bloodfeud.grunt";
    private static final String TAG_ID = "bloodfeud.id:";
    private static final String TAG_SAW = "bloodfeud.saw:";
    private static final ResourceLocation LEVEL_MOD = BloodFeud.id("level");
    private static final ResourceLocation HIDE_MOD = BloodFeud.id("hide");
    private static final int WITNESS_RADIUS = 16;
    private static final int RISE_DELAY = 200;
    private static final int BAR_RANGE = 48;
    private static final int WITHDRAW_RANGE = 96;
    private static final int PACK_ENCOUNTER_RANGE = 80;
    private static final int LONE_ENCOUNTER_RANGE = 128;
    private static final int HUNTER_ENCOUNTER_RANGE = 400;

    private static Live instance;

    final MinecraftServer server;
    final Random random = new Random();
    private final FeudData data;

    /** A Brute out in the world as a real mob. */
    static final class Active {
        final UUID brute;
        final UUID entity;
        final ResourceKey<net.minecraft.world.level.Level> dimension;
        final ServerBossEvent bar;
        long farSince = -1;
        long missingSince = -1;
        /** Players it has fought, and when. */
        final Map<UUID, Long> fought = new HashMap<>();
        long fleeUntil;
        boolean stalking;
        /** Whether it already said something about being hurt, or about the player being nearly dead. */
        boolean barkedHurt, barkedWinning;

        Active(UUID brute, UUID entity, ResourceKey<net.minecraft.world.level.Level> dimension, ServerBossEvent bar) {
            this.brute = brute;
            this.entity = entity;
            this.dimension = dimension;
            this.bar = bar;
        }
    }

    private record Mark(ResourceKey<net.minecraft.world.level.Level> dimension, UUID player, long at, UUID pack) {
    }

    private final Map<UUID, Active> active = new HashMap<>();
    private final Map<UUID, UUID> bruteOfEntity = new HashMap<>();
    /** Grunt mob -> the pack (or lone Brute) it came with. */
    private final Map<UUID, UUID> grunts = new HashMap<>();
    private final Map<UUID, ResourceKey<net.minecraft.world.level.Level>> gruntLevel = new HashMap<>();
    private final Map<UUID, Mark> marked = new HashMap<>();
    private final Map<UUID, Long> nextEncounter = new HashMap<>();
    private final Map<UUID, Long> lastRise = new HashMap<>();
    /** Brutes (and packs) that just left: they don't come straight back. */
    private final Map<UUID, Long> rested = new HashMap<>();
    /** Last time each player saw each Brute's title card. */
    private final Map<String, Long> introduced = new HashMap<>();
    /** Recent critical blade hits on Brutes, for final kills. */
    final Map<UUID, Long> lastCrit = new HashMap<>();
    private long lastNightIndex = Long.MIN_VALUE;
    private long lastDeathNight = Long.MIN_VALUE;

    private Live(MinecraftServer server) {
        this.server = server;
        this.data = FeudData.get(server);
    }

    public static Live get(MinecraftServer server) {
        if (instance == null || instance.server != server) instance = new Live(server);
        return instance;
    }

    /** The Live for a server that is already running, or null. */
    public static Live current() {
        return instance;
    }

    public static void stopped() {
        if (instance != null) for (Active a : instance.active.values()) a.bar.removeAllPlayers();
        instance = null;
    }

    public Feud feud() {
        return data.feud;
    }

    public FeudData data() {
        return data;
    }

    long now() {
        return server.overworld().getDayTime();
    }

    long gameTime() {
        return server.overworld().getGameTime();
    }

    static String dim(net.minecraft.world.level.Level level) {
        return level.dimension().location().toString();
    }

    ServerLevel level(String dimension) {
        try {
            return server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimension)));
        } catch (RuntimeException e) {
            return null;
        }
    }

    public Brute bruteOf(Entity e) {
        UUID id = bruteOfEntity.get(e.getUUID());
        return id == null ? null : data.feud.brute(id);
    }

    Active activeOf(Entity e) {
        UUID id = bruteOfEntity.get(e.getUUID());
        return id == null ? null : active.get(id);
    }

    public boolean isActive(Brute b) {
        return active.containsKey(b.id);
    }

    // ---------------------------------------------------------------- events from the loaders

    /** An entity is being added to a level. Returns false if it is a stale copy of a Brute or grunt to drop. */
    public boolean onEntityLoad(Entity e) {
        Set<String> tags = e.getTags();
        if (tags.contains(TAG_BRUTE)) return bruteOfEntity.containsKey(e.getUUID());
        if (tags.contains(TAG_GRUNT)) return grunts.containsKey(e.getUUID());
        return true;
    }

    public void onDeath(LivingEntity dead, DamageSource source) {
        if (dead.level().isClientSide) return;
        if (dead instanceof ServerPlayer player) {
            playerDied(player, source);
            return;
        }
        if (!(dead instanceof Mob mob)) return;
        Brute b = bruteOf(mob);
        if (b != null) {
            bruteDied(mob, b, source);
            return;
        }
        UUID owner = grunts.remove(mob.getUUID());
        gruntLevel.remove(mob.getUUID());
        if (owner != null) {
            Pack p = data.feud.pack(owner);
            if (p != null) p.grunts = Math.max(0, p.grunts - 1);
            data.changed();
        }
        marked.remove(mob.getUUID());
        Family family = Families.of(mob.getType());
        if (family != null && source.getEntity() instanceof ServerPlayer killer && !killer.isCreative()) {
            witness(mob, family, killer, owner);
        }
    }

    private void witness(Mob victim, Family family, ServerPlayer killer, UUID pack) {
        AABB box = victim.getBoundingBox().inflate(WITNESS_RADIUS);
        for (Mob m : victim.level().getEntitiesOfClass(Mob.class, box, m -> m.isAlive() && m != victim)) {
            if (bruteOfEntity.containsKey(m.getUUID()) || Families.of(m.getType()) != family) continue;
            int saw = sawCount(m) + 1;
            setSawCount(m, saw);
            if (saw >= BloodFeud.config().killsToRise && !marked.containsKey(m.getUUID()) && mayRise(killer)) {
                marked.put(m.getUUID(), new Mark(m.level().dimension(), killer.getUUID(), gameTime(),
                        pack != null ? pack : grunts.get(m.getUUID())));
            }
        }
    }

    /** One mob rises per player at a time, and not again until the cooldown is over. */
    private boolean mayRise(ServerPlayer killer) {
        if (killer.level().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return false;
        for (Mark m : marked.values()) if (m.player().equals(killer.getUUID())) return false;
        return lastRise.getOrDefault(killer.getUUID(), Long.MIN_VALUE / 2) + BloodFeud.config().riseCooldownSeconds * 20L <= gameTime();
    }

    private static int sawCount(Entity e) {
        for (String t : e.getTags()) {
            if (t.startsWith(TAG_SAW)) {
                try {
                    return Integer.parseInt(t.substring(TAG_SAW.length()));
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private static void setSawCount(Entity e, int n) {
        for (String t : new ArrayList<>(e.getTags())) if (t.startsWith(TAG_SAW)) e.removeTag(t);
        e.addTag(TAG_SAW + n);
    }

    private void playerDied(ServerPlayer player, DamageSource source) {
        Entity killer = source.getEntity();
        if (!(killer instanceof Mob mob)) {
            advanceForDeath();
            return;
        }
        Feud f = data.feud;
        Brute b = bruteOf(mob);
        DeathCause cause = cause(source);
        if (b != null) {
            f.killedPlayer(b.id, player.getUUID(), cause, now());
            Relation r = b.relation(player.getUUID());
            say(player, b, Lines.killedPlayer(this, player, b, r.killedPlayer));
            dress(mob, b, f.packOf(b), false);
            Active a = active.get(b.id);
            if (a != null) a.fought.remove(player.getUUID());
        } else {
            Family family = Families.of(mob.getType());
            if (family == null || mob.getTags().contains(TAG_GRUNT) && grunts.get(mob.getUUID()) == null) {
                advanceForDeath();
                return;
            }
            UUID pack = grunts.remove(mob.getUUID());
            gruntLevel.remove(mob.getUUID());
            mob.removeTag(TAG_GRUNT);
            b = f.rise(EntityType.getKey(mob.getType()).toString(), family, dim(mob.level()), mob.getBlockX(),
                    mob.getBlockZ(), player.getUUID(), Memory.Kind.ROSE, cause, pack, now());
            becomeBrute(mob, b);
            say(player, b, Lines.rose(this, player, b, true));
            for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                if (other != player && other.level() == player.level() && other.distanceTo(player) < 256) {
                    other.sendSystemMessage(Component.literal(b.fullName() + " rose by killing " + player.getName().getString() + ".")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                }
            }
        }
        data.changed();
        f.drainNews();
        advanceForDeath();
    }

    /** Dying moves the feud along, like sleeping does, but not more than once per quarter day. */
    private void advanceForDeath() {
        long now = now();
        if (lastDeathNight != Long.MIN_VALUE && now - lastDeathNight < Feud.DAY / 4 && now >= lastDeathNight) return;
        lastDeathNight = now;
        runNight();
    }

    private void bruteDied(Mob mob, Brute b, DamageSource source) {
        Feud f = data.feud;
        ServerPlayer killer = source.getEntity() instanceof ServerPlayer p ? p : null;
        DeathCause cause = cause(source);
        Long crit = lastCrit.remove(mob.getUUID());
        boolean finalBlow = source.is(DamageTypes.LAVA) || (crit != null && gameTime() - crit <= 2);
        Rank rankBefore = f.rank(b);
        Pack packBefore = f.packOf(b);
        boolean forGood = f.killedByPlayer(b.id, killer == null ? null : killer.getUUID(), cause, finalBlow, now());
        b.x = mob.getBlockX();
        b.z = mob.getBlockZ();
        removeActive(b.id, false);
        ServerLevel level = (ServerLevel) mob.level();
        List<ItemStack> stolen = data.stolen.remove(b.id);
        if (stolen != null) for (ItemStack s : stolen) mob.spawnAtLocation(s);
        if (killer != null && Relics.dropsRelic(rankBefore, b)) {
            ItemStack relic = Relics.make(level, b, rankBefore, packBefore);
            mob.spawnAtLocation(relic);
            killer.sendSystemMessage(Component.literal(b.name + " dropped ").withStyle(ChatFormatting.GRAY)
                    .append(relic.getHoverName()).append(Component.literal(".").withStyle(ChatFormatting.GRAY)));
        }
        ExperienceOrb.award(level, mob.position(), 10 + 5 * b.level);
        if (killer != null) {
            Titles.show(killer, Component.literal(b.name + " falls").withStyle(ChatFormatting.DARK_RED),
                    Component.literal(forGood ? (finalBlow ? "A clean kill. It won't be back." : "It won't be back this time.")
                            : "But is it over?").withStyle(ChatFormatting.GRAY), 5, 50, 15);
            say(killer, b, Lines.falling(this, killer, b, forGood));
            if (forGood) io.github.pirut.bloodfeud.feud.Talk.book().forget(b.id);
        }
        announce(f.drainNews());
        data.changed();
    }

    static DeathCause cause(DamageSource s) {
        if (s.is(DamageTypeTags.IS_FIRE)) return DeathCause.FIRE;
        if (s.is(DamageTypeTags.IS_EXPLOSION)) return DeathCause.EXPLOSION;
        if (s.is(DamageTypeTags.IS_FALL)) return DeathCause.FALL;
        if (s.is(DamageTypeTags.IS_DROWNING)) return DeathCause.DROWNING;
        if (s.is(DamageTypeTags.IS_PROJECTILE)) return DeathCause.ARROW;
        if (s.getEntity() instanceof Wolf) return DeathCause.BEAST;
        if (s.getEntity() != null) return DeathCause.MELEE;
        return DeathCause.OTHER;
    }

    /** A Brute and a player traded blows. */
    void fought(Entity brute, Player player) {
        Active a = activeOf(brute);
        if (a != null) a.fought.put(player.getUUID(), gameTime());
    }

    // ---------------------------------------------------------------- the tick

    public void tick() {
        long gt = gameTime();
        long nightIndex = Math.floorDiv(now() - 13000, Feud.DAY);
        if (lastNightIndex == Long.MIN_VALUE) lastNightIndex = nightIndex;
        else if (nightIndex != lastNightIndex) {
            lastNightIndex = nightIndex;
            runNight();
        }
        if (gt % 20 == 0) {
            rises(gt);
            for (Active a : new ArrayList<>(active.values())) watch(a, gt);
            gruntCheck();
        }
        if (gt % 200 == 7) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) encounters(p, gt);
        }
    }

    public void runNight() {
        Feud f = data.feud;
        long now = now();
        if (now < f.lastNight) f.lastNight = now; // time was turned back
        List<Feud.PlayerSpot> spots = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.isSpectator()) spots.add(new Feud.PlayerSpot(p.getUUID(), dim(p.level()), p.getBlockX(), p.getBlockZ()));
        }
        f.night(now, spots, this::placeName);
        for (Active a : new ArrayList<>(active.values())) {
            Brute b = f.brute(a.brute);
            if (b == null || !b.alive) removeActive(a.brute, true);
            else refresh(a, b);
        }
        announce(f.drainNews());
        data.changed();
    }

    private String placeName(Random r, String dimension, int x, int z) {
        ServerLevel level = level(dimension);
        String biome = "";
        if (level != null) {
            biome = level.getBiome(new BlockPos(x, level.getSeaLevel(), z)).unwrapKey()
                    .map(k -> k.location().toString()).orElse("");
        }
        return PlaceNames.name(r, biome, false);
    }

    private void announce(List<News> news) {
        if (!BloodFeud.config().announceNews) return;
        for (News n : news) {
            if (n.dimension().isEmpty()) continue;
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (!dim(p.level()).equals(n.dimension())) continue;
                if (Math.hypot(p.getX() - n.x(), p.getZ() - n.z()) > Feud.AWAKE_RANGE) continue;
                if (!cares(p, n)) continue;
                p.sendSystemMessage(Component.literal("☠ ").withStyle(ChatFormatting.DARK_RED)
                        .append(Component.literal(n.text()).withStyle(ChatFormatting.GRAY)));
            }
        }
    }

    /** Players hear about Brutes they know and packs near them, not about every nobody across the map. */
    private boolean cares(ServerPlayer p, News n) {
        if (Math.hypot(p.getX() - n.x(), p.getZ() - n.z()) <= 600) return true;
        for (UUID id : n.brutes()) {
            Brute b = data.feud.brute(id);
            if (b != null && b.relations.containsKey(p.getUUID()) && b.relation(p.getUUID()).met) return true;
        }
        return false;
    }

    /** Mobs that saw their kin die and lived through it walk off as Brutes. */
    private void rises(long gt) {
        for (Iterator<Map.Entry<UUID, Mark>> it = marked.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Mark> e = it.next();
            Mark m = e.getValue();
            if (gt - m.at() < RISE_DELAY) continue;
            it.remove();
            ServerLevel level = server.getLevel(m.dimension());
            if (level == null) continue;
            if (!(level.getEntity(e.getKey()) instanceof Mob mob) || !mob.isAlive()) continue;
            if (active.size() >= BloodFeud.config().maxActiveBrutes) continue;
            Family family = Families.of(mob.getType());
            if (family == null) continue;
            UUID pack = grunts.remove(mob.getUUID());
            gruntLevel.remove(mob.getUUID());
            mob.removeTag(TAG_GRUNT);
            Brute b = data.feud.rise(EntityType.getKey(mob.getType()).toString(), family, dim(level), mob.getBlockX(),
                    mob.getBlockZ(), m.player(), Memory.Kind.SAW_KILLS, null, pack != null ? pack : m.pack(), now());
            becomeBrute(mob, b);
            lastRise.put(m.player(), gt);
            ServerPlayer player = server.getPlayerList().getPlayer(m.player());
            if (player != null && player.level() == level && player.distanceTo(mob) < 96) {
                introduce(player, mob, b, false);
            }
            data.feud.drainNews();
            data.changed();
        }
    }

    /** Keeps an active Brute's boss bar, fears and tricks going, and sends it home when players are gone. */
    private void watch(Active a, long gt) {
        Brute b = data.feud.brute(a.brute);
        ServerLevel level = server.getLevel(a.dimension);
        if (b == null || !b.alive || level == null) {
            removeActive(a.brute, true);
            return;
        }
        Entity e = level.getEntity(a.entity);
        if (!(e instanceof Mob mob) || !mob.isAlive()) {
            if (a.missingSince < 0) a.missingSince = gt;
            else if (gt - a.missingSince > 200) removeActive(a.brute, false);
            return;
        }
        a.missingSince = -1;
        double nearest = Double.MAX_VALUE;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            double d = p.distanceTo(mob);
            nearest = Math.min(nearest, d);
            boolean show = d <= BAR_RANGE;
            if (show && !a.bar.getPlayers().contains(p)) a.bar.addPlayer(p);
            else if (!show && a.bar.getPlayers().contains(p)) a.bar.removePlayer(p);
        }
        a.bar.setProgress(Math.max(0, Math.min(1, mob.getHealth() / mob.getMaxHealth())));
        bark(a, b, mob, level);
        if (b.pack == null || !b.id.equals(data.feud.pack(b.pack) == null ? null : data.feud.pack(b.pack).boss)) {
            b.x = mob.getBlockX();
            b.z = mob.getBlockZ();
        }
        if (nearest > WITHDRAW_RANGE) {
            if (a.farSince < 0) a.farSince = gt;
            else if (gt - a.farSince > 400) {
                withdraw(a, b, mob, gt);
                return;
            }
        } else {
            a.farSince = -1;
        }
        Effects.tick(this, a, b, mob, level);
    }

    /** A word in the middle of a fight: once when it gets badly hurt, once when its player is nearly dead. */
    private void bark(Active a, Brute b, Mob mob, ServerLevel level) {
        if (!a.barkedHurt && mob.getHealth() < mob.getMaxHealth() * 0.35f
                && level.getNearestPlayer(mob, 24) instanceof ServerPlayer p && !p.isSpectator()) {
            a.barkedHurt = true;
            say(p, b, Lines.hurt(this, p, b));
        }
        if (!a.barkedWinning && mob.getTarget() instanceof ServerPlayer p && p.getHealth() <= 6 && p.distanceTo(mob) < 16) {
            a.barkedWinning = true;
            say(p, b, Lines.winning(this, p, b));
        }
    }

    /** Every player went away: the Brute goes back to being a record. Anyone who ran from it is remembered. */
    private void withdraw(Active a, Brute b, Mob mob, long gt) {
        for (Map.Entry<UUID, Long> f : a.fought.entrySet()) {
            if (gt - f.getValue() > 1200) continue;
            ServerPlayer p = server.getPlayerList().getPlayer(f.getKey());
            if (p == null || !p.isAlive()) continue;
            data.feud.playerFled(b.id, p.getUUID(), now());
            say(p, b, Lines.driveOff(this, p, b));
        }
        removeActive(a.brute, true);
        rested.put(b.id, gt + 1200);
        if (b.pack != null) rested.put(b.pack, gt + 1200);
        data.changed();
    }

    private void removeActive(UUID bruteId, boolean discard) {
        Active a = active.remove(bruteId);
        if (a == null) return;
        bruteOfEntity.remove(a.entity);
        a.bar.removeAllPlayers();
        if (discard) {
            ServerLevel level = server.getLevel(a.dimension);
            Entity e = level == null ? null : level.getEntity(a.entity);
            if (e != null && !e.isRemoved()) e.discard();
        }
    }

    /** Grunts go home with their Brutes. */
    private void gruntCheck() {
        Set<UUID> owners = new HashSet<>();
        for (Active a : active.values()) {
            Brute b = data.feud.brute(a.brute);
            if (b == null) continue;
            owners.add(b.id);
            if (b.pack != null) owners.add(b.pack);
        }
        for (Iterator<Map.Entry<UUID, UUID>> it = grunts.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, UUID> g = it.next();
            if (owners.contains(g.getValue())) continue;
            ServerLevel level = server.getLevel(gruntLevel.getOrDefault(g.getKey(), net.minecraft.world.level.Level.OVERWORLD));
            Entity e = level == null ? null : level.getEntity(g.getKey());
            it.remove();
            gruntLevel.remove(g.getKey());
            if (e == null || e.isRemoved()) continue;
            if (level.getNearestPlayer(e, 48) == null) e.discard();
            else e.removeTag(TAG_GRUNT); // a player is still fighting it: it's just a mob now
        }
    }

    // ---------------------------------------------------------------- encounters

    private void encounters(ServerPlayer p, long gt) {
        if (p.isSpectator() || p.isCreative() || !p.isAlive()) return;
        Config cfg = BloodFeud.config();
        if (nextEncounter.getOrDefault(p.getUUID(), 0L) > gt) return;
        if (active.size() >= cfg.maxActiveBrutes) return;
        ServerLevel level = p.serverLevel();
        if (level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return;
        BlockPos spawn = level.getSharedSpawnPos();
        if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD
                && Math.hypot(p.getX() - spawn.getX(), p.getZ() - spawn.getZ()) < cfg.spawnSafeRadius) return;
        String dim = dim(level);
        Feud f = data.feud;
        for (Brute b : f.brutes.values()) {
            if (!b.alive || !p.getUUID().equals(b.hunting) || !b.dimension.equals(dim) || isActive(b) || resting(b.id, gt)) continue;
            if (Math.hypot(b.x - p.getX(), b.z - p.getZ()) > HUNTER_ENCOUNTER_RANGE) continue;
            Pack pack = f.packOf(b);
            if (pack != null && b.id.equals(pack.boss)) continue; // a Boss meets you at home, with its pack
            if (pack != null && pack.distanceTo(dim, p.getX(), p.getZ()) <= 300) continue; // it comes with its pack
            boolean followed = Math.hypot(b.x - p.getX(), b.z - p.getZ()) > 300;
            Mob mob = spawnBrute(level, b, p.blockPosition(), 20, 32);
            if (mob == null) continue;
            mob.setTarget(p);
            introduce(p, mob, b, followed);
            nextEncounter.put(p.getUUID(), gt + cfg.encounterCooldownSeconds * 20L);
            return;
        }
        Pack near = null;
        double best = PACK_ENCOUNTER_RANGE;
        for (Pack pk : f.packs.values()) {
            double d = pk.distanceTo(dim, p.getX(), p.getZ());
            if (d <= best && !resting(pk.id, gt) && !packOut(pk)) {
                near = pk;
                best = d;
            }
        }
        if (near != null) {
            spawnPack(level, near, p);
            nextEncounter.put(p.getUUID(), gt + cfg.encounterCooldownSeconds * 20L);
            return;
        }
        if (random.nextInt(100) >= cfg.encounterChancePercent) return;
        for (Brute b : f.alive(dim, p.getX(), p.getZ(), LONE_ENCOUNTER_RANGE)) {
            if (b.pack != null || isActive(b) || resting(b.id, gt)) continue;
            Mob mob = spawnBrute(level, b, p.blockPosition(), 18, 30);
            if (mob == null) continue;
            mob.setTarget(p);
            introduce(p, mob, b, false);
            nextEncounter.put(p.getUUID(), gt + cfg.encounterCooldownSeconds * 20L);
            return;
        }
    }

    private boolean resting(UUID id, long gt) {
        return rested.getOrDefault(id, 0L) > gt;
    }

    private boolean packOut(Pack p) {
        for (Active a : active.values()) {
            Brute b = data.feud.brute(a.brute);
            if (b != null && p.id.equals(b.pack)) return true;
        }
        return false;
    }

    /** A pack comes out to meet a player near its home: the Boss, a couple of its Brutes, and some grunts. */
    public void spawnPack(ServerLevel level, Pack pack, ServerPlayer player) {
        Feud f = data.feud;
        pack.met.add(player.getUUID());
        Vec3 at = player.position();
        double dx = pack.homeX - at.x, dz = pack.homeZ - at.z, d = Math.hypot(dx, dz);
        BlockPos center = d <= 36 ? BlockPos.containing(pack.homeX, player.getY(), pack.homeZ)
                : BlockPos.containing(at.x + dx / d * 28, player.getY(), at.z + dz / d * 28);
        List<Brute> members = f.members(pack);
        Mob bossMob = null;
        Brute boss = null;
        int out = 0;
        for (Brute b : members) {
            if (out >= 3 || isActive(b) || active.size() >= BloodFeud.config().maxActiveBrutes) continue;
            Mob mob = spawnBrute(level, b, center, 0, 8);
            if (mob == null) continue;
            b.relation(player.getUUID()).met = true;
            out++;
            if (b.id.equals(pack.boss)) {
                bossMob = mob;
                boss = b;
            }
        }
        int g = Math.min(pack.grunts, 2 + random.nextInt(3));
        Brute leader = boss != null ? boss : members.isEmpty() ? null : members.get(0);
        for (int i = 0; i < g; i++) spawnGrunt(level, pack, leader == null ? pack.family.defaultType : leader.entityType, center, pack.id);
        if (bossMob != null) introduce(player, bossMob, boss, false);
        else if (out > 0) {
            Brute first = members.get(0);
            Active a = active.get(first.id);
            Entity e = a == null ? null : level.getEntity(a.entity);
            if (e instanceof Mob m) introduce(player, m, first, false);
        }
        if (out > 1 || g > 0) {
            StringBuilder with = new StringBuilder();
            for (Brute b : members) {
                if (b == boss || !isActive(b)) continue;
                if (!with.isEmpty()) with.append(", ");
                with.append(b.name).append(" (level ").append(b.level).append(")");
            }
            if (g > 0) with.append(with.isEmpty() ? "" : " and ").append(g).append(g == 1 ? " grunt" : " grunts");
            player.sendSystemMessage(Component.literal("With it: " + with).withStyle(ChatFormatting.GRAY));
        }
        data.changed();
    }

    /** Spawns a Brute as a real mob somewhere between {@code min} and {@code max} blocks from {@code near}. */
    public Mob spawnBrute(ServerLevel level, Brute b, BlockPos near, int min, int max) {
        BlockPos pos = findSpot(level, near, min, max);
        if (pos == null) return null;
        EntityType<?> type = EntityType.byString(b.entityType).orElse(null);
        if (type == null) type = EntityType.byString(b.family.defaultType).orElse(EntityType.ZOMBIE);
        if (!(type.create(level) instanceof Mob mob)) return null;
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        b.dimension = dim(level);
        if (b.pack == null) {
            b.x = pos.getX();
            b.z = pos.getZ();
        }
        becomeBrute(mob, b);
        level.addFreshEntity(mob);
        if (b.traits.contains(Trait.PACKCALLER)) {
            Pack p = data.feud.packOf(b);
            for (int i = 0; i < 3; i++) spawnGrunt(level, p, b.entityType, pos, p != null ? p.id : b.id);
        }
        return mob;
    }

    private void spawnGrunt(ServerLevel level, Pack pack, String entityType, BlockPos near, UUID owner) {
        BlockPos pos = findSpot(level, near, 1, 8);
        if (pos == null) return;
        EntityType<?> type = EntityType.byString(entityType).orElse(EntityType.ZOMBIE);
        if (!(type.create(level) instanceof Mob mob)) return;
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        mob.addTag(TAG_GRUNT);
        mob.setPersistenceRequired();
        if (mob instanceof AbstractPiglin piglin) piglin.setImmuneToZombification(true);
        if (pack != null && wearsGear(mob)) {
            mob.setItemSlot(EquipmentSlot.HEAD, cap(pack.color));
            mob.setDropChance(EquipmentSlot.HEAD, 0);
        }
        grunts.put(mob.getUUID(), owner);
        gruntLevel.put(mob.getUUID(), level.dimension());
        level.addFreshEntity(mob);
    }

    private BlockPos findSpot(ServerLevel level, BlockPos near, int min, int max) {
        return Spots.near(level, near, min, max, level.getRandom());
    }

    // ---------------------------------------------------------------- turning a mob into a Brute

    /** Makes a mob the living body of a Brute record: name, gear, stats, boss bar. */
    void becomeBrute(Mob mob, Brute b) {
        removeActive(b.id, true);
        for (String t : new ArrayList<>(mob.getTags())) if (t.startsWith(TAG_ID) || t.startsWith(TAG_SAW)) mob.removeTag(t);
        mob.removeTag(TAG_GRUNT);
        mob.addTag(TAG_BRUTE);
        mob.addTag(TAG_ID + b.id);
        mob.setPersistenceRequired();
        if (mob instanceof AbstractPiglin piglin) piglin.setImmuneToZombification(true);
        Pack pack = data.feud.packOf(b);
        dress(mob, b, pack, true);
        Rank rank = data.feud.rank(b);
        ServerBossEvent bar = new ServerBossEvent(barName(b, rank),
                rank == Rank.KING ? BossEvent.BossBarColor.PURPLE : rank == Rank.BOSS ? BossEvent.BossBarColor.RED
                        : BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
        Active a = new Active(b.id, mob.getUUID(), mob.level().dimension(), bar);
        active.put(b.id, a);
        bruteOfEntity.put(mob.getUUID(), b.id);
    }

    private void refresh(Active a, Brute b) {
        Rank rank = data.feud.rank(b);
        a.bar.setName(barName(b, rank));
        a.bar.setColor(rank == Rank.KING ? BossEvent.BossBarColor.PURPLE : rank == Rank.BOSS
                ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.YELLOW);
        ServerLevel level = server.getLevel(a.dimension);
        if (level != null && level.getEntity(a.entity) instanceof Mob mob) dress(mob, b, data.feud.packOf(b), false);
    }

    private Component barName(Brute b, Rank rank) {
        return Component.literal(b.fullName()).withStyle(rankColor(rank))
                .append(Component.literal("  " + rank.title + ", level " + b.level).withStyle(ChatFormatting.GRAY));
    }

    static ChatFormatting rankColor(Rank rank) {
        return switch (rank) {
            case KING -> ChatFormatting.LIGHT_PURPLE;
            case BOSS -> ChatFormatting.RED;
            case BRUTE -> ChatFormatting.GOLD;
        };
    }

    private void dress(Mob mob, Brute b, Pack pack, boolean fresh) {
        Rank rank = data.feud.rank(b);
        mob.setCustomName(Component.literal(b.fullName()).withStyle(rankColor(rank)));
        mob.setCustomNameVisible(true);
        if (!wearsGear(mob)) {
            // Spiders and witches can't show gear (a witch needs its hands for potions): a tougher hide instead.
            AttributeInstance armor = mob.getAttribute(Attributes.ARMOR);
            if (armor != null) {
                armor.removeModifier(HIDE_MOD);
                armor.addPermanentModifier(new AttributeModifier(HIDE_MOD, Math.min(16, 2 + b.level * 0.6)
                        + (rank == Rank.KING ? 4 : rank == Rank.BOSS ? 2 : 0), AttributeModifier.Operation.ADD_VALUE));
            }
            stats(mob, b, rank, fresh);
            return;
        }
        ItemStack head;
        if (pack != null && rank != Rank.BRUTE) head = new ItemStack(BannerBlock.byColor(DyeColor.byId(pack.color)));
        else if (pack != null) head = cap(pack.color);
        else head = new ItemStack(b.level >= 8 ? Items.IRON_HELMET : Items.CHAINMAIL_HELMET);
        mob.setItemSlot(EquipmentSlot.HEAD, head);
        mob.setDropChance(EquipmentSlot.HEAD, 0);
        if (b.level >= 6) mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(b.level >= 16 ? Items.DIAMOND_CHESTPLATE : Items.IRON_CHESTPLATE));
        if (b.level >= 12) {
            mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        }
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) mob.setDropChance(s, 0);
        if (fresh || mob.getMainHandItem().isEmpty()) {
            ItemStack hand = mob.getMainHandItem();
            boolean ranged = hand.is(Items.BOW) || hand.is(Items.CROSSBOW) || hand.is(Items.TRIDENT);
            if (!ranged) hand = new ItemStack(weapon(b.level));
            else hand = hand.copy();
            hand.set(DataComponents.CUSTOM_NAME, Component.literal(b.name + "'s " + hand.getHoverName().getString())
                    .withStyle(ChatFormatting.GOLD));
            mob.setItemSlot(EquipmentSlot.MAINHAND, hand);
        }
        mob.setDropChance(EquipmentSlot.MAINHAND, 0); // dressed gear is not loot; strong ones drop a relic instead
        stats(mob, b, rank, fresh);
    }

    /** Mobs that show what they wear and hold: zombies, skeletons, illagers, piglins. */
    static boolean wearsGear(Mob mob) {
        return mob instanceof net.minecraft.world.entity.monster.Zombie || mob instanceof net.minecraft.world.entity.monster.AbstractSkeleton
                || mob instanceof net.minecraft.world.entity.monster.AbstractIllager || mob instanceof AbstractPiglin;
    }

    private void stats(Mob mob, Brute b, Rank rank, boolean fresh) {
        double boost = rank == Rank.KING ? 1.0 : rank == Rank.BOSS ? 0.5 : 0;
        modify(mob, Attributes.MAX_HEALTH, 0.25 * b.level + 0.5 + boost);
        modify(mob, Attributes.ATTACK_DAMAGE, 0.08 * b.level + boost * 0.3);
        modify(mob, Attributes.MOVEMENT_SPEED, Math.min(0.25, 0.012 * b.level));
        modify(mob, Attributes.FOLLOW_RANGE, 0.5);
        modify(mob, Attributes.KNOCKBACK_RESISTANCE, 0);
        AttributeInstance kb = mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null) {
            kb.removeModifier(LEVEL_MOD);
            kb.addPermanentModifier(new AttributeModifier(LEVEL_MOD, b.traits.contains(Trait.UNSHAKABLE) ? 1.0 : 0.4,
                    AttributeModifier.Operation.ADD_VALUE));
        }
        if (b.traits.contains(Trait.REGENERATING)) mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, MobEffectInstance.INFINITE_DURATION, 0, false, false));
        if (b.traits.contains(Trait.FIREPROOF)) mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
        if (fresh) mob.setHealth(mob.getMaxHealth());
    }

    private static void modify(Mob mob, Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double amount) {
        AttributeInstance inst = mob.getAttribute(attribute);
        if (inst == null) return;
        inst.removeModifier(LEVEL_MOD);
        if (amount != 0) inst.addPermanentModifier(new AttributeModifier(LEVEL_MOD, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private static Item weapon(int level) {
        if (level >= 17) return Items.NETHERITE_AXE;
        if (level >= 12) return Items.DIAMOND_SWORD;
        if (level >= 6) return Items.IRON_AXE;
        return Items.IRON_SWORD;
    }

    static ItemStack cap(int color) {
        ItemStack cap = new ItemStack(Items.LEATHER_HELMET);
        cap.set(DataComponents.DYED_COLOR, new DyedItemColor(DyeColor.byId(color).getTextureDiffuseColor() & 0xFFFFFF, false));
        return cap;
    }

    // ---------------------------------------------------------------- talking to players

    /** The title card: name, rank and level big on screen, with what the player knows about it in chat. */
    void introduce(ServerPlayer p, Mob mob, Brute b, boolean hunted) {
        String key = p.getUUID() + "/" + b.id;
        long gt = gameTime();
        Relation rel = b.relation(p.getUUID());
        Rank rank = data.feud.rank(b);
        String line = Lines.greet(this, p, b, rel.memories.isEmpty() ? null : rel, rank, hunted);
        rel.met = true;
        Pack pack = data.feud.packOf(b);
        if (pack != null) pack.met.add(p.getUUID());
        if (introduced.getOrDefault(key, Long.MIN_VALUE) > gt - 6000) {
            say(p, b, line);
            return;
        }
        introduced.put(key, gt);
        MutableComponent sub = Component.literal("Level " + b.level + " " + rank.title).withStyle(ChatFormatting.GRAY);
        if (rel.nemesis) sub = Component.literal("Your Nemesis • ").withStyle(ChatFormatting.DARK_RED).append(sub);
        Titles.show(p, Component.literal(b.fullName()).withStyle(rankColor(rank), ChatFormatting.BOLD), sub);
        if (pack != null) {
            p.sendSystemMessage(Component.literal(b.fullName() + ", " + data.feud.standing(b) + ".").withStyle(rankColor(rank)));
        }
        say(p, b, line);
        p.sendSystemMessage(knownLine(b, rel));
        data.changed();
    }

    static Component knownLine(Brute b, Relation rel) {
        List<String> known = new ArrayList<>();
        int unknown = 0;
        for (Trait t : b.traits) {
            if (rel != null && rel.known.contains(t)) known.add(t.title);
            else unknown++;
        }
        StringBuilder s = new StringBuilder(known.isEmpty() ? "You know nothing about it yet" : "Known: " + String.join(", ", known));
        if (unknown > 0) s.append(known.isEmpty() ? " (" : ". ").append(unknown).append(known.isEmpty() ? " secrets)." : " more you don't know.");
        else s.append(".");
        if (!b.scars.isEmpty()) {
            s.append(" Scars: ");
            for (int i = 0; i < b.scars.size(); i++) s.append(i == 0 ? "" : ", ").append(b.scars.get(i).key());
            s.append(".");
        }
        return Component.literal(s.toString()).withStyle(ChatFormatting.DARK_GRAY);
    }

    void say(ServerPlayer p, Brute b, String line) {
        Rank rank = data.feud.rank(b);
        p.sendSystemMessage(Component.literal("<" + b.name + "> ").withStyle(rankColor(rank))
                .append(Component.literal(line).withStyle(ChatFormatting.WHITE)));
    }

    /** A player found out one of a Brute's traits, by running into it. */
    void discover(ServerPlayer p, Brute b, Trait t) {
        Relation r = b.relation(p.getUUID());
        if (!r.known.add(t)) return;
        Titles.actionBar(p, Component.literal(b.name + ": " + t.title).withStyle(t.kind == Trait.Kind.WEAKNESS
                || t.kind == Trait.Kind.FEAR ? ChatFormatting.GREEN : ChatFormatting.RED));
        p.sendSystemMessage(Component.literal("You learned something about " + b.name + ": " + t.title + ". " + t.description)
                .withStyle(ChatFormatting.GRAY));
        data.changed();
    }

    void stolen(Brute b, ServerPlayer p, ItemStack item) {
        data.stolen.computeIfAbsent(b.id, k -> new ArrayList<>()).add(item);
        String name = item.getHoverName().getString().toLowerCase(Locale.ROOT);
        data.feud.stole(b.id, p.getUUID(), name, now());
        say(p, b, Lines.stole(this, p, b, name));
        data.changed();
    }

    /** Ops only: makes a Brute of a family at a player and brings it out. */
    public Brute makeBrute(ServerPlayer p, Family family) {
        Brute b = data.feud.rise(family.defaultType, family, dim(p.level()), p.getBlockX(), p.getBlockZ(), p.getUUID(),
                Memory.Kind.SAW_KILLS, null, null, now());
        data.feud.drainNews();
        Mob mob = spawnBrute(p.serverLevel(), b, p.blockPosition(), 6, 12);
        if (mob != null) introduce(p, mob, b, false);
        data.changed();
        return b;
    }

    public void forgetAll() {
        for (UUID id : new ArrayList<>(active.keySet())) removeActive(id, true);
        data.feud.brutes.clear();
        data.feud.packs.clear();
        data.stolen.clear();
        marked.clear();
        data.changed();
    }
}
