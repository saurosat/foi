package io.gleecy.foi;

import org.eclipse.jetty.client.HttpClient;
import org.moqui.entity.EntityValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HttpPublisher {
    private static final Logger logger = LoggerFactory.getLogger(HttpPublisher.class);

    private HttpPublisher(){}
    HttpPublisher(HttpClient httpClient){
        this.httpClient = httpClient;
    }

    private HttpClient httpClient = null;
    private final Map<String, HttpSubscriber> subscriberMap = new ConcurrentHashMap<>();

    HttpTopic newTopic(EntityValue ev, Map<String, String> params) {
        ArrayList<URI> uris = new ArrayList<>(this.subscriberMap.size());
        String eName = ev.getEntityName();
        for (Map.Entry<String, HttpSubscriber> subsEntry : this.subscriberMap.entrySet()) {
            HttpSubscriber subscriber = subsEntry.getValue();

            Set<String> subscribs = subscriber.subscribedEntities;
            if (!subscribs.contains(eName))
                continue;

            URI uri = subscriber.uri;
            if (uri == null) {
                logger.warn("Notification URI of Store ID = '" + subsEntry.getKey() + "' is null");
                continue;
            }
            uris.add(uri);
        }
        if(uris.isEmpty())
            return null;
        return new EntityTopic(ev, httpClient, uris, params);
    }
    HttpPublisher putSubscriber(EntityValue eStore) {
        String storeId = (String) eStore.getNoCheckSimple("productStoreId");
        HttpSubscriber subscriber = this.subscriberMap.get(storeId);
        if(subscriber == null) synchronized (this.subscriberMap) {
            subscriber = this.subscriberMap.get(storeId);
            if(subscriber == null) { //Check again in synchronized block
                subscriber = new HttpSubscriber();
                this.subscriberMap.put(storeId, subscriber);
            }
        }
        String sUri = (String) eStore.getNoCheckSimple("notificationUrl");
        String sENames = (String) eStore.getNoCheckSimple("subscribedEntities");
        subscriber.update(sUri, sENames);
        if(subscriber.isEmpty()) synchronized (this.subscriberMap) {
            logger.info("Notification uri or subscription list is empty or invalid, deleting subscription for store ID " + storeId);
            this.subscriberMap.remove(storeId);
            return this;
        }

        logger.info("Updated subscription info for store ID: " + storeId +
                ".\n Subscribed entities = " + sENames +
                ".\n URI = " + sUri);

        return this;
    }
}
