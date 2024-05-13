package io.gleecy.foi;

import org.apache.commons.lang3.StringUtils;
import org.moqui.entity.EntityFacade;
import org.moqui.entity.EntityFind;
import org.moqui.entity.EntityList;
import org.moqui.entity.EntityValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StoreInfo {
    private static final Object DUMMYOBJ = 0;
    public static final String[] productEntities = new String[] {
                "mantle.product.ProductAssoc",
                "mantle.product.ProductCalculatedInfo",
                "mantle.product.ProductGeo",
                "mantle.product.ProductIdentification",
                "mantle.product.feature.ProductFeatureAppl",
                "mantle.product.ProductPrice"
            };
    public static final String[] categoryEntities = new String[] {
            "mantle.product.category.ProductCategoryRollup",
            "mantle.product.category.ProductCategoryMember"
    };
    public static final String ENAME = "mantle.product.store.ProductStore";
    private static final Logger logger = LoggerFactory.getLogger(StoreInfo.class);
    private static final String[] productRelates = {"assocs", "toAssocs"};

    public static StoreInfo newInstance(EntityValue eStore, EntityFacade ef) {
        if(!ENAME.equals(eStore.getEntityName())) {
            return null;
        }
        String storeId = (String) eStore.getNoCheckSimple("productStoreId");
        if(storeId == null || storeId.isBlank()) return null;
        StoreInfo store = new StoreInfo(storeId);
        if(!validateSet(store, eStore, true)) {
            return null;
        }
        store.init(ef);
        return store;
    }
    private static boolean validateSet(StoreInfo store, EntityValue eStore, boolean requireSecretKey) {
        Map<String, Object> eValMap = eStore.getEtlValues(); //this map can contain null values
        String secretKey = (String) eValMap.get("secretKey");
        //if secretKey is explicitly set to null or empty or blank or '_NA_':
        // this store is considered not online and it's customers are
        // not allowed to register accounts
        if(secretKey == null) {
            if(requireSecretKey)
                return false;
            if(eValMap.containsKey("secretKey")) //secret key is explicitly set to null
                return false;
        } else {
            secretKey = secretKey.trim();
            if(secretKey.isEmpty() || "_NA_".equals(secretKey)) //secret key is explicitly set to empty or blank or '_NA_':
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
    boolean initialized = false;
    public StoreInfoCache storeCache = null;
    public final Map<String, String> subscribedEntities = new HashMap<>();
    public final Map<String, Object> categoryIds = new ConcurrentHashMap<>();
    public final Map<String, Object> productIds = new ConcurrentHashMap<>();
    public final Map<String, Object> viewableCategoryIds = new ConcurrentHashMap<>();

    StoreInfo (String storeId) {
        this.storeId = storeId;
        if(this.storeId.startsWith("P")) {
            this.tenantPrefix = this.storeId.substring(0, 9);
        } else {
            this.tenantPrefix = "";
        }
    }
    public void init(EntityFacade ef) {
        //if(initialized) return;
        EntityFind findCats = ef.find("mantle.product.store.ProductStoreCategory");
        findCats.disableAuthz().forUpdate(false).useCache(true);
        findCats.selectFields(Arrays.asList("productCategoryId", "storeCategoryTypeEnumId"));
        findCats.condition("productStoreId", this.storeId);
        findCats.conditionDate("fromDate", "thruDate", null);
        EntityList cats = findCats.list();
        for(EntityValue cat : cats) {
            String catId = (String) cat.getNoCheckSimple("productCategoryId");
            String storeCatType = (String) cat.getNoCheckSimple("storeCategoryTypeEnumId");
            if (storeCatType.equals("PsctViewAllow")) {
                this.viewableCategoryIds.put(catId, DUMMYOBJ);
            } else {
                this.categoryIds.put(catId, DUMMYOBJ);
            }
        }
        initialized = true;
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
    private void updateStoreProduct(EntityValue product, String on) {
        String productId = (String) product.getNoCheckSimple("productId");
        boolean delete = "delete".equals(on);
        Timestamp thruDate = (Timestamp) product.getNoCheckSimple("thruDate");
        if(thruDate != null) {
            Timestamp now = new Timestamp(System.currentTimeMillis());
            delete = thruDate.before(now);
        }
        if(delete) {
            productIds.remove(productId);
        } else {
            productIds.put(productId, DUMMYOBJ);
        }
    }
    private void updateStoreCategory(EntityValue evStore, String on) {
        String categoryId = (String) evStore.getNoCheckSimple("productCategoryId");
        String categoryType = (String) evStore.getNoCheckSimple("storeCategoryTypeEnumId");//thruDate
        boolean delete = "delete".equals(on);
        Timestamp thruDate = (Timestamp) evStore.getNoCheckSimple("thruDate");
        if(thruDate != null) {
            Timestamp now = new Timestamp(System.currentTimeMillis());
            delete = thruDate.before(now);
        }

        Map<String, Object> idMap = "PsctViewAllow".equals(categoryType) ? viewableCategoryIds : categoryIds;
        if(delete) {
            idMap.remove(categoryId);
        } else {
            idMap.put(categoryId, DUMMYOBJ);
        }
    }
    public void update(EntityValue entity, String on) {
        if(on == null || entity == null) {
            logger.error("Store Update: Mandatory params is missing or unmatched ");
            return;
        }
        if(storeCache == null) {
            logger.error("StoreCache is not set in StoreInfo: " + this.storeId);
            return;
        }
        String eName = entity.getEntityName();
        switch (eName) {
            case "mantle.product.store.ProductStoreCategory":
                logger.debug("Store update ProductStoreCategory");
                updateStoreCategory(entity, on);
                break;
            case "mantle.product.store.ProductStoreProduct":
                logger.debug("Store update ProductStoreProduct");
                updateStoreProduct(entity, on);
                break;
            case "mantle.product.store.ProductStore":
                switch (on) {
                    case "delete":
                        storeCache._removeStore(this.storeId);
                        break;
                    case "create":
                    case "update":
                    default:
                        if(secretKey == null) { //this store not exist in cache
                            if(validateSet(this, entity, true)) {
                                if (!initialized) this.init(storeCache.ecf.getEntity());
                                storeCache._addStore(this);
                            } else {
                                logger.info("Newly created Store is missing mandatory fields. Not cached: " + this.storeId);
                            }
                        } else if(!validateSet(this, entity, false)) {
                            logger.info("New value of store is missing mandatory fields, removing from cache. " + this.storeId);
                            storeCache._removeStore(this.storeId);
                        }
                        break;
                }
                break;
            default:
                break;
        }
    }

    public boolean isSubscribing(EntityValue ev) {
        return isSubscribing(ev, null);
    }
    private boolean isCatSubscribed(String categoryId) {
        return viewableCategoryIds.containsKey(categoryId) || categoryIds.containsKey(categoryId);
    }
    public boolean isSubscribing(EntityValue ev, Set<String> productCategoryIds) {
        if(!this.initialized || this.uri == null || this.subscribedEntities.isEmpty()) {
            return false;
        }
//        if(!this.subscribedEntities.containsKey(ev.getEntityName())) {
//            return false;
//        }
        String evStoreId = (String) ev.getNoCheckSimple("productStoreId");
        if(evStoreId != null){
            return evStoreId.equals(this.storeId);
        }

        String parentCategoryId = (String) ev.getNoCheckSimple("parentProductCategoryId");
        String categoryId = (String) ev.getNoCheckSimple("productCategoryId");
        String productId = (String) ev.getNoCheckSimple("productId");
        if(parentCategoryId != null) { //table ProductCategoryRollup
            logger.debug("parentCategoryId=" + parentCategoryId + ", categoryId" + (categoryId == null ? "" : categoryId));
            if(!isCatSubscribed(parentCategoryId)) return false;
            return categoryId != null && isCatSubscribed(categoryId);
        }

        if(categoryId != null) {
            logger.debug(", categoryId = " + categoryId );
            if(viewableCategoryIds.containsKey(categoryId)) return true;
            if(!categoryIds.containsKey(categoryId)) {
                logger.debug(", categoryId not in categoryIds set");
                return false;
            }
            if(productId == null) return true;
        }
        // From here categoryId is null OR subscribed
        if(productCategoryIds != null) {
            for(String productCategoryId : productCategoryIds) {
                if(this.viewableCategoryIds.containsKey(productCategoryId)) {
                    return true;
                }
            }
            //If no categories matched and productId exists in ProductStoreProducts, return true, otherwise false
        }
        //check productId and productIds from ProductStoreProducts:
        if(productId != null) {
            logger.debug(", productId = " + productId );
            return this.productIds.containsKey(productId);
        }
        return productCategoryIds == null || productIds.isEmpty(); //if productCategoryIds is null, or any Id matched, return true, otherwise false

        //No need to check tenant because this instance is taken by tenant prefix in HttpTopicFactory
//        boolean tenantMatched = true;
//        Map<String, Object> idMap = ev.getPrimaryKeys();
//        for(Object id : idMap.values()) {
//            String sId = id.toString();
//            if(sId.startsWith("P")) {
//                tenantMatched = sId.startsWith(this.tenantPrefix);
//                break;
//            }
//        }
//        return tenantMatched;
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
