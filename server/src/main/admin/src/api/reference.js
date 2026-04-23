import http from './http'

export async function fetchRegions() {
  const data = await http.get('/admin/api/regions', {
    params: {
      current: 1,
      size: 500
    }
  })
  return data.records || []
}

export async function fetchOrgs() {
  const data = await http.get('/admin/api/orgs', {
    params: {
      current: 1,
      size: 500
    }
  })
  return data.records || []
}
