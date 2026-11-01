package net.minecraft.resources;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import java.util.function.UnaryOperator;
import net.minecraft.IdentifierException;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

public final class Identifier implements Comparable<Identifier> {
   public static final Codec<Identifier> CODEC = Codec.STRING.comapFlatMap(Identifier::read, Identifier::toString).stable();
   public static final StreamCodec<ByteBuf, Identifier> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(Identifier::parse, Identifier::toString);
   public static final SimpleCommandExceptionType ERROR_INVALID = new SimpleCommandExceptionType(Component.translatable("argument.id.invalid"));
   public static final char NAMESPACE_SEPARATOR = ':';
   public static final String DEFAULT_NAMESPACE = "minecraft";
   public static final String REALMS_NAMESPACE = "realms";
   private final String namespace;
   private final String path;

   private Identifier(String namespace, String path) {
      assert isValidNamespace(namespace);

      assert isValidPath(path);

      this.namespace = namespace;
      this.path = path;
   }

   private static Identifier createUntrusted(String namespace, String path) {
      return new Identifier(assertValidNamespace(namespace, path), assertValidPath(namespace, path));
   }

   public static Identifier fromNamespaceAndPath(String namespace, String path) {
      return createUntrusted(namespace, path);
   }

   public static Identifier parse(String location) {
      return bySeparator(location, ':');
   }

   public static Identifier withDefaultNamespace(String location) {
      return new Identifier("minecraft", assertValidPath("minecraft", location));
   }

   @Nullable
   public static Identifier tryParse(String location) {
      return tryBySeparator(location, ':');
   }

   @Nullable
   public static Identifier tryBuild(String namespace, String path) {
      return isValidNamespace(namespace) && isValidPath(path) ? new Identifier(namespace, path) : null;
   }

   public static Identifier bySeparator(String location, char separator) {
      int i = location.indexOf(separator);
      if (i >= 0) {
         String string = location.substring(i + 1);
         if (i != 0) {
            String string2 = location.substring(0, i);
            return createUntrusted(string2, string);
         } else {
            return withDefaultNamespace(string);
         }
      } else {
         return withDefaultNamespace(location);
      }
   }

   @Nullable
   public static Identifier tryBySeparator(String location, char seperator) {
      int i = location.indexOf(seperator);
      if (i >= 0) {
         String string = location.substring(i + 1);
         if (!isValidPath(string)) {
            return null;
         } else if (i != 0) {
            String string2 = location.substring(0, i);
            return isValidNamespace(string2) ? new Identifier(string2, string) : null;
         } else {
            return new Identifier("minecraft", string);
         }
      } else {
         return isValidPath(location) ? new Identifier("minecraft", location) : null;
      }
   }

   public static DataResult<Identifier> read(String location) {
      try {
         return DataResult.success(parse(location));
      } catch (IdentifierException var2) {
         return DataResult.error(() -> "Not a valid resource location: " + location + " " + var2.getMessage());
      }
   }

   public String getPath() {
      return this.path;
   }

   public String getNamespace() {
      return this.namespace;
   }

   public Identifier withPath(String path) {
      return new Identifier(this.namespace, assertValidPath(this.namespace, path));
   }

   public Identifier withPath(UnaryOperator<String> pathOperator) {
      return this.withPath(pathOperator.apply(this.path));
   }

   public Identifier withPrefix(String pathPrefix) {
      return this.withPath(pathPrefix + this.path);
   }

   public Identifier withSuffix(String pathSuffix) {
      return this.withPath(this.path + pathSuffix);
   }

   @Override
   public String toString() {
      return this.namespace + ":" + this.path;
   }

   @Override
   public boolean equals(Object object) {
      if (this == object) {
         return true;
      } else {
         return !(object instanceof Identifier identifier) ? false : this.namespace.equals(identifier.namespace) && this.path.equals(identifier.path);
      }
   }

   @Override
   public int hashCode() {
      return 31 * this.namespace.hashCode() + this.path.hashCode();
   }

   public int compareTo(Identifier other) {
      int i = this.path.compareTo(other.path);
      if (i == 0) {
         i = this.namespace.compareTo(other.namespace);
      }

      return i;
   }

   public String toDebugFileName() {
      return this.toString().replace('/', '_').replace(':', '_');
   }

   public String toLanguageKey() {
      return this.namespace + "." + this.path;
   }

   public String toShortLanguageKey() {
      return this.namespace.equals("minecraft") ? this.path : this.toLanguageKey();
   }

   public String toShortString() {
      return this.namespace.equals("minecraft") ? this.path : this.toString();
   }

   public String toLanguageKey(String type) {
      return type + "." + this.toLanguageKey();
   }

   public String toLanguageKey(String type, String key) {
      return type + "." + this.toLanguageKey() + "." + key;
   }

   private static String readGreedy(StringReader reader) {
      int i = reader.getCursor();

      while (reader.canRead() && isAllowedInIdentifier(reader.peek())) {
         reader.skip();
      }

      return reader.getString().substring(i, reader.getCursor());
   }

   public static Identifier read(StringReader reader) throws CommandSyntaxException {
      int i = reader.getCursor();
      String string = readGreedy(reader);

      try {
         return parse(string);
      } catch (IdentifierException var4) {
         reader.setCursor(i);
         throw ERROR_INVALID.createWithContext(reader);
      }
   }

   public static Identifier readNonEmpty(StringReader reader) throws CommandSyntaxException {
      int i = reader.getCursor();
      String string = readGreedy(reader);
      if (string.isEmpty()) {
         throw ERROR_INVALID.createWithContext(reader);
      } else {
         try {
            return parse(string);
         } catch (IdentifierException var4) {
            reader.setCursor(i);
            throw ERROR_INVALID.createWithContext(reader);
         }
      }
   }

   public static boolean isAllowedInIdentifier(char c) {
      return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c == '_' || c == ':' || c == '/' || c == '.' || c == '-';
   }

   public static boolean isValidPath(String path) {
      for (int i = 0; i < path.length(); i++) {
         if (!validPathChar(path.charAt(i))) {
            return false;
         }
      }

      return true;
   }

   public static boolean isValidNamespace(String namespace) {
      for (int i = 0; i < namespace.length(); i++) {
         if (!validNamespaceChar(namespace.charAt(i))) {
            return false;
         }
      }

      return true;
   }

   private static String assertValidNamespace(String namespace, String path) {
      if (!isValidNamespace(namespace)) {
         throw new IdentifierException("Non [a-z0-9_.-] character in namespace of location: " + namespace + ":" + path);
      } else {
         return namespace;
      }
   }

   public static boolean validPathChar(char pathChar) {
      return pathChar == '_'
         || pathChar == '-'
         || pathChar >= 'a' && pathChar <= 'z'
         || pathChar >= '0' && pathChar <= '9'
         || pathChar == '/'
         || pathChar == '.';
   }

   private static boolean validNamespaceChar(char namespaceChar) {
      return namespaceChar == '_'
         || namespaceChar == '-'
         || namespaceChar >= 'a' && namespaceChar <= 'z'
         || namespaceChar >= '0' && namespaceChar <= '9'
         || namespaceChar == '.';
   }

   private static String assertValidPath(String namespace, String path) {
      if (!isValidPath(path)) {
         throw new IdentifierException("Non [a-z0-9/._-] character in path of location: " + namespace + ":" + path);
      } else {
         return path;
      }
   }
}
