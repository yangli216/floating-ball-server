import http from './http'

const BASE_URL = '/admin/api/recommendation-preferences'

export function fetchRecommendationPreferenceSummary(params = {}) {
  return http.get(`${BASE_URL}/summary`, { params })
}

export function fetchRecommendationPreferenceAggregates(params = {}) {
  return http.get(`${BASE_URL}/aggregates`, { params })
}

export function fetchRecommendationPreferenceEvents(params = {}) {
  return http.get(`${BASE_URL}/events`, { params })
}
