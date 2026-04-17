package com.genius.smartlight.controller.admin.store;

import com.genius.smartlight.common.ApiResponse;
import com.genius.smartlight.service.store.StoreService;
import com.genius.smartlight.vo.store.StoreRespVO;
import com.genius.smartlight.vo.store.StoreSaveReqVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @GetMapping("/current")
    public ApiResponse<StoreRespVO> current() {
        return ApiResponse.success(storeService.getCurrentStore());
    }

    @PostMapping("/setup")
    public ApiResponse<StoreRespVO> setup(@RequestBody StoreSaveReqVO reqVO) {
        return ApiResponse.success(storeService.setupCurrentStore(reqVO));
    }
}