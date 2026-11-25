package org.webnerdbot.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class CoverFormatTest {

    @Test
    void detectsJpegFromMagic() {
        byte[] jpeg = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00 };
        assertEquals( CoverFormat.JPEG, CoverFormat.from( null, jpeg ) );
        assertEquals( "id.jpg", CoverFormat.JPEG.fileName( "id" ) );
        assertEquals( "image/jpeg", CoverFormat.JPEG.getMediaType() );
    }

    @Test
    void detectsPngFromMagic() {
        byte[] png = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
        assertEquals( CoverFormat.PNG, CoverFormat.from( "application/octet-stream", png ) );
        assertEquals( "id.png", CoverFormat.PNG.fileName( "id" ) );
    }

    @Test
    void rejectsUnsupported() {
        assertThrows( IllegalArgumentException.class, () -> CoverFormat.from( "image/gif", new byte[] { 1, 2, 3 } ) );
    }
}
