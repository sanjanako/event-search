package cs1302.api;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpRequest;
import java.net.URI;
import java.net.URL;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.ParseException;

/**
 * REPLACE WITH NON-SHOUTING DESCRIPTION OF YOUR APP.
 */
public class ApiApp extends Application {

    /** File path for default image. */
    public static final String SUNSET_IMG = "file:resources/sunset.png";

    /** HTTP client. */
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    /** Google {@code Gson} object for parsing JSON-formatted strings. */
    public static Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    /** API key for Ticketmaster API. */
    private final String apiKey = getApiKey();

    /** Root URL for Ticketmaster API. */
    private static final String TICKETMASTER_API =
        "https://app.ticketmaster.com/discovery/v2/events";

    /** Root URL for Sunrise Sunset API. */
    private static final String SUNSET_API =
        "https://api.sunrise-sunset.org/json";

    // the query URI for the Ticketmaster API
    private String uri;

    // the query URI for the Sunrise Sunset API
    private String uriTwo;

    Stage stage;
    Scene scene;
    VBox root;

    HBox searchPane;
    Label searchLabel;
    TextField searchBar;
    Button searchButton;

    Label instructions;

    Separator separator;

    ImageView imgView;

    Label event;
    Label eventVenue;
    Label eventTime;
    Label sunset;

    /**
     * Constructs an {@code ApiApp} object. This default (i.e., no argument)
     * constructor is executed in Step 2 of the JavaFX Application Life-Cycle.
     */
    public ApiApp() {
        this.stage = null;
        this.scene = null;
        root = new VBox(8);

        searchPane = new HBox(4);
        searchLabel = new Label("Search: ");
        searchBar = new TextField("atlanta");
        searchButton = new Button("Find Event");

        instructions = new Label(
            "Enter a US city to find an event that will happen nearby.");

        separator = new Separator();

        event = new Label("Event: ");
        eventVenue = new Label("Event Venue: ");
        eventTime = new Label("Event Time: ");
        sunset = new Label("Sunset Time: ");

        imgView = new ImageView(SUNSET_IMG);
    } // ApiApp

    /** {@inheritDoc} */
    @Override
    public void init() {
        HBox.setHgrow(searchBar, Priority.ALWAYS);
        HBox.setHgrow(searchButton, Priority.ALWAYS);
        searchButton.setMaxWidth(Double.MAX_VALUE);
        searchPane.getChildren().addAll(searchLabel, searchBar, searchButton);
        searchPane.setAlignment(Pos.CENTER);
        imgView.setFitWidth(425);
        imgView.setPreserveRatio(true);

        root.getChildren().addAll(
            searchPane, instructions, separator, event, eventVenue, eventTime, sunset, imgView);

        // whenever user clicks search button:
        Runnable task = () -> this.searchInfo();
        this.searchButton.setOnAction(event -> runInNewThread(task));
    } // init

    /** {@inheritDoc} */
    @Override
    public void start(Stage stage) {
        // setup stage
        this.stage = stage;
        this.scene = new Scene(root);
        this.stage.setTitle("ApiApp!");
        this.stage.setScene(this.scene);
        this.stage.setOnCloseRequest(event -> Platform.exit());
        this.stage.sizeToScene();
        this.stage.show();
        Platform.runLater(() -> this.stage.setResizable(false));
    } // start

    /**
     * Returns the Ticketmaster API key from the {@code "resources/config.properties"}
     * file.
     *
     * @return API key
     */
    public String getApiKey() {
        String configPath = "resources/config.properties";
        String apiKey = "";

        try (FileInputStream configFileStream = new FileInputStream(configPath)) {
            Properties config = new Properties();
            config.load(configFileStream);
            apiKey = config.getProperty("ticketmasterapi.apikey");
        } catch (IOException ioe) {
            System.out.println(ioe);
            ioe.printStackTrace();
        } // try

        return apiKey;
    } // getApiKey

