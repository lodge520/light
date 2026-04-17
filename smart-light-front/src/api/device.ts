import http from './http'
import type { DeviceCreatePayload, DeviceItem, DeviceOnlineItem } from '../types/device'

interface CommonResult<T> {
  code: number
  msg: string
  data: T
}

export async function getDeviceList(): Promise<DeviceItem[]> {
  const res = await http.get<CommonResult<DeviceItem[]>>('/admin/device/list')
  return res.data.data || []
}

export async function getOnlineList(): Promise<DeviceOnlineItem[]> {
  const res = await http.get<CommonResult<DeviceOnlineItem[]>>('/admin/device/online-list')
  return res.data.data || []
}

export async function getMyDeviceListApi(): Promise<DeviceItem[]> {
  const res = await http.get<CommonResult<DeviceItem[]>>('/admin/device/my-list')
  return res.data.data || []
}

export async function createDevice(payload: DeviceCreatePayload): Promise<number> {
  const res = await http.post<CommonResult<number>>('/admin/device/create', payload)
  return res.data.data
}

export async function updateDevice(id: number, payload: DeviceCreatePayload): Promise<boolean> {
  const res = await http.put<CommonResult<boolean>>(`/admin/device/update/${id}`, payload)
  return res.data.data
}

export async function deleteDevice(id: number): Promise<boolean> {
  const res = await http.delete<CommonResult<boolean>>(`/admin/device/delete/${id}`)
  return res.data.data
}

export async function armControl(chipId: string, direction: string): Promise<boolean> {
  const res = await http.post<CommonResult<boolean>>(`/admin/device/arm/${chipId}`, {
    direction,
  })
  return res.data.data
}