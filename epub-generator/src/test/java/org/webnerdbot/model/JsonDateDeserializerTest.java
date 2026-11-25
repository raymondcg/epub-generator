package org.webnerdbot.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class JsonDateDeserializerTest {

    private final ObjectMapper mapper;

    public JsonDateDeserializerTest() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer( LocalDate.class, new JsonDateDeserializer() );
        mapper.registerModule( module );
    }

    @Test
    void parsesDateString() throws Exception {
        assertEquals( LocalDate.of( 2024, 1, 15 ), mapper.readValue( "\"2024-01-15\"", LocalDate.class ) );
    }

    @Test
    void nullReturnsNull() throws Exception {
        assertNull( mapper.readValue( "null", LocalDate.class ) );
    }

    @Test
    void nonStringFails() {
        assertThrows( Exception.class, () -> mapper.readValue( "123", LocalDate.class ) );
    }
}
