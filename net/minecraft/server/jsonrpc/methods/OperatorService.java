package net.minecraft.server.jsonrpc.methods;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.util.Util;

public class OperatorService {
   public static List<OperatorService.OperatorDto> get(MinecraftApi p_443232_) {
      return p_443232_.operatorListService()
         .getEntries()
         .stream()
         .filter(p_443484_ -> p_443484_.getUser() != null)
         .map(OperatorService.OperatorDto::from)
         .toList();
   }

   public static List<OperatorService.OperatorDto> clear(MinecraftApi p_442716_, ClientInfo p_443472_) {
      p_442716_.operatorListService().clear(p_443472_);
      return get(p_442716_);
   }

   public static List<OperatorService.OperatorDto> remove(MinecraftApi p_443199_, List<PlayerDto> p_443265_, ClientInfo p_443608_) {
      List<CompletableFuture<Optional<NameAndId>>> list = p_443265_.stream()
         .map(p_448888_ -> p_443199_.playerListService().getUser(p_448888_.id(), p_448888_.name()))
         .toList();

      for (Optional<NameAndId> optional : Util.sequence(list).join()) {
         optional.ifPresent(p_448886_ -> p_443199_.operatorListService().deop(p_448886_, p_443608_));
      }

      return get(p_443199_);
   }

   public static List<OperatorService.OperatorDto> add(MinecraftApi p_443613_, List<OperatorService.OperatorDto> p_443170_, ClientInfo p_442587_) {
      List<CompletableFuture<Optional<OperatorService.Op>>> list = p_443170_.stream()
         .map(
            p_448883_ -> p_443613_.playerListService()
               .getUser(p_448883_.player().id(), p_448883_.player().name())
               .thenApply(
                  p_442900_ -> p_442900_.map(p_442579_ -> new OperatorService.Op(p_442579_, p_448883_.permissionLevel(), p_448883_.bypassesPlayerLimit()))
               )
         )
         .toList();

      for (Optional<OperatorService.Op> optional : Util.sequence(list).join()) {
         optional.ifPresent(
            p_448878_ -> p_443613_.operatorListService().op(p_448878_.user(), p_448878_.permissionLevel(), p_448878_.bypassesPlayerLimit(), p_442587_)
         );
      }

      return get(p_443613_);
   }

   public static List<OperatorService.OperatorDto> set(MinecraftApi p_442756_, List<OperatorService.OperatorDto> p_443478_, ClientInfo p_443391_) {
      List<CompletableFuture<Optional<OperatorService.Op>>> list = p_443478_.stream()
         .map(
            p_448890_ -> p_442756_.playerListService()
               .getUser(p_448890_.player().id(), p_448890_.player().name())
               .thenApply(
                  p_442611_ -> p_442611_.map(p_443329_ -> new OperatorService.Op(p_443329_, p_448890_.permissionLevel(), p_448890_.bypassesPlayerLimit()))
               )
         )
         .toList();
      Set<OperatorService.Op> set = Util.sequence(list).join().stream().flatMap(Optional::stream).collect(Collectors.toSet());
      Set<OperatorService.Op> set1 = p_442756_.operatorListService()
         .getEntries()
         .stream()
         .filter(p_443549_ -> p_443549_.getUser() != null)
         .map(
            p_454424_ -> new OperatorService.Op(
               p_454424_.getUser(), Optional.of(p_454424_.permissions().level()), Optional.of(p_454424_.getBypassesPlayerLimit())
            )
         )
         .collect(Collectors.toSet());
      set1.stream().filter(p_442831_ -> !set.contains(p_442831_)).forEach(p_448875_ -> p_442756_.operatorListService().deop(p_448875_.user(), p_443391_));
      set.stream()
         .filter(p_442947_ -> !set1.contains(p_442947_))
         .forEach(p_448881_ -> p_442756_.operatorListService().op(p_448881_.user(), p_448881_.permissionLevel(), p_448881_.bypassesPlayerLimit(), p_443391_));
      return get(p_442756_);
   }

   record Op(NameAndId user, Optional<PermissionLevel> permissionLevel, Optional<Boolean> bypassesPlayerLimit) {
   }

   public record OperatorDto(PlayerDto player, Optional<PermissionLevel> permissionLevel, Optional<Boolean> bypassesPlayerLimit) {
      public static final MapCodec<OperatorService.OperatorDto> CODEC = RecordCodecBuilder.mapCodec(
         p_454425_ -> p_454425_.group(
               PlayerDto.CODEC.codec().fieldOf("player").forGetter(OperatorService.OperatorDto::player),
               PermissionLevel.INT_CODEC.optionalFieldOf("permissionLevel").forGetter(OperatorService.OperatorDto::permissionLevel),
               Codec.BOOL.optionalFieldOf("bypassesPlayerLimit").forGetter(OperatorService.OperatorDto::bypassesPlayerLimit)
            )
            .apply(p_454425_, OperatorService.OperatorDto::new)
      );

      public static OperatorService.OperatorDto from(ServerOpListEntry p_442969_) {
         return new OperatorService.OperatorDto(
            PlayerDto.from(Objects.requireNonNull(p_442969_.getUser())),
            Optional.of(p_442969_.permissions().level()),
            Optional.of(p_442969_.getBypassesPlayerLimit())
         );
      }
   }
}
