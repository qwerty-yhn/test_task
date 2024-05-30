import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CrptApi {
    private final HttpClient httpClient;
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduler;
    private final int requestLimit;
    private final TimeUnit timeUnit;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.requestLimit = requestLimit;
        this.timeUnit = timeUnit;
        this.semaphore = new Semaphore(requestLimit, true);

        long period = timeUnit.toMillis(1);
        this.scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            semaphore.release(requestLimit - semaphore.availablePermits());
        }, period, period, TimeUnit.MILLISECONDS);
    }

    public HttpResponse<String> createDocument(Document document, String signature) throws IOException, InterruptedException {
        semaphore.acquire();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(document);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .timeout(Duration.ofMinutes(2))
                    .header("Content-Type", "application/json")
                    .header("Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } finally {
            semaphore.release();
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.MINUTES)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 10);

        Document document = new Document();
        document.setDescription(new Description("participantInn"));
        document.setDocId("doc_id");
        document.setDocStatus("doc_status");
        document.setDocType("LP_INTRODUCE_GOODS");
        document.setImportRequest(true);
        document.setOwnerInn("owner_inn");
        document.setParticipantInn("participant_inn");
        document.setProducerInn("producer_inn");
        document.setProductionDate("2020-01-23");
        document.setProductionType("production_type");
        document.setProducts(List.of(new Product("certificate_document", "2020-01-23", "certificate_document_number", "owner_inn", "producer_inn", "2020-01-23", "tnved_code", "uit_code", "uitu_code")));
        document.setRegDate("2020-01-23");
        document.setRegNumber("reg_number");

        String signature = "example-signature";

        try {
            HttpResponse<String> response = crptApi.createDocument(document, signature);
            System.out.println(response.body());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            crptApi.shutdown();
        }
    }
}

class Document {
    private Description description;
    private String docId;
    private String docStatus;
    private String docType;
    private boolean importRequest;
    private String ownerInn;
    private String participantInn;
    private String producerInn;
    private String productionDate;
    private String productionType;
    private List<Product> products;
    private String regDate;
    private String regNumber;

    // Getters and Setters
}

class Description {
    private String participantInn;

    public Description(String participantInn) {
        this.participantInn = participantInn;
    }

    // Getters and Setters
}

class Product {
    private String certificateDocument;
    private String certificateDocumentDate;
    private String certificateDocumentNumber;
    private String ownerInn;
    private String producerInn;
    private String productionDate;
    private String tnvedCode;
    private String uitCode;
    private String uituCode;

    public Product(String certificateDocument, String certificateDocumentDate, String certificateDocumentNumber, String ownerInn, String producerInn, String productionDate, String tnvedCode, String uitCode, String uituCode) {
        this.certificateDocument = certificateDocument;
        this.certificateDocumentDate = certificateDocumentDate;
        this.certificateDocumentNumber = certificateDocumentNumber;
        this.ownerInn = ownerInn;
        this.producerInn = producerInn;
        this.productionDate = productionDate;
        this.tnvedCode = tnvedCode;
        this.uitCode = uitCode;
        this.uituCode = uituCode;
    }

    // Getters and Setters
}