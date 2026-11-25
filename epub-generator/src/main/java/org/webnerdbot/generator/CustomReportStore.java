package org.webnerdbot.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CustomReportStore {

    private final ConcurrentHashMap<String, EpubCheckReportDto> reports = new ConcurrentHashMap<>();

    public void put( String bookId, EpubCheckReportDto report ) {
        reports.put( bookId, report );
    }

    public Optional<EpubCheckReportDto> get( String bookId ) {
        return Optional.ofNullable( reports.get( bookId ) );
    }

    public List<EpubCheckReportDto> getAll() {
        return new ArrayList<>( reports.values() );
    }

    public void clear() {
        reports.clear();
    }
}
