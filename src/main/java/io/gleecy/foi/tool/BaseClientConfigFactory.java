package io.gleecy.foi.tool;

import org.moqui.context.ExecutionContextFactory;
import org.moqui.context.ToolFactory;
import org.moqui.util.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public abstract class BaseClientConfigFactory<T extends ClientConfig> implements ToolFactory<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseClientConfigFactory.class);

    protected ExecutionContextFactory ecf;
    protected int poolSize = 64, queueSize = 1024;
    protected RestClient.PooledRequestFactory requestFactory = new RestClient.PooledRequestFactory(this.getName());;
    private final HashMap<String, T> configMap = new HashMap<>();

    protected abstract T newClientConfig(String configId);

    @Override
    public void init(ExecutionContextFactory ecf) {
        ToolFactory.super.init(ecf);
        this.ecf = ecf;
        this.requestFactory.poolSize(poolSize);
        this.requestFactory.queueSize(queueSize);
        this.requestFactory.init();
    }
    @Override
    public void destroy() {
        this.requestFactory.destroy();
    }

    @Override
    public T getInstance(Object... parameters) {
        if(parameters == null || parameters.length < 1) return null;
        String configId = (String) parameters[0];
        if(configId.isBlank()) return null;
        T config = configMap.computeIfAbsent(configId,
                this::newClientConfig);
        if(!config.isLoaded()) config.load();
        if(config.error != null) {
            LOGGER.error("Failed to load config: " + config.error);
            return null;
        }
        config.reloadToken();
        if(config.error != null) {
            LOGGER.error("Failed to load token: " + config.error);
            return null;
        }
        return config;
    }
}
