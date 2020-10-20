package io.sym.dto;

public class MaterialDTO {
    private String nonce;
    private int chainId;
    private int forkId;
    private String nodeId;

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public int getForkId() {
        return forkId;
    }

    public void setForkId(int forkId) {
        this.forkId = forkId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

}
