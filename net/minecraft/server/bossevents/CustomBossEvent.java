package net.minecraft.server.bossevents;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;

public class CustomBossEvent extends ServerBossEvent {
   private final ResourceLocation id;
   private final Set<UUID> players = Sets.newHashSet();
   private int value;
   private int max = 100;

   public CustomBossEvent(ResourceLocation p_136261_, Component p_136262_) {
      super(p_136262_, BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
      this.id = p_136261_;
      this.setProgress(0.0F);
   }

   public ResourceLocation getTextId() {
      return this.id;
   }

   @Override
   public void addPlayer(ServerPlayer p_136267_) {
      super.addPlayer(p_136267_);
      this.players.add(p_136267_.getUUID());
   }

   public void addOfflinePlayer(UUID p_136271_) {
      this.players.add(p_136271_);
   }

   @Override
   public void removePlayer(ServerPlayer p_136281_) {
      super.removePlayer(p_136281_);
      this.players.remove(p_136281_.getUUID());
   }

   @Override
   public void removeAllPlayers() {
      super.removeAllPlayers();
      this.players.clear();
   }

   public int getValue() {
      return this.value;
   }

   public int getMax() {
      return this.max;
   }

   public void setValue(int p_136265_) {
      this.value = p_136265_;
      this.setProgress(Mth.clamp((float)p_136265_ / this.max, 0.0F, 1.0F));
   }

   public void setMax(int p_136279_) {
      this.max = p_136279_;
      this.setProgress(Mth.clamp((float)this.value / p_136279_, 0.0F, 1.0F));
   }

   public final Component getDisplayName() {
      return ComponentUtils.wrapInSquareBrackets(this.getName())
         .withStyle(
            p_329881_ -> p_329881_.withColor(this.getColor().getFormatting())
               .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(this.getTextId().toString())))
               .withInsertion(this.getTextId().toString())
         );
   }

   public boolean setPlayers(Collection<ServerPlayer> p_136269_) {
      Set<UUID> $$1 = Sets.newHashSet();
      Set<ServerPlayer> $$2 = Sets.newHashSet();

      for (UUID $$3 : this.players) {
         boolean $$4 = false;

         for (ServerPlayer $$5 : p_136269_) {
            if ($$5.getUUID().equals($$3)) {
               $$4 = true;
               break;
            }
         }

         if (!$$4) {
            $$1.add($$3);
         }
      }

      for (ServerPlayer $$6 : p_136269_) {
         boolean $$7 = false;

         for (UUID $$8 : this.players) {
            if ($$6.getUUID().equals($$8)) {
               $$7 = true;
               break;
            }
         }

         if (!$$7) {
            $$2.add($$6);
         }
      }

      for (UUID $$9 : $$1) {
         for (ServerPlayer $$10 : this.getPlayers()) {
            if ($$10.getUUID().equals($$9)) {
               this.removePlayer($$10);
               break;
            }
         }

         this.players.remove($$9);
      }

      for (ServerPlayer $$11 : $$2) {
         this.addPlayer($$11);
      }

      return !$$1.isEmpty() || !$$2.isEmpty();
   }

   public CompoundTag save(HolderLookup.Provider p_330839_) {
      CompoundTag $$1 = new CompoundTag();
      $$1.putString("Name", Component.Serializer.toJson(this.name, p_330839_));
      $$1.putBoolean("Visible", this.isVisible());
      $$1.putInt("Value", this.value);
      $$1.putInt("Max", this.max);
      $$1.putString("Color", this.getColor().getName());
      $$1.putString("Overlay", this.getOverlay().getName());
      $$1.putBoolean("DarkenScreen", this.shouldDarkenScreen());
      $$1.putBoolean("PlayBossMusic", this.shouldPlayBossMusic());
      $$1.putBoolean("CreateWorldFog", this.shouldCreateWorldFog());
      ListTag $$2 = new ListTag();

      for (UUID $$3 : this.players) {
         $$2.add(NbtUtils.createUUID($$3));
      }

      $$1.put("Players", $$2);
      return $$1;
   }

   public static CustomBossEvent load(CompoundTag p_136273_, ResourceLocation p_136274_, HolderLookup.Provider p_331359_) {
      CustomBossEvent $$3 = new CustomBossEvent(p_136274_, Component.Serializer.fromJson(p_136273_.getString("Name"), p_331359_));
      $$3.setVisible(p_136273_.getBoolean("Visible"));
      $$3.setValue(p_136273_.getInt("Value"));
      $$3.setMax(p_136273_.getInt("Max"));
      $$3.setColor(BossEvent.BossBarColor.byName(p_136273_.getString("Color")));
      $$3.setOverlay(BossEvent.BossBarOverlay.byName(p_136273_.getString("Overlay")));
      $$3.setDarkenScreen(p_136273_.getBoolean("DarkenScreen"));
      $$3.setPlayBossMusic(p_136273_.getBoolean("PlayBossMusic"));
      $$3.setCreateWorldFog(p_136273_.getBoolean("CreateWorldFog"));

      for (Tag $$5 : p_136273_.getList("Players", 11)) {
         $$3.addOfflinePlayer(NbtUtils.loadUUID($$5));
      }

      return $$3;
   }

   public void onPlayerConnect(ServerPlayer p_136284_) {
      if (this.players.contains(p_136284_.getUUID())) {
         this.addPlayer(p_136284_);
      }
   }

   public void onPlayerDisconnect(ServerPlayer p_136287_) {
      super.removePlayer(p_136287_);
   }
}
