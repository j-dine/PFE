import { api } from './api'

export interface NotificationApi {
  id: number
  destinataire: string
  sujet: string
  message?: string
  type?: string
  statut?: string
  dossierId?: number
  dateEnvoi?: string
}

export const notificationService = {
  async listByDestinataire(destinataire: string) {
    const { data } = await api.get<NotificationApi[]>(
      `/api/notifications/destinataire/${encodeURIComponent(destinataire)}`
    )
    return data
  },

  async markAsRead(id: number | string) {
    const { data } = await api.patch<NotificationApi>(`/api/notifications/${id}/lu`)
    return data
  },
}
