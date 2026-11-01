package net.neoforged.neoforge.client.event;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class RegisterDebugEntriesEvent extends Event implements IModBusEvent {
   private final Map<Identifier, DebugScreenEntry> entries;
   private final Map<Identifier, DebugScreenEntryStatus> defaultProfile;
   private final Map<Identifier, DebugScreenEntryStatus> performanceProfile;

   @Internal
   public RegisterDebugEntriesEvent(
      Map<Identifier, DebugScreenEntry> entries,
      Map<Identifier, DebugScreenEntryStatus> defaultProfile,
      Map<Identifier, DebugScreenEntryStatus> performanceProfile
   ) {
      this.entries = entries;
      this.defaultProfile = new HashMap<>(defaultProfile);
      this.performanceProfile = new HashMap<>(performanceProfile);
   }

   public void register(Identifier id, DebugScreenEntry entry) {
      if (this.entries.putIfAbsent(id, entry) != null) {
         throw new IllegalStateException("Duplicate DebugScreenEntry registration: " + id);
      }
   }

   public boolean isRegistered(Identifier id) {
      return this.entries.containsKey(id);
   }

   public void includeInProfile(Identifier id, DebugScreenProfile profile, DebugScreenEntryStatus profileStatus) {
      if (this.getProfileMap(profile).putIfAbsent(id, profileStatus) != null) {
         throw new IllegalStateException("Duplicate DebugScreenEntry " + profile.getSerializedName() + "-profile inclusion: " + id);
      }
   }

   public boolean isIncludedInProfile(Identifier id, DebugScreenProfile profile) {
      return this.getProfileMap(profile).containsKey(id);
   }

   private Map<Identifier, DebugScreenEntryStatus> getProfileMap(DebugScreenProfile profile) {
      return switch (profile) {
         case DEFAULT -> this.defaultProfile;
         case PERFORMANCE -> this.performanceProfile;
      };
   }

   @Internal
   public Map<DebugScreenProfile, Map<Identifier, DebugScreenEntryStatus>> validateProfiles() {
      IllegalStateException defaultError = this.validateProfile(DebugScreenProfile.DEFAULT);
      IllegalStateException performanceError = this.validateProfile(DebugScreenProfile.PERFORMANCE);
      if (defaultError != null && performanceError != null) {
         IllegalStateException error = new IllegalStateException("Fatal errors occurred while validating DebugScreenProfiles");
         error.addSuppressed(defaultError);
         error.addSuppressed(performanceError);
         throw error;
      } else if (defaultError != null) {
         throw defaultError;
      } else if (performanceError != null) {
         throw performanceError;
      } else {
         return Map.of(DebugScreenProfile.DEFAULT, Map.copyOf(this.defaultProfile), DebugScreenProfile.PERFORMANCE, Map.copyOf(this.performanceProfile));
      }
   }

   @Nullable
   private IllegalStateException validateProfile(DebugScreenProfile profile) {
      Map<Identifier, DebugScreenEntryStatus> profileMap = this.getProfileMap(profile);
      SetView<Identifier> invalidIds = Sets.difference(profileMap.keySet(), this.entries.keySet());
      if (!invalidIds.isEmpty()) {
         Logger logger = LogUtils.getLogger();
         logger.error("Found {} unregistered debug entries in profile: {}", invalidIds.size(), profile.getSerializedName());
         logger.error("Unregistered debug entries: {}", invalidIds.stream().map(Identifier::toString).collect(Collectors.joining(",", "[", "]")));
         if (!FMLEnvironment.isProduction()) {
            return new IllegalStateException("Fatal error occurred while validating DebugScreenProfile: " + profile.getSerializedName());
         }
      }

      return null;
   }
}