    /**
     * Retrieves the JSON string for a specific query to the
     * Ticketmaster API.
     *
     * @return JSON string
     */
    public String retrieveJson() {
        String jsonString = "";

        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        try {
            // forming the URI
            String apikey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            String startDateTime = URLEncoder.encode(now.toString(), StandardCharsets.UTF_8);
            String size = URLEncoder.encode("1", StandardCharsets.UTF_8);
            String sort = URLEncoder.encode("relevance,asc", StandardCharsets.UTF_8);
            String city = URLEncoder.encode(searchBar.getText(), StandardCharsets.UTF_8);
            String query = String.format(
                "?apikey=%s&startDateTime=%s&size=%s&sort=%s&city=%s",
                apikey, startDateTime, size, sort, city);

            uri = TICKETMASTER_API + query;

            // building the request
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .build();

            // sending request and receiving a String response
            HttpResponse<String> response = HTTP_CLIENT
                .send(request, BodyHandlers.ofString());

            // make sure request is ok
            if (response.statusCode() != 200) {
                throw new IOException(response.toString());
            } // if

            // getting request body
            jsonString = response.body();
            jsonString = jsonString.trim();
        } catch (IOException | InterruptedException e) {
            Platform.runLater(() -> alertError(e));
            searchButton.setDisable(false);
        } // try

        return jsonString;

    } // retrieveTicketJson

    /**
     * Retrieves specific information from the JSON string after parsing with GSON.
     * Certain values on scene graph are changed based on the query from Ticketmaster API.
     */
    public void retrieveInfo() {
        TicketmasterResponse ticketmasterResponse = GSON
            .fromJson(this.retrieveJson(), TicketmasterResponse.class);

        try {
            if (ticketmasterResponse.embedded == null) {
                throw new IllegalArgumentException("No events were found for the searched city.");
            } // if

            // getting values using GSON
            String eventName = ticketmasterResponse.embedded.events[0].name;
            String venueName = ticketmasterResponse.embedded.events[0].embedded.venues[0].name;
            String eventDate = ticketmasterResponse.embedded.events[0].dates.start.localDate;
            String startTime = ticketmasterResponse.embedded.events[0].dates.start.localTime;
            String formattedStartTime = to12HourTime(startTime);

            // changing image in imageview to an image representing event
            String imgUrl = ticketmasterResponse.embedded.events[0].images[0].url;
            Image img = new Image(imgUrl);

            // adding event details to scene graph
            Platform.runLater(() -> event.setText("Event: " + eventName));
            Platform.runLater(() -> eventVenue.setText("Event Venue: " + venueName));
            Platform.runLater(
                () -> eventTime.setText("Event Time: " + eventDate + " at " + formattedStartTime));
            imgView.setImage(img);

        } catch (IllegalArgumentException e) {
            Platform.runLater(() -> instructions.setText("Last attempt to find event failed..."));
            Platform.runLater(() -> alertError(e));
        } // try
    } // retrieveInfo

