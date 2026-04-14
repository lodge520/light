package com.genius.smartlight.convert.lux;

import com.genius.smartlight.dal.dataobject.LuxRecordDO;
import com.genius.smartlight.vo.lux.LuxCreateReqVO;
import com.genius.smartlight.vo.lux.LuxRespVO;

public class LuxConvert {

    public static LuxRecordDO convert(LuxCreateReqVO reqVO) {
        LuxRecordDO record = new LuxRecordDO();
        record.setDeviceCode(reqVO.getDeviceCode());
        record.setLuxValue(reqVO.getLuxValue());
        record.setCollectTime(reqVO.getCollectTime());
        return record;
    }

    public static LuxRespVO convert(LuxRecordDO record) {
        LuxRespVO respVO = new LuxRespVO();
        respVO.setId(record.getId());
        respVO.setDeviceCode(record.getDeviceCode());
        respVO.setLuxValue(record.getLuxValue());
        respVO.setCollectTime(record.getCollectTime());
        respVO.setCreateTime(record.getCreateTime());
        return respVO;
    }
}