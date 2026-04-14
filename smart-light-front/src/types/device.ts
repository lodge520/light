export interface DeviceItem {
  id: number
  deviceCode: string
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
  deviceCode: string
  ip?: string
  online: boolean
  lastSeen?: number
}

export interface DeviceCreatePayload {
  deviceCode: string
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