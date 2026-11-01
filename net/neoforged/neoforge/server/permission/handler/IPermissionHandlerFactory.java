package net.neoforged.neoforge.server.permission.handler;

import java.util.Collection;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;

@FunctionalInterface
public interface IPermissionHandlerFactory {
   IPermissionHandler create(Collection<PermissionNode<?>> var1);
}
