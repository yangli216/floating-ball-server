const MAX_NORMALIZE_DEPTH = 24
const MAX_NORMALIZE_NODES = 5000
const JSON_DISPLAY_MARKER = Symbol('json-display-marker')
const TRUNCATION_KEY = '…'

function parseJsonDocument(value) {
  if (typeof value !== 'string') {
    return value
  }
  const text = value.trim()
  const isObject = text.startsWith('{') && text.endsWith('}')
  const isArray = text.startsWith('[') && text.endsWith(']')
  if (!isObject && !isArray) {
    return value
  }
  try {
    return JSON.parse(text)
  } catch (error) {
    return value
  }
}

function createTruncationMarker(reason, omittedCount) {
  const marker = {
    reason,
    omittedCount
  }
  Object.defineProperty(marker, JSON_DISPLAY_MARKER, {
    value: true
  })
  return Object.freeze(marker)
}

function defineDisplayValue(target, key, value) {
  Object.defineProperty(target, key, {
    configurable: true,
    enumerable: true,
    value,
    writable: true
  })
}

function appendTruncationMarker(target, isArray, marker) {
  if (isArray) {
    target.push(marker)
    return
  }
  let key = TRUNCATION_KEY
  while (Object.prototype.hasOwnProperty.call(target, key)) {
    key += TRUNCATION_KEY
  }
  defineDisplayValue(target, key, marker)
}

function normalizeValue(value, depth, budget, seen) {
  const parsedValue = parseJsonDocument(value)
  if (parsedValue !== value) {
    return normalizeValue(parsedValue, depth + 1, budget, seen)
  }

  if (!parsedValue || typeof parsedValue !== 'object') {
    return { value: parsedValue, used: 1, truncated: false }
  }
  if (seen.has(parsedValue)) {
    return { value: '[Circular]', used: 1, truncated: true }
  }

  const isArray = Array.isArray(parsedValue)
  const keys = isArray ? [] : Object.keys(parsedValue)
  const entryCount = isArray ? parsedValue.length : keys.length
  if (entryCount > 0 && (depth >= MAX_NORMALIZE_DEPTH || budget <= 1)) {
    return {
      value: createTruncationMarker(
        depth >= MAX_NORMALIZE_DEPTH ? 'depth' : 'budget',
        entryCount
      ),
      used: 1,
      truncated: true
    }
  }

  seen.add(parsedValue)
  const normalized = isArray ? [] : {}
  let used = 1
  let truncated = false
  try {
    for (let index = 0; index < entryCount; index += 1) {
      const remainingBudget = budget - used
      const remainingEntries = entryCount - index
      if (remainingBudget <= 0) {
        truncated = true
        break
      }
      if (remainingBudget === 1 && remainingEntries > 1) {
        appendTruncationMarker(
          normalized,
          isArray,
          createTruncationMarker('budget', remainingEntries)
        )
        used += 1
        truncated = true
        break
      }

      const hasLaterEntries = index < entryCount - 1
      const childBudget = remainingBudget - (hasLaterEntries ? 1 : 0)
      const key = isArray ? index : keys[index]
      const child = normalizeValue(parsedValue[key], depth + 1, childBudget, seen)
      if (isArray) {
        normalized.push(child.value)
      } else {
        defineDisplayValue(normalized, key, child.value)
      }
      used += child.used
      truncated = truncated || child.truncated
    }
  } finally {
    seen.delete(parsedValue)
  }
  return { value: normalized, used, truncated }
}

export function normalizeJsonDisplayValue(value) {
  const parsedRoot = parseJsonDocument(value)
  const normalized = normalizeValue(
    parsedRoot,
    parsedRoot === value ? 0 : 1,
    MAX_NORMALIZE_NODES,
    new WeakSet()
  )
  const rootEntryCount = Array.isArray(parsedRoot)
    ? parsedRoot.length
    : parsedRoot && typeof parsedRoot === 'object'
      ? Object.keys(parsedRoot).length
      : null
  return {
    value: normalized.value,
    truncated: normalized.truncated,
    rootEntryCount
  }
}

export function normalizeJsonValue(value) {
  return normalizeJsonDisplayValue(value).value
}

export function isJsonDisplayTruncationMarker(value) {
  return Boolean(value && value[JSON_DISPLAY_MARKER])
}

export function formatJsonDisplayTruncationMarker(value) {
  const count = Number.isFinite(value && value.omittedCount) ? value.omittedCount : 0
  const subject = count > 0 ? `${count} 项内容` : '其余内容'
  if (value && value.reason === 'depth') {
    return `结构层级过深，已省略此节点的 ${subject}，请切换原文查看`
  }
  return `为保证页面流畅，已省略 ${subject}，请切换原文查看`
}

export function hasJsonDisplayValue(value) {
  if (value === null || value === undefined) {
    return false
  }
  if (typeof value === 'string') {
    return value.trim().length > 0
  }
  return true
}

export function stringifyJsonDisplayValue(value, emptyText = '') {
  if (!hasJsonDisplayValue(value)) {
    return emptyText
  }
  if (typeof value === 'string') {
    return value
  }
  try {
    return JSON.stringify(
      value,
      (key, item) => isJsonDisplayTruncationMarker(item)
        ? formatJsonDisplayTruncationMarker(item)
        : item,
      2
    )
  } catch (error) {
    return String(value)
  }
}
