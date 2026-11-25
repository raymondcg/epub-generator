package org.webnerdbot.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CustomReportStoreTest {

    @Test
    void putOverwritesSameBookId() {
        CustomReportStore store = new CustomReportStore();
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
        assertTrue( new CustomReportStore().get( "missing" ).isEmpty() );
    }
}
