package com.jfrog.ide.common.utils.usage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.jfrog.build.api.util.Log;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Builder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import static com.jfrog.ide.common.utils.Utils.createMapper;


public class EcosystemUsageReporter {
    public static final String ECO_USAGE_URL = "https://usage-ecosystem.jfrog.io/api/usage/report";
    private static final ObjectMapper mapper = createMapper();
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(2);
    private final Log log;

    public EcosystemUsageReporter(Log log) {
        this.log = log;
    }

    public void reportUsage(UsageReport report, SSLContext sslContext) throws IOException {
        byte[] reportBytes = mapper.writeValueAsBytes(List.of(report));

        Builder clientBuilder = HttpClient.newBuilder();

        if (sslContext != null) {
            clientBuilder.sslContext(sslContext);
        }
        HttpClient client = clientBuilder.build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ECO_USAGE_URL))
                .timeout(DEFAULT_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(reportBytes))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(this::logResponse);

    }

    private void logResponse(HttpResponse<String> response) {
        if (response.statusCode() != HttpStatus.SC_OK) {
            log.debug("EcosystemUsageReporter failed to report usage:\n" + response.body());
        }
    }


}
