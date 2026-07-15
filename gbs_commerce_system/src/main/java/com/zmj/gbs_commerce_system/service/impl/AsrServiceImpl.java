package com.zmj.gbs_commerce_system.service.impl;

import com.huawei.sis.bean.AuthInfo;
import com.huawei.sis.bean.RasrListener;
import com.huawei.sis.bean.SisConfig;
import com.huawei.sis.bean.SisConstant;
import com.huawei.sis.bean.request.SasrWebsocketRequest;
import com.huawei.sis.bean.response.RasrResponse;
import com.huawei.sis.bean.response.StateResponse;
import com.huawei.sis.client.SasrWebsocketClient;
import com.huawei.sis.bean.base.RasrSentence;
import com.zmj.gbs_commerce_system.config.HuaweiAsrProperties;
import com.zmj.gbs_commerce_system.service.AsrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class AsrServiceImpl implements AsrService {

    private final HuaweiAsrProperties asrProperties;
    private final AuthInfo authInfo;
    private final SisConfig config;
    
    private static final int BEGIN_TIMEOUT_SECONDS = 3;
    private static final int END_TIMEOUT_SECONDS = 10;

    public AsrServiceImpl(HuaweiAsrProperties asrProperties) {
        this.asrProperties = asrProperties;
        this.authInfo = new AuthInfo(
            asrProperties.getAk(),
            asrProperties.getSk(),
            asrProperties.getRegion(),
            asrProperties.getProjectId()
        );
        this.config = new SisConfig();
        this.config.setConnectionTimeout(5000);
        this.config.setReadTimeout(10000);
    }

    @Override
    public String recognize(String audioBase64, String audioFormat, String property) {
        CountDownLatch beginLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(1);
        AtomicReference<String> resultRef = new AtomicReference<>("");

        SasrWebsocketClient client = null;
        try {
            RasrListener listener = createListener(beginLatch, endLatch, resultRef);
            client = new SasrWebsocketClient(authInfo, listener, config);

            SasrWebsocketRequest request = new SasrWebsocketRequest(audioFormat, property);
            request.setAddPunc("yes");
            request.setIntermediateResult("no");
            request.setDigitNorm("no");
            request.setNeedWordInfo("no");

            client.sasrConnect(request);
            client.sendStart();

            if (!beginLatch.await(BEGIN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new RuntimeException("等待识别开始超时");
            }

            byte[] audioData = Base64.getDecoder().decode(audioBase64);
            client.sendByte(audioData);
            client.sendEnd();

            endLatch.await(END_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            return resultRef.get();

        } catch (Exception e) {
            log.error("语音识别异常: {}", e.getMessage());
            throw new RuntimeException("语音识别异常: " + e.getMessage());
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private RasrListener createListener(CountDownLatch beginLatch, CountDownLatch endLatch, AtomicReference<String> resultRef) {
        return new RasrListener() {
            @Override
            public void onTranscriptionConnect() {
            }

            @Override
            public void onTranscriptionClose() {
            }

            @Override
            public void onTranscriptionResponse(RasrResponse response) {
                if (response.getSentenceList() != null && !response.getSentenceList().isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (RasrSentence sentence : response.getSentenceList()) {
                        if (sentence.getText() != null) {
                            sb.append(sentence.getText());
                        }
                    }
                    resultRef.set(sb.toString());
                }
            }

            @Override
            public void onTranscriptionBegin(StateResponse response) {
                beginLatch.countDown();
            }

            @Override
            public void onSTranscriptionEnd(StateResponse response) {
                endLatch.countDown();
            }

            @Override
            public void onTranscriptionFail(StateResponse response) {
                beginLatch.countDown();
                endLatch.countDown();
            }

            @Override
            public void onEvent(String event) {
            }
        };
    }
}