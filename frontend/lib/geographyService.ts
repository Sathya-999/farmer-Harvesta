/**
 * geographyService.ts — Master Administrative Geography & Location Service
 *
 * Interacts with Spring Boot GeographyController:
 * - Hierarchical exploration: State -> District -> Mandal -> Village
 * - GPS-assisted location candidate resolution with advisory confirmation
 * - On-demand state sync requests & status polling
 */

import { api } from './apiClient'

export interface StateDto {
  id: string
  code: string
  name: string
  active?: boolean
}

export interface DistrictDto {
  id: string
  stateId: string
  name: string
  code?: string
  active?: boolean
}

export interface MandalDto {
  id: string
  districtId: string
  name: string
  code?: string
  active?: boolean
}

export interface VillageDto {
  id: string
  mandalId: string
  name: string
  code?: string
  active?: boolean
}

export interface LocationResolutionDto {
  latitude: number
  longitude: number
  candidateStateCode: string
  candidateStateName: string
  candidateDistrictCode?: string | null
  candidateDistrictName?: string | null
  candidateMandalCode?: string | null
  candidateMandalName?: string | null
  candidateVillageCode?: string | null
  candidateVillageName?: string | null
  confidence: string
  manualConfirmationRequired: boolean
  isExactBoundary: boolean
  advisoryMessage: string
}

export interface StateCoverageDto {
  stateCode: string
  stateName: string
  dbDistricts: number
  expectedDistricts: number
  dbMandals: number
  expectedMandals: number
  dbVillages: number
  expectedVillages: number
  coverageStatus: string
  syncRequired: boolean
}

export interface StateSyncStatusDto {
  stateCode: string
  stateName: string
  status: 'IDLE' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED'
  processedRows: number
  totalExpectedRows: number
  startedAt?: string | null
  completedAt?: string | null
  message: string
}

export const geographyService = {
  /**
   * Retrieves all active states and union territories in India.
   */
  async getStates(): Promise<StateDto[]> {
    return await api.get<StateDto[]>('/api/v1/geography/states')
  },

  /**
   * Retrieves active districts for a given state.
   */
  async getDistricts(stateId: string): Promise<DistrictDto[]> {
    return await api.get<DistrictDto[]>(`/api/v1/geography/states/${stateId}/districts`)
  },

  /**
   * Retrieves active mandals/taluks for a given district.
   */
  async getMandals(districtId: string): Promise<MandalDto[]> {
    return await api.get<MandalDto[]>(`/api/v1/geography/districts/${districtId}/mandals`)
  },

  /**
   * Retrieves active villages for a given mandal.
   */
  async getVillages(mandalId: string): Promise<VillageDto[]> {
    return await api.get<VillageDto[]>(`/api/v1/geography/mandals/${mandalId}/villages`)
  },

  /**
   * Resolves GPS latitude & longitude to candidate administrative boundaries.
   * Advisory only — requires manual farmer verification.
   */
  async resolveLocation(lat: number, lon: number): Promise<LocationResolutionDto> {
    return await api.get<LocationResolutionDto>(`/api/v1/geography/resolve-location?lat=${lat}&lon=${lon}`)
  },

  /**
   * Checks current administrative coverage for an approved pilot state.
   */
  async checkCoverage(stateCode: string): Promise<StateCoverageDto> {
    return await api.get<StateCoverageDto>(`/api/v1/geography/states/${stateCode}/coverage`)
  },

  /**
   * Requests on-demand background sync for a pilot state.
   */
  async requestStateSync(stateCode: string): Promise<StateSyncStatusDto> {
    return await api.post<StateSyncStatusDto>(`/api/v1/geography/states/${stateCode}/sync`)
  },

  /**
   * Polls the status of an ongoing state synchronization job.
   */
  async getSyncStatus(stateCode: string): Promise<StateSyncStatusDto> {
    return await api.get<StateSyncStatusDto>(`/api/v1/geography/states/${stateCode}/sync-status`)
  },
}
