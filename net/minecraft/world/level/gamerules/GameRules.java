package net.minecraft.world.level.gamerules;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import java.util.Objects;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import org.jspecify.annotations.Nullable;

public class GameRules {
   public static final GameRule<Boolean> ADVANCE_TIME = registerBoolean("advance_time", GameRuleCategory.UPDATES, !SharedConstants.DEBUG_WORLD_RECREATE);
   public static final GameRule<Boolean> ADVANCE_WEATHER = registerBoolean("advance_weather", GameRuleCategory.UPDATES, !SharedConstants.DEBUG_WORLD_RECREATE);
   public static final GameRule<Boolean> ALLOW_ENTERING_NETHER_USING_PORTALS = registerBoolean(
      "allow_entering_nether_using_portals", GameRuleCategory.MISC, true
   );
   public static final GameRule<Boolean> BLOCK_DROPS = registerBoolean("block_drops", GameRuleCategory.DROPS, true);
   public static final GameRule<Boolean> BLOCK_EXPLOSION_DROP_DECAY = registerBoolean("block_explosion_drop_decay", GameRuleCategory.DROPS, true);
   public static final GameRule<Boolean> COMMAND_BLOCKS_WORK = registerBoolean("command_blocks_work", GameRuleCategory.MISC, true);
   public static final GameRule<Boolean> COMMAND_BLOCK_OUTPUT = registerBoolean("command_block_output", GameRuleCategory.CHAT, true);
   public static final GameRule<Boolean> DROWNING_DAMAGE = registerBoolean("drowning_damage", GameRuleCategory.PLAYER, true);
   public static final GameRule<Boolean> ELYTRA_MOVEMENT_CHECK = registerBoolean("elytra_movement_check", GameRuleCategory.PLAYER, true);
   public static final GameRule<Boolean> ENDER_PEARLS_VANISH_ON_DEATH = registerBoolean("ender_pearls_vanish_on_death", GameRuleCategory.PLAYER, true);
   public static final GameRule<Boolean> ENTITY_DROPS = registerBoolean("entity_drops", GameRuleCategory.DROPS, true);
   public static final GameRule<Boolean> FALL_DAMAGE = registerBoolean("fall_damage", GameRuleCategory.PLAYER, true);
   public static final GameRule<Boolean> FIRE_DAMAGE = registerBoolean("fire_damage", GameRuleCategory.PLAYER, true);
   public static final GameRule<Integer> FIRE_SPREAD_RADIUS_AROUND_PLAYER = registerInteger(
      "fire_spread_radius_around_player", GameRuleCategory.UPDATES, 128, -1
   );
   public static final GameRule<Boolean> FORGIVE_DEAD_PLAYERS = registerBoolean("forgive_dead_players", GameRuleCategory.MOBS, true);
   public static final GameRule<Boolean> FREEZE_DAMAGE = registerBoolean("freeze_damage", GameRuleCategory.PLAYER, true);
   public static final GameRule<Boolean> GLOBAL_SOUND_EVENTS = registerBoolean("global_sound_events", GameRuleCategory.MISC, true);
   public static final GameRule<Boolean> IMMEDIATE_RESPAWN = registerBoolean("immediate_respawn", GameRuleCategory.PLAYER, false);
   public static final GameRule<Boolean> KEEP_INVENTORY = registerBoolean("keep_inventory", GameRuleCategory.PLAYER, false);
   public static final GameRule<Boolean> LAVA_SOURCE_CONVERSION = registerBoolean("lava_source_conversion", GameRuleCategory.UPDATES, false);
   public static final GameRule<Boolean> LIMITED_CRAFTING = registerBoolean("limited_crafting", GameRuleCategory.PLAYER, false);
   public static final GameRule<Boolean> LOCATOR_BAR = registerBoolean("locator_bar", GameRuleCategory.PLAYER, true);
   public static final GameRule<Boolean> LOG_ADMIN_COMMANDS = registerBoolean("log_admin_commands", GameRuleCategory.CHAT, true);
   public static final GameRule<Integer> MAX_BLOCK_MODIFICATIONS = registerInteger("max_block_modifications", GameRuleCategory.MISC, 32768, 1);
   public static final GameRule<Integer> MAX_COMMAND_FORKS = registerInteger("max_command_forks", GameRuleCategory.MISC, 65536, 0);
   public static final GameRule<Integer> MAX_COMMAND_SEQUENCE_LENGTH = registerInteger("max_command_sequence_length", GameRuleCategory.MISC, 65536, 0);
   public static final GameRule<Integer> MAX_ENTITY_CRAMMING = registerInteger("max_entity_cramming", GameRuleCategory.MOBS, 24, 0);
   public static final GameRule<Integer> MAX_MINECART_SPEED = registerInteger(
      "max_minecart_speed", GameRuleCategory.MISC, 8, 1, 1000, FeatureFlagSet.of(FeatureFlags.MINECART_IMPROVEMENTS)
   );
   public static final GameRule<Integer> MAX_SNOW_ACCUMULATION_HEIGHT = registerInteger("max_snow_accumulation_height", GameRuleCategory.UPDATES, 1, 0, 8);
   public static final GameRule<Boolean> MOB_DROPS = registerBoolean("mob_drops", GameRuleCategory.DROPS, true);
   public static final GameRule<Boolean> MOB_EXPLOSION_DROP_DECAY = registerBoolean("mob_explosion_drop_decay", GameRuleCategory.DROPS, true);
   public static final GameRule<Boolean> MOB_GRIEFING = registerBoolean("mob_griefing", GameRuleCategory.MOBS, true);
   public static final GameRule<Boolean> NATURAL_HEALTH_REGENERATION = registerBoolean("natural_health_regeneration", GameRuleCategory.PLAYER, true);
   public static final GameRule<Boolean> PLAYER_MOVEMENT_CHECK = registerBoolean("player_movement_check", GameRuleCategory.PLAYER, true);
   public static final GameRule<Integer> PLAYERS_NETHER_PORTAL_CREATIVE_DELAY = registerInteger(
      "players_nether_portal_creative_delay", GameRuleCategory.PLAYER, 0, 0
   );
   public static final GameRule<Integer> PLAYERS_NETHER_PORTAL_DEFAULT_DELAY = registerInteger(
      "players_nether_portal_default_delay", GameRuleCategory.PLAYER, 80, 0
   );
   public static final GameRule<Integer> PLAYERS_SLEEPING_PERCENTAGE = registerInteger("players_sleeping_percentage", GameRuleCategory.PLAYER, 100, 0);
   public static final GameRule<Boolean> PROJECTILES_CAN_BREAK_BLOCKS = registerBoolean("projectiles_can_break_blocks", GameRuleCategory.DROPS, true);
   public static final GameRule<Boolean> PVP = registerBoolean("pvp", GameRuleCategory.PLAYER, true);
   public static final GameRule<Boolean> RAIDS = registerBoolean("raids", GameRuleCategory.MOBS, true);
   public static final GameRule<Integer> RANDOM_TICK_SPEED = registerInteger("random_tick_speed", GameRuleCategory.UPDATES, 3, 0);
   public static final GameRule<Boolean> REDUCED_DEBUG_INFO = registerBoolean("reduced_debug_info", GameRuleCategory.MISC, false);
   public static final GameRule<Integer> RESPAWN_RADIUS = registerInteger("respawn_radius", GameRuleCategory.PLAYER, 10, 0);
   public static final GameRule<Boolean> SEND_COMMAND_FEEDBACK = registerBoolean("send_command_feedback", GameRuleCategory.CHAT, true);
   public static final GameRule<Boolean> SHOW_ADVANCEMENT_MESSAGES = registerBoolean("show_advancement_messages", GameRuleCategory.CHAT, true);
   public static final GameRule<Boolean> SHOW_DEATH_MESSAGES = registerBoolean("show_death_messages", GameRuleCategory.CHAT, true);
   public static final GameRule<Boolean> SPAWNER_BLOCKS_WORK = registerBoolean("spawner_blocks_work", GameRuleCategory.MISC, true);
   public static final GameRule<Boolean> SPAWN_MOBS = registerBoolean("spawn_mobs", GameRuleCategory.SPAWNING, true);
   public static final GameRule<Boolean> SPAWN_MONSTERS = registerBoolean("spawn_monsters", GameRuleCategory.SPAWNING, true);
   public static final GameRule<Boolean> SPAWN_PATROLS = registerBoolean("spawn_patrols", GameRuleCategory.SPAWNING, true);
   public static final GameRule<Boolean> SPAWN_PHANTOMS = registerBoolean("spawn_phantoms", GameRuleCategory.SPAWNING, true);
   public static final GameRule<Boolean> SPAWN_WANDERING_TRADERS = registerBoolean("spawn_wandering_traders", GameRuleCategory.SPAWNING, true);
   public static final GameRule<Boolean> SPAWN_WARDENS = registerBoolean("spawn_wardens", GameRuleCategory.SPAWNING, true);
   public static final GameRule<Boolean> SPECTATORS_GENERATE_CHUNKS = registerBoolean("spectators_generate_chunks", GameRuleCategory.PLAYER, true);
   public static final GameRule<Boolean> SPREAD_VINES = registerBoolean("spread_vines", GameRuleCategory.UPDATES, true);
   public static final GameRule<Boolean> TNT_EXPLODES = registerBoolean("tnt_explodes", GameRuleCategory.MISC, true);
   public static final GameRule<Boolean> TNT_EXPLOSION_DROP_DECAY = registerBoolean("tnt_explosion_drop_decay", GameRuleCategory.DROPS, false);
   public static final GameRule<Boolean> UNIVERSAL_ANGER = registerBoolean("universal_anger", GameRuleCategory.MOBS, false);
   public static final GameRule<Boolean> WATER_SOURCE_CONVERSION = registerBoolean("water_source_conversion", GameRuleCategory.UPDATES, true);
   private final GameRuleMap rules;

