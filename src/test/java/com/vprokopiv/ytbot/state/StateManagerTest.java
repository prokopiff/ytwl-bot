package com.vprokopiv.ytbot.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StateManagerTest {

    @Autowired
    private StateManager stateManager;

    @Test
    void setAndGetState() {
        stateManager.setState("testkey", "testval");

        var state = stateManager.getState("testkey").get();

        assertEquals("testval", state);
    }

    @Test
    void setTwice() {
        stateManager.setState("testkey", "testval");
        var state1 = stateManager.getState("testkey").get();
        stateManager.setState("testkey", "newval");
        var state2 = stateManager.getState("testkey").get();

        assertEquals("testval", state1);
        assertEquals("newval", state2);
    }

    @Test
    void getAndSetComplex() throws JsonProcessingException {
        Map<String, Serializable> values = new HashMap<>();
        values.put("one", "two");
        var l = new LinkedList<>(List.of("four"));
        values.put("three", l);
        stateManager.setState("key", values);
        var saved = stateManager.getState("key", new TypeReference<Map<String, Object>>() {}).get();

        assertEquals(values, saved);
    }
}
