package dev.nandobez.ristretto.core;

import dev.nandobez.ristretto.core.ToolRegistry.Tool;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PassThroughTest {

    @Test
    void routesJdpVerbs() {
        assertEquals(Tool.JDP, PassThrough.routeOf("add"));
        assertEquals(Tool.JDP, PassThrough.routeOf("why"));
    }

    @Test
    void routesXpressoVerbs() {
        assertEquals(Tool.XPRESSO, PassThrough.routeOf("g"));
        assertEquals(Tool.XPRESSO, PassThrough.routeOf("routes"));
    }

    @Test
    void routesMaccVerbs() {
        assertEquals(Tool.MACC, PassThrough.routeOf("codegen"));
        assertEquals(Tool.MACC, PassThrough.routeOf("dev"));
    }

    @Test
    void isCaseInsensitive() {
        assertEquals(Tool.JDP, PassThrough.routeOf("ADD"));
    }

    @Test
    void returnsNullForUnknownVerb() {
        assertNull(PassThrough.routeOf("frobnicate"));
    }
}
