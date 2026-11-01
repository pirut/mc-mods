package net.neoforged.neoforge.server.permission.events;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.server.permission.handler.DefaultPermissionHandler;
import net.neoforged.neoforge.server.permission.handler.IPermissionHandlerFactory;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;

public abstract class PermissionGatherEvent extends Event {
   public static class Handler extends PermissionGatherEvent {
      private Map<Identifier, IPermissionHandlerFactory> availableHandlers = new HashMap<>();

      public Handler() {
         this.availableHandlers.put(DefaultPermissionHandler.IDENTIFIER, DefaultPermissionHandler::new);
      }

      public Map<Identifier, IPermissionHandlerFactory> getAvailablePermissionHandlerFactories() {
         return Collections.unmodifiableMap(this.availableHandlers);
      }

      public void addPermissionHandler(Identifier identifier, IPermissionHandlerFactory handlerFactory) {
         Preconditions.checkNotNull(identifier, "Permission handler identifier cannot be null!");
         Preconditions.checkNotNull(handlerFactory, "Permission handler cannot be null!");
         if (this.availableHandlers.containsKey(identifier)) {
            throw new IllegalArgumentException("Attempted to overwrite permission handler " + identifier + ", this is not allowed.");
         } else {
            this.availableHandlers.put(identifier, handlerFactory);
         }
      }
   }

   public static class Nodes extends PermissionGatherEvent {
      private final Set<PermissionNode<?>> nodes = new HashSet<>();

      public Collection<PermissionNode<?>> getNodes() {
         return Collections.unmodifiableCollection(this.nodes);
      }

      public void addNodes(PermissionNode<?>... nodes) {
         for (PermissionNode<?> node : nodes) {
            if (!this.nodes.add(node)) {
               throw new IllegalArgumentException("Tried to register duplicate PermissionNode '" + node.getNodeName() + "'");
            }
         }
      }

      public void addNodes(Iterable<PermissionNode<?>> nodes) {
         for (PermissionNode<?> node : nodes) {
            if (!this.nodes.add(node)) {
               throw new IllegalArgumentException("Tried to register duplicate PermissionNode '" + node.getNodeName() + "'");
            }
         }
      }
   }
}
