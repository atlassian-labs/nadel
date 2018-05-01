package graphql.nadel.service;

import com.atlassian.braid.source.GraphQLRemoteRetriever;
import graphql.nadel.dsl.ServiceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class GraphQLRemoteRetrieverFactory implements graphql.nadel.GraphQLRemoteRetrieverFactory {

    private static final Logger logger = LoggerFactory.getLogger(GraphQLRemoteRetrieverFactory.class);

    @Override
    public GraphQLRemoteRetriever createRemoteRetriever(ServiceDefinition serviceDefinition) {
        return (executionInput, context) -> {
            WebClient webClient = WebClient.
                    builder()
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .build();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("query", executionInput.getQuery());
            logger.info("request to {} with body {}", serviceDefinition.getUrl(), body);
            Mono<ClientResponse> clientResponseMono = webClient.post()
                    .uri(serviceDefinition.getUrl())
                    .body(BodyInserters.fromObject(body))
                    .exchange();

            CompletableFuture<Map<String, Object>> result = new CompletableFuture<>();
            clientResponseMono.subscribe(clientResponse -> {
                logger.info("received response {}", clientResponse);
                clientResponse.toEntity(Map.class).subscribe(mapResponseEntity -> {
                    logger.info("response body {}", mapResponseEntity);
                    Map responseBody = mapResponseEntity.getBody();
                    result.complete(responseBody);
                });
            });

            return result;
        };
    }

}
