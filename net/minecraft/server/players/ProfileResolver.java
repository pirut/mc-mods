package net.minecraft.server.players;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.datafixers.util.Either;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.util.StringUtil;

public interface ProfileResolver {
   Optional<GameProfile> fetchByName(String name);

   Optional<GameProfile> fetchById(UUID id);

   default Optional<GameProfile> fetchByNameOrId(Either<String, UUID> nameOrId) {
      return (Optional<GameProfile>)nameOrId.map(this::fetchByName, this::fetchById);
   }

   public static class Cached implements ProfileResolver {
      private final LoadingCache<String, Optional<GameProfile>> profileCacheByName;
      final LoadingCache<UUID, Optional<GameProfile>> profileCacheById;

      public Cached(MinecraftSessionService sessionService, UserNameToIdResolver resolver) {
         this.profileCacheById = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10L))
            .maximumSize(256L)
            .build(new CacheLoader<UUID, Optional<GameProfile>>() {
               public Optional<GameProfile> load(UUID uUID) {
                  ProfileResult profileResult = sessionService.fetchProfile(uUID, true);
                  return Optional.ofNullable(profileResult).map(ProfileResult::profile);
               }
            });
         this.profileCacheByName = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10L))
            .maximumSize(256L)
            .build(new CacheLoader<String, Optional<GameProfile>>() {
               public Optional<GameProfile> load(String string) {
                  return resolver.get(string).flatMap(nameAndId -> (Optional<? extends GameProfile>)Cached.this.profileCacheById.getUnchecked(nameAndId.id()));
               }
            });
      }

      @Override
      public Optional<GameProfile> fetchByName(String name) {
         return StringUtil.isValidPlayerName(name) ? (Optional)this.profileCacheByName.getUnchecked(name) : Optional.empty();
      }

      @Override
      public Optional<GameProfile> fetchById(UUID id) {
         return (Optional<GameProfile>)this.profileCacheById.getUnchecked(id);
      }
   }
}
