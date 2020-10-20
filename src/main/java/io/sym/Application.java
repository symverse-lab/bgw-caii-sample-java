package io.sym;

import io.sym.dto.BgwSendRawDTO;
import io.sym.dto.WalletDTO;

import java.io.IOException;
import java.math.BigInteger;

public class Application {
    public static void main(String[] args) throws IOException {
        Examples examples = new Examples();
        // sendNormalRawTransaction(examples);
        sendSct21RawTransaction(examples);
    }

    public static void sendSct21RawTransaction(Examples examples) throws IOException {
        WalletDTO wallet = examples.createWallet();
        String sctRawTx = examples.createSct21TransferRawTx(wallet, "0x5c0a30214df7120a2759", "0x000231313d7950710002", new BigInteger("10000000000000000000"));
        System.out.println("sctRawTx : " + sctRawTx);
        BgwSendRawDTO bgwSendRawDTO = new BgwSendRawDTO();
        bgwSendRawDTO.setData(sctRawTx);
        examples.sendSctRawTx(bgwSendRawDTO);
    }
}
