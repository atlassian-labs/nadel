package com.graphqljava.nadel.service;

import graphql.language.AstPrinter;
import graphql.nadel.GraphqlCallResult;
import graphql.nadel.GraphqlCaller;
import graphql.nadel.dsl.ServiceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class GraphqlCallerFactory implements graphql.nadel.GraphqlCallerFactory {

    private static final Logger logger = LoggerFactory.getLogger(GraphqlCallerFactory.class);

    @Override
    public GraphqlCaller createGraphqlCaller(ServiceDefinition serviceDefinition) {
        return query -> {
            WebClient webClient = WebClient.
                    builder()
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .build();
            Map<String, Object> body = Map.of("query", AstPrinter.printAst(query));
            logger.info("request to {} with body {}", serviceDefinition.getUrl(), body);
            Mono<ClientResponse> clientResponseMono = webClient.post()
                    .uri(serviceDefinition.getUrl())
                    .body(BodyInserters.fromObject(body))
                    .exchange();
            CompletableFuture<GraphqlCallResult> result = new CompletableFuture<>();
//            clientResponseMono.flatMapMany( clientResponse -> result.complete(toGraphqlCallResult(clientResponse)));
//            clientResponseMono.doOnNext((clientResponse) -> {
            ClientResponse clientResponse = clientResponseMono.block();
            logger.info("received response {} {}", clientResponse);
//                if (clientResponse != null) {
            result.complete(toGraphqlCallResult(clientResponse));
//                } else {
//                    result.completeExceptionally(throwable);
//                }
//            });

            return result;
        };
    }

    private GraphqlCallResult toGraphqlCallResult(ClientResponse clientResponse) {
        ResponseEntity<Map> responseBody = clientResponse.toEntity(Map.class).block();
        logger.info("response body {}", responseBody);
        Map body = responseBody.getBody();
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        logger.info("response data {}", data);
        return new GraphqlCallResult(data);
    }


}
