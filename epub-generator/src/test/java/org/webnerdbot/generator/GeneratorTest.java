package org.webnerdbot.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.webnerdbot.Application;
import org.webnerdbot.config.ApplicationProperties;
import org.webnerdbot.config.EnvLoader;
import org.webnerdbot.dao.file.BookDao;
import org.webnerdbot.dao.file.DataAccessImpl;
import org.webnerdbot.generator.util.FileHelper;
import org.webnerdbot.generator.util.TemplateCreation;
import org.webnerdbot.model.Book;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public class GeneratorTest {

    private static final Path BOOK_DB_DIR;
    private static final Path EPUB_DIR;

    static {
        EnvLoader.load();
        try {
            BOOK_DB_DIR = Files.createTempDirectory( "epub-generator-bookdb-" );
            EPUB_DIR = Files.createTempDirectory( "epub-generator-epub-" );
        } catch ( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    @Autowired
    private GeneratorService generatorService;

    @Autowired
    private BookDao bookDao;

    @DynamicPropertySource
    static void registerProperties( DynamicPropertyRegistry registry ) {
        registry.add( "BOOK_DB", () -> BOOK_DB_DIR.toAbsolutePath().toString() );
        registry.add( "EPUB_OUTPUT", () -> EPUB_DIR.toAbsolutePath().toString() );
        registry.add( "CHAPTERS_PER_VOLUME", () -> "1000" );
    }

    @BeforeEach
    void clearEpubOutput() throws IOException {
        if ( !Files.isDirectory( EPUB_DIR ) ) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream( EPUB_DIR )) {
            for ( Path entry : stream ) {
                deleteRecursively( entry );
            }
        }
    }

    @DisplayName("generate happy path produces a valid EPUB")
    @Test
    public void generateHappyPath() throws Exception {
        Book book = loadSampleBook();
        copyCoverToBookDb();

        List<CustomReport> reports = generatorService.generate( book );

        assertEquals( 1, reports.size() );
        assertReportClean( reports.get( 0 ) );
        assertTrue( Files.exists( EPUB_DIR.resolve( "ancientgodlymonarch.epub" ) ) );
    }

    @DisplayName("go reads JSON from BOOK_DB and writes EPUB to EPUB_OUTPUT")
    @Test
    public void goGeneratesFromFilesystem() throws Exception {
        copySampleJsonToBookDb();
        copyCoverToBookDb();

        assertEquals( 0, generatorService.go() );

        assertTrue( Files.exists( EPUB_DIR.resolve( "ancientgodlymonarch.epub" ) ) );
    }

    @DisplayName("empty chapter bodies are filtered out")
    @Test
    public void emptyBodiesFiltered() throws Exception {
        Book book = loadSampleBook();
        copyCoverToBookDb();

        long emptyBodies = book.getChapters().stream().filter( c -> c.getBody() == null || c.getBody().isEmpty() )
                .count();
        assertTrue( emptyBodies >= 1 );

        List<CustomReport> reports = generatorService.generate( book );
        assertEquals( 1, reports.size() );
        assertReportClean( reports.get( 0 ) );

        String content = readEpubContentSnippet( "ancientgodlymonarch.epub" );
        assertFalse( content.contains( "chapter_6" ) );
        assertTrue( content.contains( "chapter_1" ) );
    }

    @DisplayName("chapters are partitioned into volumes when over chaptersPerVolume")
    @Test
    public void multiVolumePartition() throws Exception {
        Book book = loadSampleBook();
        copyCoverToBookDb();

        GeneratorService localService = createServiceWithChaptersPerVolume( 2 );
        List<CustomReport> reports = localService.generate( book );

        assertEquals( 3, reports.size() );
        assertTrue( Files.exists( EPUB_DIR.resolve( "ancientgodlymonarchvolume1.epub" ) ) );
        assertTrue( Files.exists( EPUB_DIR.resolve( "ancientgodlymonarchvolume2.epub" ) ) );
        assertTrue( Files.exists( EPUB_DIR.resolve( "ancientgodlymonarchvolume3.epub" ) ) );
    }

    @DisplayName("missing cover does not abort go")
    @Test
    public void missingCoverDoesNotAbortGo() throws Exception {
        Files.deleteIfExists( BOOK_DB_DIR.resolve( "ancientgodlymonarch.jpg" ) );
        copySampleJsonToBookDb();

        assertEquals( 1, generatorService.go() );
    }

    @DisplayName("skips recreate when lastUpdated is older than existing EPUB date")
    @Test
    public void skipsWhenLastUpdatedOlderThanEpub() throws Exception {
        Book book = loadSampleBook();
        copyCoverToBookDb();

        generatorService.generate( book );

        Path epubPath = EPUB_DIR.resolve( "ancientgodlymonarch.epub" );
        FileTime before = Files.getLastModifiedTime( epubPath );

        book.setLastUpdated( LocalDate.now().minusDays( 1 ) );
        List<CustomReport> reports = generatorService.generate( book );

        assertEquals( 1, reports.size() );
        assertReportClean( reports.get( 0 ) );
        assertEquals( before, Files.getLastModifiedTime( epubPath ) );
    }

    @DisplayName("recreates when lastUpdated is missing (defaults to today)")
    @Test
    public void recreatesWhenLastUpdatedMissingDefaultsToToday() throws Exception {
        Book book = loadSampleBook();
        copyCoverToBookDb();
        book.setLastUpdated( LocalDate.now().minusDays( 1 ) );

        generatorService.generate( book );

        Path epubPath = EPUB_DIR.resolve( "ancientgodlymonarch.epub" );
        FileTime before = Files.getLastModifiedTime( epubPath );
        Thread.sleep( 1100 );

        book.setLastUpdated( null );
        List<CustomReport> reports = generatorService.generate( book );

        assertEquals( 1, reports.size() );
        assertReportClean( reports.get( 0 ) );
        assertTrue( Files.getLastModifiedTime( epubPath ).compareTo( before ) > 0 );
    }

    @DisplayName("go summary path returns clean reports with zero collected issues")
    @Test
    public void goSummaryHasCleanReports() throws Exception {
        copySampleJsonToBookDb();
        copyCoverToBookDb();

        assertEquals( 0, generatorService.go() );

        Book book = loadSampleBook();
        book.setLastUpdated( LocalDate.now().minusDays( 1 ) );
        List<CustomReport> reports = generatorService.generate( book );
        assertEquals( 1, reports.size() );
        assertReportClean( reports.get( 0 ) );
        assertTrue( reports.get( 0 ).getCollectedMessages().isEmpty() );
    }

    private GeneratorService createServiceWithChaptersPerVolume( int chaptersPerVolume ) throws Exception {
        ApplicationProperties props = new ApplicationProperties();
        setField( props, "bookDb", BOOK_DB_DIR.toAbsolutePath().toString() );
        setField( props, "epubOutput", EPUB_DIR.toAbsolutePath().toString() );
        setField( props, "chaptersPerVolume", chaptersPerVolume );

        FileHelper fileHelper = new FileHelper( props );
        Configuration cfg = new Configuration( new Version( "2.3.23" ) );
        cfg.setClassForTemplateLoading( TemplateCreation.class, "/templates" );
        cfg.setDefaultEncoding( "UTF-8" );
        cfg.setLocale( Locale.US );
        cfg.setTemplateExceptionHandler( TemplateExceptionHandler.RETHROW_HANDLER );
        TemplateCreation templates = new TemplateCreation( cfg );
        BookDao localDao = new BookDao( new DataAccessImpl(), props );
        return new GeneratorService( props, templates, fileHelper, localDao );
    }

    private static void setField( Object target, String name, Object value ) throws Exception {
        Field field = target.getClass().getDeclaredField( name );
        field.setAccessible( true );
        field.set( target, value );
    }

    private static void deleteRecursively( Path path ) throws IOException {
        if ( Files.isDirectory( path ) ) {
            try (Stream<Path> children = Files.list( path )) {
                for ( Path child : children.toList() ) {
                    deleteRecursively( child );
                }
            }
        }
        Files.deleteIfExists( path );
    }

    private Book loadSampleBook() throws IOException {
        String json = IOUtils.toString( resource( "/generator/ancientgodlymonarch.json" ), StandardCharsets.UTF_8 );
        Book book = bookDao.stringToPojo( json );
        assertTrue( book != null && book.getChapters() != null && !book.getChapters().isEmpty() );
        return book;
    }

    private void copySampleJsonToBookDb() throws IOException {
        Files.writeString( BOOK_DB_DIR.resolve( "ancientgodlymonarch.json" ),
                IOUtils.toString( resource( "/generator/ancientgodlymonarch.json" ), StandardCharsets.UTF_8 ) );
    }

    private void copyCoverToBookDb() throws IOException {
        try (InputStream in = resource( "/generator/ancientgodlymonarch.jpg" )) {
            Files.copy( in, BOOK_DB_DIR.resolve( "ancientgodlymonarch.jpg" ), StandardCopyOption.REPLACE_EXISTING );
        }
    }

    private InputStream resource( String path ) {
        InputStream in = getClass().getResourceAsStream( path );
        if ( in == null ) {
            throw new UncheckedIOException( new IOException( "Missing resource " + path ) );
        }
        return in;
    }

    private void assertReportClean( CustomReport report ) {
        assertEquals( 0, report.getFatalErrorCount(), "fatal errors" );
        assertEquals( 0, report.getErrorCount(), "errors" );
        assertEquals( 0, report.getWarningCount(), "warnings" );
    }

    private String readEpubContentSnippet( String epubFileName ) throws IOException {
        File epub = EPUB_DIR.resolve( epubFileName ).toFile();
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile( epub )) {
            java.util.zip.ZipEntry entry = zip.getEntry( "OEBPS/content.xhtml" );
            assertTrue( entry != null );
            try (InputStream in = zip.getInputStream( entry )) {
                return IOUtils.toString( in, StandardCharsets.UTF_8 );
            }
        }
    }
}
