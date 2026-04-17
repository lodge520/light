package com.genius.smartlight.service.ai.impl;

import com.genius.smartlight.common.ServiceException;
import com.genius.smartlight.integration.ai.FabricAiClient;
import com.genius.smartlight.integration.ai.PersonDetectClient;
import com.genius.smartlight.service.ai.AiService;
import com.genius.smartlight.vo.ai.FabricRecognizeRespVO;
import com.genius.smartlight.vo.ai.PersonDetectRespVO;
import com.genius.smartlight.websocket.WebSocketPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final FabricAiClient fabricAiClient;
    private final PersonDetectClient personDetectClient;
    private final WebSocketPushService webSocketPushService;

    @Override
    public FabricRecognizeRespVO fabricRecognize(String id, MultipartFile file) {
        validateFile(file);
        FabricRecognizeRespVO result = fabricAiClient.recognize(file);
        webSocketPushService.pushFabricRecognize(id, file.getOriginalFilename(), result);
        return result;
    }

    @Override
    public PersonDetectRespVO personDetect(String id, MultipartFile file) {
        validateFile(file);
        PersonDetectRespVO result = personDetectClient.detect(file);
        webSocketPushService.pushPersonDetect(id, file.getOriginalFilename(), result);
        return result;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
    }
}