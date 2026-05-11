const TENANTS = {
  agenta: {
    key: 'agenta',
    hostnames: ['agenta.local'],
    agentId: 1,
    brandName: 'TravelEase Alpha',
    brandLogo: '🚀',
    themeName: 'agenta',
    navLabels: [
      { key: 'trips', label: 'My Trips', icon: '📋' },
      { key: 'deals', label: 'Special Offers', icon: '⭐' },
      { key: 'support', label: 'Support', icon: '💬' },
    ],
  },
  agentb: {
    key: 'agentb',
    hostnames: ['agentb.local'],
    agentId: 2,
    brandName: 'TravelEase Horizon',
    brandLogo: '🌍',
    themeName: 'agentb',
    navLabels: [
      { key: 'trips', label: 'My Trips', icon: '🗺️' },
      { key: 'deals', label: 'Best Prices', icon: '💰' },
      { key: 'support', label: 'Help Center', icon: '❓' },
    ],
  },
}

const FALLBACK_TENANT_KEY = 'agenta'

function normalizeHost(value) {
  return String(value || '').trim().toLowerCase()
}

function findTenantByHost(hostname) {
  const host = normalizeHost(hostname)
  return Object.values(TENANTS).find((tenant) => tenant.hostnames.includes(host)) || null
}

function findTenantByKey(key) {
  const tenantKey = normalizeHost(key)
  return TENANTS[tenantKey] || null
}

function resolveTenant() {
  const explicitTenant = findTenantByKey(import.meta.env.VITE_TENANT)
  if (explicitTenant) return explicitTenant

  if (typeof window !== 'undefined') {
    const byHost = findTenantByHost(window.location.hostname)
    if (byHost) return byHost
  }

  return TENANTS[FALLBACK_TENANT_KEY]
}

export const tenantConfig = resolveTenant()
