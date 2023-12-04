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
import org.moqui.context.ExecutionContextFactory;
import org.moqui.context.ToolFactory;
import org.moqui.entity.EntityCondition;
import org.moqui.entity.EntityFind;
import org.moqui.entity.EntityList;
import org.moqui.entity.EntityValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.jetty.http2.client.http.ClientConnectionFactoryOverHTTP2;

import java.util.HashMap;
import java.util.Map;

public class HttpTopicFactory implements ToolFactory<HttpTopic> {
    private static final Logger logger = LoggerFactory.getLogger(HttpTopicFactory.class);
    private static final String STORE_ENTITY_NAME = "mantle.product.store.ProductStore";

    private HttpClient httpClient = null;
    private Map<String, HttpPublisher> publisherMap = new HashMap<>();
    private HttpPublisher publicPublisher = null;

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
        this.initHttpClient();
        this.publicPublisher = new HttpPublisher(this.httpClient);
        this.publisherMap.clear();

        EntityFind find = ecf.getEntity().find(STORE_ENTITY_NAME);
        find.selectField("productStoreId")
                //.selectField("organizationPartyId")
                .selectField("notificationUrl")
                .selectField("subscribedEntities");
        find.condition("notificationUrl", EntityCondition.IS_NOT_NULL, null)
                .condition("subscribedEntities", EntityCondition.IS_NOT_NULL, null);
        find.useCache(true);
        find.disableAuthz();
        EntityList eList = find.list();
        for(EntityValue e : eList) {
            this.initInstance(e);
        }
    }

    private void initHttpClient() {
        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client(true);
        ClientConnector clientConnector = new ClientConnector();
        clientConnector.setSslContextFactory(sslContextFactory);

        //ClientConnectionFactoryO
        // Prepare the application protocols.
        ClientConnectionFactory.Info h1 = HttpClientConnectionFactory.HTTP11;

        HTTP2Client http2Client = new HTTP2Client(clientConnector);
        ClientConnectionFactory.Info h2 = new ClientConnectionFactoryOverHTTP2.HTTP2(http2Client);

        // Create the HttpClientTransportDynamic, preferring h2 over h1.
        HttpClientTransport transport = new HttpClientTransportDynamic(clientConnector, h2, h1);
        transport.setConnectionPoolFactory(destination ->
                new MultiplexConnectionPool(destination, Pool.StrategyType.THREAD_ID, 10,true, destination, 1));

        this.httpClient = new HttpClient(transport);
        httpClient.setFollowRedirects(false);
        httpClient.setMaxConnectionsPerDestination(10);

        try {
            httpClient.start();
        } catch (Exception e) {
            logger.error("Cannot start HttpClient: ", e);
        }
    }
    private HttpPublisher initInstance(EntityValue eStore) {
        String tenantPrefix = eStore.getTenantPrefix();
        String storeId = (String) eStore.getNoCheckSimple("productStoreId");
        String subscribedEntities = (String) eStore.getNoCheckSimple("subscribedEntities");
        if(tenantPrefix == null
                || storeId == null || storeId.isEmpty()
                || subscribedEntities == null
                || subscribedEntities.isBlank()) {
            return null;
        }

        this.publicPublisher.putSubscriber(eStore);
        HttpPublisher publisher = this.publisherMap.computeIfAbsent(tenantPrefix, k -> new HttpPublisher(this.httpClient));
        publisher.putSubscriber(eStore);
        return publisher;
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
        boolean isStoreEntity = STORE_ENTITY_NAME.equals(ev.getEntityName());
        boolean isSubscriberChanged = false;

        String on = ((String) parameters[1]).trim().toLowerCase();
        switch (on) {
            case "delete":
                if (isStoreEntity) {
                    ev.set("notificationUrl", "");
                    ev.set("subscribedEntities", "");
                    isSubscriberChanged = true;
                }
                break;
            case "create":
                if(isStoreEntity) {
                    isSubscriberChanged = ev.getNoCheckSimple("notificationUrl") != null
                                    && ev.getNoCheckSimple("subscribedEntities") != null;
                }
                break;
            case "update":
                if(isStoreEntity) {
                    isSubscriberChanged = ev.getNoCheckSimple("notificationUrl") != null
                                    || ev.getNoCheckSimple("subscribedEntities") != null;
                }
                break;
            default:
                logger.error("Invalid 'on' parameter: " + on);
                return null;
        }

        HttpPublisher publisher = this.publicPublisher;
        String tenantPrefix = ev.getTenantPrefix();
        if(tenantPrefix != null && !tenantPrefix.isEmpty()) {
            publisher = this.publisherMap.get(tenantPrefix);
        }

        if(isSubscriberChanged) {
            if(publisher == null) { //The input store has not ever subscribed before
                publisher = initInstance(ev); //already putSubscriber
            } else {
                publisher.putSubscriber(ev);
            }
            if(this.publicPublisher != publisher) {
                this.publicPublisher.putSubscriber(ev);
            }
        }

        if(publisher == null) {
            return null;
        }

        return publisher.newTopic(ev, Map.of("on", on));
    }

    /**
     * Called on destroy/shutdown of Moqui to destroy (shutdown, close, etc) the underlying tool.
     */
    @Override
    public void destroy() {
        this.publisherMap = null;
        this.publicPublisher = null;
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
