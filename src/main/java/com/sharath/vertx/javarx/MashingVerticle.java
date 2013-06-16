package com.sharath.vertx.javarx;

import meez.rxvertx.java.http.RxHttpClient;
import meez.rxvertx.java.http.RxHttpClientResponse;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.deploy.Verticle;

import rx.Observable;
import rx.Observer;
import rx.util.functions.Action1;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

import com.sharath.vertx.javarx.model.FulfilledHttpResponse;

public class MashingVerticle extends Verticle {
    @Override
    public void start() {
        final Logger logger = container.getLogger();
        final HttpServer server = vertx.createHttpServer();
        server.requestHandler(new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest request) {
                logger.info("Received: " + request.uri);
                
                // invoke two get requests 
                final Observable<FulfilledHttpResponse> ob1 = getClientResponseObservable("en.wikipedia.org", "/wiki/Jiroemon_Kimura", 80);
                final Observable<FulfilledHttpResponse> ob2 = getClientResponseObservable("en.wikipedia.org", "/wiki/Main_Page", 80);

                // zip the responses to make sure both are fulfilled
                final Observable<FulfilledHttpResponse[]> obzip = Observable.zip(ob1, ob2, new Func2<FulfilledHttpResponse, FulfilledHttpResponse, FulfilledHttpResponse[]>() {
                    @Override
                    public FulfilledHttpResponse[] call(final FulfilledHttpResponse t1, final FulfilledHttpResponse t2) {
                        return new FulfilledHttpResponse[] { t1, t2 };
                    }
                });
                
                // get the last (here it would be always 1, but make sure takeLast() is used instead of thread blocking take()
                obzip.takeLast(1).subscribe(new Observer<FulfilledHttpResponse[]>() {
                    @Override
                    public void onNext(final FulfilledHttpResponse[] responses) {
                        // mash the responses
                    	String mashedResponse = getMashedResponse(responses[0].getBody().toString(), responses[1].getBody().toString());
                        request.response.end(mashedResponse);
                    }

                    @Override
                    public void onError(final Exception e) {
                        request.response.statusCode = 500;
                        logger.error(e.getMessage());
                        request.response.end(e.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                        // do nothing - we already committed the response in one and only invocation of onNext()
                    }
                });
            }
        });
        server.listen(8080);
    }
    
    private String getMashedResponse(String response1, String response2) {
    	// here we are simply concatenating responses - to mash responses, but this could be easily modified to say mash json/xml responses etc.. 	
    	final StringBuilder sb = new StringBuilder();
        sb.append("1:");
        // append just the first 200 chars
        sb.append(response1.substring(0, 200));
        sb.append("\n2:");
        // append just the first 200 chars
        sb.append(response2.substring(0, 200));
    	return sb.toString();
    }

    private Observable<FulfilledHttpResponse> getClientResponseObservable(final String host, final String path, final int port) {
        final Logger logger = container.getLogger();
        final HttpClient client = vertx.createHttpClient().setHost(host);
        if (80 != port) {
            client.setPort(port);
        }
        final RxHttpClient rxClient = new RxHttpClient(client);
        final Observable<RxHttpClientResponse> ob1 = rxClient.get(path, new Action1<HttpClientRequest>() {
            @Override
            public void call(final HttpClientRequest clientRequest) {
                logger.info("Requesting: " + host + path);
                clientRequest.end();
            }
        });
        
        // map the observable to a FulfilledHttpResponse observable
        return ob1.flatMap(new Func1<RxHttpClientResponse, Observable<FulfilledHttpResponse>>() {
            @Override
            public Observable<FulfilledHttpResponse> call(final RxHttpClientResponse response) {
                logger.info("Received response: " + host + path + "; status: " + response.statusCode);
                return FulfilledHttpResponse.asObservable(response);
            }
        });
    }
}