package org.webnerdbot.dao.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.webnerdbot.dao.DataAccess;

public class DataAccessImpl implements DataAccess {

    private static final Logger LOGGER = LogManager.getLogger( DataAccessImpl.class );

    @Override
    public boolean fileExist( String folderLocation, String fileName ) {
        File tmpFile = new File( folderLocation + fileName );
        return tmpFile.exists();
    }

    @Override
    public List<String> getFoldersInFolder( String folderLocation ) {
        LOGGER.debug( "Folder: {}", folderLocation );
        File directory = new File( folderLocation );
        File[] files = null;
        if ( directory.isDirectory() ) {
            files = directory.listFiles();
            LOGGER.debug( "Folder location {} is a directory. Found {} files", folderLocation, files.length );
        } else {
            LOGGER.error( "Folder location {} isn't a directory.", folderLocation );
        }

        List<String> folders = new ArrayList<String>();
        if ( files != null ) {
            for ( File file : files ) {
                if ( file.isDirectory() ) {
                    folders.add( file.getName() );
                    LOGGER.debug( "File: {}, is a directory.", file.getAbsoluteFile() );
                } else {
                    LOGGER.debug( "File: {}, is not a directory", file.getAbsoluteFile() );
                }
            }
        } else {
            LOGGER.debug( "No folders found." );
        }
        return folders;
    }

    @Override
    public List<String> getFilesInFolder( String folderLocation ) {
        LOGGER.debug( "Folder: {}", folderLocation );
        File folder = new File( folderLocation );

        File[] files = null;
        if ( folder.isDirectory() ) {
            files = folder.listFiles();
            LOGGER.debug( "Folder location {} is a directory. Found {} files", folderLocation, files.length );
        } else {
            LOGGER.error( "Folder location {} isn't a directory.", folderLocation );
        }

        List<String> result = new ArrayList<String>();
        if ( files != null ) {
            for ( File file : files ) {
                if ( !file.isDirectory() ) {
                    result.add( file.getName() );
                    LOGGER.debug( "File: {}, is a directory.", file.getAbsoluteFile() );
                } else {
                    LOGGER.debug( "File: {}, is not a directory", file.getAbsoluteFile() );
                }
            }
        } else {
            LOGGER.debug( "No folders found." );
        }
        return result;
    }

    @Override
    public void saveFile( String folderLocation, String fileName, String contents ) {
        saveFile( folderLocation, fileName, contents, StandardCharsets.UTF_8 );
    }

    @Override
    public void saveFile( String folderLocation, String fileName, String contents, Charset charset ) {
        File directory = new File( folderLocation );
        if ( !directory.exists() ) {
            directory.mkdirs();
        }

        try (PrintWriter out = new PrintWriter( folderLocation + fileName, charset )) {
            out.println( contents );
        } catch ( FileNotFoundException e ) {
            String message = String.format( "Error saving file %s.", fileName );
            LOGGER.error( message, e );
        } catch ( IOException e ) {
            String message = String.format( "Error saving file %s.", fileName );
            LOGGER.error( message, e );
        }
    }

    @Override
    public String readFile( String folderLocation, String fileName ) {
        return readFile( folderLocation, fileName, StandardCharsets.UTF_8 );
    }

    @Override
    public String readFile( String folderLocation, String fileName, Charset charset ) {
        StringBuilder contentBuilder = new StringBuilder();

        try (Stream<String> stream = Files.lines( Paths.get( folderLocation + File.separator + fileName ), charset )) {
            stream.forEach( s -> contentBuilder.append( s ).append( System.getProperty( "line.separator" ) ) );
        } catch ( IOException e ) {
            String message = String.format( "Error reading file %s.", fileName );
            LOGGER.error( message, e );
        }

        return contentBuilder.toString();
    }
}
