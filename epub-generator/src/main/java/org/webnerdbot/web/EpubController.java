package org.webnerdbot.web;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.webnerdbot.generator.CustomReportStore;
import org.webnerdbot.generator.EpubCheckReportDto;
import org.webnerdbot.generator.GenerationResult;
import org.webnerdbot.generator.GeneratorService;
import org.webnerdbot.model.Book;
import org.webnerdbot.model.BookMapper;

@RestController
@RequestMapping("/api/v1/epub")
public class EpubController {

    private static final MediaType EPUB_MEDIA_TYPE = MediaType.parseMediaType( "application/epub+zip" );

    private final GeneratorService generatorService;
    private final CustomReportStore customReportStore;
    private final BookMapper bookMapper;

    public EpubController( GeneratorService generatorService, CustomReportStore customReportStore ) {
        this.generatorService = generatorService;
        this.customReportStore = customReportStore;
        this.bookMapper = new BookMapper();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> generate( @RequestPart("book") String bookJson,
            @RequestPart("cover") MultipartFile cover ) throws IOException {
        if ( bookJson == null || bookJson.isBlank() ) {
            throw new IllegalArgumentException( "Book JSON part is required" );
        }
        if ( cover == null || cover.isEmpty() ) {
            throw new IllegalArgumentException( "Cover image part is required" );
        }

        Book book;
        try {
            book = bookMapper.fromJson( bookJson );
        } catch ( IOException e ) {
            throw new IllegalArgumentException( "Invalid book JSON: " + e.getMessage(), e );
        }
        if ( book.getId() == null || book.getId().isBlank() ) {
            throw new IllegalArgumentException( "Book id is required" );
        }

        GenerationResult result = generatorService.generateEpub( book, cover.getBytes() );
        return ResponseEntity.ok().contentType( EPUB_MEDIA_TYPE )
                .header( HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.epubFileName() + "\"" )
                .body( result.epubBytes() );
    }

    @GetMapping("/reports")
    public ResponseEntity<?> reports( @RequestParam(value = "bookId", required = false) String bookId ) {
        if ( bookId == null || bookId.isBlank() ) {
            List<EpubCheckReportDto> all = customReportStore.getAll();
            return ResponseEntity.ok( all );
        }
        return customReportStore.get( bookId ).<ResponseEntity<?>> map( ResponseEntity::ok )
                .orElseGet( () -> ResponseEntity.status( HttpStatus.NOT_FOUND ).build() );
    }
}
