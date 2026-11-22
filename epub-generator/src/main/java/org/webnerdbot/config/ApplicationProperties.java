package org.webnerdbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApplicationProperties {

    @Value("${BOOK_DB}")
    private String bookDb;

    @Value("${EPUB_OUTPUT}")
    private String epubOutput;

    @Value("${CHAPTERS_PER_VOLUME:1000}")
    private int chaptersPerVolume;

    public String getBookDb() {
        return bookDb;
    }

    public String getEpubOutput() {
        return epubOutput;
    }

    public int getChaptersPerVolume() {
        return chaptersPerVolume;
    }
}
