package net.neoforged.neoforge.network.negotiation;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class NetworkComponentNegotiator {
   public static NegotiationResult negotiate(List<NegotiableNetworkComponent> server, List<NegotiableNetworkComponent> client) {
      List<NegotiableNetworkComponent> var15 = new ArrayList<>(server);
      List<NegotiableNetworkComponent> var16 = new ArrayList<>(client);
      List<NegotiableNetworkComponent> disabledOptionalOnClient = buildDisabledOptionalComponents(var16, var15);
      var16.removeAll(disabledOptionalOnClient);
      List<NegotiableNetworkComponent> disabledOptionalOnServer = buildDisabledOptionalComponents(var15, var16);
      var15.removeAll(disabledOptionalOnServer);
      Table<ResourceLocation, NegotiableNetworkComponent, NegotiableNetworkComponent> matches = HashBasedTable.create();
      var15.forEach(s -> var16.forEach(c -> {
         if (s.id().equals(c.id())) {
            matches.put(s.id(), s, c);
         }
      }));
      var16.removeIf(c -> matches.containsRow(c.id()));
      var15.removeIf(c -> matches.containsRow(c.id()));
      if (!var16.isEmpty()) {
         Map<ResourceLocation, Component> failureReasons = new HashMap<>();
         var16.forEach(
            c -> {
               Component channelFailureReason = Component.translatable("neoforge.network.negotiation.failure.missing.client.server");
               String modDisplayNamex = ModList.get().getModContainerById(c.id().getNamespace()).map(mc -> mc.getModInfo().getDisplayName()).orElse("");
               failureReasons.put(
                  c.id(),
                  (Component)(modDisplayNamex.isEmpty()
                     ? channelFailureReason
                     : Component.translatable("neoforge.network.negotiation.failure.mod", modDisplayNamex, channelFailureReason))
               );
            }
         );
         return new NegotiationResult(List.of(), false, failureReasons);
      } else if (!var15.isEmpty()) {
         Map<ResourceLocation, Component> failureReasons = new HashMap<>();
         var15.forEach(
            c -> {
               Component channelFailureReason = Component.translatable("neoforge.network.negotiation.failure.missing.server.client");
               String modDisplayNamex = ModList.get().getModContainerById(c.id().getNamespace()).map(mc -> mc.getModInfo().getDisplayName()).orElse("");
               failureReasons.put(
                  c.id(),
                  (Component)(modDisplayNamex.isEmpty()
                     ? channelFailureReason
                     : Component.translatable("neoforge.network.negotiation.failure.mod", modDisplayNamex, channelFailureReason))
               );
            }
         );
         return new NegotiationResult(List.of(), false, failureReasons);
      } else {
         List<NegotiatedNetworkComponent> result = new ArrayList<>();
         Map<ResourceLocation, Component> failureReasons = new HashMap<>();

         for (Cell<ResourceLocation, NegotiableNetworkComponent, NegotiableNetworkComponent> match : matches.cellSet()) {
            NegotiableNetworkComponent serverComponent = (NegotiableNetworkComponent)match.getColumnKey();
            NegotiableNetworkComponent clientComponent = (NegotiableNetworkComponent)match.getValue();
            String modDisplayName = ModList.get()
               .getModContainerById(serverComponent.id().getNamespace())
               .map(mc -> mc.getModInfo().getDisplayName())
               .orElse("");
            Optional<NetworkComponentNegotiator.ComponentNegotiationResult> serverToClientComparison = validateComponent(
               serverComponent, clientComponent, "client"
            );
            if (serverToClientComparison.isPresent() && !serverToClientComparison.get().success()) {
               failureReasons.put(
                  serverComponent.id(),
                  (Component)(modDisplayName.isEmpty()
                     ? serverToClientComparison.get().failureReason()
                     : Component.translatable("neoforge.network.negotiation.failure.mod", modDisplayName, serverToClientComparison.get().failureReason()))
               );
            } else {
               Optional<NetworkComponentNegotiator.ComponentNegotiationResult> clientToServerComparison = validateComponent(
                  clientComponent, serverComponent, "server"
               );
               if (clientToServerComparison.isPresent() && !clientToServerComparison.get().success()) {
                  failureReasons.put(
                     serverComponent.id(),
                     (Component)(modDisplayName.isEmpty()
                        ? clientToServerComparison.get().failureReason()
                        : Component.translatable("neoforge.network.negotiation.failure.mod", modDisplayName, clientToServerComparison.get().failureReason()))
                  );
               } else {
                  result.add(new NegotiatedNetworkComponent(serverComponent.id(), serverComponent.version()));
               }
            }
         }

         return failureReasons.isEmpty() ? new NegotiationResult(result, true, failureReasons) : new NegotiationResult(List.of(), false, failureReasons);
      }
   }

   private static List<NegotiableNetworkComponent> buildDisabledOptionalComponents(
      List<NegotiableNetworkComponent> currentSide, List<NegotiableNetworkComponent> otherSide
   ) {
      return currentSide.stream().filter(NegotiableNetworkComponent::optional).filter(c -> otherSide.stream().noneMatch(c2 -> c2.id().equals(c.id()))).toList();
   }

   @VisibleForTesting
   public static Optional<NetworkComponentNegotiator.ComponentNegotiationResult> validateComponent(
      NegotiableNetworkComponent left, NegotiableNetworkComponent right, String requestingSide
   ) {
      if (left.flow().isPresent()) {
         if (right.flow().isEmpty()) {
            return Optional.of(
               new NetworkComponentNegotiator.ComponentNegotiationResult(
                  false, Component.translatable("neoforge.network.negotiation.failure.flow.%s.missing".formatted(requestingSide), left.flow().get().toString())
               )
            );
         }

         if (left.flow().get() != right.flow().get()) {
            return Optional.of(
               new NetworkComponentNegotiator.ComponentNegotiationResult(
                  false,
                  Component.translatable(
                     "neoforge.network.negotiation.failure.flow.%s.mismatch".formatted(requestingSide),
                     left.flow().get().toString(),
                     right.flow().get().toString()
                  )
               )
            );
         }
      }

      return !left.version().equals(right.version())
         ? Optional.of(
            new NetworkComponentNegotiator.ComponentNegotiationResult(
               false, Component.translatable("neoforge.network.negotiation.failure.version.mismatch", left.version(), right.version())
            )
         )
         : Optional.empty();
   }

   public record ComponentNegotiationResult(boolean success, @Nullable Component failureReason) {
   }
}
