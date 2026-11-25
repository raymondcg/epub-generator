package org.webnerdbot.generator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CustomReportStore {

    private final int maxEntries;
    private final Map<String, EpubCheckReportDto> reports;

    public CustomReportStore( int maxEntries ) {
        if ( maxEntries < 1 ) {
            throw new IllegalArgumentException( "maxEntries must be at least 1" );
        }
        this.maxEntries = maxEntries;
        this.reports = new LinkedHashMap<>( 16, 0.75f, false ) {

            @Override
            protected boolean removeEldestEntry( Map.Entry<String, EpubCheckReportDto> eldest ) {
                return size() > CustomReportStore.this.maxEntries;
            }
        };
    }

    public synchronized void put( String bookId, EpubCheckReportDto report ) {
        // Re-insert so overwritten keys move to newest (end) of insertion order.
        reports.remove( bookId );
        reports.put( bookId, report );
    }

    public synchronized Optional<EpubCheckReportDto> get( String bookId ) {
        return Optional.ofNullable( reports.get( bookId ) );
    }

    public synchronized List<EpubCheckReportDto> getAll() {
        return new ArrayList<>( reports.values() );
    }

    public synchronized void clear() {
        reports.clear();
    }

    public int getMaxEntries() {
        return maxEntries;
    }
}
