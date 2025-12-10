# Connection Monitoring Feature

## Overview
Real-time network connection monitoring, VPN detection, and connectivity status tracking for external service integrations.

## What This Feature Does
- Monitor internet connectivity
- Detect VPN status
- Track API connection health
- Display connection status in UI
- Provide offline mode support

## Acceptance Criteria

✅ **Connectivity Monitoring**
- Detect online/offline status
- Monitor connection quality
- Track latency to key endpoints

✅ **VPN Detection**
- Identify when VPN is active
- Detect VPN connection changes
- Provide VPN guidance for restricted networks

✅ **Service Health**
- Check Slack API status
- Track Git provider connectivity

## Technology Stack

**Services**:
- `ConnectionMonitorService.ts` - Core monitoring
- `VPNDetectionService.ts` - VPN detection
- `HttpTracingService.ts` - HTTP request tracing

**Frontend Component**: `ConnectionStatus.tsx`

## Key Methods

```typescript
class ConnectionMonitorService {
  async checkConnectivity(): Promise<ConnectionStatus>
  async ping(url: string): Promise<number>
  startMonitoring(intervalMs: number): void
  stopMonitoring(): void
  getCurrentStatus(): ConnectionStatus
  on(event: 'status-change', callback: (status: ConnectionStatus) => void): void
}

class VPNDetectionService {
  async detectVPN(): Promise<VPNStatus>
  async getVPNType(): Promise<'corporate' | 'personal' | 'none'>
  async suggestVPNSettings(): Promise<VPNGuidance>
}

interface ConnectionStatus {
  isOnline: boolean;
  isVPNActive: boolean;
  latency: number;
  slackAccessible: boolean;
  gitAccessible: boolean;
  lastChecked: Date;
}
```

## Monitoring Flow

```
1. Start periodic checks (every 30s)
2. Ping test endpoints
3. Measure latency
4. Detect VPN status
5. Update connection status
6. Emit status-change event
7. UI updates with new status
```

## VPN Detection Techniques

```typescript
async detectVPN(): Promise<boolean> {
  // Check for VPN network interfaces
  const interfaces = os.networkInterfaces();
  for (const [name, addresses] of Object.entries(interfaces)) {
    if (name.includes('tun') || name.includes('tap') || 
        name.includes('vpn') || name.includes('utun')) {
      return true;
    }
  }
  
  // Check for VPN-specific DNS servers
  const dnsServers = await this.getDNSServers();
  // Corporate VPN often uses internal DNS
  
  // Check routing table for VPN routes
  const routes = await this.getRoutingTable();
  
  return false;
}
```

## UI Status Indicator

```
Online + No VPN: 🟢 Connected
Online + VPN: 🟡 Connected (VPN Active)
Offline: 🔴 Offline
```

## Challenges and Solutions

### Challenge: False Positives
Brief network hiccups trigger false offline status.

**Solution**: Implement debouncing and multiple ping attempts.

```typescript
async checkWithRetry(maxRetries: number = 3): Promise<boolean> {
  for (let i = 0; i < maxRetries; i++) {
    if (await this.ping('https://www.google.com')) {
      return true;
    }
    await sleep(1000);
  }
  return false;
}
```

### Challenge: Corporate Firewall
Some networks block connectivity checks.

**Solution**: Use multiple test endpoints and fallback strategies.

## What Else Can Be Added
1. Network speed testing
2. Proxy detection and configuration
3. Auto-reconnect for APIs
4. Offline queue for actions
5. Network usage analytics

## API Reference

```typescript
ipcRenderer.invoke('check-connection'): Promise<ConnectionStatus>
ipcRenderer.invoke('detect-vpn'): Promise<VPNStatus>
ipcRenderer.on('connection-status-changed', (event, status: ConnectionStatus) => {})
```

## Related Documentation
- [Slack Integration](06-slack-integration.md)
- [Authentication](07-authentication.md)
