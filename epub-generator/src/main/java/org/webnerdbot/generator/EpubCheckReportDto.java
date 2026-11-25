package org.webnerdbot.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EpubCheckReportDto {

    private String bookId;
    private String epubName;
    private int fatalErrorCount;
    private int errorCount;
    private int warningCount;
    private List<String> messages = new ArrayList<>();

    public EpubCheckReportDto() {
    }

    public EpubCheckReportDto( String bookId, CustomReport report ) {
        this.bookId = bookId;
        this.epubName = report.getEpubFileName();
        this.fatalErrorCount = report.getFatalErrorCount();
        this.errorCount = report.getErrorCount();
        this.warningCount = report.getWarningCount();
        this.messages = new ArrayList<>( report.getCollectedMessages() );
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId( String bookId ) {
        this.bookId = bookId;
    }

    public String getEpubName() {
        return epubName;
    }

    public void setEpubName( String epubName ) {
        this.epubName = epubName;
    }

    public int getFatalErrorCount() {
        return fatalErrorCount;
    }

    public void setFatalErrorCount( int fatalErrorCount ) {
        this.fatalErrorCount = fatalErrorCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount( int errorCount ) {
        this.errorCount = errorCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public void setWarningCount( int warningCount ) {
        this.warningCount = warningCount;
    }

    public List<String> getMessages() {
        return Collections.unmodifiableList( messages );
    }

    public void setMessages( List<String> messages ) {
        this.messages = messages != null ? new ArrayList<>( messages ) : new ArrayList<>();
    }
}
