package org.webnerdbot.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Chapter implements Comparable<Chapter> {

    private String id;

    @JsonProperty("chapterNumber")
    private String number;

    @JsonProperty("chapterTitle")
    private String title;

    private String body = "";
    private String url;

    public Chapter() {
        super();
    }

    public String getId() {
        return id;
    }

    public void setId( String id ) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl( String url ) {
        this.url = url;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber( String number ) {
        this.number = number;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle( String title ) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody( String body ) {
        this.body = body;
    }

    @Override
    public int hashCode() {
        return Objects.hash( body, id, number, title, url );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }
        if ( obj == null || getClass() != obj.getClass() ) {
            return false;
        }
        Chapter other = (Chapter) obj;
        return Objects.equals( body, other.body ) && Objects.equals( id, other.id )
                && Objects.equals( number, other.number ) && Objects.equals( title, other.title )
                && Objects.equals( url, other.url );
    }

    @Override
    public String toString() {
        return "Chapter [id=" + id + ", number=" + number + ", title=" + title + "]";
    }

    @Override
    public int compareTo( Chapter otherChapter ) {
        return Integer.compare( parseChapterNumber( this.number ), parseChapterNumber( otherChapter.number ) );
    }

    private static int parseChapterNumber( String number ) {
        if ( number == null || number.isBlank() ) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt( number.trim() );
        } catch ( NumberFormatException e ) {
            return Integer.MAX_VALUE;
        }
    }
}