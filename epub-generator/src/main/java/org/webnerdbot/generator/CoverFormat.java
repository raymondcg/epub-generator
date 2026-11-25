package org.webnerdbot.generator;

/**
 * Supported cover image formats for EPUB packaging.
 */
public enum CoverFormat {

    JPEG("jpg", "image/jpeg"), PNG("png", "image/png");

    private final String extension;
    private final String mediaType;

    CoverFormat( String extension, String mediaType ) {
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public String getExtension() {
        return extension;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String fileName( String bookId ) {
        return bookId + "." + extension;
    }

    /**
     * Resolves format from multipart content type and/or magic bytes.
     */
    public static CoverFormat from( String contentType, byte[] bytes ) {
        String type = contentType != null ? contentType.toLowerCase().trim() : "";
        if ( type.contains( "png" ) || isPng( bytes ) ) {
            return PNG;
        }
        if ( type.contains( "jpeg" ) || type.contains( "jpg" ) || isJpeg( bytes ) ) {
            return JPEG;
        }
        throw new IllegalArgumentException( "Unsupported cover image type; use JPEG or PNG" );
    }

    private static boolean isJpeg( byte[] bytes ) {
        return bytes != null && bytes.length >= 3 && ( bytes[0] & 0xFF ) == 0xFF && ( bytes[1] & 0xFF ) == 0xD8
                && ( bytes[2] & 0xFF ) == 0xFF;
    }

    private static boolean isPng( byte[] bytes ) {
        return bytes != null && bytes.length >= 8 && ( bytes[0] & 0xFF ) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E
                && bytes[3] == 0x47 && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A;
    }
}
