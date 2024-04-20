import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.InvalidParameterException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private int currentLimit;
    private final String url;
    private final HttpClient httpClient;


    public CrptApi(TimeUnit timeUnit, int requestLimit) throws InvalidParameterException {

        if (requestLimit < 0) {
            throw new InvalidParameterException("Only non-negative numbers");
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        this.currentLimit = requestLimit;
        this.url = "https://ismp.crpt.ru/api/v3/lk/documents/create";
        this.httpClient = HttpClient.newHttpClient();

        scheduler.scheduleAtFixedRate(() -> currentLimit = requestLimit, 0, 1, timeUnit);

    }


    public boolean createDocument(String document, String signature) throws InterruptedException, IOException {

        if (currentLimit == 0) {
            System.out.println("System overloaded");
            return false;
        }

        String json;
        try (InputStream inputStream = CrptApi.class.getResourceAsStream(document)) {
            if (inputStream == null) {
                throw new IOException(document + " not found");
            }
            json = new String(inputStream.readAllBytes());
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());


        //Comment it for test
        if (response.statusCode() != 200) {
            throw new RuntimeException("Error creating document: " + response.statusCode());
        }

        currentLimit--;
        System.out.println("200");
        return true;

    }


    public static void main(String[] args) throws IOException, InterruptedException {
        CrptApi api = new CrptApi(TimeUnit.MINUTES, 2);

        String document = "/data.json";

        String signature1 = "1111";
        String signature2 = "2222";
        String signature3 = "3333";

        api.createDocument(document, signature1);
        api.createDocument(document, signature2);
        api.createDocument(document, signature3);
    }
}
