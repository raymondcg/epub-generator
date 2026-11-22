package org.webnerdbot.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Book {

    @JsonDeserialize(using = JsonDateDeserializer.class)
    @JsonSerialize(using = JsonDateSerializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate crawldate;

    private String id;
    private UUID uid = UUID.randomUUID();
    private String publisher;
    private String title;
    private String author;
    private String translator;
    private String editor;
    private String subject;
    private String description;
    private List<Chapter> chapters = new ArrayList<>();

    public Book() {
        super();
    }

    /**
     * Cover / EPUB id: prefer JSON {@code id}, otherwise derive from title.
     */
    public String getBookIdString() {
        if ( id != null && !id.isBlank() ) {
            return id;
        }
        if ( title == null ) {
            return "";
        }
        return title.replace( " ", "" ).toLowerCase();
    }

    public String getId() {
        return id;
    }

    public void setId( String id ) {
        this.id = id;
    }

    public UUID getUid() {
        return uid;
    }

    public void setUid( UUID uid ) {
        this.uid = uid;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher( String publisher ) {
        this.publisher = publisher;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle( String title ) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor( String author ) {
        this.author = author;
    }

    public String getTranslator() {
        return translator;
    }

    public void setTranslator( String translator ) {
        this.translator = translator;
    }

    public String getEditor() {
        return editor;
    }

    public void setEditor( String editor ) {
        this.editor = editor;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject( String subject ) {
        this.subject = subject;
    }

    public LocalDate getCrawldate() {
        return crawldate;
    }

    public void setCrawldate( LocalDate crawldate ) {
        this.crawldate = crawldate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public List<Chapter> getChapters() {
        return chapters;
    }

    public void setChapters( List<Chapter> chapters ) {
        this.chapters = chapters != null ? chapters : new ArrayList<>();
    }

    @Override
    public int hashCode() {
        return Objects.hash( author, chapters, crawldate, description, editor, id, publisher, subject, title,
                translator, uid );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }
        if ( obj == null || getClass() != obj.getClass() ) {
            return false;
        }
        Book other = (Book) obj;
        return Objects.equals( author, other.author ) && Objects.equals( chapters, other.chapters )
                && Objects.equals( crawldate, other.crawldate ) && Objects.equals( description, other.description )
                && Objects.equals( editor, other.editor ) && Objects.equals( id, other.id )
                && Objects.equals( publisher, other.publisher ) && Objects.equals( subject, other.subject )
                && Objects.equals( title, other.title ) && Objects.equals( translator, other.translator )
                && Objects.equals( uid, other.uid );
    }

    @Override
    public String toString() {
        return "Book [id=" + id + ", title=" + title + ", author=" + author + ", chapters="
                + ( chapters == null ? 0 : chapters.size() ) + "]";
    }
}
