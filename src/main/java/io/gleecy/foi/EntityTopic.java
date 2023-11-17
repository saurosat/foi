package io.gleecy.foi;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Result;
import org.moqui.entity.EntityValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

public class EntityTopic implements HttpTopic {
    private final static Logger logger = LoggerFactory.getLogger(EntityTopic.class);
    public static final int MAX_RETRIES = 5;
    static void logResponse(Result result) {
        Request req = result.getRequest();
        if(result.isSucceeded()) {
            logger.info("Notified" + req.getURI().toString() + ". OK");
            return;
        }
        StringBuilder logError = new StringBuilder("Failed to notify").append(req.getURI().toString());
        logError.append("\n Notification request: \n");
        logError.append("\n\t Headers:");
        req.getHeaders().forEach(reqHF -> logError.append(reqHF.toString()).append("; "));
        logError.append("\n\t Params:");
        req.getParams().forEach(reqHF -> logError.append(reqHF.toString()).append("; "));

        Throwable err = result.getRequestFailure();
        if(err != null) {
            logError.append("\n Failed when sending request. Error details: ");
            logger.error(logError.toString(), err);
            return;
        }

        err = result.getResponseFailure();
        if(err != null) {
            logError.append("\n Failed when receiving response. Error details: ");
            logger.error(logError.toString(), err);
            return;
        }

        logError.append("\n Remote server returned ");
        result.getResponse().getHeaders().forEach(header -> logError.append(header.toString()).append("; "));
        if(result.getFailure() != null) {
            logError.append(" exception: ");
            logger.error(logError.toString(), result.getFailure());
        } else
            logger.error(logError.toString());
    }

    final HttpClient httpClient;
    final EntityValue ev;
    final Map<String, String> params;
    Collection<URI> uris;
    int attempts = 0;
    EntityTopic(EntityValue ev, HttpClient httpClient, Collection<URI> uris, Map<String, String> params) {
        this.ev = ev;
        this.uris = uris;
        this.httpClient = httpClient;
        this.params = params;
    }

    public void send() {
        if (ev == null || ev.isEmpty())
            return;
        Map<String, Object> vMap = ev.getMap();
        if (vMap == null || vMap.isEmpty())
            return;

        if (!startIfNeeded())
            return;
        for (URI uri: uris) {
            Request request = httpClient.POST(uri).accept("text/html");
            for (Map.Entry<String, Object> entry : vMap.entrySet()) {
                if (entry.getKey() == null)
                    continue;
                request.param(entry.getKey(),
                        entry.getValue() == null ? "" : entry.getValue().toString());
            }
            if (params != null) {
                for (Map.Entry<String, String> paramEntry : params.entrySet()) {
                    request.param(paramEntry.getKey(), paramEntry.getValue());
                }
            }
            request.send(EntityTopic::logResponse);
        }
    }

    private boolean startIfNeeded() {
        String state = httpClient.getState();
        if (state.equals("STARTED"))
            return true;
        if (state.equals("FAILED")) {
            logger.error("HttpClient has failed to start");
            return false;
        }
        if (state.equals("STOPPED")) {
            try {
                logger.warn("HTTPClient was stopped. Restarting HttpClient");
                httpClient.start();
            } catch (Exception e) {
                logger.error("HTTPClient was stopped and failed to restart: ", e);
                return false;
            }
            return true;
        }

        //Check case STARTING and STOPPING
        if (state.equals("STARTING") || state.equals("STOPPING")) {
            if (attempts >= MAX_RETRIES) {
                logger.error("HttpClient is in '" + state + "'. Retried "
                        + attempts + " times");
                return false;
            }
            logger.warn("HttpClient state is '" + state + "'. Waiting 0.1s then retry");
            try {
                this.wait(100);
                this.attempts++;
                return startIfNeeded();
            } catch (InterruptedException e) {
                logger.error("Error while waiting: ", e);
                return false;
            }
        }

        logger.error("Unknown state of HttpClient: " + state);
        return false;
    }
}
