package guestbook;

import com.fasterxml.jackson.annotation.JsonSetter;

public class EntryRequestModel {
    EntryRequestModel() {}

    private String author;
    private String message;
    private String imageUrl;

    public String getAuthor() {
        return author;
    }

    @JsonSetter("name")
    public void setAuthor(String author) {
        this.author = author;
    }

    public String getMessage() {
        return message;
    }

    @JsonSetter("msg")
    public void setMessage(String message) {
        this.message = message;
    }

    public String getImageUrl() {
        return "";
    }

    @JsonSetter("img_url")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
