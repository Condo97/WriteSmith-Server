package writesmith.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import spark.Request;
import spark.Response;
import spark.Route;
import writesmith.constants.Constants;
import writesmith.keys.Keys;
import writesmith.objects.ImportantConstants;

import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {

    private static final int MAX_THREADS = 4;
    private static final int MIN_THREADS = 1;
    private static final int TIMEOUT_MS = 30000;

    private static final int DEFAULT_PORT = 445;

    public static void main(String... args) {
        // Set up MySQL Driver
        try {
            DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Set up Policy static file location
        spark.Spark.staticFiles.location("/policies");

        // Set up Spark thread pool and port
        spark.Spark.threadPool(MAX_THREADS, MIN_THREADS, TIMEOUT_MS);
        spark.Spark.port(DEFAULT_PORT);

        // Set up SSL
        spark.Spark.secure("chitchatserver.com.jks", Keys.sslPassword, null, null);

        // Important Post Functions
        spark.Spark.post(Constants.REGISTER_USER_URI, Main::registerUser);
        spark.Spark.post(Constants.GET_CHAT_URI, Main::getChat);
        spark.Spark.post(Constants.FULL_VALIDATE_PREMIUM_URI, Main::fullValidatePremium);

        // Get Constants
        spark.Spark.post(Constants.GET_IMPORTANT_CONSTANTS_URI, (req, res) -> new ObjectMapper().writeValueAsString(new ImportantConstants()));


        // Exception Handling
        spark.Spark.exception(IllegalArgumentException.class, (error, req, res) -> {
            res.status(400);
            res.body("Illegal Argument");
        });

        spark.Spark.exception(Exception.class, (error, req, res) -> {
            res.status(400);
            res.body("Exception");
        });

        spark.Spark.notFound((req, res) -> {
            res.status(404);
            return "asdf";
        });
    }

    private static Object registerUser(Request request, Response response) {
        return null;
    }

    private static Object getChat(Request req, Response res) {

        System.out.println(req.body());
        System.out.println(res.body());

        // Return response
        return "ree";
    }

    private static Object fullValidatePremium(Request request, Response response) {
        return "";
    }
}