   public static Codec<GameRules> codec(FeatureFlagSet p_460710_) {
      return GameRuleMap.CODEC.xmap(p_460996_ -> new GameRules(p_460710_, p_460996_), p_460793_ -> p_460793_.rules);
   }

   public GameRules(FeatureFlagSet p_461011_, GameRuleMap p_460727_) {
      this(p_461011_);
      this.rules.setFromIf(p_460727_, this.rules::has);
   }

   public GameRules(FeatureFlagSet p_460956_) {
      this.rules = GameRuleMap.of(BuiltInRegistries.GAME_RULE.filterFeatures(p_460956_).listElements().map(Holder::value));
   }

   public Stream<GameRule<?>> availableRules() {
      return this.rules.keySet().stream();
   }

   public <T> T get(GameRule<T> p_460962_) {
      T t = this.rules.get(p_460962_);
      if (t == null) {
         throw new IllegalArgumentException("Tried to access invalid game rule");
      } else {
         return t;
      }
   }

   public <T> void set(GameRule<T> p_460634_, T p_461218_, @Nullable MinecraftServer p_461092_) {
      if (!this.rules.has(p_460634_)) {
         throw new IllegalArgumentException("Tried to set invalid game rule");
      } else {
         this.rules.set(p_460634_, p_461218_);
         if (p_461092_ != null) {
            p_461092_.onGameRuleChanged(p_460634_, p_461218_);
         }
      }
   }

