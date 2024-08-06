package com.selsup;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    public static void main(String[] args) throws InterruptedException {
        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 5);
        Thread thread1 = new Thread(() -> doJob(crptApi));
        Thread thread2 = new Thread(() -> doJob(crptApi));
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
    }

    private static void doJob(CrptApi crptApi) {
        for (int i = 0; i < 1000; i++) {
            crptApi.createDocument(new DocumentDTO(), "digest");
        }
    }


    private static final URI URL = URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create");
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final long timeUnitMillis;
    private final int requestLimit;
    private final Queue<Instant> requestTimeLines;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnitMillis = timeUnit.toMillis(1);
        this.requestLimit = requestLimit;
        this.requestTimeLines = new ConcurrentLinkedQueue<>();
    }

    public ResponseDto createDocument(DocumentDTO document, String digest) {
        try {
            checkRequestLimit();

            final var request = HttpRequest.newBuilder()
                    .uri(URL)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(OBJECT_MAPPER.writeValueAsBytes(document)))
                    .header("Content-Type", "application/json")
                    .build();
            final var httpResponse = HTTP_CLIENT.send(request, BodyHandlers.ofString());
            final var statusCode = httpResponse.statusCode();
            if (statusCode != 200) {
                return new ResponseDto("Document has not been created.", statusCode);
            }
            if (!digest.equals(httpResponse.body())) {
                return new ResponseDto("Document has been modified.", statusCode);
            }
            return new ResponseDto("Document has been successfully created.", statusCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ResponseDto(String.format("Document has not been created: %s", e.getMessage()), 500);
        } catch (IOException e) {
            return new ResponseDto(String.format("Document has not been created: %s", e.getMessage()), 500);
        }
    }

    private void checkRequestLimit() throws InterruptedException {
        while (isLimitExceeded()) {
            if (isDurationExceeded()) {
                requestTimeLines.poll();
            } else {
                Thread.sleep(100);
            }
        }
        requestTimeLines.offer(Instant.now());
    }

    private boolean isLimitExceeded() {
        return requestTimeLines.size() >= requestLimit;
    }

    private boolean isDurationExceeded() {
        return requestTimeLines.peek() != null && Duration.between(requestTimeLines.peek(), Instant.now()).toMillis() >= timeUnitMillis;
    }

    public static class ResponseDto {

        private final String message;
        private final int statusCode;

        public ResponseDto(String message, int statusCode) {
            this.message = message;
            this.statusCode = statusCode;
        }

        public String getMessage() {
            return message;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }

    public static class DocumentDTO {

        private DescriptionDTO description;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private LocalDate productionDate;
        private String productionType;
        private List<ProductDTO> products;
        private LocalDate regDate;
        private String regNumber;

        public DescriptionDTO getDescription() {
            return description;
        }

        public void setDescription(DescriptionDTO description) {
            this.description = description;
        }

        public String getDocId() {
            return docId;
        }

        public void setDocId(String docId) {
            this.docId = docId;
        }

        public String getDocStatus() {
            return docStatus;
        }

        public void setDocStatus(String docStatus) {
            this.docStatus = docStatus;
        }

        public String getDocType() {
            return docType;
        }

        public void setDocType(String docType) {
            this.docType = docType;
        }

        public boolean isImportRequest() {
            return importRequest;
        }

        public void setImportRequest(boolean importRequest) {
            this.importRequest = importRequest;
        }

        public String getOwnerInn() {
            return ownerInn;
        }

        public void setOwnerInn(String ownerInn) {
            this.ownerInn = ownerInn;
        }

        public String getParticipantInn() {
            return participantInn;
        }

        public void setParticipantInn(String participantInn) {
            this.participantInn = participantInn;
        }

        public String getProducerInn() {
            return producerInn;
        }

        public void setProducerInn(String producerInn) {
            this.producerInn = producerInn;
        }

        public LocalDate getProductionDate() {
            return productionDate;
        }

        public void setProductionDate(LocalDate productionDate) {
            this.productionDate = productionDate;
        }

        public String getProductionType() {
            return productionType;
        }

        public void setProductionType(String productionType) {
            this.productionType = productionType;
        }

        public List<ProductDTO> getProducts() {
            return products;
        }

        public void setProducts(List<ProductDTO> products) {
            this.products = products;
        }

        public LocalDate getRegDate() {
            return regDate;
        }

        public void setRegDate(LocalDate regDate) {
            this.regDate = regDate;
        }

        public String getRegNumber() {
            return regNumber;
        }

        public void setRegNumber(String regNumber) {
            this.regNumber = regNumber;
        }
    }

    public static class DescriptionDTO {

        private String participantInn;

        public String getParticipantInn() {
            return participantInn;
        }

        public void setParticipantInn(String participantInn) {
            this.participantInn = participantInn;
        }
    }

    public static class ProductDTO {

        private String certificateDocument;
        private LocalDate certificateDocumentDate;
        private String certificateDocumentNumber;
        private String ownerInn;
        private String producerInn;
        private LocalDate productionDate;
        private String tnvedCode;
        private String uitCode;
        private String uituCode;

        public String getCertificateDocument() {
            return certificateDocument;
        }

        public void setCertificateDocument(String certificateDocument) {
            this.certificateDocument = certificateDocument;
        }

        public LocalDate getCertificateDocumentDate() {
            return certificateDocumentDate;
        }

        public void setCertificateDocumentDate(LocalDate certificateDocumentDate) {
            this.certificateDocumentDate = certificateDocumentDate;
        }

        public String getCertificateDocumentNumber() {
            return certificateDocumentNumber;
        }

        public void setCertificateDocumentNumber(String certificateDocumentNumber) {
            this.certificateDocumentNumber = certificateDocumentNumber;
        }

        public String getOwnerInn() {
            return ownerInn;
        }

        public void setOwnerInn(String ownerInn) {
            this.ownerInn = ownerInn;
        }

        public String getProducerInn() {
            return producerInn;
        }

        public void setProducerInn(String producerInn) {
            this.producerInn = producerInn;
        }

        public LocalDate getProductionDate() {
            return productionDate;
        }

        public void setProductionDate(LocalDate productionDate) {
            this.productionDate = productionDate;
        }

        public String getTnvedCode() {
            return tnvedCode;
        }

        public void setTnvedCode(String tnvedCode) {
            this.tnvedCode = tnvedCode;
        }

        public String getUitCode() {
            return uitCode;
        }

        public void setUitCode(String uitCode) {
            this.uitCode = uitCode;
        }

        public String getUituCode() {
            return uituCode;
        }

        public void setUituCode(String uituCode) {
            this.uituCode = uituCode;
        }
    }
}
