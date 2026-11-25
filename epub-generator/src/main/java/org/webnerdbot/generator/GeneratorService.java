package org.webnerdbot.generator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.webnerdbot.generator.util.FileHelper;
import org.webnerdbot.generator.util.TemplateCreation;
import org.webnerdbot.model.Book;
import org.webnerdbot.model.Chapter;

import com.adobe.epubcheck.api.EpubCheck;

public class GeneratorService {

    private static final Logger LOGGER = LogManager.getLogger( GeneratorService.class );
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern( "yyyy-MM-dd" );

    private final TemplateCreation templateCreation;
    private final FileHelper fileHelper;
    private final CustomReportStore customReportStore;

    public GeneratorService( TemplateCreation templateCreation, FileHelper fileHelper,
            CustomReportStore customReportStore ) {
        this.templateCreation = templateCreation;
        this.fileHelper = fileHelper;
        this.customReportStore = customReportStore;
    }

    /**
     * Builds a single EPUB in memory from the given book and cover image bytes, validates with EpubCheck, and stores
     * the latest report for the book id.
     */
    public GenerationResult generateEpub( Book book, byte[] coverBytes ) {
        return generateEpub( book, coverBytes, null );
    }

    public GenerationResult generateEpub( Book book, byte[] coverBytes, String coverContentType ) {
        if ( book == null ) {
            throw new IllegalArgumentException( "Book is required" );
        }
        String bookId = book.getBookIdString();
        if ( bookId == null || bookId.isBlank() ) {
            throw new IllegalArgumentException( "Book id is required" );
        }
        if ( coverBytes == null || coverBytes.length == 0 ) {
            throw new IllegalArgumentException( "Cover image is required" );
        }

        CoverFormat coverFormat = CoverFormat.from( coverContentType, coverBytes );

        List<Chapter> chapters = new ArrayList<>();
        if ( book.getChapters() != null ) {
            for ( Chapter chapter : book.getChapters() ) {
                if ( chapter.getBody() != null && !chapter.getBody().isEmpty() ) {
                    chapters.add( chapter );
                }
            }
        }
        Collections.sort( chapters );

        String title = book.getTitle() != null ? book.getTitle() : bookId;
        GenerationResult result = createEpub( chapters, book.getCrawldate(), bookId, title, book.getAuthor(),
                book.getTranslator(), book.getEditor(), book.getPublisher(), book.getSubject(), book.getDescription(),
                coverBytes, coverFormat );

        customReportStore.put( bookId, new EpubCheckReportDto( bookId, result.report() ) );
        return result;
    }

    private GenerationResult createEpub( List<Chapter> chapters, LocalDate crawlDate, String bookIdString,
            String bookTitle, String author, String translator, String editor, String publisher, String subject,
            String description, byte[] coverBytes, CoverFormat coverFormat ) {
        Instant start = Instant.now();
        String normalizedName = bookTitle.replace( " ", "" ).toLowerCase();
        if ( normalizedName.isBlank() ) {
            normalizedName = bookIdString;
        }
        CustomReport customReport = new CustomReport( normalizedName );
        LOGGER.info( "{} ePub -- Started", bookTitle );

        String epubFileName = normalizedName + ".epub";
        String coverFileName = coverFormat.fileName( bookIdString );

        Map<String, String> zipContentsNotCompressed = new HashMap<>();
        zipContentsNotCompressed.put( "mimetype", "application/epub+zip" );

        Map<String, String> zipContentsCompress = new HashMap<>();
        zipContentsCompress.put( "META-INF/container.xml", createContainerXml() );
        zipContentsCompress.put( "OEBPS/content.opf", createOpf( crawlDate, bookIdString, bookTitle, author, publisher,
                subject, description, chapters, coverFileName, coverFormat.getMediaType() ) );
        zipContentsCompress.put( "OEBPS/toc.xhtml", createTocHtml( chapters ) );
        zipContentsCompress.put( "OEBPS/css/style.css", getStyleSheet() );
        zipContentsCompress.put( "OEBPS/cover.xhtml", createCoverHtml( bookTitle, author, coverFileName ) );
        zipContentsCompress.put( "OEBPS/preface_001.xhtml", createPreface( author, translator, editor ) );
        zipContentsCompress.put( "OEBPS/copyright.xhtml", createCopyright( bookTitle, author ) );
        zipContentsCompress.put( "OEBPS/content.xhtml", createContent( bookTitle, chapters ) );

        byte[] epubBytes;
        try {
            epubBytes = fileHelper.zip( zipContentsNotCompressed, zipContentsCompress, coverFileName, coverBytes );
        } catch ( IOException e ) {
            throw new UncheckedIOException( "Failed to create EPUB " + epubFileName, e );
        }

        LOGGER.info( "{} ePub -- created", bookTitle );
        customReport = validateEpub( bookTitle, epubFileName, epubBytes, customReport, start );
        return new GenerationResult( epubBytes, epubFileName, customReport );
    }

