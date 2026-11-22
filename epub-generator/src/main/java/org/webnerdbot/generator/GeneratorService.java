package org.webnerdbot.generator;

import java.io.IOException;
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
import org.webnerdbot.config.ApplicationProperties;
import org.webnerdbot.dao.file.BookDao;
import org.webnerdbot.generator.util.FileHelper;
import org.webnerdbot.generator.util.TemplateCreation;
import org.webnerdbot.model.Book;
import org.webnerdbot.model.Chapter;

import com.adobe.epubcheck.api.EpubCheck;
import com.google.common.collect.Lists;

public class GeneratorService {

    private static final Logger LOGGER = LogManager.getLogger( GeneratorService.class );

    private final ApplicationProperties applicationProperties;
    private final TemplateCreation templateCreation;
    private final FileHelper fileHelper;
    private final BookDao bookDao;

    public GeneratorService( ApplicationProperties applicationProperties, TemplateCreation templateCreation,
            FileHelper fileHelper, BookDao bookDao ) {
        this.applicationProperties = applicationProperties;
        this.templateCreation = templateCreation;
        this.fileHelper = fileHelper;
        this.bookDao = bookDao;
    }

    public void go() {
        for ( String bookName : bookDao.getBookNames() ) {
            try {
                Book book = bookDao.getBook( bookName );
                if ( book == null ) {
                    LOGGER.error( "Skipping {}; failed to parse JSON.", bookName );
                    continue;
                }
                generate( book );
            } catch ( Exception e ) {
                LOGGER.error( "Failed to generate EPUB for book {}.", bookName, e );
            }
        }
    }

    public List<CustomReport> generate( Book book ) {
        int chaptersPerVolume = applicationProperties.getChaptersPerVolume();

        List<Chapter> chapters = new ArrayList<>();
        if ( book.getChapters() != null ) {
            for ( Chapter chapter : book.getChapters() ) {
                if ( chapter.getBody() != null && !chapter.getBody().isEmpty() ) {
                    chapters.add( chapter );
                }
            }
        }
        Collections.sort( chapters );

        List<CustomReport> customReports = new ArrayList<>();
        if ( chapters.size() > chaptersPerVolume ) {
            List<List<Chapter>> subSetChapters = Lists.partition( chapters, chaptersPerVolume );
            int i = 1;
            for ( List<Chapter> subSetChapter : subSetChapters ) {
                String volumeTitle = String.format( "%s volume %s", book.getTitle(), i++ );
                customReports.add( createEpub( subSetChapter, book.getCrawldate(), book.getBookIdString(), volumeTitle,
                        book.getAuthor(), book.getTranslator(), book.getEditor(), book.getPublisher(),
                        book.getSubject(), book.getDescription() ) );
            }
        } else {
            customReports.add( createEpub( chapters, book.getCrawldate(), book.getBookIdString(), book.getTitle(),
                    book.getAuthor(), book.getTranslator(), book.getEditor(), book.getPublisher(), book.getSubject(),
                    book.getDescription() ) );
        }
        return customReports;
    }

    private CustomReport createEpub( List<Chapter> chapters, LocalDate crawlDate, String bookIdString,
            String bookTitle, String author, String translator, String editor, String publisher, String subject,
            String description ) {
        Instant start = Instant.now();
        CustomReport customReport = new CustomReport( bookTitle.replace( " ", "" ).toLowerCase() );
        LOGGER.info( "{} ePub -- Started", bookTitle );

        String epubFileName = bookTitle.replace( " ", "" ).toLowerCase() + ".epub";
        String coverFile = bookIdString + ".jpg";

        Map<String, String> zipContentsNotCompressed = new HashMap<>();
        zipContentsNotCompressed.put( "mimetype", "application/epub+zip" );

        Map<String, String> zipContentsCompress = new HashMap<>();
        zipContentsCompress.put( "META-INF/container.xml", createContainerXml() );
        zipContentsCompress.put( "OEBPS/content.opf",
                createOpf( crawlDate, bookIdString, bookTitle, author, publisher, subject, description, chapters ) );
        zipContentsCompress.put( "OEBPS/toc.xhtml", createTocHtml( chapters ) );
        zipContentsCompress.put( "OEBPS/css/style.css", getStyleSheet() );

        zipContentsCompress.put( "OEBPS/cover.xhtml", createCoverHtml( bookTitle, author, bookIdString ) );
        zipContentsCompress.put( "OEBPS/preface_001.xhtml", createPreface( author, translator, editor ) );
        zipContentsCompress.put( "OEBPS/copyright.xhtml", createCopyright( bookTitle, author ) );

        String fileName = "OEBPS/content.xhtml";
        zipContentsCompress.put( fileName, createContent( bookTitle, chapters ) );

        fileHelper.zip( epubFileName, zipContentsNotCompressed, zipContentsCompress, coverFile );

        LOGGER.info( "{} ePub -- created", bookTitle );

        EpubCheck epubcheck = new EpubCheck( fileHelper.getEpub( epubFileName ), customReport );

        if ( epubcheck.validate() ) {
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
        String styleSheet = "";
        try {
            styleSheet = IOUtils.toString( this.getClass().getResourceAsStream( "/templates/style.css" ), "UTF-8" );
        } catch ( IOException e ) {
            LOGGER.error( "Problem reading the style.css file.", e );
        }
        return styleSheet;
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
            String subject, String description, List<Chapter> chapters ) {
        String dateString = crawlDate.format( DateTimeFormatter.ofPattern( "yyyy-MM-dd" ) );
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
        return templateCreation.createString( input, "bookopf.ftl" );
    }

    private String createCoverHtml( String title, String author, String bookIdString ) {
        Map<String, Object> input = new HashMap<>();
        input.put( "title", title );
        input.put( "author", author );
        input.put( "bookId", bookIdString );
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
