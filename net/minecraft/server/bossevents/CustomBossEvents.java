package net.minecraft.server.bossevents;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class CustomBossEvents {
   private final Map<ResourceLocation, CustomBossEvent> events = Maps.newHashMap();

   @Nullable
   public CustomBossEvent get(ResourceLocation p_136298_) {
      return this.events.get(p_136298_);
   }

   public CustomBossEvent create(ResourceLocation p_136300_, Component p_136301_) {
      CustomBossEvent $$2 = new CustomBossEvent(p_136300_, p_136301_);
      this.events.put(p_136300_, $$2);
      return $$2;
   }

   public void remove(CustomBossEvent p_136303_) {
      this.events.remove(p_136303_.getTextId());
   }

   public Collection<ResourceLocation> getIds() {
      return this.events.keySet();
   }

   public Collection<CustomBossEvent> getEvents() {
      return this.events.values();
   }

   public CompoundTag save(HolderLookup.Provider p_330783_) {
      CompoundTag $$1 = new CompoundTag();

      for (CustomBossEvent $$2 : this.events.values()) {
         $$1.put($$2.getTextId().toString(), $$2.save(p_330783_));
      }

      return $$1;
   }

   public void load(CompoundTag p_136296_, HolderLookup.Provider p_331580_) {
      for (String $$2 : p_136296_.getAllKeys()) {
         ResourceLocation $$3 = ResourceLocation.parse($$2);
         this.events.put($$3, CustomBossEvent.load(p_136296_.getCompound($$2), $$3, p_331580_));
      }
   }

   public void onPlayerConnect(ServerPlayer p_136294_) {
      for (CustomBossEvent $$1 : this.events.values()) {
         $$1.onPlayerConnect(p_136294_);
      }
   }

   public void onPlayerDisconnect(ServerPlayer p_136306_) {
      for (CustomBossEvent $$1 : this.events.values()) {
         $$1.onPlayerDisconnect(p_136306_);
      }
   }
}
