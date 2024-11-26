package io.gleecy.foi.tool.paypal;

import io.gleecy.foi.tool.BaseClientConfigFactory;

public class PPClientConfigFactory extends BaseClientConfigFactory<PPClientConfig> {
    @Override
    public String getName() {
        return "PayPalClientConfig";
    }

    @Override
    protected PPClientConfig newClientConfig(String configId) {
        return new PPClientConfig(configId, ecf, requestFactory);
    }
}
