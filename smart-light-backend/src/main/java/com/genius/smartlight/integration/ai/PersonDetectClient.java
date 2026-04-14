package com.genius.smartlight.integration.ai;

import com.genius.smartlight.common.ServiceException;
import com.genius.smartlight.vo.ai.PersonDetectRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class PersonDetectClient {

    private final RestTemplate restTemplate;

    @Value("${ai.flow.url}")
    private String flowUrl;

    public PersonDetectRespVO detect(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            HttpEntity<byte[]> requestEntity = new HttpEntity<>(file.getBytes(), headers);

            ResponseEntity<PersonDetectRespVO> response = restTemplate.exchange(
                    flowUrl,
                    HttpMethod.POST,
                    requestEntity,
                    PersonDetectRespVO.class
            );

            if (response.getBody() == null) {
                throw new ServiceException("人流检测服务返回为空");
            }
            return response.getBody();
        } catch (Exception e) {
            throw new ServiceException("调用人流检测服务失败：" + e.getMessage());
        }
    }
}