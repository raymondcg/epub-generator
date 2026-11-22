package org.webnerdbot.generator;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern( "yyyy-MM-dd" );

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

    /**
     * @return {@code 0} if every book generated cleanly; {@code 1} if any book failed or EpubCheck reported errors
     */
    public int go() {
        int failures = 0;
        List<CustomReport> allReports = new ArrayList<>();
        for ( String bookName : bookDao.getBookNames() ) {
            try {
                Book book = bookDao.getBook( bookName );
                if ( book == null ) {
                    LOGGER.error( "Skipping {}; failed to parse JSON.", bookName );
                    failures++;
                    continue;
                }
                List<CustomReport> reports = generate( book );
                allReports.addAll( reports );
                for ( CustomReport report : reports ) {
                    if ( report.getFatalErrorCount() > 0 || report.getErrorCount() > 0 ) {
                        failures++;
                        break;
                    }
                }
            } catch ( Exception e ) {
                failures++;
                LOGGER.error( "Failed to generate EPUB for book {}.", bookName, e );
            }
        }
        printEpubCheckSummary( allReports );
        return failures > 0 ? 1 : 0;
    }

    public List<CustomReport> generate( Book book ) {
        int chaptersPerVolume = applicationProperties.getChaptersPerVolume();
        LocalDate lastUpdated = book.getLastUpdated() != null ? book.getLastUpdated() : LocalDate.now();

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
                        book.getSubject(), book.getDescription(), lastUpdated ) );
            }
        } else {
            customReports.add( createEpub( chapters, book.getCrawldate(), book.getBookIdString(), book.getTitle(),
                    book.getAuthor(), book.getTranslator(), book.getEditor(), book.getPublisher(), book.getSubject(),
                    book.getDescription(), lastUpdated ) );
        }
        return customReports;
    }

    private CustomReport createEpub( List<Chapter> chapters, LocalDate crawlDate, String bookIdString,
            String bookTitle, String author, String translator, String editor, String publisher, String subject,
            String description, LocalDate lastUpdated ) {
        Instant start = Instant.now();
        String normalizedName = bookTitle.replace( " ", "" ).toLowerCase();
        CustomReport customReport = new CustomReport( normalizedName );
        LOGGER.info( "{} ePub -- Started", bookTitle );

        String epubFileName = normalizedName + ".epub";
        File epubFile = fileHelper.getEpub( epubFileName );
        if ( epubFile.isFile() ) {
            LocalDate epubDate = readEpubFileDate( epubFile );
            if ( lastUpdated.isBefore( epubDate ) ) {
                LOGGER.info( "{} ePub -- skipped; already exists (created/modified {}), lastUpdated {} is older",
                        bookTitle, epubDate.format( DATE_FORMAT ), lastUpdated.format( DATE_FORMAT ) );
                return customReport;
            }
            LOGGER.info( "{} ePub -- recreating; lastUpdated {} is on or after existing file date {}", bookTitle,
                    lastUpdated.format( DATE_FORMAT ), epubDate.format( DATE_FORMAT ) );
        }

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

        try {
            fileHelper.zip( epubFileName, zipContentsNotCompressed, zipContentsCompress, coverFile );
        } catch ( IOException e ) {
            throw new UncheckedIOException( "Failed to create EPUB " + epubFileName, e );
        }

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

    private LocalDate readEpubFileDate( File epubFile ) {
        try {
            BasicFileAttributes attrs = Files.readAttributes( epubFile.toPath(), BasicFileAttributes.class );
            FileTime fileTime = attrs.creationTime();
            if ( fileTime == null || fileTime.toMillis() <= 0 ) {
                fileTime = attrs.lastModifiedTime();
            }
            return fileTime.toInstant().atZone( ZoneId.systemDefault() ).toLocalDate();
        } catch ( IOException e ) {
            throw new UncheckedIOException( "Failed to read attributes for " + epubFile, e );
        }
    }

    private void printEpubCheckSummary( List<CustomReport> reports ) {
        int fatal = 0;
        int errors = 0;
        int warnings = 0;
        List<String> messages = new ArrayList<>();
        for ( CustomReport report : reports ) {
            fatal += report.getFatalErrorCount();
            errors += report.getErrorCount();
            warnings += report.getWarningCount();
            messages.addAll( report.getCollectedMessages() );
        }
        LOGGER.info( "EpubCheck summary -- Fatal Errors {}, Errors {}, Warnings {}", fatal, errors, warnings );
        if ( messages.isEmpty() ) {
            LOGGER.info( "EpubCheck summary -- no EpubCheck issues" );
            return;
        }
        LOGGER.info( "EpubCheck summary -- reprinting all issues:" );
        for ( String message : messages ) {
            LOGGER.error( message );
        }
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
