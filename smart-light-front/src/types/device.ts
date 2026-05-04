export interface DeviceItem {
  id: number
  chipId: string
  displayName?: string
  deviceType?: string
  deviceNo?: string
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
  firmwareVersion?: string
  firmwareVersionCode?: number
  firmwareChannel?: FirmwareChannel
  otaStatus?: OtaStatus
}


export interface DeviceOnlineItem {
  chipId: string
  ip?: string
  online: boolean
  lastSeen?: number
}

export interface DeviceCreatePayload {
  chipId: string
  ip: string
  displayName?: string
  deviceType?: string
  deviceNo?: string
  brightness?: number
  temp?: number
  autoMode?: boolean
  recommendedBrightness: number
  recommendedTemp: number
  fabric?: string
  mainColorRgb?: string
}

export type FirmwareChannel = 'stable' | 'test'
export type FirmwareDeviceType = 'lamp' | 'cam' | 'camlamp'

export type OtaStatus = 'idle' | 'updating' | 'success' | 'failed'

export interface OtaCheckResult {
  chipId: string
  deviceType?: string
  channel: FirmwareChannel
  currentVersion?: string
  currentVersionCode?: number
  firmwareId?: number
  latestVersion?: string
  latestVersionCode?: number
  fileUrl?: string
  md5?: string
  changelog?: string
  hasUpdate: boolean
  otaStatus?: OtaStatus
}

export interface FirmwareUploadResult {
  id: number
  deviceType: FirmwareDeviceType
  channel: FirmwareChannel
  version: string
  versionCode: number
  fileUrl: string
  md5?: string
  changelog?: string
  enabled: boolean
  createTime?: string
  updateTime?: string
}

export type FirmwareItem = FirmwareUploadResult

export interface FirmwareHistoryParams {
  deviceType?: FirmwareDeviceType | ''
  channel?: FirmwareChannel | ''
}

export type DashboardTab = 'main' | 'flow' | 'settings' | 'firmware'
