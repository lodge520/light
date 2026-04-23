import http from './http'

interface CommonResult<T> {
  code: number
  msg: string
  data: T | null
}

export interface FabricRecognizeRespVO {
  chipId?: string

  fabric?: string
  label?: string
  confidence?: number
  fabricConfidence?: number

  mainColorRgb?: string
  recommendedBrightness?: number
  recommendedTemp?: number

  clothDetected?: boolean
  clothX?: number
  clothY?: number
  clothW?: number
  clothH?: number

  annotatedImageBase64?: string
  clothMaskedPngBase64?: string

  imageUrl?: string
  [key: string]: any
}

/**
 * 面料识别
 * 后端接口：POST /admin/ai/fabric-recognize
 */
export async function fabricRecognize(file: File, chipId?: string): Promise<FabricRecognizeRespVO> {
  const formData = new FormData()
  formData.append('file', file)

  const res = await http.post<CommonResult<FabricRecognizeRespVO>>(
    '/admin/ai/fabric-recognize',
    formData,
    {
      params: chipId ? { chipId } : {},
    }
  )

  if (res.data.code !== 200 || !res.data.data) {
    throw new Error(res.data.msg || '面料识别失败，后端未返回识别结果')
  }

  return res.data.data
}