    private CustomReport validateEpub( String bookTitle, String epubFileName, byte[] epubBytes,
            CustomReport customReport, Instant start ) {
        EpubCheck epubcheck = new EpubCheck( new ByteArrayInputStream( epubBytes ), customReport, epubFileName );

        if ( epubcheck.doValidate() == 0 ) {
            LOGGER.info( "{} ePub -- valid", bookTitle );
        } else {
            LOGGER.error( "{} ePub -- NOT valid", bookTitle );
        }
        LOGGER.info( "{} ePub -- Fatal Errors {}, Errors {}, Warnings {} ", bookTitle,
                customReport.getFatalErrorCount(), customReport.getErrorCount(), customReport.getWarningCount() );

        Instant finish = Instant.now();
        long timeElapsed = Duration.between( start, finish ).toMillis();
        LOGGER.info( "{} ePub -- Complete in {} ms.", bookTitle, timeElapsed );
        return customReport;
    }

    private String getStyleSheet() {
        try {
            return IOUtils.toString( this.getClass().getResourceAsStream( "/templates/style.css" ), "UTF-8" );
        } catch ( IOException e ) {
            throw new UncheckedIOException( "Problem reading the style.css file", e );
        }
    }

    private String createTocHtml( List<Chapter> chapters ) {
        Map<String, Object> input = new HashMap<>();
        input.put( "chapters", chapters );
        return templateCreation.createString( input, "toc.ftl" );
    }

    private String createContent( String bookName, List<Chapter> chapters ) {
        Map<String, Object> input = new HashMap<>();
        input.put( "bookName", bookName );
        input.put( "chapters", chapters );
        return templateCreation.createString( input, "content.ftl" );
    }

    private String createOpf( LocalDate crawlDate, String bookIdString, String title, String author, String publisher,
            String subject, String description, List<Chapter> chapters, String coverFileName, String coverMediaType ) {
        LocalDate effectiveCrawlDate = crawlDate != null ? crawlDate : LocalDate.now();
        String dateString = effectiveCrawlDate.format( DATE_FORMAT );
        String modifiedDate = LocalDateTime.now().format( DateTimeFormatter.ofPattern( "yyyy-MM-dd'T'HH:mm:ss'Z'" ) );

        Map<String, Object> input = new HashMap<>();
        input.put( "bookId", bookIdString );
        input.put( "title", title );
        input.put( "author", author );
        input.put( "publisher", publisher );
        input.put( "subject", subject );
        input.put( "crawldate", dateString );
        input.put( "modifieddate", modifiedDate );
        input.put( "description", description );
        input.put( "coverFileName", coverFileName );
        input.put( "coverMediaType", coverMediaType );
        return templateCreation.createString( input, "bookopf.ftl" );
    }

    private String createCoverHtml( String title, String author, String coverFileName ) {
        Map<String, Object> input = new HashMap<>();
        input.put( "title", title );
        input.put( "author", author );
        input.put( "coverFileName", coverFileName );
        return templateCreation.createString( input, "cover.ftl" );
    }

    private String createPreface( String author, String translator, String editor ) {
        Map<String, Object> input = new HashMap<>();
        input.put( "author", author );
        input.put( "translator", translator );
        input.put( "editor", editor );
        return templateCreation.createString( input, "preface_001.ftl" );
    }

    private String createCopyright( String title, String author ) {
        Map<String, Object> input = new HashMap<>();
        input.put( "title", title );
        input.put( "author", author );
        return templateCreation.createString( input, "copyright.ftl" );
    }

    private String createContainerXml() {
        Map<String, Object> input = new HashMap<>();
        return templateCreation.createString( input, "container.ftl" );
    }
}
