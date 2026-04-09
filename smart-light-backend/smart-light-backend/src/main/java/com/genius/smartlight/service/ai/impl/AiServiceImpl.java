package com.genius.smartlight.service.ai.impl;

import com.genius.smartlight.common.ServiceException;
import com.genius.smartlight.integration.ai.FabricAiClient;
import com.genius.smartlight.integration.ai.PersonDetectClient;
import com.genius.smartlight.service.ai.AiService;
import com.genius.smartlight.vo.ai.FabricRecognizeRespVO;
import com.genius.smartlight.vo.ai.PersonDetectRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final FabricAiClient fabricAiClient;
    private final PersonDetectClient personDetectClient;

    @Override
    public FabricRecognizeRespVO fabricRecognize(MultipartFile file) {
        validateFile(file);
        return fabricAiClient.recognize(file);
    }

    @Override
    public PersonDetectRespVO personDetect(MultipartFile file) {
        validateFile(file);
        return personDetectClient.detect(file);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
    }
}