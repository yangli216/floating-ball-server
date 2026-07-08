import http from './http'

const BASE_URL = '/admin/api/patient-memories'

export function fetchPatientMemories(params = {}) {
  return http.get(BASE_URL, { params })
}

export function fetchPatientMemoryDetail(memoryId) {
  return http.get(`${BASE_URL}/${memoryId}`)
}

export function updatePatientMemoryFact(memoryId, factId, body) {
  return http.put(`${BASE_URL}/${memoryId}/facts/${factId}`, body)
}

export function suppressPatientMemoryFact(memoryId, factId, note) {
  return http.post(`${BASE_URL}/${memoryId}/facts/${factId}/suppress`, { note })
}

export function restorePatientMemoryFact(memoryId, factId, note) {
  return http.post(`${BASE_URL}/${memoryId}/facts/${factId}/restore`, { note })
}
