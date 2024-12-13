package io.gleecy.foi;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.client.MultiplexConnectionPool;
import org.eclipse.jetty.client.dynamic.HttpClientTransportDynamic;
import org.eclipse.jetty.client.http.HttpClientConnectionFactory;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.io.ClientConnectionFactory;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.Pool;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.moqui.context.ExecutionContextFactory;
import org.moqui.context.ToolFactory;
import org.moqui.entity.EntityFacade;
import org.moqui.entity.EntityFind;
import org.moqui.entity.EntityList;
import org.moqui.entity.EntityValue;
import org.moqui.impl.context.ExecutionContextFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.jetty.http2.client.http.ClientConnectionFactoryOverHTTP2;

import java.net.URI;
import java.util.*;

public class HttpTopicFactory implements ToolFactory<HttpTopic> {
    private static final Logger logger = LoggerFactory.getLogger(HttpTopicFactory.class);
    private static final String STORE_ENTITY_NAME = "mantle.product.store.ProductStore";

    private HttpClient httpClient = null;
    private ExecutionContextFactory ecf = null;

    /**
     * Return a name that the factory will be available under through the ExecutionContextFactory.getToolFactory()
     * method and instances will be available under through the ExecutionContextFactory.getTool() method.
     */
    @Override
    public String getName() {
        return "HttpTopic";
    }

    /**
     * Initialize the underlying tool and if the instance is a singleton also the instance.
     *
     */
    @Override
    public void init(ExecutionContextFactory ecf) {
        this.ecf = ecf;
        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client(true);

        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setName("httpTopicPublisher");
        threadPool.setMaxThreads(10);

        ClientConnector clientConnector = new ClientConnector();
        //clientConnector.setExecutor(threadPool);
        clientConnector.setSslContextFactory(sslContextFactory);
        clientConnector.setReuseAddress(true);
        clientConnector.setReusePort(true);

        //ClientConnectionFactoryO
        // Prepare the application protocols.
        ClientConnectionFactory.Info h1 = HttpClientConnectionFactory.HTTP11;

        HTTP2Client http2Client = new HTTP2Client(clientConnector);
        http2Client.setMaxConcurrentPushedStreams(128);

        ClientConnectionFactory.Info h2 = new ClientConnectionFactoryOverHTTP2.HTTP2(http2Client);

        // Create the HttpClientTransportDynamic, preferring h2 over h1.
        HttpClientTransport transport = new HttpClientTransportDynamic(clientConnector, h1, h2);
        transport.setConnectionPoolFactory(destination ->
                new MultiplexConnectionPool(destination, Pool.StrategyType.THREAD_ID, 10,true, destination, 1));


        this.httpClient = new HttpClient(transport);
        httpClient.setFollowRedirects(false);
        httpClient.setMaxConnectionsPerDestination(10);
        httpClient.setExecutor(threadPool);
        httpClient.setRequestBufferSize(50000000);
        httpClient.setMaxRequestsQueuedPerDestination(2000);
        httpClient.setMaxConnectionsPerDestination(10);

        try {
            httpClient.start();
            httpClient.getContentDecoderFactories().clear();
        } catch (Exception e) {
            logger.error("Cannot start HttpClient: ", e);
        }
    }

