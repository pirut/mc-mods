package net.minecraft.server.jsonrpc.methods;

import com.google.common.net.InetAddresses;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.network.chat.Component;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.util.ExtraCodecs;
import org.jspecify.annotations.Nullable;

public class IpBanlistService {
   private static final String BAN_SOURCE = "Management server";

   public static List<IpBanlistService.IpBanDto> get(MinecraftApi p_443437_) {
      return p_443437_.banListService().getIpBanEntries().stream().map(IpBanlistService.IpBan::from).map(IpBanlistService.IpBanDto::from).toList();
   }

   public static List<IpBanlistService.IpBanDto> add(MinecraftApi p_443077_, List<IpBanlistService.IncomingIpBanDto> p_443509_, ClientInfo p_443536_) {
      p_443509_.stream()
         .map(p_443411_ -> banIp(p_443077_, p_443411_, p_443536_))
         .flatMap(Collection::stream)
         .forEach(p_442542_ -> p_442542_.connection.disconnect(Component.translatable("multiplayer.disconnect.ip_banned")));
      return get(p_443077_);
   }

   private static List<ServerPlayer> banIp(MinecraftApi p_443378_, IpBanlistService.IncomingIpBanDto p_443183_, ClientInfo p_443587_) {
      IpBanlistService.IpBan ipbanlistservice$ipban = p_443183_.toIpBan();
      if (ipbanlistservice$ipban != null) {
         return banIp(p_443378_, ipbanlistservice$ipban, p_443587_);
      } else {
         if (p_443183_.player().isPresent()) {
            Optional<ServerPlayer> optional = p_443378_.playerListService().getPlayer(p_443183_.player().get().id(), p_443183_.player().get().name());
            if (optional.isPresent()) {
               return banIp(p_443378_, p_443183_.toIpBan(optional.get()), p_443587_);
            }
         }

         return List.of();
      }
   }

   private static List<ServerPlayer> banIp(MinecraftApi p_443052_, IpBanlistService.IpBan p_442706_, ClientInfo p_443257_) {
      p_443052_.banListService().addIpBan(p_442706_.toIpBanEntry(), p_443257_);
      return p_443052_.playerListService().getPlayersWithAddress(p_442706_.ip());
   }

   public static List<IpBanlistService.IpBanDto> clear(MinecraftApi p_443078_, ClientInfo p_442572_) {
      p_443078_.banListService().clearIpBans(p_442572_);
      return get(p_443078_);
   }

   public static List<IpBanlistService.IpBanDto> remove(MinecraftApi p_443372_, List<String> p_442982_, ClientInfo p_443048_) {
      p_442982_.forEach(p_448867_ -> p_443372_.banListService().removeIpBan(p_448867_, p_443048_));
      return get(p_443372_);
   }

   public static List<IpBanlistService.IpBanDto> set(MinecraftApi p_443061_, List<IpBanlistService.IpBanDto> p_442973_, ClientInfo p_442669_) {
      Set<IpBanlistService.IpBan> set = p_442973_.stream()
         .filter(p_442743_ -> InetAddresses.isInetAddress(p_442743_.ip()))
         .map(IpBanlistService.IpBanDto::toIpBan)
         .collect(Collectors.toSet());
      Set<IpBanlistService.IpBan> set1 = p_443061_.banListService().getIpBanEntries().stream().map(IpBanlistService.IpBan::from).collect(Collectors.toSet());
      set1.stream().filter(p_442573_ -> !set.contains(p_442573_)).forEach(p_448864_ -> p_443061_.banListService().removeIpBan(p_448864_.ip(), p_442669_));
      set.stream()
         .filter(p_443035_ -> !set1.contains(p_443035_))
         .forEach(p_448872_ -> p_443061_.banListService().addIpBan(p_448872_.toIpBanEntry(), p_442669_));
      set.stream()
         .filter(p_443230_ -> !set1.contains(p_443230_))
         .flatMap(p_448869_ -> p_443061_.playerListService().getPlayersWithAddress(p_448869_.ip()).stream())
         .forEach(p_443294_ -> p_443294_.connection.disconnect(Component.translatable("multiplayer.disconnect.ip_banned")));
      return get(p_443061_);
   }

