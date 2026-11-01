package net.minecraft.network;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ByteProcessor;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class FriendlyByteBuf extends ByteBuf {
   private final ByteBuf source;
   public static final short MAX_STRING_LENGTH = 32767;
   public static final int MAX_COMPONENT_STRING_LENGTH = 262144;
   private static final int PUBLIC_KEY_SIZE = 256;
   private static final int MAX_PUBLIC_KEY_HEADER_SIZE = 256;
   private static final int MAX_PUBLIC_KEY_LENGTH = 512;
   private static final Gson GSON = new Gson();

   public FriendlyByteBuf(ByteBuf source) {
      this.source = source;
   }

   @Deprecated
   public <T> T readWithCodecTrusted(DynamicOps<Tag> ops, Codec<T> codec) {
      return this.readWithCodec(ops, codec, NbtAccounter.unlimitedHeap());
   }

   @Deprecated
   public <T> T readWithCodec(DynamicOps<Tag> ops, Codec<T> codec, NbtAccounter nbtAccounter) {
      Tag tag = this.readNbt(nbtAccounter);
      return (T)codec.parse(ops, tag).getOrThrow(string -> new DecoderException("Failed to decode: " + string + " " + tag));
   }

   @Deprecated
   public <T> FriendlyByteBuf writeWithCodec(DynamicOps<Tag> ops, Codec<T> codec, T value) {
      Tag tag = (Tag)codec.encodeStart(ops, value).getOrThrow(string -> new EncoderException("Failed to encode: " + string + " " + value));
      this.writeNbt(tag);
      return this;
   }

   public <T> T readLenientJsonWithCodec(Codec<T> codec) {
      JsonElement jsonElement = LenientJsonParser.parse(this.readUtf());
      DataResult<T> dataResult = codec.parse(JsonOps.INSTANCE, jsonElement);
      return (T)dataResult.getOrThrow(string -> new DecoderException("Failed to decode JSON: " + string));
   }

   public <T> void writeJsonWithCodec(Codec<T> codec, T value) {
      DataResult<JsonElement> dataResult = codec.encodeStart(JsonOps.INSTANCE, value);
      this.writeUtf(GSON.toJson((JsonElement)dataResult.getOrThrow(string -> new EncoderException("Failed to encode: " + string + " " + value))));
   }

   public static <T> IntFunction<T> limitValue(IntFunction<T> function, int limit) {
      return j -> {
         if (j > limit) {
            throw new DecoderException("Value " + j + " is larger than limit " + limit);
         } else {
            return function.apply(j);
         }
      };
   }

   public <T, C extends Collection<T>> C readCollection(IntFunction<C> collectionFactory, StreamDecoder<? super FriendlyByteBuf, T> elementReader) {
      int i = this.readVarInt();
      C collection = (C)collectionFactory.apply(i);

      for (int j = 0; j < i; j++) {
         collection.add(elementReader.decode(this));
      }

      return collection;
   }

   public <T> void writeCollection(Collection<T> collection, StreamEncoder<? super FriendlyByteBuf, T> elementWriter) {
      this.writeVarInt(collection.size());

      for (T object : collection) {
         elementWriter.encode(this, object);
      }
   }

   public <T> List<T> readList(StreamDecoder<? super FriendlyByteBuf, T> elementReader) {
      return this.readCollection(Lists::newArrayListWithCapacity, elementReader);
   }

   public IntList readIntIdList() {
      int i = this.readVarInt();
      IntList intList = new IntArrayList();

      for (int j = 0; j < i; j++) {
         intList.add(this.readVarInt());
      }

      return intList;
   }

   public void writeIntIdList(IntList itIdList) {
      this.writeVarInt(itIdList.size());
      itIdList.forEach(this::writeVarInt);
   }

   public <K, V, M extends Map<K, V>> M readMap(
      IntFunction<M> mapFactory, StreamDecoder<? super FriendlyByteBuf, K> keyReader, StreamDecoder<? super FriendlyByteBuf, V> valueReader
   ) {
      int i = this.readVarInt();
      M map = (M)mapFactory.apply(i);

      for (int j = 0; j < i; j++) {
         K object = keyReader.decode(this);
         V object2 = valueReader.decode(this);
         map.put(object, object2);
      }

      return map;
   }

   public <K, V> Map<K, V> readMap(StreamDecoder<? super FriendlyByteBuf, K> keyReader, StreamDecoder<? super FriendlyByteBuf, V> valueReader) {
      return this.readMap(Maps::newHashMapWithExpectedSize, keyReader, valueReader);
   }

   public <K, V> void writeMap(Map<K, V> map, StreamEncoder<? super FriendlyByteBuf, K> keyWriter, StreamEncoder<? super FriendlyByteBuf, V> valueWriter) {
      this.writeVarInt(map.size());
      map.forEach((object, object2) -> {
         keyWriter.encode(this, (K)object);
         valueWriter.encode(this, (V)object2);
      });
   }

   public void readWithCount(Consumer<FriendlyByteBuf> reader) {
      int i = this.readVarInt();

      for (int j = 0; j < i; j++) {
         reader.accept(this);
      }
   }

   public <E extends Enum<E>> void writeEnumSet(EnumSet<E> enumSet, Class<E> enumClass) {
      E[] enums = (E[])enumClass.getEnumConstants();
      BitSet bitSet = new BitSet(enums.length);

      for (int i = 0; i < enums.length; i++) {
         bitSet.set(i, enumSet.contains(enums[i]));
      }

      this.writeFixedBitSet(bitSet, enums.length);
   }

   public <E extends Enum<E>> EnumSet<E> readEnumSet(Class<E> enumClass) {
      E[] enums = (E[])enumClass.getEnumConstants();
      BitSet bitSet = this.readFixedBitSet(enums.length);
      EnumSet<E> enumSet = EnumSet.noneOf(enumClass);

      for (int i = 0; i < enums.length; i++) {
         if (bitSet.get(i)) {
            enumSet.add(enums[i]);
         }
      }

      return enumSet;
   }

   public <T> void writeOptional(Optional<T> optional, StreamEncoder<? super FriendlyByteBuf, T> writer) {
      if (optional.isPresent()) {
         this.writeBoolean(true);
         writer.encode(this, optional.get());
      } else {
         this.writeBoolean(false);
      }
   }

   public <T> Optional<T> readOptional(StreamDecoder<? super FriendlyByteBuf, T> reader) {
      return this.readBoolean() ? Optional.of(reader.decode(this)) : Optional.empty();
   }

   public <L, R> void writeEither(
      Either<L, R> either, StreamEncoder<? super FriendlyByteBuf, L> leftWriter, StreamEncoder<? super FriendlyByteBuf, R> rightWriter
   ) {
      either.ifLeft(object -> {
         this.writeBoolean(true);
         leftWriter.encode(this, (L)object);
      }).ifRight(object -> {
         this.writeBoolean(false);
         rightWriter.encode(this, (R)object);
      });
   }

   public <L, R> Either<L, R> readEither(StreamDecoder<? super FriendlyByteBuf, L> leftReader, StreamDecoder<? super FriendlyByteBuf, R> rightReader) {
      return this.readBoolean() ? Either.left(leftReader.decode(this)) : Either.right(rightReader.decode(this));
   }

   @Nullable
   public <T> T readNullable(StreamDecoder<? super FriendlyByteBuf, T> reader) {
      return readNullable(this, reader);
   }

   @Nullable
   public static <T, B extends ByteBuf> T readNullable(B buffer, StreamDecoder<? super B, T> reader) {
      return buffer.readBoolean() ? reader.decode(buffer) : null;
   }

   public <T> void writeNullable(@Nullable T value, StreamEncoder<? super FriendlyByteBuf, T> writer) {
      writeNullable(this, value, writer);
   }

   public static <T, B extends ByteBuf> void writeNullable(B buffer, @Nullable T value, StreamEncoder<? super B, T> writer) {
      if (value != null) {
         buffer.writeBoolean(true);
         writer.encode(buffer, value);
      } else {
         buffer.writeBoolean(false);
      }
   }

   public byte[] readByteArray() {
      return readByteArray(this);
   }

   public static byte[] readByteArray(ByteBuf buffer) {
      return readByteArray(buffer, buffer.readableBytes());
   }

   public FriendlyByteBuf writeByteArray(byte[] array) {
      writeByteArray(this, array);
      return this;
   }

   public static void writeByteArray(ByteBuf buffer, byte[] array) {
      VarInt.write(buffer, array.length);
      buffer.writeBytes(array);
   }

   public byte[] readByteArray(int maxLength) {
      return readByteArray(this, maxLength);
   }

   public static byte[] readByteArray(ByteBuf buffer, int maxSize) {
      int i = VarInt.read(buffer);
      if (i > maxSize) {
         throw new DecoderException("ByteArray with size " + i + " is bigger than allowed " + maxSize);
      } else {
         byte[] bs = new byte[i];
         buffer.readBytes(bs);
         return bs;
      }
   }

   public FriendlyByteBuf writeVarIntArray(int[] array) {
      this.writeVarInt(array.length);

      for (int i : array) {
         this.writeVarInt(i);
      }

      return this;
   }

   public int[] readVarIntArray() {
      return this.readVarIntArray(this.readableBytes());
   }

   public int[] readVarIntArray(int maxLength) {
      int i = this.readVarInt();
      if (i > maxLength) {
         throw new DecoderException("VarIntArray with size " + i + " is bigger than allowed " + maxLength);
      } else {
         int[] is = new int[i];

         for (int j = 0; j < is.length; j++) {
            is[j] = this.readVarInt();
         }

         return is;
      }
   }

   public FriendlyByteBuf writeLongArray(long[] array) {
      writeLongArray(this, array);
      return this;
   }

   public static void writeLongArray(ByteBuf buffer, long[] array) {
      VarInt.write(buffer, array.length);
      writeFixedSizeLongArray(buffer, array);
   }

   public FriendlyByteBuf writeFixedSizeLongArray(long[] array) {
      writeFixedSizeLongArray(this, array);
      return this;
   }

   public static void writeFixedSizeLongArray(ByteBuf buffer, long[] array) {
      for (long l : array) {
         buffer.writeLong(l);
      }
   }

   public long[] readLongArray() {
      return readLongArray(this);
   }

   public long[] readFixedSizeLongArray(long[] output) {
      return readFixedSizeLongArray(this, output);
   }

   public static long[] readLongArray(ByteBuf buffer) {
      int i = VarInt.read(buffer);
      int j = buffer.readableBytes() / 8;
      if (i > j) {
         throw new DecoderException("LongArray with size " + i + " is bigger than allowed " + j);
      } else {
         return readFixedSizeLongArray(buffer, new long[i]);
      }
   }

   public static long[] readFixedSizeLongArray(ByteBuf buffer, long[] output) {
      for (int i = 0; i < output.length; i++) {
         output[i] = buffer.readLong();
      }

      return output;
   }

   public BlockPos readBlockPos() {
      return readBlockPos(this);
   }

   public static BlockPos readBlockPos(ByteBuf buffer) {
      return BlockPos.of(buffer.readLong());
   }

   public FriendlyByteBuf writeBlockPos(BlockPos pos) {
      writeBlockPos(this, pos);
      return this;
   }

   public static void writeBlockPos(ByteBuf buffer, BlockPos pos) {
      buffer.writeLong(pos.asLong());
   }

   public ChunkPos readChunkPos() {
      return new ChunkPos(this.readLong());
   }

   public FriendlyByteBuf writeChunkPos(ChunkPos chunkPos) {
      this.writeLong(chunkPos.toLong());
      return this;
   }

   public static ChunkPos readChunkPos(ByteBuf buffer) {
      return new ChunkPos(buffer.readLong());
   }

   public static void writeChunkPos(ByteBuf buffer, ChunkPos chunkPos) {
      buffer.writeLong(chunkPos.toLong());
   }

   public GlobalPos readGlobalPos() {
      ResourceKey<Level> resourceKey = this.readResourceKey(Registries.DIMENSION);
      BlockPos blockPos = this.readBlockPos();
      return GlobalPos.of(resourceKey, blockPos);
   }

   public void writeGlobalPos(GlobalPos pos) {
      this.writeResourceKey(pos.dimension());
      this.writeBlockPos(pos.pos());
   }

   public Vector3f readVector3f() {
      return readVector3f(this);
   }

   public static Vector3f readVector3f(ByteBuf buffer) {
      return new Vector3f(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
   }

   public void writeVector3f(Vector3f vector3f) {
      writeVector3f(this, vector3f);
   }

   public static void writeVector3f(ByteBuf buffer, Vector3fc vector3f) {
      buffer.writeFloat(vector3f.x());
      buffer.writeFloat(vector3f.y());
      buffer.writeFloat(vector3f.z());
   }

   public Quaternionf readQuaternion() {
      return readQuaternion(this);
   }

   public static Quaternionf readQuaternion(ByteBuf buffer) {
      return new Quaternionf(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
   }

   public void writeQuaternion(Quaternionf quaternion) {
      writeQuaternion(this, quaternion);
   }

   public static void writeQuaternion(ByteBuf buffer, Quaternionfc quaternion) {
      buffer.writeFloat(quaternion.x());
      buffer.writeFloat(quaternion.y());
      buffer.writeFloat(quaternion.z());
      buffer.writeFloat(quaternion.w());
   }

   public static Vec3 readVec3(ByteBuf buffer) {
      return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
   }

   public Vec3 readVec3() {
      return readVec3(this);
   }

   public static void writeVec3(ByteBuf buffer, Vec3 vec3) {
      buffer.writeDouble(vec3.x());
      buffer.writeDouble(vec3.y());
      buffer.writeDouble(vec3.z());
   }

   public void writeVec3(Vec3 vec3) {
      writeVec3(this, vec3);
   }

   public Vec3 readLpVec3() {
      return LpVec3.read(this);
   }

   public void writeLpVec3(Vec3 vec3) {
      LpVec3.write(this, vec3);
   }

   public <T extends Enum<T>> T readEnum(Class<T> enumClass) {
      return enumClass.getEnumConstants()[this.readVarInt()];
   }

   public FriendlyByteBuf writeEnum(Enum<?> value) {
      return this.writeVarInt(value.ordinal());
   }

   public <T> T readById(IntFunction<T> idLookup) {
      int i = this.readVarInt();
      return idLookup.apply(i);
   }

   public <T> FriendlyByteBuf writeById(ToIntFunction<T> idGetter, T value) {
      int i = idGetter.applyAsInt(value);
      return this.writeVarInt(i);
   }

   public int readVarInt() {
      return VarInt.read(this.source);
   }

   public long readVarLong() {
      return VarLong.read(this.source);
   }

   public FriendlyByteBuf writeUUID(UUID uuid) {
      writeUUID(this, uuid);
      return this;
   }

   public static void writeUUID(ByteBuf buffer, UUID id) {
      buffer.writeLong(id.getMostSignificantBits());
      buffer.writeLong(id.getLeastSignificantBits());
   }

   public UUID readUUID() {
      return readUUID(this);
   }

   public static UUID readUUID(ByteBuf buffer) {
      return new UUID(buffer.readLong(), buffer.readLong());
   }

   public FriendlyByteBuf writeVarInt(int input) {
      VarInt.write(this.source, input);
      return this;
   }

   public FriendlyByteBuf writeVarLong(long value) {
      VarLong.write(this.source, value);
      return this;
   }

   public FriendlyByteBuf writeNbt(@Nullable Tag tag) {
      writeNbt(this, tag);
      return this;
   }

   public static void writeNbt(ByteBuf buffer, @Nullable Tag nbt) {
      if (nbt == null) {
         nbt = EndTag.INSTANCE;
      }

      try {
         NbtIo.writeAnyTag(nbt, new ByteBufOutputStream(buffer));
      } catch (IOException var3) {
         throw new EncoderException(var3);
      }
   }

   @Nullable
   public CompoundTag readNbt() {
      return readNbt(this);
   }

   @Nullable
   public static CompoundTag readNbt(ByteBuf buffer) {
      Tag tag = readNbt(buffer, NbtAccounter.defaultQuota());
      if (tag != null && !(tag instanceof CompoundTag)) {
         throw new DecoderException("Not a compound tag: " + tag);
      } else {
         return (CompoundTag)tag;
      }
   }

   @Nullable
   public static Tag readNbt(ByteBuf buffer, NbtAccounter nbtAccounter) {
      try {
         Tag tag = NbtIo.readAnyTag(new ByteBufInputStream(buffer), nbtAccounter);
         return tag.getId() == 0 ? null : tag;
      } catch (IOException var3) {
         throw new EncoderException(var3);
      }
   }

   @Nullable
   public Tag readNbt(NbtAccounter nbtAccounter) {
      return readNbt(this, nbtAccounter);
   }

   public String readUtf() {
      return this.readUtf(32767);
   }

   public String readUtf(int maxLength) {
      return Utf8String.read(this.source, maxLength);
   }

   public FriendlyByteBuf writeUtf(String string) {
      return this.writeUtf(string, 32767);
   }

   public FriendlyByteBuf writeUtf(String string, int maxLength) {
      Utf8String.write(this.source, string, maxLength);
      return this;
   }

   public Identifier readIdentifier() {
      return Identifier.parse(this.readUtf(32767));
   }

   public FriendlyByteBuf writeIdentifier(Identifier id) {
      this.writeUtf(id.toString());
      return this;
   }

   public <T> ResourceKey<T> readResourceKey(ResourceKey<? extends Registry<T>> registryKey) {
      Identifier identifier = this.readIdentifier();
      return ResourceKey.create(registryKey, identifier);
   }

   public void writeResourceKey(ResourceKey<?> resourceKey) {
      this.writeIdentifier(resourceKey.identifier());
   }

   public <T> ResourceKey<? extends Registry<T>> readRegistryKey() {
      Identifier identifier = this.readIdentifier();
      return ResourceKey.createRegistryKey(identifier);
   }

   public Instant readInstant() {
      return Instant.ofEpochMilli(this.readLong());
   }

   public void writeInstant(Instant instant) {
      this.writeLong(instant.toEpochMilli());
   }

   public PublicKey readPublicKey() {
      try {
         return Crypt.byteToPublicKey(this.readByteArray(512));
      } catch (CryptException var2) {
         throw new DecoderException("Malformed public key bytes", var2);
      }
   }

   public FriendlyByteBuf writePublicKey(PublicKey publicKey) {
      this.writeByteArray(publicKey.getEncoded());
      return this;
   }

   public BlockHitResult readBlockHitResult() {
      BlockPos blockPos = this.readBlockPos();
      Direction direction = this.readEnum(Direction.class);
      float f = this.readFloat();
      float g = this.readFloat();
      float h = this.readFloat();
      boolean bl = this.readBoolean();
      boolean bl2 = this.readBoolean();
      return new BlockHitResult(new Vec3((double)blockPos.getX() + f, (double)blockPos.getY() + g, (double)blockPos.getZ() + h), direction, blockPos, bl, bl2);
   }

   public void writeBlockHitResult(BlockHitResult result) {
      BlockPos blockPos = result.getBlockPos();
      this.writeBlockPos(blockPos);
      this.writeEnum(result.getDirection());
      Vec3 vec3 = result.getLocation();
      this.writeFloat((float)(vec3.x - blockPos.getX()));
      this.writeFloat((float)(vec3.y - blockPos.getY()));
      this.writeFloat((float)(vec3.z - blockPos.getZ()));
      this.writeBoolean(result.isInside());
      this.writeBoolean(result.isWorldBorderHit());
   }

   public BitSet readBitSet() {
      return BitSet.valueOf(this.readLongArray());
   }

   public void writeBitSet(BitSet bitSet) {
      this.writeLongArray(bitSet.toLongArray());
   }

   public BitSet readFixedBitSet(int size) {
      byte[] bs = new byte[Mth.positiveCeilDiv(size, 8)];
      this.readBytes(bs);
      return BitSet.valueOf(bs);
   }

   public void writeFixedBitSet(BitSet bitSet, int size) {
      if (bitSet.length() > size) {
         throw new EncoderException("BitSet is larger than expected size (" + bitSet.length() + ">" + size + ")");
      } else {
         byte[] bs = bitSet.toByteArray();
         this.writeBytes(Arrays.copyOf(bs, Mth.positiveCeilDiv(size, 8)));
      }
   }

   public static int readContainerId(ByteBuf buffer) {
      return VarInt.read(buffer);
   }

   public int readContainerId() {
      return readContainerId(this.source);
   }

   public static void writeContainerId(ByteBuf buffer, int containerId) {
      VarInt.write(buffer, containerId);
   }

   public void writeContainerId(int containerId) {
      writeContainerId(this.source, containerId);
   }

   public boolean isContiguous() {
      return this.source.isContiguous();
   }

   public int maxFastWritableBytes() {
      return this.source.maxFastWritableBytes();
   }

   public int capacity() {
      return this.source.capacity();
   }

   public FriendlyByteBuf capacity(int newCapacity) {
      this.source.capacity(newCapacity);
      return this;
   }

   public int maxCapacity() {
      return this.source.maxCapacity();
   }

   public ByteBufAllocator alloc() {
      return this.source.alloc();
   }

   public ByteOrder order() {
      return this.source.order();
   }

   public ByteBuf order(ByteOrder byteOrder) {
      return this.source.order(byteOrder);
   }

   public ByteBuf unwrap() {
      return this.source;
   }

   public boolean isDirect() {
      return this.source.isDirect();
   }

   public boolean isReadOnly() {
      return this.source.isReadOnly();
   }

   public ByteBuf asReadOnly() {
      return this.source.asReadOnly();
   }

   public int readerIndex() {
      return this.source.readerIndex();
   }

   public FriendlyByteBuf readerIndex(int readerIndex) {
      this.source.readerIndex(readerIndex);
      return this;
   }

   public int writerIndex() {
      return this.source.writerIndex();
   }

   public FriendlyByteBuf writerIndex(int writerIndex) {
      this.source.writerIndex(writerIndex);
      return this;
   }

   public FriendlyByteBuf setIndex(int readerIndex, int writerIndex) {
      this.source.setIndex(readerIndex, writerIndex);
      return this;
   }

   public int readableBytes() {
      return this.source.readableBytes();
   }

   public int writableBytes() {
      return this.source.writableBytes();
   }

   public int maxWritableBytes() {
      return this.source.maxWritableBytes();
   }

   public boolean isReadable() {
      return this.source.isReadable();
   }

   public boolean isReadable(int i) {
      return this.source.isReadable(i);
   }

   public boolean isWritable() {
      return this.source.isWritable();
   }

   public boolean isWritable(int i) {
      return this.source.isWritable(i);
   }

   public FriendlyByteBuf clear() {
      this.source.clear();
      return this;
   }

   public FriendlyByteBuf markReaderIndex() {
      this.source.markReaderIndex();
      return this;
   }

   public FriendlyByteBuf resetReaderIndex() {
      this.source.resetReaderIndex();
      return this;
   }

   public FriendlyByteBuf markWriterIndex() {
      this.source.markWriterIndex();
      return this;
   }

   public FriendlyByteBuf resetWriterIndex() {
      this.source.resetWriterIndex();
      return this;
   }

   public FriendlyByteBuf discardReadBytes() {
      this.source.discardReadBytes();
      return this;
   }

   public FriendlyByteBuf discardSomeReadBytes() {
      this.source.discardSomeReadBytes();
      return this;
   }

   public FriendlyByteBuf ensureWritable(int size) {
      this.source.ensureWritable(size);
      return this;
   }

   public int ensureWritable(int i, boolean bl) {
      return this.source.ensureWritable(i, bl);
   }

   public boolean getBoolean(int i) {
      return this.source.getBoolean(i);
   }

   public byte getByte(int i) {
      return this.source.getByte(i);
   }

   public short getUnsignedByte(int i) {
      return this.source.getUnsignedByte(i);
   }

   public short getShort(int i) {
      return this.source.getShort(i);
   }

   public short getShortLE(int i) {
      return this.source.getShortLE(i);
   }

   public int getUnsignedShort(int i) {
      return this.source.getUnsignedShort(i);
   }

   public int getUnsignedShortLE(int i) {
      return this.source.getUnsignedShortLE(i);
   }

   public int getMedium(int i) {
      return this.source.getMedium(i);
   }

   public int getMediumLE(int i) {
      return this.source.getMediumLE(i);
   }

   public int getUnsignedMedium(int i) {
      return this.source.getUnsignedMedium(i);
   }

   public int getUnsignedMediumLE(int i) {
      return this.source.getUnsignedMediumLE(i);
   }

   public int getInt(int i) {
      return this.source.getInt(i);
   }

   public int getIntLE(int i) {
      return this.source.getIntLE(i);
   }

   public long getUnsignedInt(int i) {
      return this.source.getUnsignedInt(i);
   }

   public long getUnsignedIntLE(int i) {
      return this.source.getUnsignedIntLE(i);
   }

   public long getLong(int i) {
      return this.source.getLong(i);
   }

   public long getLongLE(int i) {
      return this.source.getLongLE(i);
   }

   public char getChar(int i) {
      return this.source.getChar(i);
   }

   public float getFloat(int i) {
      return this.source.getFloat(i);
   }

   public double getDouble(int i) {
      return this.source.getDouble(i);
   }

   public FriendlyByteBuf getBytes(int index, ByteBuf destination) {
      this.source.getBytes(index, destination);
      return this;
   }

   public FriendlyByteBuf getBytes(int index, ByteBuf destination, int length) {
      this.source.getBytes(index, destination, length);
      return this;
   }

   public FriendlyByteBuf getBytes(int index, ByteBuf destination, int destinationIndex, int length) {
      this.source.getBytes(index, destination, destinationIndex, length);
      return this;
   }

   public FriendlyByteBuf getBytes(int index, byte[] destination) {
      this.source.getBytes(index, destination);
      return this;
   }

   public FriendlyByteBuf getBytes(int index, byte[] destination, int destinationIndex, int length) {
      this.source.getBytes(index, destination, destinationIndex, length);
      return this;
   }

   public FriendlyByteBuf getBytes(int index, ByteBuffer destination) {
      this.source.getBytes(index, destination);
      return this;
   }

   public FriendlyByteBuf getBytes(int index, OutputStream out, int length) throws IOException {
      this.source.getBytes(index, out, length);
      return this;
   }

   public int getBytes(int i, GatheringByteChannel gatheringByteChannel, int j) throws IOException {
      return this.source.getBytes(i, gatheringByteChannel, j);
   }

   public int getBytes(int i, FileChannel fileChannel, long l, int j) throws IOException {
      return this.source.getBytes(i, fileChannel, l, j);
   }

   public CharSequence getCharSequence(int i, int j, Charset charset) {
      return this.source.getCharSequence(i, j, charset);
   }

   public FriendlyByteBuf setBoolean(int index, boolean value) {
      this.source.setBoolean(index, value);
      return this;
   }

   public FriendlyByteBuf setByte(int index, int value) {
      this.source.setByte(index, value);
      return this;
   }

   public FriendlyByteBuf setShort(int index, int value) {
      this.source.setShort(index, value);
      return this;
   }

   public FriendlyByteBuf setShortLE(int index, int value) {
      this.source.setShortLE(index, value);
      return this;
   }

   public FriendlyByteBuf setMedium(int index, int value) {
      this.source.setMedium(index, value);
      return this;
   }

   public FriendlyByteBuf setMediumLE(int index, int value) {
      this.source.setMediumLE(index, value);
      return this;
   }

   public FriendlyByteBuf setInt(int index, int value) {
      this.source.setInt(index, value);
      return this;
   }

   public FriendlyByteBuf setIntLE(int index, int value) {
      this.source.setIntLE(index, value);
      return this;
   }

   public FriendlyByteBuf setLong(int index, long value) {
      this.source.setLong(index, value);
      return this;
   }

   public FriendlyByteBuf setLongLE(int index, long value) {
      this.source.setLongLE(index, value);
      return this;
   }

   public FriendlyByteBuf setChar(int index, int value) {
      this.source.setChar(index, value);
      return this;
   }

   public FriendlyByteBuf setFloat(int index, float value) {
      this.source.setFloat(index, value);
      return this;
   }

   public FriendlyByteBuf setDouble(int index, double value) {
      this.source.setDouble(index, value);
      return this;
   }

   public FriendlyByteBuf setBytes(int index, ByteBuf source) {
      this.source.setBytes(index, source);
      return this;
   }

   public FriendlyByteBuf setBytes(int index, ByteBuf source, int length) {
      this.source.setBytes(index, source, length);
      return this;
   }

   public FriendlyByteBuf setBytes(int index, ByteBuf source, int sourceIndex, int length) {
      this.source.setBytes(index, source, sourceIndex, length);
      return this;
   }

   public FriendlyByteBuf setBytes(int index, byte[] source) {
      this.source.setBytes(index, source);
      return this;
   }

   public FriendlyByteBuf setBytes(int index, byte[] source, int sourceIndex, int length) {
      this.source.setBytes(index, source, sourceIndex, length);
      return this;
   }

   public FriendlyByteBuf setBytes(int index, ByteBuffer source) {
      this.source.setBytes(index, source);
      return this;
   }

   public int setBytes(int i, InputStream inputStream, int j) throws IOException {
      return this.source.setBytes(i, inputStream, j);
   }

   public int setBytes(int i, ScatteringByteChannel scatteringByteChannel, int j) throws IOException {
      return this.source.setBytes(i, scatteringByteChannel, j);
   }

   public int setBytes(int i, FileChannel fileChannel, long l, int j) throws IOException {
      return this.source.setBytes(i, fileChannel, l, j);
   }

   public FriendlyByteBuf setZero(int index, int length) {
      this.source.setZero(index, length);
      return this;
   }

   public int setCharSequence(int i, CharSequence charSequence, Charset charset) {
      return this.source.setCharSequence(i, charSequence, charset);
   }

   public boolean readBoolean() {
      return this.source.readBoolean();
   }

   public byte readByte() {
      return this.source.readByte();
   }

   public short readUnsignedByte() {
      return this.source.readUnsignedByte();
   }

   public short readShort() {
      return this.source.readShort();
   }

   public short readShortLE() {
      return this.source.readShortLE();
   }

   public int readUnsignedShort() {
      return this.source.readUnsignedShort();
   }

   public int readUnsignedShortLE() {
      return this.source.readUnsignedShortLE();
   }

   public int readMedium() {
      return this.source.readMedium();
   }

   public int readMediumLE() {
      return this.source.readMediumLE();
   }

   public int readUnsignedMedium() {
      return this.source.readUnsignedMedium();
   }

   public int readUnsignedMediumLE() {
      return this.source.readUnsignedMediumLE();
   }

   public int readInt() {
      return this.source.readInt();
   }

   public int readIntLE() {
      return this.source.readIntLE();
   }

   public long readUnsignedInt() {
      return this.source.readUnsignedInt();
   }

   public long readUnsignedIntLE() {
      return this.source.readUnsignedIntLE();
   }

   public long readLong() {
      return this.source.readLong();
   }

   public long readLongLE() {
      return this.source.readLongLE();
   }

   public char readChar() {
      return this.source.readChar();
   }

   public float readFloat() {
      return this.source.readFloat();
   }

   public double readDouble() {
      return this.source.readDouble();
   }

   public ByteBuf readBytes(int i) {
      return this.source.readBytes(i);
   }

   public ByteBuf readSlice(int i) {
      return this.source.readSlice(i);
   }

   public ByteBuf readRetainedSlice(int i) {
      return this.source.readRetainedSlice(i);
   }

   public FriendlyByteBuf readBytes(ByteBuf destination) {
      this.source.readBytes(destination);
      return this;
   }

   public FriendlyByteBuf readBytes(ByteBuf destination, int length) {
      this.source.readBytes(destination, length);
      return this;
   }

   public FriendlyByteBuf readBytes(ByteBuf destination, int destinationIndex, int length) {
      this.source.readBytes(destination, destinationIndex, length);
      return this;
   }

   public FriendlyByteBuf readBytes(byte[] destination) {
      this.source.readBytes(destination);
      return this;
   }

   public FriendlyByteBuf readBytes(byte[] destination, int destinationIndex, int length) {
      this.source.readBytes(destination, destinationIndex, length);
      return this;
   }

   public FriendlyByteBuf readBytes(ByteBuffer destination) {
      this.source.readBytes(destination);
      return this;
   }

   public FriendlyByteBuf readBytes(OutputStream out, int length) throws IOException {
      this.source.readBytes(out, length);
      return this;
   }

   public int readBytes(GatheringByteChannel gatheringByteChannel, int i) throws IOException {
      return this.source.readBytes(gatheringByteChannel, i);
   }

   public CharSequence readCharSequence(int i, Charset charset) {
      return this.source.readCharSequence(i, charset);
   }

   public String readString(int i, Charset charset) {
      return this.source.readString(i, charset);
   }

   public int readBytes(FileChannel fileChannel, long l, int i) throws IOException {
      return this.source.readBytes(fileChannel, l, i);
   }

   public FriendlyByteBuf skipBytes(int length) {
      this.source.skipBytes(length);
      return this;
   }

   public FriendlyByteBuf writeBoolean(boolean value) {
      this.source.writeBoolean(value);
      return this;
   }

   public FriendlyByteBuf writeByte(int value) {
      this.source.writeByte(value);
      return this;
   }

   public FriendlyByteBuf writeShort(int value) {
      this.source.writeShort(value);
      return this;
   }

   public FriendlyByteBuf writeShortLE(int value) {
      this.source.writeShortLE(value);
      return this;
   }

   public FriendlyByteBuf writeMedium(int value) {
      this.source.writeMedium(value);
      return this;
   }

   public FriendlyByteBuf writeMediumLE(int value) {
      this.source.writeMediumLE(value);
      return this;
   }

   public FriendlyByteBuf writeInt(int value) {
      this.source.writeInt(value);
      return this;
   }

   public FriendlyByteBuf writeIntLE(int value) {
      this.source.writeIntLE(value);
      return this;
   }

   public FriendlyByteBuf writeLong(long value) {
      this.source.writeLong(value);
      return this;
   }

   public FriendlyByteBuf writeLongLE(long value) {
      this.source.writeLongLE(value);
      return this;
   }

   public FriendlyByteBuf writeChar(int value) {
      this.source.writeChar(value);
      return this;
   }

   public FriendlyByteBuf writeFloat(float value) {
      this.source.writeFloat(value);
      return this;
   }

   public FriendlyByteBuf writeDouble(double value) {
      this.source.writeDouble(value);
      return this;
   }

   public FriendlyByteBuf writeBytes(ByteBuf source) {
      this.source.writeBytes(source);
      return this;
   }

   public FriendlyByteBuf writeBytes(ByteBuf source, int length) {
      this.source.writeBytes(source, length);
      return this;
   }

   public FriendlyByteBuf writeBytes(ByteBuf source, int sourceIndex, int length) {
      this.source.writeBytes(source, sourceIndex, length);
      return this;
   }

   public FriendlyByteBuf writeBytes(byte[] source) {
      this.source.writeBytes(source);
      return this;
   }

   public FriendlyByteBuf writeBytes(byte[] source, int sourceIndex, int length) {
      this.source.writeBytes(source, sourceIndex, length);
      return this;
   }

   public FriendlyByteBuf writeBytes(ByteBuffer source) {
      this.source.writeBytes(source);
      return this;
   }

   public int writeBytes(InputStream inputStream, int i) throws IOException {
      return this.source.writeBytes(inputStream, i);
   }

   public int writeBytes(ScatteringByteChannel scatteringByteChannel, int i) throws IOException {
      return this.source.writeBytes(scatteringByteChannel, i);
   }

   public int writeBytes(FileChannel fileChannel, long l, int i) throws IOException {
      return this.source.writeBytes(fileChannel, l, i);
   }

   public FriendlyByteBuf writeZero(int length) {
      this.source.writeZero(length);
      return this;
   }

   public int writeCharSequence(CharSequence charSequence, Charset charset) {
      return this.source.writeCharSequence(charSequence, charset);
   }

   public int indexOf(int i, int j, byte b) {
      return this.source.indexOf(i, j, b);
   }

   public int bytesBefore(byte b) {
      return this.source.bytesBefore(b);
   }

   public int bytesBefore(int i, byte b) {
      return this.source.bytesBefore(i, b);
   }

   public int bytesBefore(int i, int j, byte b) {
      return this.source.bytesBefore(i, j, b);
   }

   public int forEachByte(ByteProcessor byteProcessor) {
      return this.source.forEachByte(byteProcessor);
   }

   public int forEachByte(int i, int j, ByteProcessor byteProcessor) {
      return this.source.forEachByte(i, j, byteProcessor);
   }

   public int forEachByteDesc(ByteProcessor byteProcessor) {
      return this.source.forEachByteDesc(byteProcessor);
   }

   public int forEachByteDesc(int i, int j, ByteProcessor byteProcessor) {
      return this.source.forEachByteDesc(i, j, byteProcessor);
   }

   public ByteBuf copy() {
      return this.source.copy();
   }

   public ByteBuf copy(int i, int j) {
      return this.source.copy(i, j);
   }

   public ByteBuf slice() {
      return this.source.slice();
   }

   public ByteBuf retainedSlice() {
      return this.source.retainedSlice();
   }

   public ByteBuf slice(int i, int j) {
      return this.source.slice(i, j);
   }

   public ByteBuf retainedSlice(int i, int j) {
      return this.source.retainedSlice(i, j);
   }

   public ByteBuf duplicate() {
      return this.source.duplicate();
   }

   public ByteBuf retainedDuplicate() {
      return this.source.retainedDuplicate();
   }

   public int nioBufferCount() {
      return this.source.nioBufferCount();
   }

   public ByteBuffer nioBuffer() {
      return this.source.nioBuffer();
   }

   public ByteBuffer nioBuffer(int i, int j) {
      return this.source.nioBuffer(i, j);
   }

   public ByteBuffer internalNioBuffer(int i, int j) {
      return this.source.internalNioBuffer(i, j);
   }

   public ByteBuffer[] nioBuffers() {
      return this.source.nioBuffers();
   }

   public ByteBuffer[] nioBuffers(int i, int j) {
      return this.source.nioBuffers(i, j);
   }

   public boolean hasArray() {
      return this.source.hasArray();
   }

   public byte[] array() {
      return this.source.array();
   }

   public int arrayOffset() {
      return this.source.arrayOffset();
   }

   public boolean hasMemoryAddress() {
      return this.source.hasMemoryAddress();
   }

   public long memoryAddress() {
      return this.source.memoryAddress();
   }

   public String toString(Charset charset) {
      return this.source.toString(charset);
   }

   public String toString(int i, int j, Charset charset) {
      return this.source.toString(i, j, charset);
   }

   public int hashCode() {
      return this.source.hashCode();
   }

   public boolean equals(Object object) {
      return this.source.equals(object);
   }

   public int compareTo(ByteBuf byteBuf) {
      return this.source.compareTo(byteBuf);
   }

   public String toString() {
      return this.source.toString();
   }

   public FriendlyByteBuf retain(int increment) {
      this.source.retain(increment);
      return this;
   }

   public FriendlyByteBuf retain() {
      this.source.retain();
      return this;
   }

   public FriendlyByteBuf touch() {
      this.source.touch();
      return this;
   }

   public FriendlyByteBuf touch(Object hint) {
      this.source.touch(hint);
      return this;
   }

   public int refCnt() {
      return this.source.refCnt();
   }

   public boolean release() {
      return this.source.release();
   }

   public boolean release(int i) {
      return this.source.release(i);
   }
}
