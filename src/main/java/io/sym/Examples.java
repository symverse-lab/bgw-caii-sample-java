package io.sym;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.symverse.library.gsymweb3j.extensions.KeyExtensions;
import com.symverse.library.gsymweb3j.util.RLPComposer;
import com.symverse.library.gsymweb3j.util.SCTParameters;
import io.sym.dto.*;
import okhttp3.*;
import org.web3j.crypto.Credentials;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 프로세스
 * 1. Wallet 생성 ( SymID 발급 ) : createWallet 함수 호출
 *   1-1. privateKey, publicKeyHash 생성
 *   1-2. 위에서 생성한 publicKeyHash 를 CA를 통해 등록 하여 SymID 발급
 *   1-3. SymID 발급이 정상처리 되었으면, privateKey 문자열 혹은 keyStore 파일 형태로 저장
 *
 * 2. RawTransaction 생성
 *   2-1. 일반 트랜잭션 : createNormalRawTx 함수 호출
 *   2-2. SCT 트랜잭션 : createSCTRawTx 함수 호출
 *
 * 3. Transaction 생성 ( RawTransaction 전송 ) : BGW 문서 참조
 *   3-1. 일반 트랜잭션 : BGW API (POST /api/v1/sym/send-raw ) 호출
 *   3-2. SCT 트랜잭션 : BGW API (POST /api/v1/sct/send-raw ) 호출
 */
public class Examples {

    private final ObjectMapper mapper;
    private final OkHttpClient okHttpClient;

    private final String BGW_API_URL = "https://bgw-prod-001.wisem.com";
    private final String CA_API_URL = "https://mainnet-ca.symverse.com";
    private final String DAPP_KEY = "발급받은 DAPP_KEY"; // BGW-CA 를 통해 발급 받은 DAPP_KEY 를 입력

    public Examples() {
        this.mapper = new ObjectMapper();
        ConnectionPool pool = new ConnectionPool(10, 300, TimeUnit.SECONDS);
        this.okHttpClient = new OkHttpClient.Builder()
                .connectionPool(pool)
                .connectTimeout(1500, TimeUnit.MILLISECONDS)
                .build();
    }

    /**
     * 아래의 두가지 값을 생성
     * PrivateKey 와 publicKeyHash 를 생성 후, CA를 통해 SymID 발급
     * @return WalletDTO
     *         (String) privateKey : 트랜잭션 서명에 사용
     *         (String) publicKeyHash : SymID 발급 요청( --> CA )시 사용, 중복된 publicKeyHash 등록 불가
     *         (String) symId : CA를 통해 발급 받은 SymID
     */
    public WalletDTO createWallet() throws IOException {

        byte[] privateKeyBytes = KeyExtensions.generatePrivateKey();
        String privateKey = DatatypeConverter.printHexBinary(privateKeyBytes); // 생성한 (byte[])privateKey 를 hexString 으로 변환

        Credentials credentials = KeyExtensions.credentials(privateKeyBytes);
        assert credentials != null;
        String publicKeyHash = KeyExtensions.publicKeyHash(credentials.getEcKeyPair()); // SymID 발급 요청( --> CA )시 사용

        // CA API 호출 - SymID 발급 요청
        Request request = new Request.Builder()
                .url(CA_API_URL + "/caii/d1/" + DAPP_KEY + "/account/new/" + publicKeyHash)
                .post(RequestBody.create("", MediaType.parse("application/json; charset=utf-8")))
                .build();

        Response response = this.okHttpClient.newCall(request).execute();
        String stringResult = Objects.requireNonNull(response.body()).string();
        System.out.println("caResult : " + stringResult);
        CaiiResDTO caiiResDTO = this.mapper.readValue(stringResult, CaiiResDTO.class);
        CaiiRegSymResDTO caiiRegSymResDTO = this.mapper.convertValue(caiiResDTO.getResult(), CaiiRegSymResDTO.class);
        String symId = caiiRegSymResDTO.getSymid();

        WalletDTO walletDTO = new WalletDTO();
        walletDTO.setPrivateKey(privateKey);
        walletDTO.setSymId(symId);

        System.out.println("symId : " + symId);
        System.out.println("privateKey : " + privateKey);

        return walletDTO;
    }

    /**
     * 서명을 위해 Wallet privateKey 를 Credentials 로 변환
     * @param privateKey
     * @return
     */
    public Credentials getCredential(String privateKey) {
        return Credentials.create(privateKey);
    }

