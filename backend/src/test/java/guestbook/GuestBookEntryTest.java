package guestbook;

import org.junit.jupiter.api.Test;

import static java.time.OffsetDateTime.now;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GuestBookEntryTest {

    @Test
    void test_author() {
        GuestBookEntry entry = new GuestBookEntry("author", "Hello!", "imageUrl", now());

        assertEquals("author", entry.getAuthor());
    }

    @Test
    void test_message() {
        GuestBookEntry entry = new GuestBookEntry("author", "Hello!", "imageUrl", now());

        assertEquals("Hello!", entry.getMessage());
    }
}