   public record IncomingIpBanDto(Optional<PlayerDto> player, Optional<String> ip, Optional<String> reason, Optional<String> source, Optional<Instant> expires) {
      public static final MapCodec<IpBanlistService.IncomingIpBanDto> CODEC = RecordCodecBuilder.mapCodec(
         p_442610_ -> p_442610_.group(
               PlayerDto.CODEC.codec().optionalFieldOf("player").forGetter(IpBanlistService.IncomingIpBanDto::player),
               Codec.STRING.optionalFieldOf("ip").forGetter(IpBanlistService.IncomingIpBanDto::ip),
               Codec.STRING.optionalFieldOf("reason").forGetter(IpBanlistService.IncomingIpBanDto::reason),
               Codec.STRING.optionalFieldOf("source").forGetter(IpBanlistService.IncomingIpBanDto::source),
               ExtraCodecs.INSTANT_ISO8601.optionalFieldOf("expires").forGetter(IpBanlistService.IncomingIpBanDto::expires)
            )
            .apply(p_442610_, IpBanlistService.IncomingIpBanDto::new)
      );

      IpBanlistService.IpBan toIpBan(ServerPlayer p_443233_) {
         return new IpBanlistService.IpBan(p_443233_.getIpAddress(), this.reason().orElse(null), this.source().orElse("Management server"), this.expires());
      }

      @Nullable
      IpBanlistService.IpBan toIpBan() {
         return !this.ip().isEmpty() && InetAddresses.isInetAddress(this.ip().get())
            ? new IpBanlistService.IpBan(this.ip().get(), this.reason().orElse(null), this.source().orElse("Management server"), this.expires())
            : null;
      }
   }

   record IpBan(String ip, @Nullable String reason, String source, Optional<Instant> expires) {
      static IpBanlistService.IpBan from(IpBanListEntry p_442634_) {
         return new IpBanlistService.IpBan(
            Objects.requireNonNull(p_442634_.getUser()),
            p_442634_.getReason(),
            p_442634_.getSource(),
            Optional.ofNullable(p_442634_.getExpires()).map(Date::toInstant)
         );
      }

      IpBanListEntry toIpBanEntry() {
         return new IpBanListEntry(this.ip(), null, this.source(), this.expires().map(Date::from).orElse(null), this.reason());
      }
   }

   public record IpBanDto(String ip, Optional<String> reason, Optional<String> source, Optional<Instant> expires) {
      public static final MapCodec<IpBanlistService.IpBanDto> CODEC = RecordCodecBuilder.mapCodec(
         p_442908_ -> p_442908_.group(
               Codec.STRING.fieldOf("ip").forGetter(IpBanlistService.IpBanDto::ip),
               Codec.STRING.optionalFieldOf("reason").forGetter(IpBanlistService.IpBanDto::reason),
               Codec.STRING.optionalFieldOf("source").forGetter(IpBanlistService.IpBanDto::source),
               ExtraCodecs.INSTANT_ISO8601.optionalFieldOf("expires").forGetter(IpBanlistService.IpBanDto::expires)
            )
            .apply(p_442908_, IpBanlistService.IpBanDto::new)
      );

      private static IpBanlistService.IpBanDto from(IpBanlistService.IpBan p_443303_) {
         return new IpBanlistService.IpBanDto(p_443303_.ip(), Optional.ofNullable(p_443303_.reason()), Optional.of(p_443303_.source()), p_443303_.expires());
      }

      public static IpBanlistService.IpBanDto from(IpBanListEntry p_442708_) {
         return from(IpBanlistService.IpBan.from(p_442708_));
      }

      private IpBanlistService.IpBan toIpBan() {
         return new IpBanlistService.IpBan(this.ip(), this.reason().orElse(null), this.source().orElse("Management server"), this.expires());
      }
   }
}
