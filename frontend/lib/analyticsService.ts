/**
 * analyticsService.ts — Regional and Farmer Analytics Service (Task 10)
 *
 * Interacts with Spring Boot AnalyticsController:
 * - GET /api/v1/analytics/geography/summary -> Regional metrics & aggregate land facts
 * - GET /api/v1/analytics/geography/crops -> Paginated crop distribution within region
 * - GET /api/v1/analytics/geography/trends -> Multi-year temporal trends
 * - GET /api/v1/analytics/geography/signals -> Decision support signals & insights
 * - GET /api/v1/analytics/farmer/summary -> Private, IDOR-protected farmer summary
 */

import { api } from './apiClient'

export type GeographicLevel = 'VILLAGE' | 'MANDAL' | 'DISTRICT' | 'STATE'

export interface GeographicSummaryDto {
  level: GeographicLevel
  id: string
  name: string
  totalPlots: number
  totalRegisteredAcreage: number
  activeCultivations: number
  activeCultivatedAcreage: number
  seasonName?: string
  cultivationYear?: number
}

export interface CropDistributionItemDto {
  cropId: string
  cropName: string
  cultivatedAreaAcres: number
  percentageOfTotal: number
  cultivationCount: number
}

export interface CropDistributionPageResponseDto {
  items: CropDistributionItemDto[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export interface TemporalTrendDto {
  year: number
  seasonName: string
  cropName: string
  totalCultivatedAcres: number
  cultivationCount: number
}

export interface DecisionSignalDto {
  type: string
  title: string
  description: string
  severity: 'INFO' | 'OPPORTUNITY' | 'WARNING'
  confidenceScore: number
}

export interface SignalsAndInsightsResponseDto {
  signals: DecisionSignalDto[]
  summaryHeadline: string
}

export interface FarmerAnalyticsSummaryDto {
  farmerId: string
  registeredPlotsCount: number
  totalLandAreaAcres: number
  activeCultivationsCount: number
  currentlyCultivatedAcres: number
  utilizationPercentage: number
  primaryCrop?: string
}

export const analyticsService = {
  /**
   * Retrieves high-level aggregate summary metrics for a geographic boundary.
   */
  async getGeographicSummary(
    level: GeographicLevel,
    id: string,
    seasonId?: string,
    cultivationYear?: number
  ): Promise<GeographicSummaryDto> {
    const q = new URLSearchParams({ level, id })
    if (seasonId) q.set('seasonId', seasonId)
    if (cultivationYear) q.set('cultivationYear', String(cultivationYear))
    return await api.get<GeographicSummaryDto>(`/api/v1/analytics/geography/summary?${q.toString()}`)
  },

  /**
   * Retrieves paginated crop distribution within a geographic boundary.
   */
  async getCropDistribution(
    level: GeographicLevel,
    id: string,
    page = 0,
    size = 20,
    seasonId?: string,
    cultivationYear?: number
  ): Promise<CropDistributionPageResponseDto> {
    const q = new URLSearchParams({ level, id, page: String(page), size: String(size) })
    if (seasonId) q.set('seasonId', seasonId)
    if (cultivationYear) q.set('cultivationYear', String(cultivationYear))
    return await api.get<CropDistributionPageResponseDto>(`/api/v1/analytics/geography/crops?${q.toString()}`)
  },

  /**
   * Retrieves chronological temporal trends across years and seasons.
   */
  async getTemporalTrends(
    level: GeographicLevel,
    id: string,
    cropId?: string,
    startYear?: number,
    endYear?: number
  ): Promise<TemporalTrendDto[]> {
    const q = new URLSearchParams({ level, id })
    if (cropId) q.set('cropId', cropId)
    if (startYear) q.set('startYear', String(startYear))
    if (endYear) q.set('endYear', String(endYear))
    return await api.get<TemporalTrendDto[]>(`/api/v1/analytics/geography/trends?${q.toString()}`)
  },

  /**
   * Retrieves decision-support signals and insights.
   */
  async getSignalsAndInsights(
    level: GeographicLevel,
    id: string,
    seasonId?: string,
    cultivationYear?: number
  ): Promise<SignalsAndInsightsResponseDto> {
    const q = new URLSearchParams({ level, id })
    if (seasonId) q.set('seasonId', seasonId)
    if (cultivationYear) q.set('cultivationYear', String(cultivationYear))
    return await api.get<SignalsAndInsightsResponseDto>(`/api/v1/analytics/geography/signals?${q.toString()}`)
  },

  /**
   * Retrieves private, IDOR-protected cultivation and land capacity summary for authenticated farmer.
   */
  async getFarmerSummary(seasonId?: string, cultivationYear?: number): Promise<FarmerAnalyticsSummaryDto> {
    const q = new URLSearchParams()
    if (seasonId) q.set('seasonId', seasonId)
    if (cultivationYear) q.set('cultivationYear', String(cultivationYear))
    const query = q.toString() ? `?${q.toString()}` : ''
    return await api.get<FarmerAnalyticsSummaryDto>(`/api/v1/analytics/farmer/summary${query}`)
  },
}
