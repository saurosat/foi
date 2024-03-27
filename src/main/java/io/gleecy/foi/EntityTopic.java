package io.gleecy.foi;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Result;
import org.moqui.entity.EntityValue;
import org.moqui.impl.entity.EntityDefinition;
import org.moqui.impl.entity.EntityValueBase;
import org.moqui.impl.entity.FieldInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class EntityTopic implements HttpTopic {
    private final static Logger logger = LoggerFactory.getLogger(EntityTopic.class);
    public static final int MAX_RETRIES = 5;
    static void logResponse(Result result) {
        Request req = result.getRequest();
        if(result.isSucceeded()) {
            logger.info("Notified" + req.getURI().toString() + ". OK");
            return;
        }
        StringBuilder logError = new StringBuilder("Failed to notify ").append(req.getURI().toString());
//        logError.append("\n Notification request: \n");
//        logError.append("\n\t Headers:");
//        req.getHeaders().forEach(reqHF -> logError.append(reqHF.toString()).append("; "));
//        logError.append("\n\t Params:");
//        req.getParams().forEach(reqHF -> logError.append(reqHF.toString()).append("; "));

        Throwable err = result.getRequestFailure();
        if(err != null) {
            logError.append("\n Request Error: ").append(err.getMessage());
            logger.error(logError.toString());
            return;
        }

        err = result.getResponseFailure();
        if(err != null) {
            logError.append("\n Response Error: ").append(err.getMessage());
            logger.error(logError.toString());
            return;
        }

        //logError.append("\n Remote server returned ");
        //result.getResponse().getHeaders().forEach(header -> logError.append(header.toString()).append("; "));
        err = result.getFailure();
        if(err != null) {
            logError.append(" Error: ").append(err.getMessage());
            logger.error(logError.toString(), result.getFailure());
        }
    }

    final HttpClient httpClient;
    final EntityValue ev;
    final Map<String, String> params;
    final List<URI> uris = new LinkedList<>();
    final ThreadPoolExecutor workerPool;
    int attempts = 0;
    EntityTopic(HttpClient httpClient, EntityValue ev, Map<String, String> params, ThreadPoolExecutor workerPool) {
        this.ev = ev;
        this.httpClient = httpClient;
        this.params = params;
        this.workerPool = workerPool;
    }

    public void send() {
        if (ev == null || ev.isEmpty())
            return;
        EntityValueBase entity = ((EntityValueBase) ev);
        EntityDefinition ed = entity.getEntityDefinition();
        FieldInfo[] fieldInfos = ed.entityInfo.allFieldInfoArray;

        Map<String, String> vMap = new HashMap<>();
        int numFields = fieldInfos.length;
        for (int i = 0; i < numFields; i++) {
            FieldInfo fieldInfo = fieldInfos[i];
            Object fieldValue = entity.getKnownField(fieldInfo);
            if(fieldValue != null)
                vMap.put(fieldInfo.name, fieldInfo.convertToString(fieldValue));
        }
        if (vMap.isEmpty())
            return;

        if (!startIfNeeded())
            return;
        for (URI uri: uris) {
            workerPool.execute(new Runnable() {
                @Override
                public void run() {
                    Request request = httpClient.POST(uri).accept("text/html");
                    request.param("entityName", ed.getEntityName());
                    request.idleTimeout(30, TimeUnit.SECONDS);
                    for (Map.Entry<String, String> entry : vMap.entrySet()) {
                        request.param(entry.getKey(), entry.getValue());
                    }
                    if (params != null) {
                        for (Map.Entry<String, String> paramEntry : params.entrySet()) {
                            request.param(paramEntry.getKey(), paramEntry.getValue());
                        }
                    }
                    request.send(EntityTopic::logResponse);                }
            });
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
