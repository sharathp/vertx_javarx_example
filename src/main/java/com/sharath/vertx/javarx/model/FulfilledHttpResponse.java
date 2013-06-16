package com.sharath.vertx.javarx.model;

import java.util.List;
import java.util.Map;

import meez.rxvertx.java.RxSupport;
import meez.rxvertx.java.http.RxHttpClientResponse;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClientResponse;

import rx.Observable;
import rx.util.functions.Func1;


public class FulfilledHttpResponse extends HttpClientResponse {
    /** Real response */
    private final HttpClientResponse nested;
    private Buffer body;

    /** Create new HttpClientResponse */
    protected FulfilledHttpResponse(final HttpClientResponse real) {
        super(real.statusCode, real.statusMessage);
        this.nested = real;
    }

    public static Observable<FulfilledHttpResponse> asObservable(final RxHttpClientResponse rxHttpClientResponse) {
        return rxHttpClientResponse.asObservable().reduce(RxSupport.mergeBuffers).map(new Func1<Buffer, FulfilledHttpResponse>() {
            @Override
            public FulfilledHttpResponse call(final Buffer body) {
                final FulfilledHttpResponse fulfilledHttpResponse = new FulfilledHttpResponse(rxHttpClientResponse);
                fulfilledHttpResponse.body = body;
                return fulfilledHttpResponse;
            }
        });
    }

    public Buffer getBody() {
        return body;
    }

    // HttpClientResponse implementation
    @Override
    public Map<String, String> headers() {
        return nested.headers();
    }

    @Override
    public Map<String, String> trailers() {
        return nested.trailers();
    }

    @Override
    public List<String> cookies() {
        return nested.cookies();
    }

    // HttpReadStreamBase implementation
    @Override
    public void bodyHandler(final Handler<org.vertx.java.core.buffer.Buffer> bodyHandler) {
        throw new UnsupportedOperationException("Not supported - body already fulfilled");
    }

    // ReadStream implementation
    @Override
    public void dataHandler(final Handler<Buffer> bufferHandler) {
        throw new UnsupportedOperationException("Not supported - body already fulfilled");
    }

    @Override
    public void pause() {
        nested.pause();
    }

    @Override
    public void resume() {
        nested.resume();
    }

    @Override
    public void exceptionHandler(final Handler<Exception> exceptionHandler) {
        throw new UnsupportedOperationException("Not supported - body already fulfilled");
    }

    @Override
    public void endHandler(final Handler<Void> voidHandler) {
        throw new UnsupportedOperationException("Not supported - body already fulfilled");
    }
}