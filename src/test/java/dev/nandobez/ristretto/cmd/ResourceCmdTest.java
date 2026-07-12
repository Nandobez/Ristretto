package dev.nandobez.ristretto.cmd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ResourceCmdTest {

    @ParameterizedTest
    @CsvSource({
        "Post,     Posts",
        "Category, Categories",
        "Box,      Boxes",
        "Dish,     Dishes",
        "Church,   Churches",
        "Buzz,     Buzzes",
        "Day,      Days",       // vowel + y keeps the y
        "User,     Users",
    })
    void pluralizesCommonEnglishNouns(String singular, String plural) {
        assertEquals(plural, ResourceCmd.pluralize(singular));
    }

    @Test
    void handlesEmptyAndNull() {
        assertEquals("", ResourceCmd.pluralize(""));
        assertNull(ResourceCmd.pluralize(null));
    }
}
