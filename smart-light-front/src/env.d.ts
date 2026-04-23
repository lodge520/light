/// <reference types="vite/client" />

export {}

declare global {
  interface Window {
    AndroidSmartConfig?: {
      getWifiInfo: (callback: (res: any) => void) => void
      smartConfig: (payload: any, callback: (res: any) => void) => void
      cancel?: () => void
      stop?: () => void
    }
  }
}