package io.gleecy.foi;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Result;
import org.moqui.context.ExecutionContext;
import org.moqui.context.ToolFactory;
import org.moqui.entity.EntityFind;
import org.moqui.entity.EntityList;
import org.moqui.entity.EntityValue;
import org.moqui.impl.entity.EntityDefinition;
import org.moqui.impl.entity.EntityFacadeImpl;
import org.moqui.impl.entity.EntityValueBase;
import org.moqui.impl.entity.FieldInfo;
import org.moqui.util.ContextStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class EntityTopic implements HttpTopic {
    public static final String[] productEntities = new String[] {
            "mantle.product.category.ProductCategoryMember",
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

    public static final Map<String, Function<Map<String, String>, Map<String, String>>> transformerMap =
            Map.of(
//               "mantle.product.feature.ProductFeatureAppl", (ev) -> Map.of(
//                            "entityName", "Product",
//                            "productId", ev.remove("productId"), //Update product with ID==toProductId: add FK column --fk-> productId
//                            ev.remove("applTypeEnumId") + ev.remove("sequenceNum") + "PfId", ev.remove("productFeatureId")
//                        ),
//                "mantle.product.ProductAssoc", (ev) -> Map.of(
//                                "entityName", "Product",
//                                "columnPrefix", ev.get("productAssocTypeEnumId") + "_" + ev.get("productId") + "_",
//                                "productId", ev.remove("toProductId"), //Update product with ID==toProductId: add FK column --fk-> productId
//                                ev.remove("productAssocTypeEnumId") + "ParentId", ev.remove("productId")
//                            ),
                    "ProductStoreCategory", (ev) -> Map.of(
                            "entityName", "ProductCategory",
                            "productCategoryId", ev.remove("productCategoryId")
                            ),
                "ProductCalculatedInfo", (ev) -> Map.of(
                            "entityName", "Product",
                            "columnPrefix", "statistic.",
                            "productId", ev.remove("productId")
                            ),
                "ProductGeo", (ev) -> Map.of(
                            "entityName", "Product",
                            "columnPrefix", ev.get("geoId") + "_",
                            "productId", ev.remove("productId"),
                            ev.remove("productGeoPurposeEnumId") + "GeoId", ev.remove("geoId")
                            ),
                "ProductIdentification", (ev) -> Map.of(
                            "entityName", "Product",
                            "columnPrefix", ev.get("idValue") + "_",
                            "productId", ev.remove("productId"),
                            ev.remove("productIdTypeEnumId") + "PiId", ev.remove("idValue")
                        ),
                "ProductPrice", (ev) -> Map.of(
                            "entityName", "Product",
                            // "columnPrefix", ev.get("productPriceId") + "_",
                            "productId", ev.remove("productId"),
                            ev.remove("pricePurposeEnumId") + ev.remove("priceTypeEnumId") + "Price",
                                ev.remove("price") + " " + ev.remove("priceUomId")
                        )
            );

    private final static Logger logger = LoggerFactory.getLogger(EntityTopic.class);
    private final static ThreadLocal<Set<String>> processedIds = new ThreadLocal<>(){
        @Override protected Set<String> initialValue() {
            return  new HashSet<>();
        }
    };
    public static void publishProductId(ExecutionContext ec) {
        final ContextStack cs = ec.getContext();
        String productId = (String) cs.get("productId");
        if(processedIds.get().contains(productId)) {
            return;
        }
        processedIds.get().add(productId);

        publishProductId(
                (EntityFacadeImpl) ec.getEntity(),
                ec.getFactory().getToolFactory("HttpTopic"),
                (String) productId,
                (String) cs.get("on"),
                (String) cs.get("storeId"),
                (String) cs.get("productCategoryId"));
    }
    private static void publishProductId(EntityFacadeImpl ef, ToolFactory<HttpTopic> toolFactory, String productId, String on, String storeId, String catId) {
        EntityValue product =
                ef.fastFindOne("mantle.product.Product", true, true, productId);
        if(product == null) {
            logger.error("Cannot find product with ID=" + productId);
            return;
        }
        publishProduct(ef, toolFactory, product, on, storeId, catId);
    }
    public static void publishProduct(ExecutionContext ec) {
        final ContextStack cs = ec.getContext();
        EntityValue product = (EntityValue) cs.get("product");
        if(product == null) return;
        String productId = (String) product.getNoCheckSimple("productId");
        if(processedIds.get().contains(productId)) {
            return;
        }
        processedIds.get().add(productId);

        publishProduct(
            (EntityFacadeImpl) ec.getEntity()
            , ec.getFactory().getToolFactory("HttpTopic")
            , product
            , (String) cs.get("on")
            , (String) cs.get("storeId")
            , (String) cs.get("productCategoryId"));
    }

    /**
     * Must check if this product is processed before calling this method
     *         String productId = (String) product.getNoCheckSimple("productId");
     *         if(processedIds.get().contains(productId)) {
     *             return;
     *         }
     * @param ef
     * @param toolFactory
     * @param product
     * @param on
     * @param storeId
     * @param catId
     */
    private static void publishProduct(EntityFacadeImpl ef, ToolFactory<HttpTopic> toolFactory, EntityValue product, String on, String storeId, String catId) {
        HttpTopic topic = toolFactory.getInstance(product, on, storeId, catId);
        if(topic == null) {
            return;
        }
        topic.send();
        String productId = (String) product.getNoCheckSimple("productId");
        for (String productEntity : productEntities) {
            EntityDefinition ed = ef.getEntityDefinition(productEntity);
            EntityFind finder = ed.makeEntityFind()
                    .condition("productId", productId)
                    .forUpdate(false).useCache(true).disableAuthz();
            if (ed.isField("thruDate"))
                finder.conditionDate(null, null, null);

            EntityList evs = finder.list();
            for (EntityValue ev : evs) {
                String keyStr = ev.getPrimaryKeysString();
                if(!processedIds.get().contains(keyStr)) {
                    processedIds.get().add(keyStr);
                    topic = toolFactory.getInstance(ev, on, storeId, catId);
                    if (topic != null) {
                        topic.send();
                    }
                }
            }
        }

        if("PatVirtual".equals((String) product.getNoCheckSimple("productTypeEnumId"))) {
            EntityFind finder = ef.find("mantle.product.ProductAssoc")
                    .disableAuthz().useCache(true).forUpdate(false)
                    .selectField("toProductId")
                    .condition("productId", productId)
                    .condition("productAssocTypeEnumId", "PatVariant")
                    .conditionDate(null, null, null);
            EntityList children = finder.list();
            for(EntityValue child : children) {
                String keyStr = child.getPrimaryKeysString();
                if(!processedIds.get().contains(keyStr)) {
                    processedIds.get().add(keyStr);
                    topic = toolFactory.getInstance(child, on, storeId, catId);
                    if (topic != null) {
                        topic.send();
                    }
                }

                String childProductId = (String) child.getNoCheckSimple("toProductId");
                if(!processedIds.get().contains(childProductId)) {
                    processedIds.get().add(childProductId);
                    publishProductId(ef, toolFactory, childProductId, on, storeId, catId);
                }
            }
        }
    }

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
    final String entityName;
    final Map<String, String> params = new HashMap<>();
    final List<URI> uris;
    final ThreadPoolExecutor workerPool;
    int attempts = 0;
    EntityTopic(HttpClient httpClient, EntityValue ev, Map<String, String> params, ThreadPoolExecutor workerPool, List<URI> uris) {
        this.uris = uris;
        this.httpClient = httpClient;
        if(params != null) {
            this.params.putAll(params);
        }
        this.workerPool = workerPool;

        boolean isDelete = "delete".equalsIgnoreCase(this.params.get("on"));
        if(!isDelete) {
            Timestamp thruDate = (Timestamp) ev.getNoCheckSimple("thruDate");
            if(thruDate != null) {
                Timestamp now = new Timestamp(System.currentTimeMillis());
                isDelete = thruDate.before(now);
            }
        }

        EntityValueBase entity = ((EntityValueBase) ev);
        EntityDefinition ed = entity.getEntityDefinition();
        this.entityName = ed.getEntityName();
        FieldInfo[] fieldInfos = ed.entityInfo.allFieldInfoArray;
        Map<String, String> evMap = new HashMap<>();
        int numFields = fieldInfos.length;
        for (int i = 0; i < numFields; i++) {
            FieldInfo fieldInfo = fieldInfos[i];
            Object fieldValue = entity.getKnownField(fieldInfo);
            String sFValue = fieldValue != null ? fieldInfo.convertToString(fieldValue) : "_NA_";
            if(fieldValue != null)
                evMap.put(fieldInfo.name, sFValue);
        }

        Function<Map<String, String>, Map<String, String>> transformer = transformerMap.get(this.entityName);
        if(transformer != null) {
            Map<String, String> transformedMap = transformer.apply(evMap);
            String colPrefix = transformedMap.get("columnPrefix");
            final String prefix = colPrefix != null ? colPrefix : "";
            if(isDelete) {
                evMap.keySet().forEach((key) -> this.params.put(prefix + key, "_NA_"));
            } else {
                evMap.forEach((key, value) -> this.params.put(prefix + key, value));
            }
            if(colPrefix != null) {
                this.params.put("on", "update");
            } else if (isDelete) {
                this.params.put("on", "delete");
                this.params.put("delete", "true");
            }
            this.params.putAll((transformedMap));
        } else {
            this.params.put("entityName", this.entityName);
            if(isDelete) {
                this.params.put("delete", "true");
            }
            this.params.putAll(evMap);
        }
    }

    @Override
    public void addParam(String key, String value) {
        params.put(key, value == null ? "_NA_" : value);
    }

    public void send() {
        if (!startIfNeeded())
            return;
        for (URI uri: uris) {
            workerPool.execute(new Runnable() {
                @Override
                public void run() {
                    Request request = httpClient.POST(uri).accept("text/html");
                    request.idleTimeout(200, TimeUnit.SECONDS);
                    for (Map.Entry<String, String> paramEntry : params.entrySet()) {
                        request.param(paramEntry.getKey(), paramEntry.getValue());
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