    private Set<String> getCatsFromProductId(EntityValue ev) {
        Set<String> productCategories = new HashSet<>();
        String productId = (String) ev.getNoCheckSimple("productId"); //productTypeEnumId
        if(productId == null) {
            return productCategories;
        }
        String productTypeEnumId = (String) ev.getNoCheckSimple("productTypeEnumId");
        EntityFacade ef = this.ecf.getEntity();
        if(productTypeEnumId == null || !productTypeEnumId.equals("PtVirtual")) {
            EntityFind finder = ef.find("mantle.product.ProductAssoc")
                    .disableAuthz().useCache(true).forUpdate(false)
                    .selectField("productId")
                    .condition("toProductId", productId)
                    .condition("productAssocTypeEnumId", "PatVariant")
                    .conditionDate(null, null, null);
            EntityList parents = finder.list();
            if(parents != null && !parents.isEmpty()) {
                productId = (String) parents.getFirst().getNoCheckSimple("productId");
            }
        }
        EntityFind finder = ef.find("mantle.product.category.ProductCategoryMember")
                .disableAuthz().useCache(true).forUpdate(false)
                .selectField("productCategoryId")
                .condition("productId", productId)
                .conditionDate(null, null, null);
        EntityList catMems = finder.list();
        for(EntityValue catMem : catMems) {
            productCategories.add((String) catMem.getNoCheckSimple("productCategoryId"));
        }
        return productCategories;
    }
    private EntityTopic newEntityTopic(EntityValue ev, Map<String, String> params) {

        List<StoreInfo> stores = null;
        String storeId = params.get("storeId");
        if(storeId != null && !storeId.isEmpty()) {
            StoreInfo store = this.ecf.getTool("StoreInfo",
                    StoreInfo.class, storeId);
            if(store != null) {
                stores = new ArrayList<>();
                stores.add(store);
            }
        } else {
            StoreInfoCache storeCache = this.ecf.getTool("StoreInfo",
                    StoreInfo.class, "_NA_").storeCache;
            String tenantPrefix = ev.getTenantPrefix();
            stores = storeCache.storesByTenant.get(tenantPrefix);
        }

        List<URI> uris = new ArrayList<>();
        if(stores != null) {
            String catId = params.get("productCategoryId");
            Set<String> productCategories = new HashSet<>(); //List of product category to
            if(catId != null && !catId.isEmpty()) {
                productCategories.add(catId);
            }
            catId = (String) ev.getNoCheckSimple("productCategoryId");
            if(catId != null) {
                productCategories.add(catId);
            }

            Set<String> evCategories = null;
            for(StoreInfo store : stores) {
                if(store.isSubscribing(ev, productCategories)) {
                    uris.add(store.uri);
                } else {
                    if(evCategories == null) {
                        evCategories = getCatsFromProductId(ev);
                    }
                    if(!evCategories.isEmpty() && store.isSubscribing(ev, evCategories)) {
                        uris.add(store.uri);
                    }
                }
            }
        }

        if(uris.isEmpty()) {
            logger.info("Topic on " + ev.getEntityName() + " " + params.get("on") + ": No subscribers");
            return null;
        }
        return new EntityTopic(this.httpClient, ev, params,
                ((ExecutionContextFactoryImpl) this.ecf).workerPool, uris);
    }

    /**
     * Rarely used, initialize before Moqui Facades are initialized; useful for tools that ResourceReference,
     * ScriptRunner, TemplateRenderer, ServiceRunner, .etc implementations depend on.
     *
     */
    @Override
    public void preFacadeInit(ExecutionContextFactory ecf) {
        //DO nothing
    }

    /**
     * Called by ExecutionContextFactory.getTool() to get an instance object for this tool.
     * May be created for each call or a singleton.
     *
     * @param parameters: first param must be the entity value, <br>
     *                  second is one of "create", "update" or "delete
     * @throws IllegalStateException if not initialized
     */
    @Override
    public HttpTopic getInstance(Object... parameters) {
        if(parameters == null || parameters.length < 2
                || parameters[0] == null || !(parameters[0] instanceof EntityValue)
                || parameters[1] == null || !(parameters[1] instanceof String)) {
            if (logger.isErrorEnabled()) {
                StringBuilder errSb = new StringBuilder("HttpPublisher invoked with invalid parameters: ");
                if (parameters == null) errSb.append("parameters == NULL");
                else {
                    errSb.append("\n parameters.length = ").append(parameters.length);
                    if (parameters.length >= 1)
                        if (parameters[0] == null) errSb.append("\n parameters[0] is null");
                        else errSb.append("\n Type of parameters[0]: ").append(parameters[0].getClass().getName());
                    if (parameters.length >= 2)
                        if (parameters[1] == null) errSb.append("\n parameters[1] is null");
                        else errSb.append("\n Type of parameters[1]: ").append(parameters[1].getClass().getName());
                }
                logger.error(errSb.toString());
            }
            return null;
        }

        EntityValue ev = (EntityValue) parameters[0];
        String on = ((String) parameters[1]).trim().toLowerCase();
        String storeId = "";
        if(parameters.length >= 3 && parameters[2] != null) {
            storeId = ((String) parameters[2]).trim();
        }
        String productCategoryId = "";
        if(parameters.length >= 4 && parameters[3] != null) {
            productCategoryId = ((String) parameters[3]).trim();
        }

        return this.newEntityTopic(ev, Map.of("on", on, "storeId", storeId, "productCategoryId", productCategoryId));
    }

    /**
     * Called on destroy/shutdown of Moqui to destroy (shutdown, close, etc) the underlying tool.
     */
    @Override
    public void destroy() {
        try {
            httpClient.stop();
        } catch (Exception e) {
            logger.error("Error stopping http client", e);
        }
    }

    /**
     * Rarely used, like destroy() but runs after the facades are destroyed.
     */
    @Override
    public void postFacadeDestroy() {
        ToolFactory.super.postFacadeDestroy();
    }
}
