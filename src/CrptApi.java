import com.google.gson.Gson;

import javax.swing.text.Document;
import java.io.IOException;
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
    private final Gson gson;


    public CrptApi(TimeUnit timeUnit, int requestLimit) throws InvalidParameterException {

        if (requestLimit < 0) {
            throw new InvalidParameterException("Only non-negative numbers");
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        this.currentLimit = requestLimit;
        this.url = "https://ismp.crpt.ru/api/v3/lk/documents/create";
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();

        scheduler.scheduleAtFixedRate(() -> currentLimit = requestLimit, 0, 1, timeUnit);

    }


    public boolean createDocument(Document document, String signature) throws InterruptedException, IOException {

        if (currentLimit == 0) {
            return false;
        }

        String json = gson.toJson(document);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Error creating document: " + response.statusCode());
        }

        currentLimit--;
        return true;

    }
}
