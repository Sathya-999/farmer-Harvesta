/**
 * farmerService.ts — Farmer Profile and Onboarding Service
 *
 * Interacts with Spring Boot FarmerProfileController:
 * - GET  /api/v1/farmers/me -> Retrieve authenticated farmer profile
 * - POST /api/v1/farmers/me -> Initialize profile & complete onboarding
 * - PUT  /api/v1/farmers/me -> Update profile with optimistic locking
 */

import { api } from './apiClient'

export interface FarmerProfileDto {
  id: string
  fullName: string
  homeVillageId: string
  villageName?: string
  homeVillageName?: string
  mandalId?: string
  mandalName?: string
  districtId?: string
  districtName?: string
  stateId?: string
  stateName?: string
  onboardingCompleted: boolean
  profileStatus: 'ACTIVE' | 'SUSPENDED' | 'DELETED'
  version: number
  createdAt?: string
  updatedAt?: string
}

export interface CreateFarmerProfileRequestDto {
  fullName: string
  homeVillageId: string
  mandalId?: string
  districtId?: string
  stateId?: string
}

export interface UpdateFarmerProfileRequestDto {
  fullName: string
  homeVillageId: string
  expectedVersion: number
}

export const farmerService = {
  /**
   * Retrieves profile for the authenticated farmer derived strictly from JWT sub.
   */
  async getProfile(): Promise<FarmerProfileDto | null> {
    try {
      return await api.get<FarmerProfileDto>('/api/v1/farmers/me')
    } catch (err: any) {
      if (err.status === 404) {
        return null // Profile not yet initialized (unonboarded farmer)
      }
      throw err
    }
  },

  /**
   * Initializes profile for newly registered farmer with home village.
   */
  async initProfile(payload: CreateFarmerProfileRequestDto): Promise<FarmerProfileDto> {
    return await api.post<FarmerProfileDto>('/api/v1/farmers/me', payload)
  },

  /**
   * Updates profile fields enforcing optimistic locking via expectedVersion.
   */
  async updateProfile(payload: UpdateFarmerProfileRequestDto): Promise<FarmerProfileDto> {
    return await api.put<FarmerProfileDto>('/api/v1/farmers/me', payload)
  },
}
