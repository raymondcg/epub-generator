package org.webnerdbot.generator.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.webnerdbot.config.ApplicationProperties;

public class FileHelper {

    private static final Logger LOGGER = LogManager.getLogger( FileHelper.class );

    private final ApplicationProperties applicationProperties;

    public FileHelper( ApplicationProperties applicationProperties ) {
        this.applicationProperties = applicationProperties;
    }

    public String readFile( String file ) {
        return this.readFile( file, StandardCharsets.UTF_8 );
    }

    public String readFile( String file, Charset charset ) {
        try {
            return FileUtils.readFileToString( new File( file ), charset );
        } catch ( IOException e ) {
            LOGGER.error( "Problems reading in file {}.", file, e );
        }
        return "";
    }

    /**
     * Creates a zip file from the given strings under {@code EPUB_OUTPUT}.
     */
    public void zip( String zipFileName, Map<String, String> filesToNotCompress, Map<String, String> filesToCompress,
            String coverFile ) throws IOException {

        String destination = applicationProperties.getEpubOutput() + File.separator;

        File directory = new File( destination );
        if ( !directory.exists() ) {
            directory.mkdirs();
        }

        File cover = new File( applicationProperties.getBookDb() + File.separator + coverFile );
        if ( !cover.isFile() ) {
            throw new IOException( "Cover image not found: " + cover.getAbsolutePath() );
        }

        try (ZipOutputStream zipOut = new ZipOutputStream( new FileOutputStream( destination + zipFileName ) )) {
            for ( String fileName : filesToNotCompress.keySet() ) {
                LOGGER.debug( "Zipping File: {}", fileName );
                zipString( fileName, filesToNotCompress.get( fileName ), zipOut, false );
            }
            for ( String fileName : filesToCompress.keySet() ) {
                LOGGER.debug( "Zipping File: {}", fileName );
                zipString( fileName, filesToCompress.get( fileName ), zipOut, true );
            }

            zipFile( cover, "OEBPS/images/", zipOut );
        }
    }

    private void zipFile( File fileToZip, String path, ZipOutputStream zipOut ) throws IOException {
        try (FileInputStream fis = new FileInputStream( fileToZip )) {
            ZipEntry zipEntry = new ZipEntry( path + fileToZip.getName() );

            zipOut.putNextEntry( zipEntry );
            byte[] bytes = new byte[1024];
            int length;
            while ( ( length = fis.read( bytes ) ) >= 0 ) {
                zipOut.write( bytes, 0, length );
            }
        }
    }

    private void zipString( String fileNameToZip, String stringToZip, ZipOutputStream zipOut, boolean compressed )
            throws IOException {
        byte[] content = stringToZip.getBytes( StandardCharsets.UTF_8 );
        try (InputStream inputStream = new ByteArrayInputStream( content )) {
            ZipEntry zipEntry = new ZipEntry( fileNameToZip );
            if ( !compressed ) {
                zipOut.setMethod( ZipOutputStream.STORED );
                zipOut.setLevel( Deflater.NO_COMPRESSION );
                zipEntry.setMethod( ZipOutputStream.STORED );
                zipEntry.setSize( content.length );
                zipEntry.setCompressedSize( content.length );
                CRC32 crc = new CRC32();
                crc.update( content );
                zipEntry.setCrc( crc.getValue() );
            } else {
                zipOut.setMethod( ZipOutputStream.DEFLATED );
                zipOut.setLevel( Deflater.BEST_COMPRESSION );
            }
            zipOut.putNextEntry( zipEntry );
            byte[] bytes = new byte[1024];
            int length;
            while ( ( length = inputStream.read( bytes ) ) >= 0 ) {
                zipOut.write( bytes, 0, length );
            }
            zipOut.closeEntry();
        }
    }

    public File getEpub( String epubFileName ) {
        return new File( applicationProperties.getEpubOutput() + File.separator + epubFileName );
    }
}