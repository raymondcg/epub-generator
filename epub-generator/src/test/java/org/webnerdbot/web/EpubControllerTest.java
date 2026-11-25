package org.webnerdbot.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.webnerdbot.generator.CustomReport;
import org.webnerdbot.generator.CustomReportStore;
import org.webnerdbot.generator.EpubCheckReportDto;
import org.webnerdbot.generator.GenerationResult;
import org.webnerdbot.generator.GeneratorService;
import org.webnerdbot.model.Book;

@WebMvcTest(controllers = EpubController.class)
@Import(ApiExceptionHandler.class)
public class EpubControllerTest {

    private static final String BOOK_JSON = """
            {"id":"ancientgodlymonarch","title":"Ancient Godly Monarch","chapters":[]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeneratorService generatorService;

    @MockitoBean
    private CustomReportStore customReportStore;

    @DisplayName("POST generate returns epub bytes")
    @Test
    void generateHappyPath() throws Exception {
        CustomReport report = new CustomReport( "ancientgodlymonarch" );
        byte[] epub = new byte[] { 0x50, 0x4B, 0x03, 0x04, 0x00 };
        when( generatorService.generateEpub( any( Book.class ), any( byte[].class ), nullable( String.class ) ) )
                .thenReturn( new GenerationResult( epub, "ancientgodlymonarch.epub", report ) );

        MockMultipartFile book = new MockMultipartFile( "book", "", "application/json", BOOK_JSON.getBytes() );
        MockMultipartFile cover = new MockMultipartFile( "cover", "cover.jpg", "image/jpeg",
                new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF } );

        mockMvc.perform( multipart( "/api/v1/epub" ).file( book ).file( cover ) ).andExpect( status().isOk() )
                .andExpect( content().contentType( MediaType.parseMediaType( "application/epub+zip" ) ) )
                .andExpect(
                        header().string( "Content-Disposition", "attachment; filename=\"ancientgodlymonarch.epub\"" ) )
                .andExpect( content().bytes( epub ) );
    }

    @DisplayName("POST sanitizes Content-Disposition filename")
    @Test
    void generateSanitizesFileName() throws Exception {
        CustomReport report = new CustomReport( "x" );
        byte[] epub = new byte[] { 0x50, 0x4B };
        when( generatorService.generateEpub( any( Book.class ), any( byte[].class ), nullable( String.class ) ) )
                .thenReturn( new GenerationResult( epub, "bad\"name\r\n.epub", report ) );

        MockMultipartFile book = new MockMultipartFile( "book", "", "application/json", BOOK_JSON.getBytes() );
        MockMultipartFile cover = new MockMultipartFile( "cover", "cover.jpg", "image/jpeg",
                new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF } );

        mockMvc.perform( multipart( "/api/v1/epub" ).file( book ).file( cover ) ).andExpect( status().isOk() )
                .andExpect( header().string( "Content-Disposition", "attachment; filename=\"badname.epub\"" ) );
    }

    @DisplayName("POST unsupported cover type returns 400")
    @Test
    void generateUnsupportedCover() throws Exception {
        when( generatorService.generateEpub( any( Book.class ), any( byte[].class ), nullable( String.class ) ) )
                .thenThrow( new IllegalArgumentException( "Unsupported cover image type; use JPEG or PNG" ) );

        MockMultipartFile book = new MockMultipartFile( "book", "", "application/json", BOOK_JSON.getBytes() );
        MockMultipartFile cover = new MockMultipartFile( "cover", "cover.gif", "image/gif", new byte[] { 1, 2, 3 } );

        mockMvc.perform( multipart( "/api/v1/epub" ).file( book ).file( cover ) ).andExpect( status().isBadRequest() )
                .andExpect( jsonPath( "$.error" ).value( "Unsupported cover image type; use JPEG or PNG" ) );
    }

    @DisplayName("POST without cover returns 400")
    @Test
    void generateMissingCover() throws Exception {
        MockMultipartFile book = new MockMultipartFile( "book", "", "application/json", BOOK_JSON.getBytes() );
        mockMvc.perform( multipart( "/api/v1/epub" ).file( book ) ).andExpect( status().isBadRequest() );
    }

    @DisplayName("POST without book returns 400")
    @Test
    void generateMissingBook() throws Exception {
        MockMultipartFile cover = new MockMultipartFile( "cover", "cover.jpg", "image/jpeg", new byte[] { 1 } );
        mockMvc.perform( multipart( "/api/v1/epub" ).file( cover ) ).andExpect( status().isBadRequest() );
    }

    @DisplayName("POST with invalid JSON returns 400")
    @Test
    void generateInvalidJson() throws Exception {
        MockMultipartFile book = new MockMultipartFile( "book", "", "application/json", "{not-json".getBytes() );
        MockMultipartFile cover = new MockMultipartFile( "cover", "cover.jpg", "image/jpeg", new byte[] { 1 } );
        mockMvc.perform( multipart( "/api/v1/epub" ).file( book ).file( cover ) ).andExpect( status().isBadRequest() )
                .andExpect( jsonPath( "$.error" ).exists() );
    }

    @DisplayName("POST with blank id returns 400")
    @Test
    void generateBlankId() throws Exception {
        String json = "{\"id\":\"\",\"title\":\"No Id\"}";
        MockMultipartFile book = new MockMultipartFile( "book", "", "application/json", json.getBytes() );
        MockMultipartFile cover = new MockMultipartFile( "cover", "cover.jpg", "image/jpeg", new byte[] { 1 } );
        mockMvc.perform( multipart( "/api/v1/epub" ).file( book ).file( cover ) ).andExpect( status().isBadRequest() );
    }

    @DisplayName("POST maps generation failure to 500")
    @Test
    void generateServerError() throws Exception {
        when( generatorService.generateEpub( any( Book.class ), any( byte[].class ), nullable( String.class ) ) )
                .thenThrow( new RuntimeException( "boom" ) );
        MockMultipartFile book = new MockMultipartFile( "book", "", "application/json", BOOK_JSON.getBytes() );
        MockMultipartFile cover = new MockMultipartFile( "cover", "cover.jpg", "image/jpeg", new byte[] { 1 } );
        mockMvc.perform( multipart( "/api/v1/epub" ).file( book ).file( cover ) )
                .andExpect( status().isInternalServerError() )
                .andExpect( jsonPath( "$.error" ).value( "Internal server error" ) );
    }

    @DisplayName("Max upload size maps to 413")
    @Test
    void maxUploadReturns413() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        ResponseEntity<Map<String, String>> response = handler
                .handleMaxUpload( new MaxUploadSizeExceededException( 1024 ) );
        assertEquals( HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode() );
        assertEquals( "Upload exceeds configured max size", response.getBody().get( "error" ) );
    }

    @DisplayName("sanitizeFileName strips quotes and CRLF")
    @Test
    void sanitizeFileName() {
        assertEquals( "ok.epub", EpubController.sanitizeFileName( "ok\"\r\n.epub", "fallback" ) );
        assertEquals( "fallback.epub", EpubController.sanitizeFileName( "   ", "fallback" ) );
        assertEquals( "fallback.epub", EpubController.sanitizeFileName( ".epub", "fallback" ) );
    }

    @DisplayName("GET reports without bookId returns list")
    @Test
    void reportsAll() throws Exception {
        EpubCheckReportDto dto = new EpubCheckReportDto();
        dto.setBookId( "ancientgodlymonarch" );
        dto.setEpubName( "ancientgodlymonarch" );
        when( customReportStore.getAll() ).thenReturn( List.of( dto ) );

        mockMvc.perform( get( "/api/v1/epub/reports" ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$[0].bookId" ).value( "ancientgodlymonarch" ) )
                .andExpect( jsonPath( "$[0].epubName" ).value( "ancientgodlymonarch" ) );
    }

    @DisplayName("GET reports empty list")
    @Test
    void reportsEmpty() throws Exception {
        when( customReportStore.getAll() ).thenReturn( List.of() );
        mockMvc.perform( get( "/api/v1/epub/reports" ) ).andExpect( status().isOk() )
                .andExpect( content().json( "[]" ) );
    }

    @DisplayName("GET report by bookId returns single report")
    @Test
    void reportById() throws Exception {
        EpubCheckReportDto dto = new EpubCheckReportDto();
        dto.setBookId( "ancientgodlymonarch" );
        dto.setFatalErrorCount( 0 );
        dto.setErrorCount( 1 );
        dto.setWarningCount( 2 );
        dto.setMessages( List.of( "ERROR(X): issue" ) );
        when( customReportStore.get( eq( "ancientgodlymonarch" ) ) ).thenReturn( Optional.of( dto ) );

        mockMvc.perform( get( "/api/v1/epub/reports" ).param( "bookId", "ancientgodlymonarch" ) )
                .andExpect( status().isOk() ).andExpect( jsonPath( "$.bookId" ).value( "ancientgodlymonarch" ) )
                .andExpect( jsonPath( "$.errorCount" ).value( 1 ) )
                .andExpect( jsonPath( "$.messages[0]" ).value( "ERROR(X): issue" ) );
    }

    @DisplayName("GET unknown bookId returns 404")
    @Test
    void reportUnknown() throws Exception {
        when( customReportStore.get( eq( "missing" ) ) ).thenReturn( Optional.empty() );
        mockMvc.perform( get( "/api/v1/epub/reports" ).param( "bookId", "missing" ) )
                .andExpect( status().isNotFound() );
    }
}