    /**
     * 일반 Sym 전송 Raw 트랜잭션을 생성
     * @param walletDTO
     * @param to : 코인을 받을 SymID (ex. 0x0003b20162705cc50002)
     * @param amount : 전송할 코인 수량 x 10^18 (ex. 1sym -> 1000000000000000000)
     * @return Raw Transaction 값, BGW에 API 요청을 통해 Transaction 으로 처리
     */
    public String createNormalRawTx(WalletDTO walletDTO, String to, BigInteger amount) throws IOException {

        // BGW API 호출로 nonce, chainId, forkId, nodeId 조회
        MaterialDTO material = this.getMaterialsFromBGW(walletDTO.getSymId());
        BigInteger nonce = new BigInteger(material.getNonce());
        int chainId = material.getChainId();
        int forkId = material.getForkId();
        String nodeId = material.getNodeId();

        Credentials credentials = getCredential(walletDTO.getPrivateKey());
        RLPComposer.RLPSendRawTransaction rawTransaction = new RLPComposer.RLPSendRawTransaction.Builder()
                .setFrom(walletDTO.getSymId())
                .setTo(to)
                .setNonce(nonce)
                .setGasPrice(BigInteger.valueOf(100_000_000_000L))
                .setGasLimit(BigInteger.valueOf(49_000L))
                .setValue(amount)
                .setData(null)
                .setType(RLPComposer.Type.TYPE_ORIGINAL)
                .build(credentials, nodeId, (byte) chainId, forkId);
        String rawTransactionStr = rawTransaction.hexSignMessage();
        return rawTransactionStr;
    }

    /**
     * SCT-21-Transfer Raw 트랜잭션을 생성
     * @param walletDTO
     * @param contract : 컨트랙트 (SCT) 주소
     * @param to : 코인을 받을 SymID (ex. 0x0003b20162705cc50002)
     * @param amount : 전송할 토큰 수량 x 10^18 (ex. 1sym -> 1000000000000000000)
     * @return Raw Transaction 값, BGW에 API 요청을 통해 Transaction 으로 처리
     * @throws IOException
     */
    public String createSct21TransferRawTx(WalletDTO walletDTO, String contract, String to, BigInteger amount) throws IOException {

        // BGW API 호출로 nonce, chainId, forkId, nodeId 조회
        MaterialDTO material = this.getMaterialsFromBGW(walletDTO.getSymId());
        BigInteger nonce = new BigInteger(material.getNonce());
        int chainId = material.getChainId();
        int forkId = material.getForkId();
        String nodeId = material.getNodeId();

        Credentials credentials = getCredential(walletDTO.getPrivateKey());
        RLPComposer.RLPSendRawTransaction rawTransaction = new SCTParameters
                .SCT20Builder(contract, walletDTO.getSymId())
                .setSCT20Transfer(to, amount)
                .makeBuilder(nonce)
                .build(credentials, nodeId, (byte) chainId, forkId);
        String rawTransactionStr = rawTransaction.hexSignMessage();
        return rawTransactionStr;
    }

    /**
     * Raw 트랜잭션 생성에 필요한 값들을 조회
     * @param symId
     * @return
     * @throws IOException
     */
    public MaterialDTO getMaterialsFromBGW(String symId) throws IOException {
        MaterialDTO materialDTO = null;
        Request request = new Request.Builder()
                .url(BGW_API_URL + "/api/v1/sym/materials/" + symId)
                .get()
                .build();

        Response response = this.okHttpClient.newCall(request).execute();
        String stringResult = Objects.requireNonNull(response.body()).string();
        BgwResDTO bgwResDTO = this.mapper.readValue(stringResult, BgwResDTO.class);
        materialDTO = this.mapper.convertValue(bgwResDTO.getResult(), MaterialDTO.class);
        return materialDTO;
    }

    /**
     * 생성한 SCT Raw 트랜잭션을 BGW로 전송하여 트랜잭션을 처리한다
     * @param bgwSendRawDTO : 관련 문서 BGW-API 문서 참조
     * @throws IOException
     */
    public void sendSctRawTx(BgwSendRawDTO bgwSendRawDTO) throws IOException {
        String jsonString = this.mapper.writeValueAsString(bgwSendRawDTO);
        Request request = new Request.Builder()
                .url(BGW_API_URL + "/api/v1/sct/send-raw")
                .post(RequestBody.create(jsonString, MediaType.parse("application/json; charset=utf-8")))
                .build();
        Response response = this.okHttpClient.newCall(request).execute();
        String stringResult = Objects.requireNonNull(response.body()).string();
        BgwResDTO bgwResDTO = this.mapper.readValue(stringResult, BgwResDTO.class);
        System.out.println(this.mapper.writeValueAsString(bgwResDTO));
    }
}
