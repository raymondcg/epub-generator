package org.webnerdbot.model;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class JsonDateDeserializer extends JsonDeserializer<LocalDate> {

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern( "yyyy-MM-dd" ).withLocale( Locale.US );

    @Override
    public LocalDate deserialize( JsonParser jp, DeserializationContext ctxt )
            throws IOException, JsonProcessingException {

        ObjectCodec oc = jp.getCodec();
        JsonNode node = oc.readTree( jp );
        if ( node == null || node.isNull() ) {
            return null;
        }
        if ( !node.isTextual() ) {
            throw new IOException( "Date value must be a string in yyyy-MM-dd format" );
        }
        String dateString = node.textValue();
        if ( dateString == null || dateString.isBlank() ) {
            return null;
        }
        return LocalDate.parse( dateString, formatter );
    }
}
