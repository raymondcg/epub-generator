package org.webnerdbot.dao.file;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.webnerdbot.config.ApplicationProperties;
import org.webnerdbot.dao.DataAccess;
import org.webnerdbot.model.Book;

import com.fasterxml.jackson.databind.ObjectMapper;

public class BookDao {

    private static final Logger LOGGER = LogManager.getLogger( BookDao.class );

    private final DataAccess dataAccess;
    private final ApplicationProperties applicationProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BookDao( DataAccess dataAccess, ApplicationProperties applicationProperties ) {
        this.dataAccess = dataAccess;
        this.applicationProperties = applicationProperties;
    }

    public Collection<String> getBookNames() {
        String folder = applicationProperties.getBookDb() + File.separator;
        LOGGER.debug( "Get books in folder: {}", folder );
        List<String> files = dataAccess.getFilesInFolder( folder );
        Set<String> books = new HashSet<>();
        for ( String file : files ) {
            if ( file.endsWith( ".json" ) ) {
                books.add( file.substring( 0, file.length() - ".json".length() ) );
            }
        }
        LOGGER.debug( "Files found: {}", books );
        return books;
    }

    public Book getBook( String bookName ) {
        String fileName = bookName + ".json";
        String folder = applicationProperties.getBookDb() + File.separator;
        LOGGER.debug( "Getting file: {}, folder: {}", fileName, folder );
        return stringToPojo( dataAccess.readFile( folder, fileName ) );
    }

    public Book stringToPojo( String fileContents ) {
        if ( fileContents == null || fileContents.isBlank() ) {
            return null;
        }
        try {
            return objectMapper.readValue( fileContents, Book.class );
        } catch ( IOException e ) {
            LOGGER.error( "Error mapping json file to pojo.", e );
            return null;
        }
    }
}
