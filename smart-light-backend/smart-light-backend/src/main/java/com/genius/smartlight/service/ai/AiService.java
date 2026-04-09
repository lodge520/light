package com.genius.smartlight.service.ai;

import com.genius.smartlight.vo.ai.FabricRecognizeRespVO;
import com.genius.smartlight.vo.ai.PersonDetectRespVO;
import org.springframework.web.multipart.MultipartFile;

public interface AiService {

    FabricRecognizeRespVO fabricRecognize(MultipartFile file);

    PersonDetectRespVO personDetect(MultipartFile file);
}
