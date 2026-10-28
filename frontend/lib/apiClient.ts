/**
 * apiClient.ts — Core HTTP Client for Agricultural Intelligence Platform
 *
 * Handles:
 * - Next.js rewrites to backend /api/* (Spring Boot OAuth2 Resource Server)
 * - Bearer JWT authorization token injection from auth store
 * - Unified ApiResponse<T> envelope handling
 * - Structured error handling and graceful fallbacks
 */

import { getToken, logout } from './authService'

export interface ApiResponse<T> {
  success: boolean
  data: T | null
  error: string | null
  timestamp?: string
}

export interface ApiError {
  status: number
  message: string
  code?: string
  fieldErrors?: Record<string, string>
}

// In Next.js client, requests to /api/* are proxied via next.config.ts rewrites
const BASE_URL = ''

export async function request<T>(
  path: string,
  options: RequestInit & { idempotencyKey?: string } = {}
): Promise<T> {
  const token = typeof window !== 'undefined' ? getToken() : null
  const headers = new Headers(options.headers || {})

  headers.set('Accept', 'application/json')
  if (!headers.has('Content-Type') && options.body && typeof options.body === 'string') {
    headers.set('Content-Type', 'application/json')
  }

  // Guarded development session handler (strictly disabled in production)
  if (
    process.env.NODE_ENV !== 'production' &&
    token &&
    token.startsWith('simulated-')
  ) {
    const sim = getSimulatedDevResponse<T>(path, options)
    if (sim !== undefined) {
      return sim
    }
  }

  // Omit simulated dev tokens from actual HTTP headers to avoid Spring Security 401 Malformed Token on public endpoints
  if (token && !token.startsWith('simulated-')) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  if (options.idempotencyKey) {
    headers.set('Idempotency-Key', options.idempotencyKey)
  }

  const endpoint = path.startsWith('/') ? path : `/${path}`
  const url = `${BASE_URL}${endpoint}`

  let response: Response
  try {
    response = await fetch(url, {
      ...options,
      headers,
    })
  } catch (err) {
    const error: ApiError = {
      status: 0,
      message: 'Unable to connect to the agricultural intelligence server. Please check your network connection.',
      code: 'NETWORK_ERROR',
    }
    throw error
  }

  // Handle 204 No Content
  if (response.status === 204) {
    return null as T
  }

  // Parse JSON response safely
  let json: any = null
  try {
    json = await response.json()
  } catch {
    json = null
  }

  if (!response.ok) {
    if (response.status === 401) {
      if (typeof window !== 'undefined') {
        // Clear expired session and prompt re-login only for real tokens
        if (!token?.startsWith('simulated-')) {
          logout()
        }
      }
    }

    const message =
      json?.error ||
      json?.message ||
      `Server request failed with status ${response.status}`

    const apiError: ApiError = {
      status: response.status,
      message,
      code: json?.code,
      fieldErrors: json?.fieldErrors,
    }
    throw apiError
  }

  // Unwrap unified ApiResponse<T> envelope if present: { success: true, data: T }
  if (json && typeof json === 'object' && 'success' in json && 'data' in json) {
    return json.data as T
  }

  return json as T
}

export const api = {
  get: <T>(path: string, headers?: HeadersInit) =>
    request<T>(path, { method: 'GET', headers }),

  post: <T>(path: string, body?: unknown, idempotencyKey?: string) =>
    request<T>(path, {
      method: 'POST',
      body: body ? JSON.stringify(body) : undefined,
      idempotencyKey,
    }),

  put: <T>(path: string, body?: unknown) =>
    request<T>(path, {
      method: 'PUT',
      body: body ? JSON.stringify(body) : undefined,
    }),

  patch: <T>(path: string, body?: unknown) =>
    request<T>(path, {
      method: 'PATCH',
      body: body ? JSON.stringify(body) : undefined,
    }),

  delete: <T>(path: string) =>
    request<T>(path, { method: 'DELETE' }),
}

/**
 * Provides safe in-memory fixtures during local development simulation.
 * STRICT SECURITY INVARIANT: Only invoked when process.env.NODE_ENV !== 'production'
 * and the active token starts with 'simulated-'.
 */
