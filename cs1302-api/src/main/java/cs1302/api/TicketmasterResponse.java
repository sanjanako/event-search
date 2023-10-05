package cs1302.api;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a response from the Ticketmaster API.
 */
public class TicketmasterResponse {
    @SerializedName("_embedded") Embedded embedded;
} // TicketmasterResponse
