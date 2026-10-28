/**
 * cultivationService.ts — Cultivation Cycle Management Service
 *
 * Interacts with Spring Boot CultivationController & ReferenceDataController:
 * - GET    /api/v1/cultivations -> Paginated cultivation intentions
 * - GET    /api/v1/cultivations/{id} -> Single cultivation detail
 * - POST   /api/v1/cultivations -> Create intention on plot
 * - PUT    /api/v1/cultivations/{id} -> Update planned cultivation
 * - PATCH  /api/v1/cultivations/{id}/status -> Execute lifecycle status transition
 * - GET    /api/v1/cultivations/{id}/events -> Status change audit events
 * - GET    /api/v1/crops, /api/v1/seasons -> Reference data
 */

import { api } from './apiClient'
import { PageResponse } from './plotService'

export type CultivationStatus =
  | 'PLANNED'
  | 'STARTED'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'ABANDONED'

export interface CultivationResponseDto {
  id: string
  farmerId: string
  plotId: string
  plotLabel?: string
  cropId: string
  cropName?: string
  cropScientificName?: string
  varietyId?: string
  varietyName?: string
  seasonId: string
  seasonName?: string
  cultivationYear: number
  cultivatedAreaAcres: number
  plannedPlantingDate?: string
  actualPlantingDate?: string
  expectedHarvestDate?: string
  actualHarvestDate?: string
  status: CultivationStatus
  notes?: string
  version: number
  createdAt: string
  updatedAt: string
}

export interface CreateCultivationRequestDto {
  plotId: string
  cropId: string
  cropVarietyId?: string
  varietyId?: string
  seasonId: string
  cultivationYear: number
  cultivatedAreaAcres: number
  plannedPlantingDate: string
  expectedHarvestDate?: string
  notes?: string
}

export interface UpdateCultivationRequestDto {
  cultivatedAreaAcres?: number
  plannedPlantingDate?: string
  expectedHarvestDate?: string
  notes?: string
  expectedVersion?: number
}

export interface CultivationStatusUpdateRequestDto {
  targetStatus: CultivationStatus
  actualDate?: string
  reason?: string
}

export interface CultivationStatusEventDto {
  id: string
  cultivationId: string
  fromStatus: CultivationStatus
  toStatus: CultivationStatus
  changedAt: string
  changedBy: string
  notes?: string
}

export interface CropDto {
  id: string
  name: string
  scientificName?: string
  category?: string
  active?: boolean
}

export interface CropVarietyDto {
  id: string
  cropId: string
  name: string
  durationDays?: number
  active?: boolean
}

export interface SeasonDto {
  id: string
  name: string
  code: string
  startMonth?: number
  endMonth?: number
  active?: boolean
}

export const cultivationService = {
  /**
   * Lists cultivations for authenticated farmer with optional filters.
   */
  async listCultivations(params: {
    page?: number
    size?: number
    plotId?: string
    seasonId?: string
    cultivationYear?: number
    cropId?: string
    status?: string
    sortBy?: string
    direction?: string
  } = {}): Promise<PageResponse<CultivationResponseDto>> {
    const q = new URLSearchParams()
    if (params.page !== undefined) q.set('page', String(params.page))
    if (params.size !== undefined) q.set('size', String(params.size))
    if (params.plotId) q.set('plotId', params.plotId)
    if (params.seasonId) q.set('seasonId', params.seasonId)
    if (params.cultivationYear) q.set('cultivationYear', String(params.cultivationYear))
    if (params.cropId) q.set('cropId', params.cropId)
    if (params.status) q.set('status', params.status)
    if (params.sortBy) q.set('sortBy', params.sortBy)
    if (params.direction) q.set('direction', params.direction)

    return await api.get<PageResponse<CultivationResponseDto>>(`/api/v1/cultivations?${q.toString()}`)
  },

  /**
   * Retrieves single cultivation intention by UUID.
   */
  async getCultivation(id: string): Promise<CultivationResponseDto> {
    return await api.get<CultivationResponseDto>(`/api/v1/cultivations/${id}`)
  },

  /**
   * Creates a new cultivation cycle attached to a plot.
   */
  async createCultivation(
    payload: CreateCultivationRequestDto,
    idempotencyKey?: string
  ): Promise<CultivationResponseDto> {
    const key = idempotencyKey || (typeof crypto !== 'undefined' ? crypto.randomUUID() : undefined)
    const body = {
      plotId: payload.plotId,
      cropId: payload.cropId,
      cropVarietyId: payload.cropVarietyId || payload.varietyId || undefined,
      seasonId: payload.seasonId,
      cultivationYear: payload.cultivationYear,
      cultivatedAreaAcres: payload.cultivatedAreaAcres,
      plannedPlantingDate: payload.plannedPlantingDate || new Date().toISOString().split('T')[0],
    }
    return await api.post<CultivationResponseDto>('/api/v1/cultivations', body, key)
  },

  /**
   * Updates mutable fields of a PLANNED cultivation cycle.
   */
  async updateCultivation(
    id: string,
    payload: UpdateCultivationRequestDto
  ): Promise<CultivationResponseDto> {
    return await api.put<CultivationResponseDto>(`/api/v1/cultivations/${id}`, payload)
  },

  /**
   * Transitions lifecycle status: PLANNED -> STARTED -> COMPLETED / CANCELLED / ABANDONED.
   */
  async updateStatus(
    id: string,
    payload: CultivationStatusUpdateRequestDto
  ): Promise<CultivationResponseDto> {
    return await api.patch<CultivationResponseDto>(`/api/v1/cultivations/${id}/status`, payload)
  },

  /**
   * Retrieves audit trail of status change events.
   */
  async getEvents(id: string): Promise<CultivationStatusEventDto[]> {
    return await api.get<CultivationStatusEventDto[]>(`/api/v1/cultivations/${id}/events`)
  },

  // Reference Data endpoints
  async getCrops(): Promise<CropDto[]> {
    return await api.get<CropDto[]>('/api/v1/crops')
  },

  async getVarieties(cropId: string): Promise<CropVarietyDto[]> {
    return await api.get<CropVarietyDto[]>(`/api/v1/crops/${cropId}/varieties`)
  },

  async getSeasons(): Promise<SeasonDto[]> {
    return await api.get<SeasonDto[]>('/api/v1/seasons')
  },
}