function getSimulatedDevResponse<T>(path: string, options: RequestInit): T | undefined {
  const norm = path.toLowerCase().split('?')[0]

  if (norm === '/api/v1/farmers/me') {
    const onboarded = typeof window !== 'undefined' ? localStorage.getItem('sf_farmer_onboarded') !== 'false' : true
    return {
      id: 'dev-simulated-farmer',
      firstName: 'Ramesh',
      lastName: 'Patel',
      email: 'farmer@agriplatform.in',
      phone: '+91 98765 43210',
      onboardingCompleted: onboarded,
      stateName: 'Andhra Pradesh',
      districtName: 'Guntur',
      mandalName: 'Mangalagiri',
      villageName: 'Nidamarru',
    } as T
  }

  if (norm === '/api/v1/plots') {
    if (options.method === 'POST') {
      return {
        id: 'plot-dev-created',
        name: 'New Registered Plot',
        totalAreaAcres: 3.5,
        status: 'ACTIVE',
      } as T
    }
    return {
      content: [
        {
          id: 'plot-1',
          name: 'Krishna North Field',
          totalAreaAcres: 4.5,
          surveyNumber: 'SY-104/A',
          soilType: 'BLACK_COTTON',
          irrigationType: 'DRIP',
          status: 'ACTIVE',
          villageName: 'Nidamarru',
          districtName: 'Guntur',
          stateName: 'Andhra Pradesh',
        },
      ],
      totalElements: 1,
      totalPages: 1,
      pageNumber: 0,
    } as T
  }

  if (norm === '/api/v1/cultivations') {
    if (options.method === 'POST') {
      return {
        id: 'cult-dev-created',
        plotId: 'plot-1',
        cropName: 'Paddy / Rice',
        status: 'PLANNED',
      } as T
    }
    return {
      content: [
        {
          id: 'cult-1',
          plotId: 'plot-1',
          cropName: 'Paddy / Rice',
          varietyName: 'BPT 5204 (Samba Mahsuri)',
          seasonName: 'Kharif 2026',
          totalAreaAcres: 4.5,
          status: 'STARTED',
          startDate: '2026-06-15',
          targetHarvestDate: '2026-11-20',
        },
      ],
      totalElements: 1,
      totalPages: 1,
      pageNumber: 0,
    } as T
  }

  if (norm === '/api/v1/crops') {
    return [
      { id: 'crop-1', name: 'Paddy / Rice', category: 'CEREALS' },
      { id: 'crop-2', name: 'Cotton', category: 'FIBRE' },
      { id: 'crop-3', name: 'Chilli', category: 'SPICES' },
    ] as T
  }

  if (norm.startsWith('/api/v1/crops/') && norm.endsWith('/varieties')) {
    return [
      { id: 'var-1', name: 'BPT 5204 (Samba Mahsuri)', maturityDays: 145 },
      { id: 'var-2', name: 'MTU 1010 (Cottondora Sannalu)', maturityDays: 120 },
    ] as T
  }

  if (norm === '/api/v1/seasons') {
    return [
      { id: 'season-1', name: 'Kharif 2026', startDate: '2026-06-01', endDate: '2026-11-30' },
      { id: 'season-2', name: 'Rabi 2026-27', startDate: '2026-10-15', endDate: '2027-03-31' },
    ] as T
  }

  if (norm === '/api/v1/analytics/farmer/summary') {
    return {
      farmerId: 'dev-simulated-farmer',
      registeredPlotsCount: 1,
      totalLandAreaAcres: 4.5,
      activeCultivationsCount: 1,
      currentlyCultivatedAcres: 4.5,
      utilizationPercentage: 100.0,
      primaryCrop: 'Paddy / Rice',
    } as T
  }

  if (norm === '/api/v1/analytics/geography/summary') {
    return {
      level: 'MANDAL',
      id: 'mandal-1',
      name: 'Mangalagiri',
      totalPlots: 124,
      totalRegisteredAcreage: 450.5,
      activeCultivations: 98,
      activeCultivatedAcreage: 380.2,
      seasonName: 'Kharif 2026',
      cultivationYear: 2026,
    } as T
  }

  if (norm === '/api/v1/analytics/geography/crops') {
    return {
      items: [
        { cropId: 'crop-1', cropName: 'Paddy / Rice', cultivatedAreaAcres: 240.5, percentageOfTotal: 63.2, cultivationCount: 52 },
        { cropId: 'crop-2', cropName: 'Cotton', cultivatedAreaAcres: 139.7, percentageOfTotal: 36.8, cultivationCount: 46 },
      ],
      totalElements: 2,
      totalPages: 1,
      page: 0,
      size: 10,
    } as T
  }

  if (norm === '/api/v1/analytics/geography/signals') {
    return {
      signals: [
        {
          type: 'OPPORTUNITY',
          title: 'High-Yield Paddy Cycle Detected',
          description: 'Favorable rainfall patterns in Guntur district support optimal harvest timelines.',
          severity: 'OPPORTUNITY',
          confidenceScore: 0.92,
        },
      ],
    } as T
  }

  if (norm === '/api/v1/analytics/geography/trends') {
    return [] as T
  }

  if (norm === '/api/v1/notifications/unread/count' || norm === '/api/v1/notifications/unread-count') {
    return { unreadCount: 0 } as T
  }

  if (norm === '/api/v1/notifications' || norm === '/api/v1/notifications/unread') {
    return {
      content: [
        {
          id: 'notif-1',
          farmerId: 'dev-simulated-farmer',
          title: 'Monsoon Sowing Advisory',
          message: 'Optimal soil moisture reached in Guntur region for Kharif paddy nursery preparation.',
          category: 'CULTIVATION',
          severity: 'INFO',
          read: true,
          createdAt: new Date().toISOString(),
        },
      ],
      totalElements: 1,
      totalPages: 1,
      size: 20,
      number: 0,
      first: true,
      last: true,
      empty: false,
    } as T
  }

  if (norm.startsWith('/api/v1/history/')) {
    if (norm.endsWith('/snapshots')) {
      return {
        content: [
          {
            id: 'snap-1',
            cultivationId: 'cult-1',
            version: 1,
            cultivatedAreaAcres: 4.5,
            status: 'STARTED',
            plannedPlantingDate: '2026-06-15',
            notes: 'Initial planting verification',
            recordedAt: new Date().toISOString(),
            recordedBy: 'system',
          },
        ],
        totalElements: 1,
        totalPages: 1,
        size: 20,
        number: 0,
        first: true,
        last: true,
        empty: false,
      } as T
    }
    return [] as T
  }

  return undefined
}
