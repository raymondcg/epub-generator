package org.webnerdbot.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.webnerdbot.Application;
import org.webnerdbot.model.Book;
import org.webnerdbot.model.BookMapper;
import org.webnerdbot.model.Chapter;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public class GeneratorTest {

    @Autowired
    private GeneratorService generatorService;

    @Autowired
    private CustomReportStore customReportStore;

    private final BookMapper bookMapper = new BookMapper();

    @BeforeEach
    void clearReports() {
        customReportStore.clear();
    }

    @DisplayName("generateEpub happy path produces a valid in-memory EPUB")
    @Test
    public void generateHappyPath() throws Exception {
        Book book = loadSampleBook();
        byte[] cover = loadSampleCover();

        GenerationResult result = generatorService.generateEpub( book, cover );

        assertTrue( result.epubBytes().length > 100 );
        assertEquals( "ancientgodlymonarch.epub", result.epubFileName() );
        assertReportClean( result.report() );

        String toc = readEpubEntry( result.epubBytes(), "OEBPS/toc.xhtml" );
        assertTrue( toc.contains( "content.xhtml#chapter_1" ) );

        assertTrue( epubContainsEntry( result.epubBytes(), "OEBPS/images/" + book.getBookIdString() + ".jpg" ) );
        assertTrue( customReportStore.get( book.getBookIdString() ).isPresent() );
    }

    @DisplayName("empty chapter bodies are skipped")
    @Test
    public void skipsEmptyChapterBodies() throws Exception {
        Book book = loadSampleBook();
        Chapter empty = new Chapter();
        empty.setId( "empty" );
        empty.setNumber( "99" );
        empty.setTitle( "Empty" );
        empty.setBody( "" );
        book.getChapters().add( empty );

        GenerationResult result = generatorService.generateEpub( book, loadSampleCover() );
        String content = readEpubEntry( result.epubBytes(), "OEBPS/content.xhtml" );
        assertFalse( content.contains( "Empty" ) );
    }

    @DisplayName("book without id or title is rejected")
    @Test
    public void rejectsMissingBookId() {
        Book book = new Book();
        book.setTitle( null );
        assertThrows( IllegalArgumentException.class, () -> generatorService.generateEpub( book, new byte[] { 1 } ) );
    }

    @DisplayName("empty cover is rejected")
    @Test
    public void rejectsEmptyCover() throws Exception {
        Book book = loadSampleBook();
        assertThrows( IllegalArgumentException.class, () -> generatorService.generateEpub( book, new byte[0] ) );
    }

    @DisplayName("report store keeps latest report per book id")
    @Test
    public void storesLatestReportPerBook() throws Exception {
        Book book = loadSampleBook();
        generatorService.generateEpub( book, loadSampleCover() );
        generatorService.generateEpub( book, loadSampleCover() );
        assertEquals( 1, customReportStore.getAll().size() );
    }

    private Book loadSampleBook() throws IOException {
        try (InputStream in = getClass().getResourceAsStream( "/generator/ancientgodlymonarch.json" )) {
            String json = IOUtils.toString( in, StandardCharsets.UTF_8 );
            return bookMapper.fromJson( json );
        }
    }

    private byte[] loadSampleCover() throws IOException {
        try (InputStream in = getClass().getResourceAsStream( "/generator/ancientgodlymonarch.jpg" )) {
            return IOUtils.toByteArray( in );
        }
    }

    private void assertReportClean( CustomReport report ) {
        assertEquals( 0, report.getFatalErrorCount(), "fatal" );
        assertEquals( 0, report.getErrorCount(), "errors" );
    }

    private boolean epubContainsEntry( byte[] epubBytes, String entryName ) throws IOException {
        try (ZipInputStream zis = new ZipInputStream( new ByteArrayInputStream( epubBytes ) )) {
            ZipEntry entry;
            while ( ( entry = zis.getNextEntry() ) != null ) {
                if ( entryName.equals( entry.getName() ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    private String readEpubEntry( byte[] epubBytes, String entryName ) throws IOException {
        try (ZipInputStream zis = new ZipInputStream( new ByteArrayInputStream( epubBytes ) )) {
            ZipEntry entry;
            while ( ( entry = zis.getNextEntry() ) != null ) {
                if ( entryName.equals( entry.getName() ) ) {
                    return IOUtils.toString( zis, StandardCharsets.UTF_8 );
                }
            }
        }
        throw new IllegalStateException( "Missing entry " + entryName );
    }
}
