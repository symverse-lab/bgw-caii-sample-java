package io.sym.dto;

public class BgwSendRawDTO {
    private String callbackId;
    private String callbackUrl;
    private String data;

    public String getCallbackId() {
        return callbackId;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public String getData() {
        return data;
    }

    public void setCallbackId(String callbackId) {
        this.callbackId = callbackId;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public void setData(String data) {
        this.data = data;
    }
}
