package org.webnerdbot.generator.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileHelper {

    private static final Logger LOGGER = LogManager.getLogger( FileHelper.class );

    /**
     * Builds an EPUB zip in memory. Cover is stored as {@code OEBPS/images/{coverFileName}}.
     */
    public byte[] zip( Map<String, String> filesToNotCompress, Map<String, String> filesToCompress,
            String coverFileName, byte[] coverBytes ) throws IOException {
        if ( coverBytes == null || coverBytes.length == 0 ) {
            throw new IOException( "Cover image bytes are required" );
        }
        if ( coverFileName == null || coverFileName.isBlank() ) {
            throw new IOException( "Cover file name is required" );
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream( baos )) {
            for ( Map.Entry<String, String> entry : filesToNotCompress.entrySet() ) {
                LOGGER.debug( "Zipping File: {}", entry.getKey() );
                zipString( entry.getKey(), entry.getValue(), zipOut, false );
            }
            for ( Map.Entry<String, String> entry : filesToCompress.entrySet() ) {
                LOGGER.debug( "Zipping File: {}", entry.getKey() );
                zipString( entry.getKey(), entry.getValue(), zipOut, true );
            }
            zipBytes( coverBytes, "OEBPS/images/" + coverFileName, zipOut );
        }
        return baos.toByteArray();
    }

    private void zipBytes( byte[] content, String path, ZipOutputStream zipOut ) throws IOException {
        ZipEntry zipEntry = new ZipEntry( path );
        zipOut.putNextEntry( zipEntry );
        zipOut.write( content );
        zipOut.closeEntry();
    }

    private void zipString( String fileNameToZip, String stringToZip, ZipOutputStream zipOut, boolean compressed )
            throws IOException {
        byte[] content = stringToZip.getBytes( java.nio.charset.StandardCharsets.UTF_8 );
        try (InputStream inputStream = new ByteArrayInputStream( content )) {
            ZipEntry zipEntry = new ZipEntry( fileNameToZip );
            if ( !compressed ) {
                zipOut.setMethod( ZipOutputStream.STORED );
                zipOut.setLevel( Deflater.NO_COMPRESSION );
                zipEntry.setMethod( ZipEntry.STORED );
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
}
