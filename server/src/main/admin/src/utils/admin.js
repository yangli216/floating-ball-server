export const deviceStatusOptions = [
  { value: '0', label: '未激活', type: 'info' },
  { value: '1', label: '活跃', type: 'success' }
]

export const configStatusOptions = [
  { value: '0', label: '停用', type: 'info' },
  { value: '1', label: '启用', type: 'success' }
]

export function buildLabelMap(list, keyField, labelField) {
  return (list || []).reduce((result, item) => {
    result[item[keyField]] = item[labelField]
    return result
  }, {})
}

export function findStatusMeta(options, value) {
  return options.find(item => item.value === value) || { label: value || '--', type: 'info' }
}

export function statusTone(type) {
  if (type === 'success') return 'success'
  if (type === 'warning') return 'warning'
  if (type === 'danger') return 'danger'
  return 'muted'
}

export function flagToBoolean(value) {
  return value === true || value === '1' || value === 1
}

export function formatDateTime(value) {
  if (!value) {
    return '--'
  }
  if (typeof value === 'string') {
    return value.replace('T', ' ').split('.')[0]
  }
  return String(value)
}

export function truncate(value, maxLength = 48) {
  if (!value) {
    return '--'
  }
  return value.length > maxLength ? `${value.slice(0, maxLength)}…` : value
}

export function resolveScopeLabel(row, orgMap, regionMap) {
  if (row.naOrg) {
    return `机构 / ${row.naOrg}`
  }
  if (row.idOrg && orgMap[row.idOrg]) {
    return `机构 / ${orgMap[row.idOrg]}`
  }
  if (row.naRegion) {
    return `区域 / ${row.naRegion}`
  }
  if (row.idRegion && regionMap[row.idRegion]) {
    return `区域 / ${regionMap[row.idRegion]}`
  }
  return '全局'
}
