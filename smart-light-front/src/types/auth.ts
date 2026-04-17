export interface LoginReq {
  username: string
  password: string
}

export interface RegisterReq {
  username: string
  password: string
  confirmPassword: string
  storeName: string
  storeStyle: string
  area: number
  province: string
  city: string
}

export interface LoginResp {
  token: string
  userId: number
  username: string
  storeId: number
  storeName: string
  storeStyle: string
  province: string
  city: string
}