package org.webnerdbot.model;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class BookMapper {

    private final ObjectMapper objectMapper;

    public BookMapper() {
        this( new ObjectMapper() );
    }

    public BookMapper( ObjectMapper objectMapper ) {
        this.objectMapper = objectMapper;
    }

    public Book fromJson( String json ) throws IOException {
        if ( json == null || json.isBlank() ) {
            throw new IOException( "Book JSON is required" );
        }
        return objectMapper.readValue( json, Book.class );
    }
}
