import http from './http'

export async function fetchRegions(params = {}) {
  const data = await http.get('/admin/api/regions', {
    params: {
      current: 1,
      size: 500,
      ...params
    }
  })
  return data.records || []
}

export async function fetchOrgs(params = {}) {
  const data = await http.get('/admin/api/orgs', {
    params: {
      current: 1,
      size: 500,
      ...params
    }
  })
  return data.records || []
}

export async function refOptions() {
  const [regions, orgs] = await Promise.all([fetchRegions(), fetchOrgs()])
  return { regions, orgs }
}

export async function activeRefOptions() {
  const [regions, orgs] = await Promise.all([
    fetchRegions({ sdStatus: '1' }),
    fetchOrgs({ sdStatus: '1' })
  ])
  const enabledRegionIds = new Set((regions || []).map(item => item.idRegion))
  return {
    regions,
    orgs: (orgs || []).filter(item => enabledRegionIds.has(item.idRegion))
  }
}

export async function fetchHisOrgOptions() {
  const data = await http.get('/admin/api/analytics/his-org-options')
  return data || []
}