   public GameRules copy(FeatureFlagSet p_460717_) {
      return new GameRules(p_460717_, this.rules);
   }

   public void setAll(GameRules p_461100_, @Nullable MinecraftServer p_460689_) {
      this.setAll(p_461100_.rules, p_460689_);
   }

   public void setAll(GameRuleMap p_460853_, @Nullable MinecraftServer p_460800_) {
      p_460853_.keySet().forEach(p_461153_ -> this.setFromOther(p_460853_, (GameRule<?>)p_461153_, p_460800_));
   }

   private <T> void setFromOther(GameRuleMap p_460767_, GameRule<T> p_461277_, @Nullable MinecraftServer p_460681_) {
      this.set(p_461277_, Objects.requireNonNull(p_460767_.get(p_461277_)), p_460681_);
   }

   public void visitGameRuleTypes(GameRuleTypeVisitor p_460737_) {
      this.rules.keySet().forEach(p_460686_ -> {
         p_460737_.visit((GameRule<?>)p_460686_);
         p_460686_.callVisitor(p_460737_);
      });
   }

   public static GameRule<Boolean> registerBoolean(String p_460984_, GameRuleCategory p_461188_, boolean p_460844_) {
      return register(
         p_460984_,
         p_461188_,
         GameRuleType.BOOL,
         BoolArgumentType.bool(),
         Codec.BOOL,
         p_460844_,
         FeatureFlagSet.of(),
         GameRuleTypeVisitor::visitBoolean,
         p_460985_ -> p_460985_ ? 1 : 0
      );
   }

   public static GameRule<Integer> registerInteger(String p_460928_, GameRuleCategory p_461055_, int p_460698_, int p_461037_) {
      return registerInteger(p_460928_, p_461055_, p_460698_, p_461037_, Integer.MAX_VALUE, FeatureFlagSet.of());
   }

   public static GameRule<Integer> registerInteger(String p_461050_, GameRuleCategory p_460633_, int p_460640_, int p_460797_, int p_460881_) {
      return registerInteger(p_461050_, p_460633_, p_460640_, p_460797_, p_460881_, FeatureFlagSet.of());
   }

   public static GameRule<Integer> registerInteger(
      String p_460894_, GameRuleCategory p_461194_, int p_460937_, int p_461085_, int p_461030_, FeatureFlagSet p_460990_
   ) {
      return register(
         p_460894_,
         p_461194_,
         GameRuleType.INT,
         IntegerArgumentType.integer(p_461085_, p_461030_),
         Codec.intRange(p_461085_, p_461030_),
         p_460937_,
         p_460990_,
         GameRuleTypeVisitor::visitInteger,
         p_461178_ -> p_461178_
      );
   }

   public static <T> GameRule<T> register(
      String p_461189_,
      GameRuleCategory p_460642_,
      GameRuleType p_460651_,
      ArgumentType<T> p_461259_,
      Codec<T> p_461238_,
      T p_460991_,
      FeatureFlagSet p_460959_,
      GameRules.VisitorCaller<T> p_460922_,
      ToIntFunction<T> p_461000_
   ) {
      return Registry.register(
         BuiltInRegistries.GAME_RULE, p_461189_, new GameRule<>(p_460642_, p_460651_, p_461259_, p_460922_, p_461238_, p_461000_, p_460991_, p_460959_)
      );
   }

   public static GameRule<?> bootstrap(Registry<GameRule<?>> p_460779_) {
      return ADVANCE_TIME;
   }

   public <T> String getAsString(GameRule<T> p_460667_) {
      return p_460667_.serialize(this.get(p_460667_));
   }

   public interface VisitorCaller<T> {
      void call(GameRuleTypeVisitor var1, GameRule<T> var2);
   }
}
