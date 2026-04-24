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

const data = await setupCurrentStoreApi({
  storeName: form.storeName,
  area: Number(form.area),
  storeStyle: form.storeStyle,
  province: regionValue.provinceLabel,
  city: regionValue.cityLabel,
})