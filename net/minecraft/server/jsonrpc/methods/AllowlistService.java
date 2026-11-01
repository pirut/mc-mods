package net.minecraft.server.jsonrpc.methods;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.server.jsonrpc.api.PlayerDto;
import net.minecraft.server.jsonrpc.internalapi.MinecraftApi;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.StoredUserEntry;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.util.Util;

public class AllowlistService {
   public static List<PlayerDto> get(MinecraftApi p_442767_) {
      return p_442767_.allowListService()
         .getEntries()
         .stream()
         .filter(p_443314_ -> p_443314_.getUser() != null)
         .map(p_443621_ -> PlayerDto.from(p_443621_.getUser()))
         .toList();
   }

   public static List<PlayerDto> add(MinecraftApi p_443345_, List<PlayerDto> p_443285_, ClientInfo p_443567_) {
      List<CompletableFuture<Optional<NameAndId>>> list = p_443285_.stream()
         .map(p_448839_ -> p_443345_.playerListService().getUser(p_448839_.id(), p_448839_.name()))
         .toList();

      for (Optional<NameAndId> optional : Util.sequence(list).join()) {
         optional.ifPresent(p_448834_ -> p_443345_.allowListService().add(new UserWhiteListEntry(p_448834_), p_443567_));
      }

      return get(p_443345_);
   }

   public static List<PlayerDto> clear(MinecraftApi p_443491_, ClientInfo p_442759_) {
      p_443491_.allowListService().clear(p_442759_);
      return get(p_443491_);
   }

   public static List<PlayerDto> remove(MinecraftApi p_442823_, List<PlayerDto> p_442689_, ClientInfo p_442824_) {
      List<CompletableFuture<Optional<NameAndId>>> list = p_442689_.stream()
         .map(p_448841_ -> p_442823_.playerListService().getUser(p_448841_.id(), p_448841_.name()))
         .toList();

      for (Optional<NameAndId> optional : Util.sequence(list).join()) {
         optional.ifPresent(p_448837_ -> p_442823_.allowListService().remove(p_448837_, p_442824_));
      }

      p_442823_.allowListService().kickUnlistedPlayers(p_442824_);
      return get(p_442823_);
   }

   public static List<PlayerDto> set(MinecraftApi p_443109_, List<PlayerDto> p_442959_, ClientInfo p_443436_) {
      List<CompletableFuture<Optional<NameAndId>>> list = p_442959_.stream()
         .map(p_448849_ -> p_443109_.playerListService().getUser(p_448849_.id(), p_448849_.name()))
         .toList();
      Set<NameAndId> set = Util.sequence(list).join().stream().flatMap(Optional::stream).collect(Collectors.toSet());
      Set<NameAndId> set1 = p_443109_.allowListService().getEntries().stream().map(StoredUserEntry::getUser).collect(Collectors.toSet());
      set1.stream().filter(p_443565_ -> !set.contains(p_443565_)).forEach(p_448844_ -> p_443109_.allowListService().remove(p_448844_, p_443436_));
      set.stream()
         .filter(p_442755_ -> !set1.contains(p_442755_))
         .forEach(p_448847_ -> p_443109_.allowListService().add(new UserWhiteListEntry(p_448847_), p_443436_));
      p_443109_.allowListService().kickUnlistedPlayers(p_443436_);
      return get(p_443109_);
   }
}
