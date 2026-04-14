package com.genius.smartlight.service.ai;

import com.genius.smartlight.vo.ai.FabricRecognizeRespVO;
import com.genius.smartlight.vo.ai.PersonDetectRespVO;
import org.springframework.web.multipart.MultipartFile;

public interface AiService {

    FabricRecognizeRespVO fabricRecognize(String deviceCode, MultipartFile file);

    PersonDetectRespVO personDetect(String deviceCode, MultipartFile file);
}