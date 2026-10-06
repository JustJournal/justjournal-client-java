package com.justjournal.client;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Lucas Holt
 */
public class MoodsTest {

    @Test
    public void testTitlesMatchServerTable() {
        final String[] titles = Moods.titles();
        assertEquals(125, titles.length);
        assertEquals(titles.length, new HashSet<>(Arrays.asList(titles)).size());
    }

    @Test
    public void testNotSpecifiedFirstThenSorted() {
        final String[] titles = Moods.titles();
        assertEquals(Moods.NOT_SPECIFIED, titles[0]);

        final String[] rest = Arrays.copyOfRange(titles, 1, titles.length);
        final String[] sorted = rest.clone();
        Arrays.sort(sorted, String.CASE_INSENSITIVE_ORDER);
        assertEquals(Arrays.asList(sorted), Arrays.asList(rest));
    }

    @Test
    public void testIdsCoverServerRange() {
        final Set<Integer> ids = new HashSet<>();
        for (final String title : Moods.titles()) {
            ids.add(Moods.idFor(title));
        }
        assertEquals(125, ids.size());
        for (int id = 1; id <= 125; id++) {
            assertTrue("missing mood id " + id, ids.contains(id));
        }
    }

    @Test
    public void testIdFor() {
        assertEquals(1, Moods.idFor("Happy"));
        assertEquals(2, Moods.idFor("Sad"));
        assertEquals(36, Moods.idFor("Pissed off"));
        assertEquals(Moods.NOT_SPECIFIED_ID, Moods.idFor(Moods.NOT_SPECIFIED));
    }

    @Test
    public void testIdForUnknown() {
        assertEquals(Moods.NOT_SPECIFIED_ID, Moods.idFor("Meh"));
        assertEquals(Moods.NOT_SPECIFIED_ID, Moods.idFor("happy"));
        assertEquals(Moods.NOT_SPECIFIED_ID, Moods.idFor(""));
        assertEquals(Moods.NOT_SPECIFIED_ID, Moods.idFor(null));
    }
}
