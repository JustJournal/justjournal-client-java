package com.justjournal.client;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Moods known to the justjournal server, from the mood table in its
 * database/jj_data_load.sql. "Not Specified" comes first, then the rest by title.
 *
 * @author Lucas Holt
 */
final class Moods {

    static final String NOT_SPECIFIED = "Not Specified";
    static final int NOT_SPECIFIED_ID = 12;

    private static final Map<String, Integer> IDS = new LinkedHashMap<>();

    static {
        put(NOT_SPECIFIED, NOT_SPECIFIED_ID);
        put("Accepted", 19);
        put("Accomplished", 14);
        put("Aggravated", 25);
        put("Alone", 20);
        put("Amused", 15);
        put("Angry", 5);
        put("Annoyed", 16);
        put("Anxious", 17);
        put("Apathetic", 97);
        put("Ashamed", 21);
        put("Awake", 3);
        put("Bewildered", 10);
        put("Bitchy", 26);
        put("Bittersweet", 22);
        put("Blah", 92);
        put("Blank", 98);
        put("Blissful", 23);
        put("Bored", 18);
        put("Bouncy", 44);
        put("Calm", 64);
        put("Cheerful", 47);
        put("Chipper", 49);
        put("Cold", 115);
        put("Complacent", 70);
        put("Confused", 9);
        put("Content", 69);
        put("Cranky", 27);
        put("Crappy", 99);
        put("Crazy", 78);
        put("Crushed", 100);
        put("Curious", 39);
        put("Cynical", 28);
        put("Dark", 24);
        put("Depressed", 101);
        put("Determined", 40);
        put("Devious", 42);
        put("Dirty", 116);
        put("Disappointed", 102);
        put("Discontent", 103);
        put("Ditzy", 79);
        put("Dorky", 89);
        put("Drained", 119);
        put("Drunk", 117);
        put("Ecstatic", 51);
        put("Energetic", 43);
        put("Enraged", 29);
        put("Enthralled", 46);
        put("Envious", 104);
        put("Exanimate", 96);
        put("Excited", 52);
        put("Exhausted", 118);
        put("Flirty", 80);
        put("Frustrated", 30);
        put("Full", 72);
        put("Gay", 7);
        put("Geeky", 90);
        put("Giddy", 81);
        put("Giggly", 82);
        put("Gloomy", 105);
        put("Good", 53);
        put("Grateful", 54);
        put("Groggy", 120);
        put("Grumpy", 31);
        put("Guilty", 122);
        put("Happy", 1);
        put("High", 50);
        put("Hopeful", 59);
        put("Horny", 48);
        put("Hot", 123);
        put("Hungry", 13);
        put("Hyper", 45);
        put("Impressed", 55);
        put("Indescribable", 87);
        put("Indifferent", 71);
        put("Infuriated", 32);
        put("Irate", 33);
        put("Irritated", 34);
        put("Jealous", 107);
        put("Jubilant", 56);
        put("Lazy", 93);
        put("Lethargic", 94);
        put("Listless", 95);
        put("Lonely", 108);
        put("Loved", 57);
        put("Mad", 6);
        put("Melancholy", 109);
        put("Mellow", 65);
        put("Mischievous", 83);
        put("Moody", 35);
        put("Morose", 110);
        put("Naughty", 84);
        put("Nerdy", 88);
        put("Numb", 111);
        put("Okay", 91);
        put("Optimistic", 58);
        put("Peaceful", 66);
        put("Pessimistic", 106);
        put("Pissed off", 36);
        put("Pleased", 60);
        put("Predatory", 41);
        put("Quixotic", 85);
        put("Recumbent", 67);
        put("Refreshed", 61);
        put("Rejected", 112);
        put("Rejuvenated", 62);
        put("Relaxed", 63);
        put("Relieved", 73);
        put("Restless", 124);
        put("Rushed", 38);
        put("Sad", 2);
        put("Satisfied", 68);
        put("Shocked", 77);
        put("Sick", 125);
        put("Silly", 8);
        put("Sleepy", 121);
        put("Smart", 11);
        put("Stressed", 37);
        put("Surprised", 76);
        put("Sympathetic", 113);
        put("Thankful", 74);
        put("Tired", 4);
        put("Touched", 75);
        put("Uncomfortable", 114);
        put("Weird", 86);
    }

    private Moods() {
    }

    private static void put(final String title, final int id) {
        IDS.put(title, id);
    }

    /**
     * @return mood titles in display order
     */
    static String[] titles() {
        return IDS.keySet().toArray(new String[0]);
    }

    /**
     * Looks up the server id for a mood title.
     *
     * @param title mood title
     * @return the mood id, or the "Not Specified" id for an unknown or null title
     */
    static int idFor(final String title) {
        if (title == null) {
            return NOT_SPECIFIED_ID;
        }
        return IDS.getOrDefault(title, NOT_SPECIFIED_ID);
    }
}
