/**
 * historyService.ts — Cultivation History Service (Task 09)
 *
 * Interacts with Spring Boot HistoryController:
 * - GET /api/v1/history/cultivations/{cultivationId}/snapshots -> Immutable version snapshots
 * - GET /api/v1/history/cultivations/{cultivationId}/snapshots/{historyId} -> Single snapshot
 * - GET /api/v1/history/cultivations/{cultivationId}/events -> Status change events
 */

import { api } from './apiClient'
import { PageResponse } from './plotService'

export interface HistorySnapshotDto {
  id: string
  cultivationId: string
  version: number
  cultivatedAreaAcres: number
  status: string
  plannedPlantingDate?: string
  actualPlantingDate?: string
  expectedHarvestDate?: string
  actualHarvestDate?: string
  notes?: string
  recordedAt: string
  recordedBy: string
}

export interface HistoryStatusEventDto {
  id: string
  cultivationId: string
  fromStatus: string
  toStatus: string
  changedAt: string
  changedBy: string
  notes?: string
}

export const historyService = {
  /**
   * Retrieves paginated field-change history snapshots for a cultivation intention.
   * Ordered version DESC.
   */
  async getSnapshots(
    cultivationId: string,
    page = 0,
    size = 20
  ): Promise<PageResponse<HistorySnapshotDto>> {
    return await api.get<PageResponse<HistorySnapshotDto>>(
      `/api/v1/history/cultivations/${cultivationId}/snapshots?page=${page}&size=${size}`
    )
  },

  /**
   * Retrieves single immutable snapshot by UUID.
   */
  async getSnapshot(cultivationId: string, historyId: string): Promise<HistorySnapshotDto> {
    return await api.get<HistorySnapshotDto>(
      `/api/v1/history/cultivations/${cultivationId}/snapshots/${historyId}`
    )
  },

  /**
   * Retrieves paginated status-change events for a cultivation.
   */
  async getEvents(cultivationId: string): Promise<HistoryStatusEventDto[]> {
    return await api.get<HistoryStatusEventDto[]>(
      `/api/v1/history/cultivations/${cultivationId}/events`
    )
  },
}