     /**
     * Retrieves the JSON string for a specific query to the
     * Sunrise Sunset API.
     *
     * @return JSON string
     */
    public String retrieveJsonTwo() {
        TicketmasterResponse ticketmasterResponse = GSON
            .fromJson(this.retrieveJson(), TicketmasterResponse.class);
        String jsonString = "";

        if (ticketmasterResponse != null) {
            try {
                String eventLat =
                    "" +
                    ticketmasterResponse.embedded.events[0].embedded.venues[0].location.latitude;
                String eventLong =
                    "" +
                    ticketmasterResponse.embedded.events[0].embedded.venues[0].location.longitude;
                String eventDate = ticketmasterResponse.embedded.events[0].dates.start.localDate;

                // forming the URI
                String lat = URLEncoder.encode(eventLat, StandardCharsets.UTF_8);
                String lng = URLEncoder.encode(eventLong, StandardCharsets.UTF_8);
                String date = URLEncoder.encode(eventDate, StandardCharsets.UTF_8);
                String query = String.format("?lat=%s&lng=%s&date=%s", lat, lng, date);

                uriTwo = SUNSET_API + query;

                // building the request
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uriTwo))
                    .build();

                // sending request and receiving a String response
                HttpResponse<String> response = HTTP_CLIENT
                    .send(request, BodyHandlers.ofString());

                // make sure request is ok
                if (response.statusCode() != 200) {
                    throw new IOException(response.toString());
                } // if

                // getting request body
                jsonString = response.body();
                jsonString = jsonString.trim();
            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> alertError(e));
                searchButton.setDisable(false);
            } // try

        } // if

        return jsonString;
    } // retrieveJsonTwo

    /**
     * Displays the sunset time on app based on query from Ticketmaster APi
     * and Sunrise Sunset API.
     */
    public void setSunsetTime() {
        SunsetResponse sunsetResponse = GSON
            .fromJson(this.retrieveJsonTwo(), SunsetResponse.class);

        if (sunsetResponse != null) {
            String sunsetTime = sunsetResponse.results.sunset;
            String sunsetTime24 = to24HourTime(sunsetTime);
            String sunsetTime12 = convertTime(sunsetTime24);

            // adding sunset time to scene graph
            Platform.runLater(
                () -> sunset.setText(
                    "Sunset Time: " +
                    sunsetTime12.substring(0, sunsetTime12.length() - 3) + " PM"));
        } // if
    } // setSunsetTime

    /**
     * Loads and displays information about a certain event and sunset time.
     */
    public void searchInfo() {
        searchButton.setDisable(true);
        Platform.runLater(() -> instructions.setText(
            "Searching for event (intentional delay due to API rate limit)"));

        uri = "";
        uriTwo = "";

        // causes intentional delay to prevent Ticketmaster API rate limit to be reached
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } // try

        retrieveInfo();
        setSunsetTime();

        searchButton.setDisable(false);
        Platform.runLater(() -> instructions.setText(
            "Enter a US city to find an event that will happen nearby."));
    } // searchInfo

    /**
     * Shows an alert based on {@code cause}.
     *
     * @param cause a {@link java.lang.Throwable Throwable} that caused the alert
     */
    public void alertError(Throwable cause) {
        TextArea text = new TextArea(cause.toString());
        text.setEditable(false);
        Alert alert = new Alert(AlertType.ERROR);
        alert.getDialogPane().setContent(text);
        alert.setResizable(true);
        alert.showAndWait();
    } // alertError

    /**
     * Creates and starts a new daemon thread.
     *
     * @param target the object whose run method is invoked
     */
    public static void runInNewThread(Runnable target) {
        Thread t = new Thread(target);
        t.setDaemon(true);
        t.start();
    } // runInNewThread

    /**
     * Converts a 24-hour time to a 12-hour time.
     *
     * @param time the 24-hour time to be converted
     * @return the 12-hour equivalent time
     */
    public String to12HourTime(String time) {
        try {
            SimpleDateFormat time24 = new SimpleDateFormat("HH:mm");
            SimpleDateFormat time12 = new SimpleDateFormat("hh:mm a");
            Date hour24 = time24.parse(time);
            return time12.format(hour24).toString();
        } catch (ParseException e) {
            return time;
        } // try
    } // to12HourTime

    /**
     * Converts a 24-hour time to a 12-hour time.
     *
     * @param time the 24-hour time to be converted
     * @return the 12-hour equivalent time
     */
    public String to24HourTime(String time) {
        try {
            if (time.charAt(1) == ':') {
                time = "0" + time;
            } // if

            time = time.substring(0, time.length() - 6);

            SimpleDateFormat time24 = new SimpleDateFormat("HH:mm");
            SimpleDateFormat time12 = new SimpleDateFormat("hh:mm a");
            Date hour12 = time12.parse(time);
            return time12.format(hour12).toString();
        } catch (ParseException e) {
            return time;
        } // try
    } // to24HourTime

     /**
     * Converts a 24-hour UTC time to a 12-hour time
     * in a specific US timezone.
     *
     * @param time the 24-hour UTC time to be converted
     * @return the 12-hour equivalent time
     */
    public String convertTime(String time) {
        TicketmasterResponse ticketmasterResponse = GSON
            .fromJson(this.retrieveJson(), TicketmasterResponse.class);

        // getting timezone from Ticketmaster using GSON:
        String timezone = ticketmasterResponse.embedded.events[0].dates.timezone;

        if (timezone != null) {
            String hour = time.substring(0, 2);
            String mins = time.substring(3, time.length());
            int hourInt = Integer.parseInt(hour);

            // converting UTC time to specified US time zone
            if (timezone.equals("America/New_York")) {
                hourInt = hourInt - 4;
            } else if (timezone.equals("America/Chicago")) {
                hourInt = hourInt - 5;
            } else if (timezone.equals("America/Denver")) {
                hourInt = hourInt - 6;
            } else if (timezone.equals("America/Phoenix")) {
                hourInt = hourInt - 7;
            } else if (timezone.equals("America/Anchorage")) {
                hourInt = hourInt - 8;
            } else if (timezone.equals("Pacifif/Honolulu")) {
                hourInt = hourInt - 10;
            } // if

            String convertedTime24 = "" + hourInt + ":" + mins;
            String convertedTime12 = to12HourTime(convertedTime24);

            return convertedTime12;
        } else {
            return time;
        } // if
    } // converTime

} // ApiApp
