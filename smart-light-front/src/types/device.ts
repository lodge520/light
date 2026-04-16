export interface DeviceItem {
  id: number
  chipId: string
  ip?: string
  brightness?: number
  temp?: number
  autoMode?: boolean
  recommendedBrightness?: number
  recommendedTemp?: number
  fabric?: string
  mainColorRgb?: string
  createTime?: string
  updateTime?: string
  online?: boolean
  lastSeen?: number
}

export interface DeviceOnlineItem {
  chipId: string
  ip?: string
  online: boolean
  lastSeen?: number
}

export interface DeviceCreatePayload {
  chipId: string
  ip?: string
  brightness?: number
  temp?: number
  autoMode?: boolean
  recommendedBrightness?: number
  recommendedTemp?: number
  fabric?: string
  mainColorRgb?: string
}

export type DashboardTab = 'main' | 'flow' | 'settings'