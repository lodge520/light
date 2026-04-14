package com.genius.smartlight.integration.ai;

import com.genius.smartlight.common.ServiceException;
import com.genius.smartlight.vo.ai.FabricRecognizeRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class FabricAiClient {

    private final RestTemplate restTemplate;

    @Value("${ai.fabric.url}")
    private String fabricUrl;

    public FabricRecognizeRespVO recognize(MultipartFile file) {
        try {
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg";
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", resource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<FabricRecognizeRespVO> response = restTemplate.exchange(
                    fabricUrl,
                    HttpMethod.POST,
                    requestEntity,
                    FabricRecognizeRespVO.class
            );

            if (response.getBody() == null) {
                throw new ServiceException("面料识别服务返回为空");
            }
            return response.getBody();
        } catch (Exception e) {
            throw new ServiceException("调用面料识别服务失败：" + e.getMessage());
        }
    }
}