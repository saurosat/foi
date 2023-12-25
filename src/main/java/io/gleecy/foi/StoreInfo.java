package io.gleecy.foi;

import org.apache.commons.lang3.StringUtils;
import org.moqui.entity.EntityValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class StoreInfo {
    public static final String ENAME = "mantle.product.store.ProductStore";
    private static final Logger logger = LoggerFactory.getLogger(StoreInfo.class);
    public static StoreInfo newInstance(EntityValue eStore) {
        if(!ENAME.equals(eStore.getEntityName())) {
            return null;
        }
        String storeId = (String) eStore.getNoCheckSimple("productStoreId");
        if(storeId == null || storeId.isBlank()) return null;
        StoreInfo store = new StoreInfo(storeId);
        if(!validateSet(store, eStore, true)) {
            return null;
        }
        return store;
    }
    private static boolean validateSet(StoreInfo store, EntityValue eStore, boolean requireSecretKey) {
        Map<String, Object> eValMap = eStore.getEtlValues();
        String secretKey = (String) eValMap.get("secretKey");
        //if secretKey is explicitly set to null or empty or blank or '_NA_':
        // this store is considered not online and it's customers are
        // not allowed to register accounts
        if(secretKey == null) {
            if(requireSecretKey)
                return false;
            if(eValMap.containsKey("secretKey"))
                return false;
        } else {
            secretKey = secretKey.trim();
            if(secretKey.isEmpty() || "_NA_".equals(secretKey))
                return false;
            store.secretKey = secretKey;
            store.hash = hash(0, secretKey);
        }

        String ownerPartyId = (String) eStore.getNoCheckSimple("organizationPartyId");
        if(ownerPartyId != null && !ownerPartyId.isBlank()) {
            store.ownerPartyId = ownerPartyId;
        }
        String sENames = (String) eStore.getNoCheckSimple("subscribedEntities");
        if(sENames != null) {
            String[] eNames = null;
            sENames = sENames.trim();
            //1. Update subscribed entities.
            if(!sENames.isEmpty() && !"_NA_".equals(sENames)) {
                eNames = sENames.split(",");
            }
            for (String eName : eNames) {
                if(eName == null || (eName = eName.trim()).isEmpty() || "_NA_".equals(eName))
                    continue;
                store.subscribedEntities.put(eName, eName); //Use Map for upgrade later
            }
        }
        String sUri = (String) eStore.getNoCheckSimple("notificationUrl");
        if(sUri != null && !(sUri = sUri.trim()).isEmpty()) {
            try {
                store.uri = URI.create(sUri);
            } catch (Exception e) {
                logger.error("Malformed URL: " + sUri, e);
            }
        }
        return true;
    }
    private static int hash(int oriHash, String msg) {
        byte[] bytes = msg.getBytes();
        int h = oriHash;
        for (byte v : bytes) {
            h = 31 * h + (v & 0xff);
        }
        return h;
    }

    public final String storeId;
    public final String tenantPrefix;
    int hash = 0;
    String ownerPartyId = null;
    String secretKey = null;
    URI uri = null;
    public StoreInfoCache storeCache = null;
    public final Map<String, String> subscribedEntities = new HashMap<>();

    StoreInfo (String storeId) {
        this.storeId = storeId;
        if(this.storeId.startsWith("P")) {
            this.tenantPrefix = this.storeId.substring(0, 9);
        } else {
            this.tenantPrefix = "";
        }
    }
    public String getOwnerPartyId() {
        return ownerPartyId;
    }
    public String getSecretKey() {
        return secretKey;
    }
    public URI getUri() {
        return uri;
    }
    public void update(EntityValue entity, String on) {
        if(on == null || entity == null || !ENAME.equals(entity.getEntityName())) {
            return;
        }
        if(storeCache == null) {
            logger.error("StoreCache is not set in StoreInfo: " + this.storeId);
            return;
        }
        switch (on) {
            case "delete":
                storeCache._removeStore(this.storeId);
                break;
            case "create":
            case "update":
            default:
                if(secretKey == null) { //this store not exist in cache
                    if(validateSet(this, entity, true)) {
                        storeCache._addStore(this);
                    } else {
                        logger.info("Newly created Store is missing mandatory fields. Not cached: " + this.storeId);
                    }
                    break;
                }
                if(!validateSet(this, entity, false)) {
                    logger.info("New value of store is missing mandatory fields, removing from cache. " + this.storeId);
                    storeCache._removeStore(this.storeId);
                }
                break;
        }
    }

    public boolean isSubscribing(EntityTopic topic) {
        return this.subscribedEntities.containsKey(topic.ev.getEntityName());
    }

    public boolean verify(String message, String hash) {
        int receivedHash = 0;
        try {
            receivedHash = Integer.parseInt(hash, 16);
        } catch (NumberFormatException e) {
            logger.info("NumberFormatException thrown while parsing '"
                    + hash + "'. " + e.getMessage());
            return false;
        }
        int msgHash = hash(this.hash, message);
        return msgHash == receivedHash;
    }
    public String hash(String message) {
        int msgHash = hash(this.hash, message);
        return StringUtils.leftPad(Integer.toHexString(msgHash), 8, '0');
    }
}
