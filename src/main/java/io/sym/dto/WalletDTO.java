package io.sym.dto;

public class WalletDTO {
    private String privateKey;
    private String symId;

    public String getPrivateKey() {
        return privateKey;
    }

    public String getSymId() {
        return symId;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public void setSymId(String symId) {
        this.symId = symId;
    }
}
