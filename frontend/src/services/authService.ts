import { api, setAuthToken } from './api'

export interface LoginPayload {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  tokenType: string
  username: string
}

export const authService = {
  async login(payload: LoginPayload, remember: boolean = true) {
    // Le user-service expose également POST /api/auth/login via le gateway.
    const { data } = await api.post<LoginResponse>('/api/auth/login', payload)
    if (data.token) setAuthToken(data.token, remember)
    return data
  },
  logout() {
    setAuthToken('')
  },
}
