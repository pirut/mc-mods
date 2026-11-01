package net.minecraft.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.advancements.AdvancementVisibilityEvaluator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.FileUtil;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class PlayerAdvancements {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private final PlayerList playerList;
   private final Path playerSavePath;
   private AdvancementTree tree;
   private final Map<AdvancementHolder, AdvancementProgress> progress = new LinkedHashMap<>();
   private final Set<AdvancementHolder> visible = new HashSet<>();
   private final Set<AdvancementHolder> progressChanged = new HashSet<>();
   private final Set<AdvancementNode> rootsToUpdate = new HashSet<>();
   private ServerPlayer player;
   @Nullable
   private AdvancementHolder lastSelectedTab;
   private boolean isFirstPacket = true;
   private final Codec<PlayerAdvancements.Data> codec;

   public PlayerAdvancements(DataFixer dataFixer, PlayerList playerList, ServerAdvancementManager manager, Path playerSavePath, ServerPlayer player) {
      this.playerList = playerList;
      this.playerSavePath = playerSavePath;
      this.player = player;
      this.tree = manager.tree();
      int i = 1343;
      this.codec = DataFixTypes.ADVANCEMENTS.wrapCodec(PlayerAdvancements.Data.CODEC, dataFixer, 1343);
      this.load(manager);
   }

   public void setPlayer(ServerPlayer player) {
      this.player = player;
   }

   public void stopListening() {
      for (CriterionTrigger<?> criterionTrigger : BuiltInRegistries.TRIGGER_TYPES) {
         criterionTrigger.removePlayerListeners(this);
      }
   }

   public void reload(ServerAdvancementManager manager) {
      this.stopListening();
      this.progress.clear();
      this.visible.clear();
      this.rootsToUpdate.clear();
      this.progressChanged.clear();
      this.isFirstPacket = true;
      this.lastSelectedTab = null;
      this.tree = manager.tree();
      this.load(manager);
   }

   private void registerListeners(ServerAdvancementManager manager) {
      for (AdvancementHolder advancementHolder : manager.getAllAdvancements()) {
         this.registerListeners(advancementHolder);
      }
   }

   private void checkForAutomaticTriggers(ServerAdvancementManager manager) {
      for (AdvancementHolder advancementHolder : manager.getAllAdvancements()) {
         Advancement advancement = advancementHolder.value();
         if (advancement.criteria().isEmpty()) {
            this.award(advancementHolder, "");
            advancement.rewards().grant(this.player);
         }
      }
   }

   private void load(ServerAdvancementManager manager) {
      if (Files.isRegularFile(this.playerSavePath)) {
         try (Reader reader = Files.newBufferedReader(this.playerSavePath, StandardCharsets.UTF_8)) {
            JsonElement jsonElement = StrictJsonParser.parse(reader);
            PlayerAdvancements.Data data = (PlayerAdvancements.Data)this.codec.parse(JsonOps.INSTANCE, jsonElement).getOrThrow(JsonParseException::new);
            this.applyFrom(manager, data);
         } catch (JsonIOException | IOException var7) {
            LOGGER.error("Couldn't access player advancements in {}", this.playerSavePath, var7);
         } catch (JsonParseException var8) {
            LOGGER.error("Couldn't parse player advancements in {}", this.playerSavePath, var8);
         }
      }

      this.checkForAutomaticTriggers(manager);
      this.registerListeners(manager);
   }

   public void save() {
      JsonElement jsonElement = (JsonElement)this.codec.encodeStart(JsonOps.INSTANCE, this.asData()).getOrThrow();

      try {
         FileUtil.createDirectoriesSafe(this.playerSavePath.getParent());

         try (Writer writer = Files.newBufferedWriter(this.playerSavePath, StandardCharsets.UTF_8)) {
            GSON.toJson(jsonElement, GSON.newJsonWriter(writer));
         }
      } catch (JsonIOException | IOException var7) {
         LOGGER.error("Couldn't save player advancements to {}", this.playerSavePath, var7);
      }
   }

   private void applyFrom(ServerAdvancementManager advancementManager, PlayerAdvancements.Data data) {
      data.forEach((identifier, advancementProgress) -> {
         AdvancementHolder advancementHolder = advancementManager.get(identifier);
         if (advancementHolder == null) {
            LOGGER.warn("Ignored advancement '{}' in progress file {} - it doesn't exist anymore?", identifier, this.playerSavePath);
         } else {
            this.startProgress(advancementHolder, advancementProgress);
            this.progressChanged.add(advancementHolder);
            this.markForVisibilityUpdate(advancementHolder);
         }
      });
   }

   private PlayerAdvancements.Data asData() {
      Map<Identifier, AdvancementProgress> map = new LinkedHashMap<>();
      this.progress.forEach((advancementHolder, advancementProgress) -> {
         if (advancementProgress.hasProgress()) {
            map.put(advancementHolder.id(), advancementProgress);
         }
      });
      return new PlayerAdvancements.Data(map);
   }

   public boolean award(AdvancementHolder advancement, String criterionKey) {
      boolean bl = false;
      AdvancementProgress advancementProgress = this.getOrStartProgress(advancement);
      boolean bl2 = advancementProgress.isDone();
      if (advancementProgress.grantProgress(criterionKey)) {
         this.unregisterListeners(advancement);
         this.progressChanged.add(advancement);
         bl = true;
         if (!bl2 && advancementProgress.isDone()) {
            advancement.value().rewards().grant(this.player);
            advancement.value().display().ifPresent(displayInfo -> {
               if (displayInfo.shouldAnnounceChat() && this.player.level().getGameRules().get(GameRules.SHOW_ADVANCEMENT_MESSAGES)) {
                  this.playerList.broadcastSystemMessage(displayInfo.getType().createAnnouncement(advancement, this.player), false);
               }
            });
         }
      }

      if (!bl2 && advancementProgress.isDone()) {
         this.markForVisibilityUpdate(advancement);
      }

      return bl;
   }

   public boolean revoke(AdvancementHolder advancement, String criterionKey) {
      boolean bl = false;
      AdvancementProgress advancementProgress = this.getOrStartProgress(advancement);
      boolean bl2 = advancementProgress.isDone();
      if (advancementProgress.revokeProgress(criterionKey)) {
         this.registerListeners(advancement);
         this.progressChanged.add(advancement);
         bl = true;
      }

      if (bl2 && !advancementProgress.isDone()) {
         this.markForVisibilityUpdate(advancement);
      }

      return bl;
   }

   private void markForVisibilityUpdate(AdvancementHolder advancement) {
      AdvancementNode advancementNode = this.tree.get(advancement);
      if (advancementNode != null) {
         this.rootsToUpdate.add(advancementNode.root());
      }
   }

   private void registerListeners(AdvancementHolder advancement) {
      AdvancementProgress advancementProgress = this.getOrStartProgress(advancement);
      if (!advancementProgress.isDone()) {
         for (Entry<String, Criterion<?>> entry : advancement.value().criteria().entrySet()) {
            CriterionProgress criterionProgress = advancementProgress.getCriterion(entry.getKey());
            if (criterionProgress != null && !criterionProgress.isDone()) {
               this.registerListener(advancement, entry.getKey(), entry.getValue());
            }
         }
      }
   }

   private <T extends CriterionTriggerInstance> void registerListener(AdvancementHolder advancement, String criterionKey, Criterion<T> criterion) {
      criterion.trigger().addPlayerListener(this, new CriterionTrigger.Listener<>(criterion.triggerInstance(), advancement, criterionKey));
   }

   private void unregisterListeners(AdvancementHolder advancement) {
      AdvancementProgress advancementProgress = this.getOrStartProgress(advancement);

      for (Entry<String, Criterion<?>> entry : advancement.value().criteria().entrySet()) {
         CriterionProgress criterionProgress = advancementProgress.getCriterion(entry.getKey());
         if (criterionProgress != null && (criterionProgress.isDone() || advancementProgress.isDone())) {
            this.removeListener(advancement, entry.getKey(), entry.getValue());
         }
      }
   }

   private <T extends CriterionTriggerInstance> void removeListener(AdvancementHolder advancement, String criterionKey, Criterion<T> criterion) {
      criterion.trigger().removePlayerListener(this, new CriterionTrigger.Listener<>(criterion.triggerInstance(), advancement, criterionKey));
   }

   public void flushDirty(ServerPlayer player, boolean showAdvancements) {
      if (this.isFirstPacket || !this.rootsToUpdate.isEmpty() || !this.progressChanged.isEmpty()) {
         Map<Identifier, AdvancementProgress> map = new HashMap<>();
         Set<AdvancementHolder> set = new HashSet<>();
         Set<Identifier> set2 = new HashSet<>();

         for (AdvancementNode advancementNode : this.rootsToUpdate) {
            this.updateTreeVisibility(advancementNode, set, set2);
         }

         this.rootsToUpdate.clear();

         for (AdvancementHolder advancementHolder : this.progressChanged) {
            if (this.visible.contains(advancementHolder)) {
               map.put(advancementHolder.id(), this.progress.get(advancementHolder));
            }
         }

         this.progressChanged.clear();
         if (!map.isEmpty() || !set.isEmpty() || !set2.isEmpty()) {
            player.connection.send(new ClientboundUpdateAdvancementsPacket(this.isFirstPacket, set, set2, map, showAdvancements));
         }
      }

      this.isFirstPacket = false;
   }

   public void setSelectedTab(@Nullable AdvancementHolder advancement) {
      AdvancementHolder advancementHolder = this.lastSelectedTab;
      if (advancement != null && advancement.value().isRoot() && advancement.value().display().isPresent()) {
         this.lastSelectedTab = advancement;
      } else {
         this.lastSelectedTab = null;
      }

      if (advancementHolder != this.lastSelectedTab) {
         this.player.connection.send(new ClientboundSelectAdvancementsTabPacket(this.lastSelectedTab == null ? null : this.lastSelectedTab.id()));
      }
   }

   public AdvancementProgress getOrStartProgress(AdvancementHolder advancement) {
      AdvancementProgress advancementProgress = this.progress.get(advancement);
      if (advancementProgress == null) {
         advancementProgress = new AdvancementProgress();
         this.startProgress(advancement, advancementProgress);
      }

      return advancementProgress;
   }

   private void startProgress(AdvancementHolder advancement, AdvancementProgress advancementProgress) {
      advancementProgress.update(advancement.value().requirements());
      this.progress.put(advancement, advancementProgress);
   }

   private void updateTreeVisibility(AdvancementNode root, Set<AdvancementHolder> advancementOutput, Set<Identifier> idOutput) {
      AdvancementVisibilityEvaluator.evaluateVisibility(
         root, advancementNode -> this.getOrStartProgress(advancementNode.holder()).isDone(), (advancementNode, bl) -> {
            AdvancementHolder advancementHolder = advancementNode.holder();
            if (bl) {
               if (this.visible.add(advancementHolder)) {
                  advancementOutput.add(advancementHolder);
                  if (this.progress.containsKey(advancementHolder)) {
                     this.progressChanged.add(advancementHolder);
                  }
               }
            } else if (this.visible.remove(advancementHolder)) {
               idOutput.add(advancementHolder.id());
            }
         }
      );
   }

   record Data(Map<Identifier, AdvancementProgress> map) {
      public static final Codec<PlayerAdvancements.Data> CODEC = Codec.unboundedMap(Identifier.CODEC, AdvancementProgress.CODEC)
         .xmap(PlayerAdvancements.Data::new, PlayerAdvancements.Data::map);

      public void forEach(BiConsumer<Identifier, AdvancementProgress> action) {
         this.map.entrySet().stream().sorted(Entry.comparingByValue()).forEach(entry -> action.accept(entry.getKey(), entry.getValue()));
      }
   }
}
