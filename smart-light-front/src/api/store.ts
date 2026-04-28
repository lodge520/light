import http from './http'

interface CommonResult<T> {
  code: number
  msg: string
  data: T
}

export interface StoreItem {
  id: number
  userId: number
  storeName: string
  storeStyle: string
  area: number
  province: string
  city: string
}

export async function getCurrentStoreApi(): Promise<StoreItem> {
  const res = await http.get<CommonResult<StoreItem>>('/api/store/current')
  return res.data.data
}

export interface StoreSavePayload {
  storeName: string
  storeStyle: string
  area: number
  province: string
  city: string
}

export async function setupCurrentStoreApi(payload: StoreSavePayload): Promise<StoreItem> {
  const res = await http.post<CommonResult<StoreItem>>('/api/store/setup', payload)
  return res.data.data
}