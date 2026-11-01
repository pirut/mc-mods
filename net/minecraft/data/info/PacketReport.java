package net.minecraft.data.info;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.game.GameProtocols;
import net.minecraft.network.protocol.handshake.HandshakeProtocols;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.status.StatusProtocols;

public class PacketReport implements DataProvider {
   private final PackOutput output;

   public PacketReport(PackOutput p_352107_) {
      this.output = p_352107_;
   }

   @Override
   public CompletableFuture<?> run(CachedOutput p_352340_) {
      Path $$1 = this.output.getOutputFolder(PackOutput.Target.REPORTS).resolve("packets.json");
      return DataProvider.saveStable(p_352340_, this.serializePackets(), $$1);
   }

   private JsonElement serializePackets() {
      JsonObject $$0 = new JsonObject();
      Stream.of(
            HandshakeProtocols.SERVERBOUND_TEMPLATE,
            StatusProtocols.CLIENTBOUND_TEMPLATE,
            StatusProtocols.SERVERBOUND_TEMPLATE,
            LoginProtocols.CLIENTBOUND_TEMPLATE,
            LoginProtocols.SERVERBOUND_TEMPLATE,
            ConfigurationProtocols.CLIENTBOUND_TEMPLATE,
            ConfigurationProtocols.SERVERBOUND_TEMPLATE,
            GameProtocols.CLIENTBOUND_TEMPLATE,
            GameProtocols.SERVERBOUND_TEMPLATE
         )
         .collect(Collectors.groupingBy(ProtocolInfo.Unbound::id))
         .forEach((p_352310_, p_352325_) -> {
            JsonObject $$3 = new JsonObject();
            $$0.add(p_352310_.id(), $$3);
            p_352325_.forEach(p_352425_ -> {
               JsonObject $$2 = new JsonObject();
               $$3.add(p_352425_.flow().id(), $$2);
               p_352425_.listPackets((p_352132_, p_352254_) -> {
                  JsonObject $$3x = new JsonObject();
                  $$3x.addProperty("protocol_id", p_352254_);
                  $$2.add(p_352132_.id().toString(), $$3x);
               });
            });
         });
      return $$0;
   }

   @Override
   public String getName() {
      return "Packet Report";
   }
}
