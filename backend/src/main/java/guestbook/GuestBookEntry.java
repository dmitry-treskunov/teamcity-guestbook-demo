package guestbook;

import java.time.OffsetDateTime;

public class GuestBookEntry {
    private final String author;
    private final String message;
    private final String imageUrl;
    private final OffsetDateTime createdAt;

    public GuestBookEntry(String author, String message, String imageUrl, OffsetDateTime createdAt){
        this.author = author;
        this.message = message;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }

    public String getAuthor() {
        return author;
    }

    public String getMessage() {
        return message;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return String.format("Entry[Author:'%s', Message: '%s']", this.author, this.message);
    }
}
