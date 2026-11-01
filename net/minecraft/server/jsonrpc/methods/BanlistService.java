package net.minecraft.server.jsonrpc.methods;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class BanlistService {
   private static final String BAN_SOURCE = "Management server";

   public static List<BanlistService.UserBanDto> get(MinecraftApi p_443371_) {
      return p_443371_.banListService()
         .getUserBanEntries()
         .stream()
         .filter(p_442571_ -> p_442571_.getUser() != null)
         .map(BanlistService.UserBan::from)
         .map(BanlistService.UserBanDto::from)
         .toList();
   }

   public static List<BanlistService.UserBanDto> add(MinecraftApi p_443151_, List<BanlistService.UserBanDto> p_443043_, ClientInfo p_442996_) {
      List<CompletableFuture<Optional<BanlistService.UserBan>>> list = p_443043_.stream()
         .map(
            p_448859_ -> p_443151_.playerListService()
               .getUser(p_448859_.player().id(), p_448859_.player().name())
               .thenApply(p_442482_ -> p_442482_.map(p_448859_::toUserBan))
         )
         .toList();

      for (Optional<BanlistService.UserBan> optional : Util.sequence(list).join()) {
         if (!optional.isEmpty()) {
            BanlistService.UserBan banlistservice$userban = optional.get();
            p_443151_.banListService().addUserBan(banlistservice$userban.toBanEntry(), p_442996_);
            ServerPlayer serverplayer = p_443151_.playerListService().getPlayer(optional.get().player().id());
            if (serverplayer != null) {
               serverplayer.connection.disconnect(Component.translatable("multiplayer.disconnect.banned"));
            }
         }
      }

      return get(p_443151_);
   }

   public static List<BanlistService.UserBanDto> clear(MinecraftApi p_443161_, ClientInfo p_442855_) {
      p_443161_.banListService().clearUserBans(p_442855_);
      return get(p_443161_);
   }

   public static List<BanlistService.UserBanDto> remove(MinecraftApi p_442957_, List<PlayerDto> p_443483_, ClientInfo p_442550_) {
      List<CompletableFuture<Optional<NameAndId>>> list = p_443483_.stream()
         .map(p_448861_ -> p_442957_.playerListService().getUser(p_448861_.id(), p_448861_.name()))
         .toList();

      for (Optional<NameAndId> optional : Util.sequence(list).join()) {
         if (!optional.isEmpty()) {
            p_442957_.banListService().removeUserBan(optional.get(), p_442550_);
         }
      }

      return get(p_442957_);
   }

   public static List<BanlistService.UserBanDto> set(MinecraftApi p_442922_, List<BanlistService.UserBanDto> p_443092_, ClientInfo p_443517_) {
      List<CompletableFuture<Optional<BanlistService.UserBan>>> list = p_443092_.stream()
         .map(
            p_448854_ -> p_442922_.playerListService()
               .getUser(p_448854_.player().id(), p_448854_.player().name())
               .thenApply(p_443560_ -> p_443560_.map(p_448854_::toUserBan))
         )
         .toList();
      Set<BanlistService.UserBan> set = Util.sequence(list).join().stream().flatMap(Optional::stream).collect(Collectors.toSet());
      Set<BanlistService.UserBan> set1 = p_442922_.banListService()
         .getUserBanEntries()
         .stream()
         .filter(p_443223_ -> p_443223_.getUser() != null)
         .map(BanlistService.UserBan::from)
         .collect(Collectors.toSet());
      set1.stream().filter(p_442816_ -> !set.contains(p_442816_)).forEach(p_448857_ -> p_442922_.banListService().removeUserBan(p_448857_.player(), p_443517_));
      set.stream().filter(p_442909_ -> !set1.contains(p_442909_)).forEach(p_448852_ -> {
         p_442922_.banListService().addUserBan(p_448852_.toBanEntry(), p_443517_);
         ServerPlayer serverplayer = p_442922_.playerListService().getPlayer(p_448852_.player().id());
         if (serverplayer != null) {
            serverplayer.connection.disconnect(Component.translatable("multiplayer.disconnect.banned"));
         }
      });
      return get(p_442922_);
   }

   record UserBan(NameAndId player, @Nullable String reason, String source, Optional<Instant> expires) {
      static BanlistService.UserBan from(UserBanListEntry p_443281_) {
         return new BanlistService.UserBan(
            Objects.requireNonNull(p_443281_.getUser()),
            p_443281_.getReason(),
            p_443281_.getSource(),
            Optional.ofNullable(p_443281_.getExpires()).map(Date::toInstant)
         );
      }

      UserBanListEntry toBanEntry() {
         return new UserBanListEntry(
            new NameAndId(this.player().id(), this.player().name()), null, this.source(), this.expires().map(Date::from).orElse(null), this.reason()
         );
      }
   }

   public record UserBanDto(PlayerDto player, Optional<String> reason, Optional<String> source, Optional<Instant> expires) {
      public static final MapCodec<BanlistService.UserBanDto> CODEC = RecordCodecBuilder.mapCodec(
         p_443601_ -> p_443601_.group(
               PlayerDto.CODEC.codec().fieldOf("player").forGetter(BanlistService.UserBanDto::player),
               Codec.STRING.optionalFieldOf("reason").forGetter(BanlistService.UserBanDto::reason),
               Codec.STRING.optionalFieldOf("source").forGetter(BanlistService.UserBanDto::source),
               ExtraCodecs.INSTANT_ISO8601.optionalFieldOf("expires").forGetter(BanlistService.UserBanDto::expires)
            )
            .apply(p_443601_, BanlistService.UserBanDto::new)
      );

      private static BanlistService.UserBanDto from(BanlistService.UserBan p_443238_) {
         return new BanlistService.UserBanDto(
            PlayerDto.from(p_443238_.player()), Optional.ofNullable(p_443238_.reason()), Optional.of(p_443238_.source()), p_443238_.expires()
         );
      }

      public static BanlistService.UserBanDto from(UserBanListEntry p_443615_) {
         return from(BanlistService.UserBan.from(p_443615_));
      }

      private BanlistService.UserBan toUserBan(NameAndId p_443419_) {
         return new BanlistService.UserBan(p_443419_, this.reason().orElse(null), this.source().orElse("Management server"), this.expires());
      }
   }
}
