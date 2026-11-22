package org.webnerdbot.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.adobe.epubcheck.api.EPUBLocation;
import com.adobe.epubcheck.api.MasterReport;
import com.adobe.epubcheck.messages.Message;
import com.adobe.epubcheck.messages.Severity;
import com.adobe.epubcheck.util.FeatureEnum;
import com.adobe.epubcheck.util.PathUtil;
import com.adobe.epubcheck.util.ReportingLevel;
import com.adobe.epubcheck.util.outWriter;

public class CustomReport extends MasterReport {

    private static final Logger LOGGER = LogManager.getLogger( CustomReport.class );

    static boolean DEBUG = false;
    boolean quiet;
    boolean saveQuiet;

    public static String ePubVersion;

    private final List<String> collectedMessages = new ArrayList<>();

    public CustomReport( String ePubName ) {
        this( ePubName, null, false );
    }

    public CustomReport( String ePubName, String info, boolean quiet ) {
        this.quiet = quiet;
        String adjustedPath = PathUtil.removeWorkingDirectory( ePubName );
        this.setEpubFileName( adjustedPath );
        if ( info != null ) {
            // warning("", 0, 0, info);
        }
    }

    /**
     * FATAL / ERROR / WARNING messages collected during EpubCheck (for end-of-run reprint).
     */
    public List<String> getCollectedMessages() {
        return Collections.unmodifiableList( collectedMessages );
    }

    String fixMessage( String message ) {
        if ( message == null ) {
            return "";
        }
        return message.replaceAll( "[\\s]+", " " );
    }

    boolean pushQuiet() {
        saveQuiet = outWriter.isQuiet();
        outWriter.setQuiet( quiet );
        return saveQuiet;
    }

    void popQuiet() {
        outWriter.setQuiet( saveQuiet );
    }

    @Override
    public void message( Message message, EPUBLocation location, Object... args ) {
        Severity severity = message.getSeverity();
        String text = formatMessage( message, location, args );
        if ( severity.equals( Severity.FATAL ) || severity.equals( Severity.ERROR )
                || severity.equals( Severity.WARNING ) ) {
            collectedMessages.add( text );
        }
        if ( severity.equals( Severity.USAGE ) ) {
            pushQuiet();
            LOGGER.info( text );
            popQuiet();
        } else {
            LOGGER.error( text );
        }
    }

    String formatMessage( Message message, EPUBLocation location, Object... args ) {
        String epubFileName = PathUtil.removeWorkingDirectory( this.getEpubFileName() );
        String fileName = PathUtil.removeWorkingDirectory( location.getPath() );
        // remove duplicate epub name from path and empty fileName variable
        fileName = epubFileName.endsWith( fileName ) ? "" : "/" + fileName;
        return String.format( "%1$s(%2$s): %3$s%4$s(%5$s,%6$s): %7$s", message.getSeverity(), message.getID(),
                epubFileName, fileName, location.getLine(), location.getColumn(), fixMessage( args != null
                        && args.length > 0 ? message.getMessage( args ) : message.getMessage() ) );
    }

    @Override
    public void info( String resource, FeatureEnum feature, String value ) {
        if ( ReportingLevel.Info >= getReportingLevel() ) {
            switch ( feature ) {
            case FORMAT_VERSION:
                if ( !quiet ) {
                    LOGGER.info( String.format( getMessages().get( "validating_version_message" ), value ) );
                }
                break;
            default:
                if ( DEBUG && !quiet ) {
                    if ( resource == null ) {
                        LOGGER.info( "INFO: [" + feature + "]=" + value );
                    } else {
                        LOGGER.info( "INFO: [" + feature + " (" + resource + ")]=" + value );
                    }
                }
                break;
            }
        }
    }

    @Override
    public int generate() {
        return 0;
    }

    @Override
    public void initialize() {
    }

}
