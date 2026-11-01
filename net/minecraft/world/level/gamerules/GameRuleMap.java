package net.minecraft.world.level.gamerules;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jspecify.annotations.Nullable;

public final class GameRuleMap {
   public static final Codec<GameRuleMap> CODEC = Codec.dispatchedMap(BuiltInRegistries.GAME_RULE.byNameCodec(), GameRule::valueCodec)
      .xmap(GameRuleMap::ofTrusted, GameRuleMap::map);
   private final Reference2ObjectMap<GameRule<?>, Object> map;

   GameRuleMap(Reference2ObjectMap<GameRule<?>, Object> p_460951_) {
      this.map = p_460951_;
   }

   private static GameRuleMap ofTrusted(Map<GameRule<?>, Object> p_461237_) {
      return new GameRuleMap(new Reference2ObjectOpenHashMap(p_461237_));
   }

   public static GameRuleMap of() {
      return new GameRuleMap(new Reference2ObjectOpenHashMap());
   }

   public static GameRuleMap of(Stream<GameRule<?>> p_460726_) {
      Reference2ObjectOpenHashMap<GameRule<?>, Object> reference2objectopenhashmap = new Reference2ObjectOpenHashMap();
      p_460726_.forEach(p_461069_ -> reference2objectopenhashmap.put(p_461069_, p_461069_.defaultValue()));
      return new GameRuleMap(reference2objectopenhashmap);
   }

   public static GameRuleMap copyOf(GameRuleMap p_460657_) {
      return new GameRuleMap(new Reference2ObjectOpenHashMap(p_460657_.map));
   }

   public boolean has(GameRule<?> p_461111_) {
      return this.map.containsKey(p_461111_);
   }

   @Nullable
   public <T> T get(GameRule<T> p_460855_) {
      return (T)this.map.get(p_460855_);
   }

   public <T> void set(GameRule<T> p_461065_, T p_461255_) {
      this.map.put(p_461065_, p_461255_);
   }

   @Nullable
   public <T> T remove(GameRule<T> p_461133_) {
      return (T)this.map.remove(p_461133_);
   }

   public Set<GameRule<?>> keySet() {
      return this.map.keySet();
   }

   public int size() {
      return this.map.size();
   }

   @Override
   public String toString() {
      return this.map.toString();
   }

   public GameRuleMap withOther(GameRuleMap p_460840_) {
      GameRuleMap gamerulemap = copyOf(this);
      gamerulemap.setFromIf(p_460840_, p_460809_ -> true);
      return gamerulemap;
   }

   public void setFromIf(GameRuleMap p_460859_, Predicate<GameRule<?>> p_461105_) {
      for (GameRule<?> gamerule : p_460859_.keySet()) {
         if (p_461105_.test(gamerule)) {
            setGameRule(p_460859_, gamerule, this);
         }
      }
   }

   private static <T> void setGameRule(GameRuleMap p_460814_, GameRule<T> p_460729_, GameRuleMap p_461007_) {
      p_461007_.set(p_460729_, Objects.requireNonNull(p_460814_.get(p_460729_)));
   }

   private Reference2ObjectMap<GameRule<?>, Object> map() {
      return this.map;
   }

   @Override
   public boolean equals(Object p_460688_) {
      if (p_460688_ == this) {
         return true;
      } else if (p_460688_ != null && p_460688_.getClass() == this.getClass()) {
         GameRuleMap gamerulemap = (GameRuleMap)p_460688_;
         return Objects.equals(this.map, gamerulemap.map);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.map);
   }

   public static class Builder {
      final Reference2ObjectMap<GameRule<?>, Object> map = new Reference2ObjectOpenHashMap();

      public <T> GameRuleMap.Builder set(GameRule<T> p_460707_, T p_461184_) {
         this.map.put(p_460707_, p_461184_);
         return this;
      }

      public GameRuleMap build() {
         return new GameRuleMap(this.map);
      }
   }
}
