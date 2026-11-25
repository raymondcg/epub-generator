package org.webnerdbot.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

public class CustomReportStoreTest {

    @Test
    void putOverwritesSameBookId() {
        CustomReportStore store = new CustomReportStore( 10 );
        EpubCheckReportDto first = new EpubCheckReportDto();
        first.setBookId( "b1" );
        first.setErrorCount( 1 );
        EpubCheckReportDto second = new EpubCheckReportDto();
        second.setBookId( "b1" );
        second.setErrorCount( 0 );

        store.put( "b1", first );
        store.put( "b1", second );

        assertEquals( 1, store.getAll().size() );
        assertEquals( 0, store.get( "b1" ).orElseThrow().getErrorCount() );
    }

    @Test
    void missingKeyIsEmpty() {
        assertTrue( new CustomReportStore( 10 ).get( "missing" ).isEmpty() );
    }

    @Test
    void evictsOldestWhenOverCap() {
        CustomReportStore store = new CustomReportStore( 2 );
        store.put( "a", dto( "a" ) );
        store.put( "b", dto( "b" ) );
        store.put( "c", dto( "c" ) );

        List<EpubCheckReportDto> all = store.getAll();
        assertEquals( 2, all.size() );
        assertTrue( store.get( "a" ).isEmpty() );
        assertTrue( store.get( "b" ).isPresent() );
        assertTrue( store.get( "c" ).isPresent() );
    }

    @Test
    void overwriteDoesNotEvictOthers() {
        CustomReportStore store = new CustomReportStore( 2 );
        store.put( "a", dto( "a" ) );
        store.put( "b", dto( "b" ) );
        store.put( "a", dto( "a" ) );

        assertEquals( 2, store.getAll().size() );
        assertTrue( store.get( "a" ).isPresent() );
        assertTrue( store.get( "b" ).isPresent() );
    }

    private static EpubCheckReportDto dto( String bookId ) {
        EpubCheckReportDto dto = new EpubCheckReportDto();
        dto.setBookId( bookId );
        return dto;
    }
}
