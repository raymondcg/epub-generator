package org.webnerdbot.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class BookMapperTest {

    private final BookMapper mapper = new BookMapper();

    @Test
    void parsesBookJson() throws Exception {
        Book book = mapper.fromJson( "{\"id\":\"x\",\"title\":\"T\",\"chapters\":[]}" );
        assertEquals( "x", book.getId() );
        assertEquals( "T", book.getTitle() );
    }

    @Test
    void rejectsBlank() {
        assertThrows( IOException.class, () -> mapper.fromJson( "  " ) );
    }

    @Test
    void rejectsInvalidJson() {
        assertThrows( IOException.class, () -> mapper.fromJson( "{bad" ) );
    }
}
