package org.webnerdbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "epub.report-store")
public class ReportStoreProperties {

    /**
     * Maximum number of EpubCheck reports retained in memory. Oldest entries are evicted when exceeded.
     */
    private int maxEntries = 100;

    public int getMaxEntries() {
        return maxEntries;
    }

    public void setMaxEntries( int maxEntries ) {
        this.maxEntries = maxEntries;
    }
}
