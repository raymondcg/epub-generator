package org.webnerdbot.web;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LogManager.getLogger( ApiExceptionHandler.class );

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest( IllegalArgumentException ex ) {
        return ResponseEntity.status( HttpStatus.BAD_REQUEST ).body( Map.of( "error", ex.getMessage() ) );
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, String>> handleMissingPart( MissingServletRequestPartException ex ) {
        return ResponseEntity.status( HttpStatus.BAD_REQUEST )
                .body( Map.of( "error", "Missing required part: " + ex.getRequestPartName() ) );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleServerError( Exception ex ) {
        LOGGER.error( "Unhandled error during request", ex );
        return ResponseEntity.status( HttpStatus.INTERNAL_SERVER_ERROR )
                .body( Map.of( "error", "Internal server error" ) );
    }
}
