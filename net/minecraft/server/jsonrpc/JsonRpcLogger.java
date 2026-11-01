package net.minecraft.server.jsonrpc;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import org.slf4j.Logger;

public class JsonRpcLogger {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String PREFIX = "RPC Connection #{}: ";

   public void log(ClientInfo p_443366_, String p_442582_, Object... p_443083_) {
      if (p_443083_.length == 0) {
         LOGGER.info("RPC Connection #{}: " + p_442582_, p_443366_.connectionId());
      } else {
         List<Object> list = new ArrayList<>(Arrays.asList(p_443083_));
         list.addFirst(p_443366_.connectionId());
         LOGGER.info("RPC Connection #{}: " + p_442582_, list.toArray());
      }
   }
}
