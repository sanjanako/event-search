package cs1302.api;

import com.google.gson.annotations.SerializedName;

/**
 * Represents part of a response from the Ticketmaster API.
 * An {@code Event} object represents one specific event.
 */
public class Event {
    String name;
    String id;
    Image[] images;
    Date dates;
    @SerializedName("_embedded") EmbeddedTwo embedded;
} // Event
