/**
 * plotService.ts — Plot Management Service
 *
 * Interacts with Spring Boot PlotController:
 * - GET    /api/v1/plots -> Paginated plots for authenticated farmer
 * - GET    /api/v1/plots/{id} -> Single plot detail
 * - POST   /api/v1/plots -> Create plot with Idempotency-Key
 * - PUT    /api/v1/plots/{id} -> Update plot with optimistic concurrency
 * - DELETE /api/v1/plots/{id} -> Soft-deactivation (is_active = false)
 * - PATCH  /api/v1/plots/{id}/reactivate -> Reactivate plot
 */

import { api } from './apiClient'

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}

export interface PlotResponseDto {
  id: string
  farmerId: string
  villageId: string
  villageName?: string
  mandalName?: string
  districtName?: string
  stateName?: string
  label?: string
  totalAreaAcres: number
  isActive: boolean
  version: string
  createdAt: string
  updatedAt: string
}

export interface CreatePlotRequestDto {
  villageId: string
  label?: string
  totalAreaAcres: number
}

export interface UpdatePlotRequestDto {
  label?: string
  totalAreaAcres: number
  expectedVersion: string
}

export const plotService = {
  /**
   * Retrieves paginated list of plots for the authenticated farmer.
   */
  async listPlots(
    page = 0,
    size = 20,
    status = 'ACTIVE',
    sortBy = 'createdAt',
    direction = 'DESC'
  ): Promise<PageResponse<PlotResponseDto>> {
    return await api.get<PageResponse<PlotResponseDto>>(
      `/api/v1/plots?page=${page}&size=${size}&status=${status}&sortBy=${sortBy}&direction=${direction}`
    )
  },

  /**
   * Retrieves a single plot by UUID.
   */
  async getPlot(id: string): Promise<PlotResponseDto> {
    return await api.get<PlotResponseDto>(`/api/v1/plots/${id}`)
  },

  /**
   * Creates a new plot with at-most-once idempotency support.
   */
  async createPlot(payload: CreatePlotRequestDto, idempotencyKey?: string): Promise<PlotResponseDto> {
    const key = idempotencyKey || (typeof crypto !== 'undefined' ? crypto.randomUUID() : undefined)
    return await api.post<PlotResponseDto>('/api/v1/plots', payload, key)
  },

  /**
   * Updates mutable plot fields (label, area) with optimistic concurrency check.
   */
  async updatePlot(id: string, payload: UpdatePlotRequestDto): Promise<PlotResponseDto> {
    return await api.put<PlotResponseDto>(`/api/v1/plots/${id}`, payload)
  },

  /**
   * Soft-deactivates a plot. Fails with 409 if active cultivation exists.
   */
  async deactivatePlot(id: string): Promise<void> {
    await api.delete(`/api/v1/plots/${id}`)
  },

  /**
   * Reactivates an inactive plot.
   */
  async reactivatePlot(id: string): Promise<PlotResponseDto> {
    return await api.patch<PlotResponseDto>(`/api/v1/plots/${id}/reactivate`)
  },
}
