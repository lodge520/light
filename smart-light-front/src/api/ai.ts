import http from './http'

interface CommonResult<T> {
  code: number
  msg: string
  data: T
}

export interface FabricRecognizeRespVO {
  chipId?: string
  fabric?: string
  label?: string
  confidence?: number
  fabricConfidence?: number
  imageUrl?: string
  [key: string]: any
}

/**
 * 面料识别
 * 后端接口：POST /admin/ai/fabric-recognize
 */
export async function fabricRecognize(file: File, chipId?: string) {
  const formData = new FormData()
  formData.append('file', file)

  const res = await http.post<CommonResult<FabricRecognizeRespVO>>(
    '/admin/ai/fabric-recognize',
    formData,
    {
      params: chipId ? { chipId } : {},
    }
  )

  return res.data.data
}