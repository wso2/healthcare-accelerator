package org.wso2.healthcare.apim.conformance.internal;

import com.google.gson.JsonObject;

public class ConformanceDataHolder {

    private static ConformanceDataHolder instance = new ConformanceDataHolder();

    private JsonObject wellKnownResponse;

    private ConformanceDataHolder() {
    }

    public static ConformanceDataHolder getInstance() {
        return instance;
    }

    public JsonObject getWellKnownResponse() {
        return wellKnownResponse;
    }

    public void setWellKnownResponse(JsonObject wellKnownResponse) {
        this.wellKnownResponse = wellKnownResponse;
    }
}
