package io.bugcourier.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RedactorTest {
    private final Redactor redactor = new Redactor();

    @Test
    void hidesLauncherTokens() {
        String out = redactor.apply("args: --username Alex --accessToken abc.def123 --version 1.21.1");
        assertFalse(out.contains("abc.def123"));
        assertTrue(out.contains("--accessToken <hidden>"));
        assertTrue(out.contains("--version 1.21.1"));
    }

    @Test
    void hidesAssignedSecretsAndJwts() {
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U";
        String out = redactor.apply("session_id=deadbeef1234 token " + jwt);
        assertFalse(out.contains("deadbeef1234"));
        assertFalse(out.contains(jwt));
    }

    @Test
    void hidesHomeFolderNames() {
        assertEquals("C:\\Users\\<user>\\AppData\\Roaming\\.minecraft", redactor.apply("C:\\Users\\alex\\AppData\\Roaming\\.minecraft"));
        assertEquals("/Users/<user>/Library/Application Support", redactor.apply("/Users/alex/Library/Application Support"));
        assertEquals("/home/<user>/.minecraft/logs", redactor.apply("/home/alex/.minecraft/logs"));
    }

    @Test
    void hidesEmailsAndAddressesButKeepsVersions() {
        String out = redactor.apply("joined 203.0.113.42:25565 as a@b.com on NeoForge 21.1.259, Java 21.0.3, local 127.0.0.1");
        assertTrue(out.contains("<ip>"));
        assertFalse(out.contains("203.0.113.42"));
        assertTrue(out.contains("<email>"));
        assertTrue(out.contains("21.1.259"));
        assertTrue(out.contains("21.0.3"));
        assertTrue(out.contains("127.0.0.1"));
    }

    @Test
    void hidesPlayerNameWhenAsked() {
        String out = new Redactor().hide("Notch").hide("069a79f4-44e9-4726-a5be-fca90e38aaf5")
                .apply("Setting user: Notch, uuid 069a79f4-44e9-4726-a5be-fca90e38aaf5");
        assertEquals("Setting user: <hidden>, uuid <hidden>", out);
    }

    @Test
    void playerNameOnlyMatchesWholeWords() {
        String out = new Redactor().hide("Dev").apply("[Render thread/INFO]: Dev joined on device /dev/dri, dev_mode=on");
        assertEquals("[Render thread/INFO]: <hidden> joined on device /<hidden>/dri, dev_mode=on", out);
    }

    @Test
    void keepsFourPartModVersions() {
        Redactor r = new Redactor().keep("1.0.0.0").keep("mc1.21.1-2.3.1.4");
        String out = r.apply("somemod 1.0.0.0  (somemod.jar)\nother 2.3.1.4\nconnecting to 203.0.113.42");
        assertTrue(out.contains("somemod 1.0.0.0"));
        assertTrue(out.contains("other 2.3.1.4"));
        assertFalse(out.contains("203.0.113.42"));
    }
}
