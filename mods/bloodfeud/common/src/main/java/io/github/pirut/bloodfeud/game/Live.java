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
import io.github.pirut.bloodfeud.feud.Scar;
import io.github.pirut.bloodfeud.feud.SettlementTier;
import io.github.pirut.bloodfeud.game.camp.CampBlueprint;
import io.github.pirut.bloodfeud.game.camp.CampBuilder;
import io.github.pirut.bloodfeud.game.camp.SiteFinder;
import io.github.pirut.bloodfeud.feud.Trait;
import io.github.pirut.lib.Spots;
import io.github.pirut.lib.Titles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
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
import net.minecraft.world.level.levelgen.Heightmap;
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
    private static final ResourceLocation SIZE_MOD = BloodFeud.id("size");
    private static final ResourceLocation SCAR_MOD = BloodFeud.id("scar");
    private static final int WITNESS_RADIUS = 16;
    private static final int BAR_RANGE = 48;
    private static final int WITHDRAW_RANGE = 96;
    private static final int PACK_ENCOUNTER_RANGE = 80;
    private static final int LONE_ENCOUNTER_RANGE = 128;
    private static final int HUNTER_ENCOUNTER_RANGE = 400;

    private static Live instance;

    final MinecraftServer server;
    final Random random = new Random();
    private final FeudData data;
    private final CampBuilder campBuilder = new CampBuilder();
    private final Settler settler = new Settler();

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
        /**
         * It just rose and is walking off: it fights no one, and is gone once nobody is near enough to see it, or at
         * this game time at the latest. -1 when it isn't leaving.
         */
        long leaveUntil = -1;
        long leftAt;
        /** Where it stood when it rose: its record goes back here if it has no pack. */
        double roseX, roseZ;
        /** Where the player it saw (or killed) stood then, which it walks away from. */
        double awayX, awayZ;
        /** Out on patrol from its pack's home: it isn't called back to its settlement while it hunts. */
        boolean patrol;
        /** What its departure switched off, to switch back on if it ever stops departing in the world. */
        Departure.Held held;

        Active(UUID brute, UUID entity, ResourceKey<net.minecraft.world.level.Level> dimension, ServerBossEvent bar) {
            this.brute = brute;
            this.entity = entity;
            this.dimension = dimension;
            this.bar = bar;
        }
    }


    private final Map<UUID, Active> active = new HashMap<>();
    private final Map<UUID, UUID> bruteOfEntity = new HashMap<>();
    /** Grunt mob -> the pack (or lone Brute) it came with. */
    private final Map<UUID, UUID> grunts = new HashMap<>();
    private final Map<UUID, ResourceKey<net.minecraft.world.level.Level>> gruntLevel = new HashMap<>();
    /** Everything packs and lone Brutes have sent out: patrols, encounters and garrisons (see {@link Outings}). */
    private final Outings outings = new Outings();
    private final Rises rises = new Rises();
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
        this.data.feud.campsEnabled = BloodFeud.config().buildCamps;
        configure(this.data.feud);
        campBuilder.setHiring(this::hireBuilder);
    }

    /** Hands the feud the settings it plays by. */
    static void configure(Feud f) {
        Config cfg = BloodFeud.config();
        f.retreatMin = cfg.retreatMinDistance;
        f.retreatMax = cfg.retreatMaxDistance;
        f.patrolRange = cfg.patrolRange;
    }

    /** One builder for a pack's settlement: a grunt of its kind, standing near the site. */
    private Mob hireBuilder(ServerLevel level, Pack pack, BlockPos near, int radius) {
        Brute boss = data.feud.brute(pack.boss);
        String type = boss != null ? boss.entityType : pack.family.defaultType;
        Mob mob = spawnGrunt(level, pack, type, near, pack.id, false);
        if (mob == null) {
            BlockPos at = Spots.near(level, near, 2, Math.max(8, Math.min(16, radius)), level.getRandom());
            if (at != null) mob = spawnGrunt(level, pack, type, at, pack.id, false);
        }
        if (mob != null) {
            // A crew is hired for the build, not for farming: it drops no loot and gives no experience. Killing it
            // still costs the pack its grunts, stalls the build and is remembered.
            CompoundTag tag = new CompoundTag();
            mob.saveWithoutId(tag);
            tag.putString("DeathLootTable", BuiltInLootTables.EMPTY.location().toString());
            mob.load(tag);
            mob.skipDropExperience();
            // No drops (its gear too, a brute's axe), no pickups, and no burning in the sun while it builds.
            CampBuilder.outfitCrew(mob);
        }
        return mob;
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
        if (instance != null) {
            for (Active a : instance.active.values()) a.bar.removeAllPlayers();
            instance.campBuilder.clearAll(instance.server);
        }
        Earth.stopped();
        instance = null;
    }

    /** Called live when buildCamps is toggled on or off. */
    public void onBuildCampsChanged(boolean enabled) {
        data.feud.campsEnabled = enabled;
        if (!enabled) campBuilder.cancelAutomatic(server);
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
        if (tags.contains(TAG_GRUNT)) {
            if (!grunts.containsKey(e.getUUID())) return false; // its outing is over (or it was before a restart)
            if (e instanceof Mob m) Outings.onLoad(m);
            return true;
        }
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
        outings.leave(mob.getUUID());
        if (owner != null) {
            Pack p = data.feud.pack(owner);
            ServerPlayer playerKiller = source.getEntity() instanceof ServerPlayer sp ? sp : null;
            if (p != null && campBuilder.isBuilder(mob.getUUID())
                    && campBuilder.builderDied(p, mob.getUUID(), playerKiller == null ? null : playerKiller.getUUID(), gameTime())) {
                String what = p.works == null ? "camp" : SettlementTier.byLevel(p.works.toTier).key;
                data.feud.wreckedBuild(p, playerKiller.getUUID(), playerKiller.getName().getString(), what, now());
                playerKiller.sendSystemMessage(Component.literal("You killed the last of " + p.title() + "'s builders. Its "
                        + what + " stands half built, until it sends more.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
            if (p != null) {
                data.feud.gruntKilled(p, now());
                announce(data.feud.drainNews());
            }
            data.changed();
        }
        rises.forget(mob.getUUID());
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
            if (saw >= BloodFeud.config().killsToRise && !rises.isMarked(m.getUUID()) && mayRise(killer)) {
                rises.mark(m.getUUID(), new Rises.Mark(m.level().dimension(), killer.getUUID(), gameTime(),
                        pack != null ? pack : grunts.get(m.getUUID()), killer.getX(), killer.getZ()));
            }
        }
    }

    /** One mob rises per player at a time, and not again until the cooldown is over. */
    private boolean mayRise(ServerPlayer killer) {
        if (killer.level().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return false;
        if (rises.hasMarkFor(killer.getUUID())) return false;
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
            takeGear(mob, b, player);
            dress(mob, b, f.packOf(b), false);
            Active a = active.get(b.id);
            if (a != null) a.fought.remove(player.getUUID());
        } else {
            Family family = Families.of(mob.getType());
            if (family == null || mob.getTags().contains(TAG_GRUNT) && grunts.get(mob.getUUID()) == null
                    || !Rises.canRise(mob, bruteOfEntity)) {
                advanceForDeath();
                return;
            }
            UUID pack = grunts.remove(mob.getUUID());
            gruntLevel.remove(mob.getUUID());
            mob.removeTag(TAG_GRUNT);
            promoteMember(mob);
            rises.forget(mob.getUUID()); // it rose this way: it won't rise again for the kills it saw
            b = f.rise(EntityType.getKey(mob.getType()).toString(), family, dim(mob.level()), mob.getBlockX(),
                    mob.getBlockZ(), player.getUUID(), Memory.Kind.ROSE, cause, pack, now());
            takeGear(mob, b, player);
            becomeBrute(mob, b);
            say(player, b, Lines.rose(this, player, b, true));
            // Where the player died, before any respawn: the fight.
            sendAway(mob, b, player.getX(), player.getZ());
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
        flushDirtyBanners();
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
        campBuilder.tick(server, data, gt);
        boolean scheduled = gt % 40 == 17;
        if (scheduled || campBuilder.isWaitingForCubes()) campBuilder.checkAutoBuilds(server, data, gt, scheduled);
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
            if (settler.tick(this::level, data.feud, gt)) data.changed();
        }
        boolean anyLeaving = false;
        for (Active a : active.values()) anyLeaving |= a.leaveUntil >= 0;
        if (anyLeaving) for (Active a : new ArrayList<>(active.values())) if (a.leaveUntil >= 0) leaving(a, gt);
        if (gt % 200 == 7) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) encounters(p, gt);
        }
        if (gt % 40 == 11) sendScars();
        if (gt % 5 == 2) {
            for (Active a : active.values()) {
                Brute b = data.feud.brute(a.brute);
                ServerLevel level = server.getLevel(a.dimension);
                if (b != null && !b.scars.isEmpty() && level != null && level.getEntity(a.entity) instanceof Mob mob) {
                    Effects.scars(b, mob, level);
                }
            }
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
        f.campsEnabled = BloodFeud.config().buildCamps;
        configure(f);
        f.night(now, spots, this::placeName);
        passOnLoot();
        for (Active a : new ArrayList<>(active.values())) {
            Brute b = f.brute(a.brute);
            if (b == null || !b.alive) removeActive(a.brute, true);
            else refresh(a, b);
        }
        List<News> news = f.drainNews();
        flushDirtyBanners();
        announce(news);
        data.changed();
    }

    /** Re-banners loaded built settlements marked dirty only while players are away; others retry off-screen via CampBuilder. */
    private void flushDirtyBanners() {
        flushDirtyBanners(true);
    }

    private void flushDirtyBanners(boolean requireAway) {
        Feud f = data.feud;
        boolean dirtied = false;
        for (Pack pk : f.packs.values()) {
            if (!pk.bannersDirty || !pk.siteSet || pk.builtTier <= 0 || campBuilder.isBuilding(pk.id)) continue;
            if (requireAway) {
                SettlementTier built = SettlementTier.byLevel(pk.builtTier);
                if (!CampBuilder.isAreaClearOfPlayers(server, pk.dimension, pk.siteX, pk.siteZ, built.radius)) {
                    continue;
                }
            }
            if (campBuilder.rebanner(level(pk.dimension), f, pk)) {
                pk.bannersDirty = false;
                dirtied = true;
            }
        }
        if (dirtied) data.changed();
    }

    /**
     * What a Brute took from players isn't lost when it dies for good away from them (in a raid, say): its pack's Boss
     * takes it, or the strongest Brute left, so there is still someone to fight for it.
     */
    private void passOnLoot() {
        Feud f = data.feud;
        for (UUID id : new ArrayList<>(data.stolen.keySet())) {
            Brute b = f.brute(id);
            if (b != null && (b.alive || !b.finalDeath)) continue;
            List<ItemStack> items = data.stolen.remove(id);
            Pack pack = b == null ? null : f.pack(b.pack);
            Brute heir = pack == null ? null : f.brute(pack.boss);
            if (heir == null || !heir.alive || heir == b) {
                heir = null;
                if (pack != null) {
                    for (Brute m : f.members(pack)) if (m.alive && m != b && (heir == null || m.level > heir.level)) heir = m;
                }
            }
            if (heir != null && items != null) data.stolen.computeIfAbsent(heir.id, k -> new ArrayList<>()).addAll(items);
        }
    }

    private String placeName(Random r, String dimension, int x, int z) {
        ServerLevel level = level(dimension);
        String biome = "";
        if (level != null) {
            if (Earth.is(level)) {
                // Only the ground's own cube knows the biome: the carrier says plains everywhere. Far away, no biome.
                if (Earth.knowsHeight(level, x, z)) {
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                    if (Earth.isLoaded(level, new BlockPos(x, y, z))) {
                        biome = level.getNoiseBiome(x >> 2, y >> 2, z >> 2).unwrapKey()
                                .map(k -> k.location().toString()).orElse("");
                    }
                }
            } else {
                biome = level.getBiome(new BlockPos(x, level.getSeaLevel(), z)).unwrapKey()
                        .map(k -> k.location().toString()).orElse("");
            }
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
        rises.tick(gt, (id, m) -> {
            ServerLevel level = server.getLevel(m.dimension());
            if (level == null) return Rises.Outcome.NEVER;
            // Not loaded (or not yet) is not the same as gone: its mark waits. A dead mob's mark went with it.
            if (!(level.getEntity(id) instanceof Mob mob)) return Rises.Outcome.NOT_YET;
            if (!Rises.canRise(mob, bruteOfEntity)) return Rises.Outcome.NEVER;
            Family family = Families.of(mob.getType());
            if (family == null) return Rises.Outcome.NEVER;
            if (active.size() >= BloodFeud.config().maxActiveBrutes) return Rises.Outcome.NOT_YET;
            UUID pack = grunts.remove(mob.getUUID());
            gruntLevel.remove(mob.getUUID());
            mob.removeTag(TAG_GRUNT);
            promoteMember(mob);
            Brute b = data.feud.rise(EntityType.getKey(mob.getType()).toString(), family, dim(level), mob.getBlockX(),
                    mob.getBlockZ(), m.player(), Memory.Kind.SAW_KILLS, null, pack != null ? pack : m.pack(), now());
            becomeBrute(mob, b);
            lastRise.put(m.player(), gt);
            ServerPlayer player = server.getPlayerList().getPlayer(m.player());
            if (player != null && player.level() == level && player.distanceTo(mob) < 96) {
                introduce(player, mob, b, false);
            }
            sendAway(mob, b, m.witnessX(), m.witnessZ());
            data.feud.drainNews();
            data.changed();
            return Rises.Outcome.ROSE;
        });
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
        if (a.leaveUntil >= 0) return; // it is walking off: see leaving()
        bark(a, b, mob, level);
        Pack pk = b.pack == null ? null : data.feud.pack(b.pack);
        if (pk == null || !b.id.equals(pk.boss)) {
            b.x = mob.getBlockX();
            b.z = mob.getBlockZ();
        }
        int withdrawRange = (pk != null && pk.builtTier > 0 && pk.siteSet)
                ? Math.max(WITHDRAW_RANGE, packEncounterRange(pk) + 24)
                : WITHDRAW_RANGE;
        if (nearest > withdrawRange) {
            if (a.farSince < 0) a.farSince = gt;
            else if (gt - a.farSince > 400) {
                withdraw(a, b, mob, gt);
                return;
            }
        } else {
            a.farSince = -1;
        }
        if (!a.patrol) enforceSettlementLeash(mob, pk);
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
        Pack pk = data.feud.packOf(b);
        if (a.patrol && pk != null) { // the patrol goes back home
            b.x = pk.campX();
            b.z = pk.campZ();
        }
        rested.put(b.id, gt + 1200);
        if (b.pack != null) rested.put(b.pack, gt + 1200);
        data.changed();
    }

    // ---------------------------------------------------------------- a risen Brute walks off

    /**
     * A mob that just rose doesn't stay to fight: it turns and walks off, away from where the fight was (toward its
     * pack's home if it has one), and is gone once nobody is near enough to see it, or after {@code risenLeaveSeconds}.
     * It can't fight while it departs (see {@link Departure}). Its rest starts now and is saved: however it leaves the
     * world, it stays away {@code risenRestSeconds} after that. It comes back later, through the feud: a pack forming
     * round it, its pack's patrols, or hunting you down.
     *
     * @param awayX where the player it saw or killed stood at the time
     */
    private void sendAway(Mob mob, Brute b, double awayX, double awayZ) {
        Active a = active.get(b.id);
        if (a == null) return;
        long gt = gameTime();
        a.leftAt = gt;
        a.leaveUntil = gt + BloodFeud.config().risenLeaveSeconds * 20L;
        a.roseX = mob.getX();
        a.roseZ = mob.getZ();
        a.awayX = awayX;
        a.awayZ = awayZ;
        b.restUntil(a.leaveUntil + restTicks());
        a.held = Departure.hold(mob);
        for (ServerPlayer p : ((ServerLevel) mob.level()).players()) {
            if (p.isSpectator() || p.distanceTo(mob) > 64) continue;
            p.sendSystemMessage(Component.literal(b.fullName() + " turns and walks off. It will be back.")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        BloodFeud.LOG.debug("Blood Feud: {} rose at {} and walks off", b.fullName(), mob.blockPosition().toShortString());
        data.changed();
    }

    private static long restTicks() {
        return BloodFeud.config().risenRestSeconds * 20L;
    }

    /** Each tick of a Brute walking off. */
    private void leaving(Active a, long gt) {
        Brute b = data.feud.brute(a.brute);
        ServerLevel level = server.getLevel(a.dimension);
        if (b == null || !b.alive || level == null) return; // watch() tidies it up
        if (!(level.getEntity(a.entity) instanceof Mob mob) || !mob.isAlive()) return;
        Departure.suppress(mob, a.held); // every tick: vanilla turns movement and looking back on every few ticks
        long walked = gt - a.leftAt;
        if (walked >= 60 && (gt >= a.leaveUntil || unseen(level, mob))) {
            vanish(a, b, mob, level, gt);
            return;
        }
        if (walked % 10 == 0 || mob.getNavigation().isDone()) steerAway(a, b, mob, level);
    }

    /** No player is close, and none farther off can see it. */
    private static boolean unseen(ServerLevel level, Mob mob) {
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            double d = p.distanceTo(mob);
            if (d < 24 || d < 96 && p.hasLineOfSight(mob)) return false;
        }
        return true;
    }

    /**
     * Walks it on toward a loaded, standable spot about 16 blocks further off. It never heads into ground that
     * isn't loaded (on the Earth, cubes nobody watches): with nowhere to go it stands, and is gone soon after.
     */
    private void steerAway(Active a, Brute b, Mob mob, ServerLevel level) {
        Pack pk = data.feud.packOf(b);
        double dx, dz;
        if (pk != null && pk.dimension.equals(dim(level))) {
            dx = pk.campX() + 0.5 - mob.getX();
            dz = pk.campZ() + 0.5 - mob.getZ();
        } else {
            dx = mob.getX() - a.awayX;
            dz = mob.getZ() - a.awayZ;
        }
        double len = Math.hypot(dx, dz);
        if (len < 1) {
            double angle = random.nextDouble() * Math.PI * 2;
            dx = Math.cos(angle);
            dz = Math.sin(angle);
        } else {
            dx /= len;
            dz /= len;
        }
        for (int reach : new int[]{16, 10, 6}) {
            // Straight on first, then a little to either side.
            for (double turn : new double[]{0, 0.6, -0.6, 1.2, -1.2}) {
                double cx = dx * Math.cos(turn) - dz * Math.sin(turn), cz = dx * Math.sin(turn) + dz * Math.cos(turn);
                int x = (int) Math.floor(mob.getX() + cx * reach), z = (int) Math.floor(mob.getZ() + cz * reach);
                BlockPos spot = standableNear(level, x, mob.getBlockY(), z);
                if (spot == null) continue;
                mob.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 1.2);
                mob.getBrain().setMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET,
                        new net.minecraft.world.entity.ai.memory.WalkTarget(spot, 1.2f, 1));
                return;
            }
        }
    }

    /** A loaded spot a mob can stand on in column {@code (x, z)}, within 6 blocks up or down of {@code y}, or null. */
    private static BlockPos standableNear(ServerLevel level, int x, int y, int z) {
        if (!level.hasChunk(x >> 4, z >> 4)) return null;
        for (int dy = 0; dy <= 6; dy++) {
            for (int sign : new int[]{1, -1}) {
                BlockPos p = new BlockPos(x, y + dy * sign, z);
                if (!Earth.isLoaded(level, p.below()) || !Earth.isLoaded(level, p.above())) continue;
                if (Spots.standable(level, p, false)) return p;
                if (dy == 0) break;
            }
        }
        return null;
    }

    /** It's gone: back to being a record, at its pack's home or where it rose (see {@link #recordGone}). */
    private void vanish(Active a, Brute b, Mob mob, ServerLevel level, long gt) {
        if (level.getNearestPlayer(mob, 96) != null) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, mob.getX(), mob.getY() + 0.8, mob.getZ(),
                    12, 0.3, 0.6, 0.3, 0.02);
        }
        Pack pk = data.feud.packOf(b);
        BloodFeud.LOG.info("Blood Feud: {} rose and left: gone {} blocks from where it rose after {} s, {}",
                b.fullName(), Math.round(Math.hypot(mob.getX() - a.roseX, mob.getZ() - a.roseZ)), (gt - a.leftAt) / 20,
                pk != null ? "back to " + pk.title() : "off on its own");
        removeActive(b.id, true); // records it (recordGone) and restarts its rest
        data.changed();
    }

    /**
     * Where a departed Brute is on paper: at its pack's camp, or, on its own, where it rose (its own position, never
     * where a player went after). A pack forming round it then withdraws from the fight itself.
     */
    static void recordGone(Brute b, Pack pk, double roseX, double roseZ) {
        if (pk != null) {
            b.x = pk.campX();
            b.z = pk.campZ();
        } else {
            b.x = (int) Math.floor(roseX);
            b.z = (int) Math.floor(roseZ);
        }
    }

    // ---------------------------------------------------------------- patrols

    /**
     * A pack's patrol finds a player out in the ground it holds: a Brute (one hunting the player if it has one, never
     * the Boss while it has others) and a few escorts, coming from the direction of its home. Everyone comes out on a
     * {@link PatrolSpots} spot: dark, clear of player blocks, on known ground.
     */
    private boolean spawnPatrol(ServerLevel level, Pack pk, ServerPlayer p, long gt, Brute leader) {
        Feud f = data.feud;
        if (active.size() >= BloodFeud.config().maxActiveBrutes) return false;
        if (leader == null) leader = f.patrolLeader(pk, p.getUUID(), b -> !isActive(b) && !resting(b.id, gt));
        if (leader == null) return false;
        double dx = pk.campX() - p.getX(), dz = pk.campZ() - p.getZ(), d = Math.hypot(dx, dz);
        if (d < 1) {
            dx = 1;
            d = 1;
        }
        Outings.Outing outing = outings.start(pk.id, true);
        BlockPos at = null;
        for (int reach : new int[]{30, 22}) {
            BlockPos base = BlockPos.containing(p.getX() + dx / d * reach, p.getY(), p.getZ() + dz / d * reach);
            at = PatrolSpots.find(level, base, 0, 6, level.getRandom(), pk);
            if (at != null) break;
        }
        if (at == null) at = PatrolSpots.find(level, p.blockPosition(), 20, 30, level.getRandom(), pk);
        Mob mob = at == null ? null : spawnBruteAt(level, leader, at, outing);
        if (mob == null) {
            outings.end(outing);
            return false;
        }
        mob.setTarget(p);
        Brute boss = f.brute(pk.boss);
        String type = boss != null ? boss.entityType : leader.entityType;
        int grunts = Math.min(pk.grunts, 2 + random.nextInt(2));
        int out = 0;
        for (int i = 0; i < grunts; i++) {
            Mob g = spawnMember(level, pk, type, mob.blockPosition(), false, outing, pk.id);
            if (g == null) continue;
            g.setTarget(p);
            out++;
        }
        pk.met.add(p.getUUID());
        introduce(p, mob, leader, p.getUUID().equals(leader.hunting));
        p.sendSystemMessage(Component.literal("A patrol of " + pk.title() + " found you"
                + (out > 0 ? ", " + out + (out == 1 ? " grunt" : " grunts") + " with it" : "") + ".")
                .withStyle(ChatFormatting.GRAY));
        BloodFeud.LOG.info("Blood Feud: a patrol of {} ({} and {} grunts) found {} {} blocks from its home",
                pk.title(), leader.fullName(), out, p.getName().getString(), Math.round(d));
        data.changed();
        return true;
    }

    /** A member that rises to a Brute leaves its outing first: the outing's end never takes the new Brute. */
    private void promoteMember(Mob mob) {
        outings.leave(mob.getUUID());
        Outings.promote(mob);
    }

    private void removeActive(UUID bruteId, boolean discard) {
        Active a = active.remove(bruteId);
        if (a == null) return;
        bruteOfEntity.remove(a.entity);
        a.bar.removeAllPlayers();
        ServerLevel level = server.getLevel(a.dimension);
        Entity e = level == null ? null : level.getEntity(a.entity);
        Brute b = data.feud.brute(bruteId);
        if (a.leaveUntil >= 0 && b != null && b.alive) {
            // It left mid-departure, by whatever path (gone from sight, unloaded, missing): it is a record now, and
            // its rest runs from now.
            recordGone(b, data.feud.packOf(b), a.roseX, a.roseZ);
            b.restUntil(gameTime() + restTicks());
            data.changed();
        }
        if (e instanceof Mob m) Departure.release(m, a.held); // if it stays in the world, it can fight again
        if (discard && e != null && !e.isRemoved()) e.discard();
    }

    /**
     * Each second: every outing that is over sends its members back into the pack (see {@link Outings}); garrison
     * members of one still out are called back to their settlement when they chase too far.
     */
    private void gruntCheck() {
        long gt = gameTime();
        for (Outings.Outing o : outings.all()) {
            boolean leaderOut = false;
            for (UUID l : o.leaders) leaderOut |= active.containsKey(l);
            long farFor = 0;
            if (o.leaders.isEmpty()) {
                boolean near = false;
                for (UUID m : o.members) {
                    Entity e = memberEntity(m);
                    if (e != null && e.level().getNearestPlayer(e, WITHDRAW_RANGE) != null) near = true;
                }
                if (near) o.farSince = -1;
                else if (o.farSince < 0) o.farSince = gt;
                farFor = o.farSince < 0 ? 0 : gt - o.farSince;
            }
            if (!Outings.over(!o.leaders.isEmpty(), leaderOut, o.members.size(), farFor)) {
                Pack pk = o.pack == null ? null : data.feud.pack(o.pack);
                if (!o.patrol && pk != null && pk.builtTier > 0 && pk.siteSet) {
                    for (UUID m : o.members) if (memberEntity(m) instanceof Mob mob && mob.isAlive()) enforceSettlementLeash(mob, pk);
                }
                continue;
            }
            for (UUID m : outings.end(o)) {
                Entity e = memberEntity(m);
                grunts.remove(m);
                gruntLevel.remove(m);
                if (e == null || e.isRemoved()) continue; // unloaded: it is dropped if it ever loads again
                if (e.level() instanceof ServerLevel level && level.getNearestPlayer(e, 96) != null) {
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, e.getX(), e.getY() + 0.8, e.getZ(),
                            8, 0.3, 0.6, 0.3, 0.02);
                }
                e.discard();
            }
            long rest = Outings.restAfter(o, gt);
            if (o.pack != null && rest > 0) rested.merge(o.pack, rest, Math::max);
        }
    }

    private Entity memberEntity(UUID id) {
        ServerLevel level = server.getLevel(gruntLevel.getOrDefault(id, net.minecraft.world.level.Level.OVERWORLD));
        return level == null ? null : level.getEntity(id);
    }

    /** Actively calls wandering garrison mobs back to their built settlement when they chase too far afield. */
    private static void enforceSettlementLeash(Mob mob, Pack pk) {
        if (pk == null || pk.builtTier <= 0 || !pk.siteSet) return;
        SettlementTier built = SettlementTier.byLevel(pk.builtTier);
        double dist = Math.hypot(mob.getX() - (pk.siteX + 0.5), mob.getZ() - (pk.siteZ + 0.5));
        LivingEntity target = mob.getTarget();
        boolean hasNearTarget = target != null && target.isAlive() && mob.distanceTo(target) <= 24.0;
        double softLeash = built.radius + 32.0;
        double hardLeash = built.radius + 52.0;
        if (dist > hardLeash || (dist > softLeash && !hasNearTarget)) {
            mob.setTarget(null);
            mob.getNavigation().moveTo(pk.siteX + 0.5, pk.siteY + 1, pk.siteZ + 0.5, 1.0);
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
        boolean earth = Earth.is(level);
        BlockPos spawn = earth ? Earth.arrival(level) : level.getSharedSpawnPos();
        if ((earth || level.dimension() == net.minecraft.world.level.Level.OVERWORLD) && spawn != null
                && Math.hypot(p.getX() - spawn.getX(), p.getZ() - spawn.getZ()) < cfg.spawnSafeRadius) return;
        String dim = dim(level);
        Feud f = data.feud;
        for (Brute b : f.brutes.values()) {
            if (!b.alive || !p.getUUID().equals(b.hunting) || !b.dimension.equals(dim) || isActive(b) || resting(b.id, gt)) continue;
            Pack own = f.patrolFor(b, dim, p.getX(), p.getZ());
            if (own != null) {
                // Anywhere in its own pack's ground (its patrol range) it comes, Boss or not, leading that pack's
                // patrol, whichever pack is nearest the player.
                if (!resting(own.id, gt) && !packOut(own) && spawnPatrol(level, own, p, gt, b)) {
                    nextEncounter.put(p.getUUID(), gt + cfg.encounterCooldownSeconds * 20L);
                    return;
                }
                continue;
            }
            if (Math.hypot(b.x - p.getX(), b.z - p.getZ()) > HUNTER_ENCOUNTER_RANGE) continue;
            Pack pack = f.packOf(b);
            if (pack != null && b.id.equals(pack.boss)) continue; // out of its ground, a Boss stays home
            if (pack != null && pack.settling) continue; // its pack is on the move
            boolean followed = Math.hypot(b.x - p.getX(), b.z - p.getZ()) > 300;
            Mob mob = spawnBrute(level, b, p.blockPosition(), 20, 32, false, null, pack);
            if (mob == null) continue;
            mob.setTarget(p);
            introduce(p, mob, b, followed);
            nextEncounter.put(p.getUUID(), gt + cfg.encounterCooldownSeconds * 20L);
            return;
        }
        Pack near = null;
        double best = Double.MAX_VALUE;
        for (Pack pk : f.packs.values()) {
            if (pk.settling) continue; // on the move to its new home
            double range = packEncounterRange(pk);
            double d = pk.distanceTo(dim, p.getX(), p.getZ());
            if (d <= range && d < best && !resting(pk.id, gt) && !packOut(pk)) {
                // A settlement far above or below the player has no cubes loaded, so nowhere for its garrison to stand.
                if (earth && pk.builtTier > 0 && pk.siteSet
                        && !Earth.isLoaded(level, new BlockPos(pk.siteX, pk.siteY, pk.siteZ))) continue;
                near = pk;
                best = d;
            }
        }
        if (near != null) {
            spawnPack(level, near, p);
            nextEncounter.put(p.getUUID(), gt + cfg.encounterCooldownSeconds * 20L);
            return;
        }
        // Away from its home but in the ground it holds, a pack's patrol can find you.
        Pack ground = null;
        double groundBest = Double.MAX_VALUE;
        for (Pack pk : f.packs.values()) {
            if (pk.settling || resting(pk.id, gt) || packOut(pk)) continue;
            double d = pk.distanceTo(dim, p.getX(), p.getZ());
            if (d > packEncounterRange(pk) && f.inTerritory(pk, dim, p.getX(), p.getZ()) && d < groundBest) {
                ground = pk;
                groundBest = d;
            }
        }
        if (ground != null) {
            if (random.nextInt(100) < cfg.patrolChancePercent && spawnPatrol(level, ground, p, gt, null)) {
                nextEncounter.put(p.getUUID(), gt + cfg.encounterCooldownSeconds * 20L);
                return;
            }
        }
        if (random.nextInt(100) >= cfg.encounterChancePercent) return;
        for (Brute b : f.alive(dim, p.getX(), p.getZ(), LONE_ENCOUNTER_RANGE)) {
            if (b.pack != null || isActive(b) || resting(b.id, gt)) continue;
            Mob mob = spawnBrute(level, b, p.blockPosition(), 18, 30, false, null, null);
            if (mob == null) continue;
            mob.setTarget(p);
            introduce(p, mob, b, false);
            nextEncounter.put(p.getUUID(), gt + cfg.encounterCooldownSeconds * 20L);
            return;
        }
    }

    /** A pack or Brute that just left doesn't come straight back; a risen Brute's rest is saved with it. */
    private boolean resting(UUID id, long gt) {
        if (rested.getOrDefault(id, 0L) > gt) return true;
        Brute b = data.feud.brute(id);
        return b != null && b.resting(gt);
    }

    /** Whether the pack has anyone out, a Brute or a grunt: it sends one outing at a time. */
    private boolean packOut(Pack p) {
        if (outings.packOut(p.id)) return true;
        for (Active a : active.values()) {
            Brute b = data.feud.brute(a.brute);
            if (b != null && p.id.equals(b.pack)) return true;
        }
        return false;
    }

    private static int packEncounterRange(Pack pk) {
        SettlementTier t = pk.builtTier > 0 ? SettlementTier.byLevel(pk.builtTier)
                : pk.displayTier(BloodFeud.config().buildCamps);
        return switch (t) {
            case TOWN -> 128;
            case STRONGHOLD -> 108;
            case OUTPOST -> 92;
            default -> PACK_ENCOUNTER_RANGE;
        };
    }

    /** A pack comes out to meet a player near its home, or mans its camp/outpost/stronghold/town garrison. */
    public void spawnPack(ServerLevel level, Pack pack, ServerPlayer player) {
        Feud f = data.feud;
        if (player != null) pack.met.add(player.getUUID());
        boolean hasSettlement = pack.builtTier > 0 && pack.siteSet;
        SettlementTier tier = hasSettlement ? SettlementTier.byLevel(pack.builtTier)
                : pack.displayTier(BloodFeud.config().buildCamps);

        int maxBrutes = switch (tier) {
            case TOWN -> 5;
            case STRONGHOLD -> 4;
            case OUTPOST -> 3;
            default -> 3;
        };
        int gruntCount = switch (tier) {
            case TOWN -> 8 + random.nextInt(5);
            case STRONGHOLD -> 6 + random.nextInt(4);
            case OUTPOST -> 4 + random.nextInt(3);
            default -> 2 + random.nextInt(3);
        };
        int g = Math.min(pack.grunts, gruntCount);

        BlockPos origin = hasSettlement ? new BlockPos(pack.siteX, pack.siteY, pack.siteZ) : null;
        CampBlueprint.Anchors anchors = hasSettlement ? CampBlueprint.spawnAnchors(pack.siteFacing, tier) : null;
        int tetherRadius = hasSettlement ? tier.radius + 16 : 0;

        BlockPos center;
        if (hasSettlement) {
            center = origin;
        } else if (player != null) {
            Vec3 at = player.position();
            double dx = pack.campX() - at.x, dz = pack.campZ() - at.z, d = Math.hypot(dx, dz);
            center = d <= 36 ? BlockPos.containing(pack.campX(), player.getY(), pack.campZ())
                    : BlockPos.containing(at.x + dx / d * 28, player.getY(), at.z + dz / d * 28);
        } else {
            center = new BlockPos(pack.campX(), level.getSeaLevel(), pack.campZ());
        }

        List<Brute> members = f.members(pack);
        Outings.Outing outing = outings.start(pack.id, false);
        Mob bossMob = null;
        Brute boss = null;
        int out = 0;
        int guardIdx = 0;
        for (Brute b : members) {
            if (out >= maxBrutes || isActive(b) || active.size() >= BloodFeud.config().maxActiveBrutes) continue;
            BlockPos spawnAnchor = center;
            int minR = 0, maxR = 8;
            if (hasSettlement && anchors != null) {
                if (b.id.equals(pack.boss)) {
                    CampBlueprint.Spot s = anchors.bossSpot();
                    spawnAnchor = origin.offset(s.rx(), s.ry(), s.rz());
                    maxR = 3;
                } else if (!anchors.guardSpots().isEmpty()) {
                    CampBlueprint.Spot s = anchors.guardSpots().get(guardIdx++ % anchors.guardSpots().size());
                    spawnAnchor = origin.offset(s.rx(), s.ry(), s.rz());
                    maxR = 4;
                }
            }
            Mob mob = spawnBrute(level, b, spawnAnchor, minR, maxR, hasSettlement, outing, pack);
            if (mob == null && hasSettlement) mob = spawnBrute(level, b, origin, 1, Math.min(12, tier.radius), false, outing, pack);
            if (mob == null) continue;
            if (hasSettlement) mob.restrictTo(origin, tetherRadius);
            if (player != null) b.relation(player.getUUID()).met = true;
            out++;
            if (b.id.equals(pack.boss)) {
                bossMob = mob;
                boss = b;
            }
        }
        Brute leader = boss != null ? boss : members.isEmpty() ? null : members.get(0);
        String gruntType = leader == null ? pack.family.defaultType : leader.entityType;
        for (int i = 0; i < g; i++) {
            BlockPos gAnchor = center;
            if (hasSettlement && anchors != null) {
                List<CampBlueprint.Spot> pool = (i % 2 == 0 && !anchors.guardSpots().isEmpty())
                        ? anchors.guardSpots() : anchors.patrolSpots();
                if (!pool.isEmpty()) {
                    CampBlueprint.Spot s = pool.get(i % pool.size());
                    gAnchor = origin.offset(s.rx(), s.ry(), s.rz());
                }
            }
            Mob grunt = spawnMember(level, pack, gruntType, gAnchor, hasSettlement, outing, pack.id);
            if (grunt != null && hasSettlement) grunt.restrictTo(origin, tetherRadius);
        }
        if (out == 0) {
            // No Brute could come (they rest, or are out already): a wave of grunts on its own, the only one until
            // its Brutes' rest is over (see the end of outings).
            long window = 0;
            for (Brute b : members) {
                window = Math.max(window, b.restUntil);
                window = Math.max(window, rested.getOrDefault(b.id, 0L));
            }
            outing.restWindow = window;
        }
        if (player != null) {
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
        }
        data.changed();
    }

    /** Ops only: brings a Brute out near {@code near}, anywhere a mob can stand (an operator asked for it there). */
    public Mob spawnBrute(ServerLevel level, Brute b, BlockPos near, int min, int max) {
        BlockPos pos = findSpot(level, near, min, max);
        return pos == null ? null : spawnBruteAt(level, b, pos, null);
    }

    /**
     * Brings a Brute of an outing out near {@code near}, on a {@link PatrolSpots} spot (in its settlement's anchors
     * first when {@code preferAnchor}). Every way a pack's Brute comes out goes through here or
     * {@link #spawnBruteAt}.
     */
    private Mob spawnBrute(ServerLevel level, Brute b, BlockPos near, int min, int max, boolean preferAnchor,
                           Outings.Outing outing, Pack pack) {
        if (b.resting(gameTime())) return null;
        BlockPos pos = preferAnchor ? anchorSpot(level, near, min, max, pack) : PatrolSpots.find(level, near, min, max, level.getRandom(), pack);
        return pos == null ? null : spawnBruteAt(level, b, pos, outing);
    }

    /**
     * Brings a Brute out as a real mob at {@code pos}, unless it is resting, as a leader of {@code outing} (a new
     * outing of its own when null). Its Packcaller grunts come out with it, as members of the same outing.
     */
    private Mob spawnBruteAt(ServerLevel level, Brute b, BlockPos pos, Outings.Outing outing) {
        if (b.resting(gameTime())) return null;
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
        Outings.Outing o = outing != null ? outing : outings.start(b.pack, false);
        outings.addLeader(o, b.id);
        Active a = active.get(b.id);
        if (a != null) a.patrol = o.patrol;
        if (b.traits.contains(Trait.PACKCALLER)) {
            Pack p = data.feud.packOf(b);
            for (int i = 0; i < 3; i++) spawnMember(level, p, b.entityType, pos, false, o, p != null ? p.id : b.id);
        }
        return mob;
    }

    /** One member of an outing near {@code near}, on a {@link PatrolSpots} spot; tagged and no farm. */
    private Mob spawnMember(ServerLevel level, Pack pack, String entityType, BlockPos near, boolean preferAnchor,
                            Outings.Outing outing, UUID owner) {
        BlockPos pos = preferAnchor ? anchorSpot(level, near, 1, 8, pack) : PatrolSpots.find(level, near, 1, 8, level.getRandom(), pack);
        if (pos == null) return null;
        Mob mob = spawnGruntAt(level, pack, entityType, pos, owner);
        if (mob == null) return null;
        Outings.outfit(mob);
        outings.addMember(outing, mob.getUUID());
        return mob;
    }

    /** A builder for a pack's crew (#32's rules, not an outing's). */
    private Mob spawnGrunt(ServerLevel level, Pack pack, String entityType, BlockPos near, UUID owner, boolean preferAnchor) {
        BlockPos pos = findSpot(level, near, 1, 8);
        return pos == null ? null : spawnGruntAt(level, pack, entityType, pos, owner);
    }

    private Mob spawnGruntAt(ServerLevel level, Pack pack, String entityType, BlockPos pos, UUID owner) {
        EntityType<?> type = EntityType.byString(entityType).orElse(EntityType.ZOMBIE);
        if (!(type.create(level) instanceof Mob mob)) return null;
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
        return mob;
    }

    /** Right round a settlement anchor first, so Bosses and guards come out in halls, not on roofs; by the same rule. */
    private BlockPos anchorSpot(ServerLevel level, BlockPos anchor, int fallbackMin, int fallbackMax, Pack pack) {
        int[][] offsets = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}};
        for (int[] off : offsets) {
            int x = anchor.getX() + off[0];
            int z = anchor.getZ() + off[1];
            if (!level.hasChunk(x >> 4, z >> 4)) continue;
            for (int dy : new int[]{0, 1, -1, 2}) {
                BlockPos candidate = new BlockPos(x, anchor.getY() + dy, z);
                if (PatrolSpots.allowed(level, candidate, pack)) return candidate;
            }
        }
        return PatrolSpots.find(level, anchor, fallbackMin, fallbackMax, level.getRandom(), pack);
    }

    private BlockPos findSpot(ServerLevel level, BlockPos near, int min, int max) {
        return Spots.near(level, near, min, max, level.getRandom());
    }

    // ---------------------------------------------------------------- turning a mob into a Brute

    /** Makes a mob the living body of a Brute record: name, gear, stats, boss bar. */
    void becomeBrute(Mob mob, Brute b) {
        removeActive(b.id, true);
        campBuilder.leaveCrew(mob); // a builder that rises is a builder no more
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

    /** Tells each player with the mod which scarred Brutes are near them, so their client can paint the scars. */
    private void sendScars() {
        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (io.github.pirut.lib.Net.canSend(p, Screens.SCARS)) players.add(p);
        }
        if (players.isEmpty()) return;
        for (ServerPlayer p : players) {
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            for (Active a : active.values()) {
                Brute b = data.feud.brute(a.brute);
                if (b == null || b.scars.isEmpty() || a.dimension != p.level().dimension()) continue;
                if (!(p.serverLevel().getEntity(a.entity) instanceof Mob mob) || mob.distanceToSqr(p) > 96 * 96) continue;
                net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
                t.putInt("id", mob.getId());
                net.minecraft.nbt.ListTag keys = new net.minecraft.nbt.ListTag();
                for (Scar s : b.scars) keys.add(net.minecraft.nbt.StringTag.valueOf(s.key()));
                t.put("scars", keys);
                t.putBoolean("flip", (b.look & 1) == 1);
                list.add(t);
            }
            net.minecraft.nbt.CompoundTag msg = new net.minecraft.nbt.CompoundTag();
            msg.put("brutes", list);
            io.github.pirut.lib.Net.send(p, Screens.SCARS, msg);
        }
    }

    /** Ops only: a scar, as if the Brute had come back from that death. */
    void scar(Brute b, Scar scar) {
        if (!b.scars.contains(scar)) b.scars.add(scar);
        Active a = active.get(b.id);
        if (a != null) refresh(a, b);
        data.changed();
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
                armor.addPermanentModifier(new AttributeModifier(HIDE_MOD, Math.min(12, 2 + b.level * 0.5)
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
        else if (b.level >= 3 && pack != null) mob.setItemSlot(EquipmentSlot.CHEST, dyed(Items.LEATHER_CHESTPLATE, pack.color));
        if (b.level >= 12) {
            mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        }
        // Its own trim on its own armor: the pattern is the Brute's, the material is its pack's color.
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            trim(mob, mob.getItemBySlot(s), b, pack);
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
        wearTaken(mob, b);
        stats(mob, b, rank, fresh);
    }

    /** Mobs that show what they wear and hold: zombies, skeletons, illagers, piglins. */
    static boolean wearsGear(Mob mob) {
        return mob instanceof net.minecraft.world.entity.monster.Zombie || mob instanceof net.minecraft.world.entity.monster.AbstractSkeleton
                || mob instanceof net.minecraft.world.entity.monster.AbstractIllager || mob instanceof AbstractPiglin;
    }

    private void stats(Mob mob, Brute b, Rank rank, boolean fresh) {
        double boost = rank == Rank.KING ? 1.0 : rank == Rank.BOSS ? 0.5 : 0;
        modify(mob, Attributes.MAX_HEALTH, 0.22 * b.level + 0.5 + boost);
        modify(mob, Attributes.ATTACK_DAMAGE, 0.06 * b.level + boost * 0.3);
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
        // No two the same size: its own build, bigger as it climbs the ranks.
        double size = Math.min(1.35, b.size * (rank == Rank.KING ? 1.18 : rank == Rank.BOSS ? 1.08 : 1.0));
        setModifier(mob, Attributes.SCALE, SIZE_MOD, size - 1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        // What its scars did to it.
        boolean broken = b.scars.contains(Scar.BROKEN);
        setModifier(mob, Attributes.MOVEMENT_SPEED, SCAR_MOD, broken ? -0.12 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        setModifier(mob, Attributes.KNOCKBACK_RESISTANCE, SCAR_MOD, broken || b.scars.contains(Scar.BLASTED) ? 0.3 : 0,
                AttributeModifier.Operation.ADD_VALUE);
        if (b.scars.contains(Scar.DROWNED)) mob.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, MobEffectInstance.INFINITE_DURATION, 0, false, false));
        if (fresh) mob.setHealth(mob.getMaxHealth());
    }

    private static void setModifier(Mob mob, Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, ResourceLocation id,
                                    double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = mob.getAttribute(attribute);
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount != 0) inst.addPermanentModifier(new AttributeModifier(id, amount, op));
    }

    private static ItemStack dyed(Item item, int color) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponents.DYED_COLOR, new DyedItemColor(DyeColor.byId(color).getTextureDiffuseColor() & 0xFFFFFF, false));
        return s;
    }

    /** Puts the Brute's own trim on a piece of its armor (never on gear it took from a player). */
    private static void trim(Mob mob, ItemStack armor, Brute b, Pack pack) {
        if (!(armor.getItem() instanceof net.minecraft.world.item.ArmorItem)) return;
        var reg = mob.level().registryAccess();
        var patterns = reg.registryOrThrow(Registries.TRIM_PATTERN);
        List<net.minecraft.resources.ResourceKey<net.minecraft.world.item.armortrim.TrimPattern>> keys = new ArrayList<>(patterns.registryKeySet());
        if (keys.isEmpty()) return;
        keys.sort(java.util.Comparator.comparing(k -> k.location().toString()));
        var pattern = patterns.getHolder(keys.get(Math.floorMod(b.look, keys.size()))).orElse(null);
        // Leather is already dyed the pack's color, so its trim stands out against it instead.
        var materialKey = armor.is(net.minecraft.tags.ItemTags.DYEABLE) && pack != null ? contrast(pack.color) : trimMaterial(pack == null ? -1 : pack.color);
        var material = reg.registryOrThrow(Registries.TRIM_MATERIAL).getHolder(materialKey).orElse(null);
        if (pattern == null || material == null) return;
        armor.set(DataComponents.TRIM, new net.minecraft.world.item.armortrim.ArmorTrim(material, pattern, false));
    }

    private static net.minecraft.resources.ResourceKey<net.minecraft.world.item.armortrim.TrimMaterial> contrast(int color) {
        return switch (DyeColor.byId(color)) {
            case WHITE, YELLOW, LIME, LIGHT_BLUE, PINK, LIGHT_GRAY, ORANGE, CYAN, MAGENTA -> net.minecraft.world.item.armortrim.TrimMaterials.NETHERITE;
            default -> net.minecraft.world.item.armortrim.TrimMaterials.GOLD;
        };
    }

    private static net.minecraft.resources.ResourceKey<net.minecraft.world.item.armortrim.TrimMaterial> trimMaterial(int color) {
        if (color < 0) return net.minecraft.world.item.armortrim.TrimMaterials.IRON;
        return switch (DyeColor.byId(color)) {
            case RED -> net.minecraft.world.item.armortrim.TrimMaterials.REDSTONE;
            case ORANGE -> net.minecraft.world.item.armortrim.TrimMaterials.COPPER;
            case YELLOW -> net.minecraft.world.item.armortrim.TrimMaterials.GOLD;
            case LIME, GREEN -> net.minecraft.world.item.armortrim.TrimMaterials.EMERALD;
            case CYAN, LIGHT_BLUE -> net.minecraft.world.item.armortrim.TrimMaterials.DIAMOND;
            case BLUE -> net.minecraft.world.item.armortrim.TrimMaterials.LAPIS;
            case PURPLE, MAGENTA, PINK -> net.minecraft.world.item.armortrim.TrimMaterials.AMETHYST;
            case WHITE -> net.minecraft.world.item.armortrim.TrimMaterials.QUARTZ;
            case BLACK, BROWN -> net.minecraft.world.item.armortrim.TrimMaterials.NETHERITE;
            default -> net.minecraft.world.item.armortrim.TrimMaterials.IRON;
        };
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
        Ledger.firstMeeting(this, p);
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

    /**
     * The Brute that killed a player takes their best weapon and the armor they wore (not the helmet: its cap is its
     * colors), so the player has to fight it to get them back. Mobs that can't show gear take only the weapon.
     */
    private void takeGear(Mob mob, Brute b, ServerPlayer p) {
        if (!BloodFeud.config().takeGear || p.isCreative() || p.isSpectator()) return;
        if (p.serverLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) return;
        // Where the player's things are now: still in the inventory (NeoForge runs this before the drops), or
        // already on the ground around them (Fabric runs it after).
        record Slot(ItemStack stack, Runnable take) {
        }
        List<Slot> loose = new ArrayList<>();
        List<Slot> worn = new ArrayList<>();
        net.minecraft.world.entity.player.Inventory inv = p.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            int k = i;
            if (!inv.items.get(i).isEmpty()) loose.add(new Slot(inv.items.get(i), () -> inv.items.set(k, ItemStack.EMPTY)));
        }
        for (int i = 0; i < 3; i++) { // feet, legs, chest; the head stays with the player: a Brute's cap is its colors
            int k = i;
            if (!inv.armor.get(i).isEmpty()) worn.add(new Slot(inv.armor.get(i), () -> inv.armor.set(k, ItemStack.EMPTY)));
        }
        if (loose.isEmpty() && worn.isEmpty()) {
            for (net.minecraft.world.entity.item.ItemEntity e : p.level().getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class, p.getBoundingBox().inflate(4), ItemEntity -> true)) {
                if (e.tickCount > 2) continue;
                ItemStack st = e.getItem();
                Slot slot = new Slot(st, e::discard);
                if (st.getItem() instanceof net.minecraft.world.item.ArmorItem armor && armor.getEquipmentSlot() != EquipmentSlot.HEAD) worn.add(slot);
                else loose.add(slot);
            }
        }
        List<ItemStack> taken = new ArrayList<>();
        boolean archer = mob instanceof net.minecraft.world.entity.monster.AbstractSkeleton;
        Slot best = null;
        double bestScore = 0;
        for (Slot sl : loose) {
            double score = weaponScore(sl.stack(), archer);
            if (score > bestScore) {
                bestScore = score;
                best = sl;
            }
        }
        if (best != null) {
            taken.add(best.stack().copy());
            best.take().run();
        }
        if (wearsGear(mob)) {
            java.util.Set<EquipmentSlot> filled = new java.util.HashSet<>();
            for (Slot sl : worn) {
                if (!(sl.stack().getItem() instanceof net.minecraft.world.item.ArmorItem armor) || !filled.add(armor.getEquipmentSlot())) continue;
                taken.add(sl.stack().copy());
                sl.take().run();
            }
        }
        if (taken.isEmpty()) return;
        List<String> names = new ArrayList<>();
        for (ItemStack it : taken) {
            data.stolen.computeIfAbsent(b.id, k -> new ArrayList<>()).add(it);
            String name = it.getHoverName().getString();
            names.add(name);
            data.feud.stole(b.id, p.getUUID(), name.toLowerCase(Locale.ROOT), now());
        }
        p.sendSystemMessage(Component.literal(b.name + " took your " + String.join(", ", names)
                + ". Kill it to get them back.").withStyle(ChatFormatting.RED));
        data.changed();
    }

    /** How good an item is as a weapon for this mob; 0 if it isn't one. */
    private static double weaponScore(ItemStack s, boolean archer) {
        if (s.isEmpty()) return 0;
        Item it = s.getItem();
        if (it instanceof net.minecraft.world.item.BowItem || it instanceof net.minecraft.world.item.CrossbowItem) {
            return archer ? 100 + s.getEnchantments().size() : 0;
        }
        // Real weapons only: a pickaxe or a shovel stays on the ground for the player.
        if (!s.is(net.minecraft.tags.ItemTags.SWORDS) && !s.is(net.minecraft.tags.ItemTags.AXES)
                && !s.is(Items.TRIDENT) && !s.is(Items.MACE)) return 0;
        double dmg = 0;
        net.minecraft.world.item.component.ItemAttributeModifiers mods = s.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,
                net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);
        for (net.minecraft.world.item.component.ItemAttributeModifiers.Entry e : mods.modifiers()) {
            if (e.attribute().is(Attributes.ATTACK_DAMAGE) && e.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) {
                dmg += e.modifier().amount();
            }
        }
        if (dmg <= 0) return 0;
        return dmg + s.getEnchantments().size() * 0.5;
    }

    /** Puts on what a Brute took from players: armor in its slots, the best weapon in hand. Dropped when it dies. */
    private void wearTaken(Mob mob, Brute b) {
        List<ItemStack> items = data.stolen.get(b.id);
        if (items == null || items.isEmpty()) return;
        boolean archer = mob instanceof net.minecraft.world.entity.monster.AbstractSkeleton;
        ItemStack weapon = null;
        double best = 0;
        for (ItemStack it : items) {
            if (it.getItem() instanceof net.minecraft.world.item.ArmorItem armor) {
                EquipmentSlot slot = armor.getEquipmentSlot();
                if (slot == EquipmentSlot.HEAD) continue;
                mob.setItemSlot(slot, it.copy());
                mob.setDropChance(slot, 0); // it drops from the stolen list instead, once
            } else {
                double score = weaponScore(it, archer);
                if (score > best) {
                    best = score;
                    weapon = it;
                }
            }
        }
        if (weapon != null) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, weapon.copy());
            mob.setDropChance(EquipmentSlot.MAINHAND, 0);
        }
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

    /** Ops only: creates a pack ahead of the player, queues its intact or ruined settlement build, and populates its garrison. */
    public Pack spawnCommandCamp(ServerPlayer player, Family family, SettlementTier tier, boolean ruined,
                                 java.util.function.Consumer<Pack> onDone) {
        ServerLevel level = player.serverLevel();
        SettlementTier target = tier == null || tier == SettlementTier.NONE ? SettlementTier.CAMP : tier;
        SiteFinder.Site site = SiteFinder.findCommandSite(level, player, target);
        if (site == null) return null;
        BlockPos origin = site.origin();
        UUID playerId = player.getUUID();
        Feud f = data.feud;
        Brute boss = f.rise(family.defaultType, family, dim(level), origin.getX(), origin.getZ(),
                playerId, Memory.Kind.SAW_KILLS, null, null, now());
        Pack pack = f.form(boss, this::placeName, now(), ruined ? 0 : target.minGrunts);
        pack.siteSet = true;
        pack.siteX = origin.getX();
        pack.siteY = origin.getY();
        pack.siteZ = origin.getZ();
        pack.siteFacing = site.facing();
        pack.met.add(playerId);
        if (ruined) {
            f.ruinSettlement(pack, target, now());
        } else {
            f.upgradeSettlement(pack, target, now());
        }
        f.drainNews();
        data.changed();

        Runnable done = () -> {
            rested.remove(pack.id);
            ServerPlayer online = server.getPlayerList().getPlayer(playerId);
            spawnPack(level, pack, online != null && online.level() == level ? online : null);
            if (onDone != null) onDone.accept(pack);
        };
        if (ruined) {
            campBuilder.queueRuin(level, f, pack, target.level, false, gameTime(), done);
        } else {
            campBuilder.queueBuild(level, f, pack, 0, target.level, false, gameTime(), done);
        }
        return pack;
    }

    public record UpgradeResult(boolean ok, String error, Pack pack, SettlementTier target,
                                List<String> skippedZones, int skippedRingColumns) {
    }

    /** Ops only: upgrades (or repairs if ruined) the nearest pack within 256 blocks and queues in-place work. */
    public UpgradeResult upgradeNearestCamp(ServerPlayer player, java.util.function.Consumer<Pack> onDone) {
        ServerLevel level = player.serverLevel();
        String dim = dim(level);
        Feud f = data.feud;
        Pack nearest = nearestPack(f, dim, player.getX(), player.getZ(), 256.0);
        if (nearest == null) {
            return new UpgradeResult(false, "No pack within 256 blocks to upgrade.", null, null, List.of(), 0);
        }
        if (campBuilder.isBuilding(nearest.id)) {
            return new UpgradeResult(false, "That pack's settlement is already under construction.", nearest, null, List.of(), 0);
        }
        int currentBuilt = nearest.builtTier;
        boolean wasRuined = nearest.ruined || nearest.builtRuined;
        if (currentBuilt >= SettlementTier.TOWN.level && !wasRuined) {
            return new UpgradeResult(false, Feud.cap(nearest.title()) + " already runs a full Town.", nearest, SettlementTier.TOWN, List.of(), 0);
        }
        SettlementTier target;
        if (wasRuined) {
            target = currentBuilt > 0 ? SettlementTier.byLevel(currentBuilt)
                    : (nearest.tier == null || nearest.tier == SettlementTier.NONE ? SettlementTier.CAMP : nearest.tier);
        } else {
            target = SettlementTier.byLevel(Math.min(SettlementTier.TOWN.level, currentBuilt + 1));
        }
        boolean assignedSite = false;
        if (!nearest.siteSet) {
            SiteFinder.Site site = SiteFinder.findAutoSite(level, nearest, target);
            if (site == null) site = SiteFinder.findCommandSite(level, player, target);
            if (site == null) {
                return new UpgradeResult(false, "No dry, solid ground nearby to build " + Feud.cap(nearest.title()) + ".", nearest, target, List.of(), 0);
            }
            nearest.siteSet = true;
            nearest.siteX = site.origin().getX();
            nearest.siteY = site.origin().getY();
            nearest.siteZ = site.origin().getZ();
            nearest.siteFacing = site.facing();
            assignedSite = true;
        }

        UUID playerId = player.getUUID();
        Pack pack = nearest;
        Runnable done = () -> {
            rested.remove(pack.id);
            ServerPlayer online = server.getPlayerList().getPlayer(playerId);
            spawnPack(level, pack, online != null && online.level() == level ? online : null);
            if (onDone != null) onDone.accept(pack);
        };
        CampBuilder.QueueResult qr = (wasRuined && currentBuilt > 0)
                ? campBuilder.queueRepair(level, f, pack, target.level, false, gameTime(), done)
                : campBuilder.queueBuild(level, f, pack, currentBuilt, target.level, false, gameTime(), done);
        if (!qr.queued()) {
            boolean loading = isGroundLoading(level, pack, target);
            if (assignedSite) {
                nearest.siteSet = false;
                nearest.buildStarted = false;
                nearest.skippedZones.clear();
                nearest.skippedColumns.clear();
            }
            if (loading) return new UpgradeResult(false, GROUND_LOADING, pack, target, List.of(), 0);
            return new UpgradeResult(false, "Cannot upgrade " + Feud.cap(pack.title())
                    + ": expansion area is blocked by player builds or terrain.", pack, target,
                    qr.skippedZones(), qr.skippedRingColumns());
        }

        pack.met.add(playerId);
        f.upgradeSettlement(pack, target, now());
        f.drainNews();
        data.changed();
        return new UpgradeResult(true, "", pack, target, qr.skippedZones(), qr.skippedRingColumns());
    }

    /** Ops only: turns the nearest pack's settlement within 256 blocks into ruins in-place (or builds ruins if unbuilt). */
    public UpgradeResult ruinNearestCamp(ServerPlayer player, java.util.function.Consumer<Pack> onDone) {
        ServerLevel level = player.serverLevel();
        String dim = dim(level);
        Feud f = data.feud;
        Pack nearest = nearestPack(f, dim, player.getX(), player.getZ(), 256.0);
        if (nearest == null) {
            return new UpgradeResult(false, "No pack within 256 blocks to ruin.", null, null, List.of(), 0);
        }
        if (campBuilder.isBuilding(nearest.id)) {
            return new UpgradeResult(false, "That pack's settlement is already under construction.", nearest, null, List.of(), 0);
        }
        if (nearest.ruined && nearest.builtRuined && nearest.builtTier > 0) {
            SettlementTier t = SettlementTier.byLevel(nearest.builtTier);
            return new UpgradeResult(false, Feud.cap(nearest.title()) + "'s " + t.key + " is already in ruins.", nearest, t, List.of(), 0);
        }
        SettlementTier target = nearest.builtTier > 0 ? SettlementTier.byLevel(nearest.builtTier)
                : (nearest.tier == null || nearest.tier == SettlementTier.NONE ? SettlementTier.CAMP : nearest.tier);
        boolean assignedSite = false;
        if (!nearest.siteSet) {
            SiteFinder.Site site = SiteFinder.findAutoSite(level, nearest, target);
            if (site == null) site = SiteFinder.findCommandSite(level, player, target);
            if (site == null) {
                return new UpgradeResult(false, "No dry, solid ground nearby to ruin " + Feud.cap(nearest.title()) + ".", nearest, target, List.of(), 0);
            }
            nearest.siteSet = true;
            nearest.siteX = site.origin().getX();
            nearest.siteY = site.origin().getY();
            nearest.siteZ = site.origin().getZ();
            nearest.siteFacing = site.facing();
            assignedSite = true;
        }

        UUID playerId = player.getUUID();
        Pack pack = nearest;
        CampBuilder.QueueResult qr = campBuilder.queueRuin(level, f, pack, target.level, List.of(), false, gameTime(), () -> {
            flushDirtyBanners(false);
            if (onDone != null) onDone.accept(pack);
        });
        if (!qr.queued()) {
            boolean loading = isGroundLoading(level, pack, target);
            if (assignedSite) {
                nearest.siteSet = false;
                nearest.buildStarted = false;
                nearest.skippedZones.clear();
                nearest.skippedColumns.clear();
            }
            if (loading) return new UpgradeResult(false, GROUND_LOADING, pack, target, List.of(), 0);
            return new UpgradeResult(false, "Cannot ruin " + Feud.cap(pack.title())
                    + ": area is blocked or nothing to ruin.", pack, target,
                    qr.skippedZones(), qr.skippedRingColumns());
        }

        pack.met.add(playerId);
        f.ruinSettlement(pack, target, now());
        f.drainNews();
        flushDirtyBanners(false);
        data.changed();
        return new UpgradeResult(true, "", pack, target, qr.skippedZones(), qr.skippedRingColumns());
    }

    private static final String GROUND_LOADING = "The ground there is still loading. Move closer or try again in a moment.";

    private static boolean isGroundLoading(ServerLevel level, Pack pack, SettlementTier target) {
        int radius = SettlementTier.byLevel(Math.max(pack.builtTier, target.level)).radius;
        return SiteFinder.isWaitingForGround(level, new BlockPos(pack.siteX, pack.siteY, pack.siteZ), radius);
    }

    private static Pack nearestPack(Feud f, String dim, double x, double z, double maxDist) {
        Pack nearest = null;
        double best = maxDist;
        for (Pack pk : f.packs.values()) {
            double d = pk.distanceTo(dim, x, z);
            if (d <= best) {
                nearest = pk;
                best = d;
            }
        }
        return nearest;
    }

    public void forgetAll() {
        for (UUID id : new ArrayList<>(active.keySet())) removeActive(id, true);
        campBuilder.clearAll(server);
        data.feud.brutes.clear();
        data.feud.packs.clear();
        data.feud.abandonedSites.clear();
        data.stolen.clear();
        settler.clear();
        outings.clear();
        rises.clear();
        data.changed();
    }
}
