package net.minecraft.network.protocol.game;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.function.BiPredicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class ClientboundCommandsPacket implements Packet<ClientGamePacketListener> {
   public static final StreamCodec<FriendlyByteBuf, ClientboundCommandsPacket> STREAM_CODEC = Packet.codec(
      ClientboundCommandsPacket::write, ClientboundCommandsPacket::new
   );
   private static final byte MASK_TYPE = 3;
   private static final byte FLAG_EXECUTABLE = 4;
   private static final byte FLAG_REDIRECT = 8;
   private static final byte FLAG_CUSTOM_SUGGESTIONS = 16;
   private static final byte FLAG_RESTRICTED = 32;
   private static final byte TYPE_ROOT = 0;
   private static final byte TYPE_LITERAL = 1;
   private static final byte TYPE_ARGUMENT = 2;
   private final int rootIndex;
   private final List<ClientboundCommandsPacket.Entry> entries;

   public <S> ClientboundCommandsPacket(RootCommandNode<S> root, ClientboundCommandsPacket.NodeInspector<S> nodeInspector) {
      Object2IntMap<CommandNode<S>> object2IntMap = enumerateNodes(root);
      this.entries = createEntries(object2IntMap, nodeInspector);
      this.rootIndex = object2IntMap.getInt(root);
   }

   private ClientboundCommandsPacket(FriendlyByteBuf buffer) {
      this.entries = buffer.readList(ClientboundCommandsPacket::readNode);
      this.rootIndex = buffer.readVarInt();
      validateEntries(this.entries);
   }

   private void write(FriendlyByteBuf buffer) {
      buffer.writeCollection(this.entries, (friendlyByteBuf, entry) -> entry.write(friendlyByteBuf));
      buffer.writeVarInt(this.rootIndex);
   }

   private static void validateEntries(List<ClientboundCommandsPacket.Entry> entries, BiPredicate<ClientboundCommandsPacket.Entry, IntSet> validator) {
      IntSet intSet = new IntOpenHashSet(IntSets.fromTo(0, entries.size()));

      while (!intSet.isEmpty()) {
         boolean bl = intSet.removeIf(i -> validator.test(entries.get(i), intSet));
         if (!bl) {
            throw new IllegalStateException("Server sent an impossible command tree");
         }
      }
   }

   private static void validateEntries(List<ClientboundCommandsPacket.Entry> entries) {
      validateEntries(entries, ClientboundCommandsPacket.Entry::canBuild);
      validateEntries(entries, ClientboundCommandsPacket.Entry::canResolve);
   }

   private static <S> Object2IntMap<CommandNode<S>> enumerateNodes(RootCommandNode<S> rootNode) {
      Object2IntMap<CommandNode<S>> object2IntMap = new Object2IntOpenHashMap();
      Queue<CommandNode<S>> queue = new ArrayDeque<>();
      queue.add(rootNode);

      CommandNode<S> commandNode;
      while ((commandNode = queue.poll()) != null) {
         if (!object2IntMap.containsKey(commandNode)) {
            int i = object2IntMap.size();
            object2IntMap.put(commandNode, i);
            queue.addAll(commandNode.getChildren());
            if (commandNode.getRedirect() != null) {
               queue.add(commandNode.getRedirect());
            }
         }
      }

      return object2IntMap;
   }

   private static <S> List<ClientboundCommandsPacket.Entry> createEntries(
      Object2IntMap<CommandNode<S>> nodes, ClientboundCommandsPacket.NodeInspector<S> nodeInspector
   ) {
      ObjectArrayList<ClientboundCommandsPacket.Entry> objectArrayList = new ObjectArrayList(nodes.size());
      objectArrayList.size(nodes.size());
      ObjectIterator var3 = Object2IntMaps.fastIterable(nodes).iterator();

      while (var3.hasNext()) {
         it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<CommandNode<S>> entry = (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<CommandNode<S>>)var3.next();
         objectArrayList.set(entry.getIntValue(), createEntry((CommandNode<S>)entry.getKey(), nodeInspector, nodes));
      }

      return objectArrayList;
   }

   private static ClientboundCommandsPacket.Entry readNode(FriendlyByteBuf buffer) {
      byte b = buffer.readByte();
      int[] is = buffer.readVarIntArray();
      int i = (b & 8) != 0 ? buffer.readVarInt() : 0;
      ClientboundCommandsPacket.NodeStub nodeStub = read(buffer, b);
      return new ClientboundCommandsPacket.Entry(nodeStub, b, i, is);
   }

   @Nullable
   private static ClientboundCommandsPacket.NodeStub read(FriendlyByteBuf buffer, byte flags) {
      int i = flags & 3;
      if (i == 2) {
         String string = buffer.readUtf();
         int j = buffer.readVarInt();
         ArgumentTypeInfo<?, ?> argumentTypeInfo = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.byId(j);
         if (argumentTypeInfo == null) {
            return null;
         } else {
            ArgumentTypeInfo.Template<?> template = argumentTypeInfo.deserializeFromNetwork(buffer);
            Identifier identifier = (flags & 16) != 0 ? buffer.readIdentifier() : null;
            return new ClientboundCommandsPacket.ArgumentNodeStub(string, template, identifier);
         }
      } else if (i == 1) {
         String string = buffer.readUtf();
         return new ClientboundCommandsPacket.LiteralNodeStub(string);
      } else {
         return null;
      }
   }

   private static <S> ClientboundCommandsPacket.Entry createEntry(
      CommandNode<S> node, ClientboundCommandsPacket.NodeInspector<S> nodeInspector, Object2IntMap<CommandNode<S>> nodes
   ) {
      int i = 0;
      int j;
      if (node.getRedirect() != null) {
         i |= 8;
         j = nodes.getInt(node.getRedirect());
      } else {
         j = 0;
      }

      if (nodeInspector.isExecutable(node)) {
         i |= 4;
      }

      if (nodeInspector.isRestricted(node)) {
         i |= 32;
      }

      ClientboundCommandsPacket.NodeStub nodeStub;
      switch (node) {
         case RootCommandNode<S> rootCommandNode:
            i |= 0;
            nodeStub = null;
            break;
         case ArgumentCommandNode<S, ?> argumentCommandNode:
            Identifier identifier = nodeInspector.suggestionId(argumentCommandNode);
            nodeStub = new ClientboundCommandsPacket.ArgumentNodeStub(
               argumentCommandNode.getName(), ArgumentTypeInfos.unpack(argumentCommandNode.getType()), identifier
            );
            i |= 2;
            if (identifier != null) {
               i |= 16;
            }
            break;
         case LiteralCommandNode<S> literalCommandNode:
            nodeStub = new ClientboundCommandsPacket.LiteralNodeStub(literalCommandNode.getLiteral());
            i |= 1;
            break;
         default:
            throw new UnsupportedOperationException("Unknown node type " + node);
      }

      int[] is = node.getChildren().stream().mapToInt(nodes::getInt).toArray();
      return new ClientboundCommandsPacket.Entry(nodeStub, i, j, is);
   }

   @Override
   public PacketType<ClientboundCommandsPacket> type() {
      return GamePacketTypes.CLIENTBOUND_COMMANDS;
   }

   public void handle(ClientGamePacketListener handler) {
      handler.handleCommands(this);
   }

   public <S> RootCommandNode<S> getRoot(CommandBuildContext context, ClientboundCommandsPacket.NodeBuilder<S> nodeBuilder) {
      return (RootCommandNode<S>)new ClientboundCommandsPacket.NodeResolver<>(context, nodeBuilder, this.entries).resolve(this.rootIndex);
   }

   record ArgumentNodeStub(String id, ArgumentTypeInfo.Template<?> argumentType, @Nullable Identifier suggestionId)
      implements ClientboundCommandsPacket.NodeStub {
      @Override
      public <S> ArgumentBuilder<S, ?> build(CommandBuildContext context, ClientboundCommandsPacket.NodeBuilder<S> nodeBuilder) {
         ArgumentType<?> argumentType = this.argumentType.instantiate(context);
         return nodeBuilder.createArgument(this.id, argumentType, this.suggestionId);
      }

      @Override
      public void write(FriendlyByteBuf buffer) {
         buffer.writeUtf(this.id);
         serializeCap(buffer, this.argumentType);
         if (this.suggestionId != null) {
            buffer.writeIdentifier(this.suggestionId);
         }
      }

      private static <A extends ArgumentType<?>> void serializeCap(FriendlyByteBuf buffer, ArgumentTypeInfo.Template<A> argumentInfoTemplate) {
         serializeCap(buffer, argumentInfoTemplate.type(), argumentInfoTemplate);
      }

      private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void serializeCap(
         FriendlyByteBuf buffer, ArgumentTypeInfo<A, T> argumentInfo, ArgumentTypeInfo.Template<A> argumentInfoTemplate
      ) {
         buffer.writeVarInt(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getId(argumentInfo));
         argumentInfo.serializeToNetwork((T)argumentInfoTemplate, buffer);
      }
   }

   record Entry(@Nullable ClientboundCommandsPacket.NodeStub stub, int flags, int redirect, int[] children) {

      public void write(FriendlyByteBuf buffer) {
         buffer.writeByte(this.flags);
         buffer.writeVarIntArray(this.children);
         if ((this.flags & 8) != 0) {
            buffer.writeVarInt(this.redirect);
         }

         if (this.stub != null) {
            this.stub.write(buffer);
         }
      }

      public boolean canBuild(IntSet children) {
         return (this.flags & 8) != 0 ? !children.contains(this.redirect) : true;
      }

      public boolean canResolve(IntSet children) {
         for (int i : this.children) {
            if (children.contains(i)) {
               return false;
            }
         }

         return true;
      }
   }

   record LiteralNodeStub(String id) implements ClientboundCommandsPacket.NodeStub {
      @Override
      public <S> ArgumentBuilder<S, ?> build(CommandBuildContext context, ClientboundCommandsPacket.NodeBuilder<S> nodeBuilder) {
         return nodeBuilder.createLiteral(this.id);
      }

      @Override
      public void write(FriendlyByteBuf buffer) {
         buffer.writeUtf(this.id);
      }
   }

   public interface NodeBuilder<S> {
      ArgumentBuilder<S, ?> createLiteral(String id);

      ArgumentBuilder<S, ?> createArgument(String id, ArgumentType<?> type, @Nullable Identifier suggestionId);

      ArgumentBuilder<S, ?> configure(ArgumentBuilder<S, ?> argumentBuilder, boolean executable, boolean restricted);
   }

   public interface NodeInspector<S> {
      @Nullable
      Identifier suggestionId(ArgumentCommandNode<S, ?> node);

      boolean isExecutable(CommandNode<S> node);

      boolean isRestricted(CommandNode<S> node);
   }

   static class NodeResolver<S> {
      private final CommandBuildContext context;
      private final ClientboundCommandsPacket.NodeBuilder<S> builder;
      private final List<ClientboundCommandsPacket.Entry> entries;
      private final List<CommandNode<S>> nodes;

      NodeResolver(CommandBuildContext context, ClientboundCommandsPacket.NodeBuilder<S> builder, List<ClientboundCommandsPacket.Entry> entries) {
         this.context = context;
         this.builder = builder;
         this.entries = entries;
         ObjectArrayList<CommandNode<S>> objectArrayList = new ObjectArrayList();
         objectArrayList.size(entries.size());
         this.nodes = objectArrayList;
      }

      public CommandNode<S> resolve(int index) {
         CommandNode<S> commandNode = this.nodes.get(index);
         if (commandNode != null) {
            return commandNode;
         } else {
            ClientboundCommandsPacket.Entry entry = this.entries.get(index);
            CommandNode<S> commandNode2;
            if (entry.stub == null) {
               commandNode2 = new RootCommandNode();
            } else {
               ArgumentBuilder<S, ?> argumentBuilder = entry.stub.build(this.context, this.builder);
               if ((entry.flags & 8) != 0) {
                  argumentBuilder.redirect(this.resolve(entry.redirect));
               }

               boolean bl = (entry.flags & 4) != 0;
               boolean bl2 = (entry.flags & 32) != 0;
               commandNode2 = this.builder.configure(argumentBuilder, bl, bl2).build();
            }

            this.nodes.set(index, commandNode2);

            for (int i : entry.children) {
               CommandNode<S> commandNode3 = this.resolve(i);
               if (!(commandNode3 instanceof RootCommandNode)) {
                  commandNode2.addChild(commandNode3);
               }
            }

            return commandNode2;
         }
      }
   }

   interface NodeStub {
      <S> ArgumentBuilder<S, ?> build(CommandBuildContext context, ClientboundCommandsPacket.NodeBuilder<S> nodeBuilder);

      void write(FriendlyByteBuf buffer);
   }
}
