package net.basov.lws;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServerHandlerTest {
    @Test
    void testFileName2URL() {
        String name2URL = ServerHandler.fileName2URL("some-file name.png");
        assertEquals("some-file%20name.png", name2URL);
    }
}