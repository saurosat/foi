package io.gleecy.foi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public class HttpSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(HttpPublisher.class);
    private static URI createUri(String sUri) {
        try {
            return URI.create(sUri);
        } catch (Exception e) {
            logger.error("Malformed URL: " + sUri, e);
        }
        return null;
    }

    URI uri;
    Set<String> subscribedEntities;
    public boolean isEmpty() {
        return uri == null || subscribedEntities == null || subscribedEntities.isEmpty();
    }
    synchronized HttpSubscriber update(String sUri, String sENames) {
        this.uri = null;
        this.subscribedEntities = new HashSet<>();
        //1. Update subscribed entities. If this is not null and empty, remove the publisher
        if(sENames != null && !(sENames = sENames.trim()).isEmpty()) {
            String[] eNames = sENames.split(",");
            for (String eName : eNames) {
                if(eName != null && !(eName = eName.trim()).isEmpty() )
                    this.subscribedEntities.add(eName);
            }
        }
        //2. Update the notification URI, if this field is not null and empty, remove the publisher
        if(sUri != null && !(sUri = sUri.trim()).isEmpty()) {
            this.uri = createUri(sUri);
        }
        return this;
    }
}
