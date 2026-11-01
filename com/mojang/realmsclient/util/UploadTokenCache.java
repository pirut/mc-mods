package com.mojang.realmsclient.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class UploadTokenCache {
   private static final Long2ObjectMap<String> TOKEN_CACHE = new Long2ObjectOpenHashMap();

   public static String get(long worldId) {
      return (String)TOKEN_CACHE.get(worldId);
   }

   public static void invalidate(long worldId) {
      TOKEN_CACHE.remove(worldId);
   }

   public static void put(long worldId, @Nullable String token) {
      TOKEN_CACHE.put(worldId, token);
   }
}
