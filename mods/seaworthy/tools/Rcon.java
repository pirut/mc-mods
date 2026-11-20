import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** A minimal RCON client for float-check.sh: {@code java Rcon.java <port> <password> <command>...}. */
public class Rcon {
    private static void send(DataOutputStream out, int id, int type, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        ByteBuffer packet = ByteBuffer.allocate(14 + body.length).order(ByteOrder.LITTLE_ENDIAN);
        packet.putInt(10 + body.length).putInt(id).putInt(type).put(body).put((byte) 0).put((byte) 0);
        out.write(packet.array());
        out.flush();
    }

    private static String receive(DataInputStream in) throws IOException {
        byte[] head = new byte[4];
        in.readFully(head);
        int length = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN).getInt();
        byte[] body = new byte[length];
        in.readFully(body);
        return new String(body, 8, length - 10, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", Integer.parseInt(args[0]))) {
            socket.setSoTimeout(60_000);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            send(out, 1, 3, args[1]);
            receive(in);
            for (int i = 2; i < args.length; i++) {
                send(out, 2, 2, args[i]);
                System.out.println(receive(in));
            }
        }
    }
}
