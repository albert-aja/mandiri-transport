import {
  CredentialSummaryDto,
  FlightDataValidationReportDto,
  PollDiffComparisonDto,
  PollExecutionResultDto,
  TokenExtractionSummaryDto,
} from '../types/flightAwareResearch';

export class FlightAwareResearchApiClient {
  constructor(
    private readonly baseUrl: string = 'http://localhost:8080/api/v1/integrations/flightaware',
    private readonly getJwtToken: () => string | null,
  ) {}

  private async request<T>(path: string, init?: RequestInit): Promise<T> {
    const token = this.getJwtToken();
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    };
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
    const response = await fetch(`${this.baseUrl}${path}`, {
      ...init,
      headers: { ...headers, ...(init?.headers as Record<string, string>) },
    });
    const payload = await response.json();
    if (!response.ok || payload.success === false) {
      throw new Error(payload.message || `HTTP ${response.status}`);
    }
    return payload.data as T;
  }

  extractToken(params: {
    trackingPageUrl: string;
    flightId?: string;
    flightNumber?: string;
    flightDate?: string;
    persistCredential?: boolean;
    validateImmediately?: boolean;
  }): Promise<TokenExtractionSummaryDto> {
    return this.request<TokenExtractionSummaryDto>('/extract', {
      method: 'POST',
      body: JSON.stringify(params),
    });
  }

  listCredentials(flightNumber?: string, tokenStatus?: string): Promise<CredentialSummaryDto[]> {
    const query = new URLSearchParams();
    if (flightNumber) query.set('flightNumber', flightNumber);
    if (tokenStatus) query.set('tokenStatus', tokenStatus);
    const qs = query.toString();
    return this.request<CredentialSummaryDto[]>(`/credentials${qs ? `?${qs}` : ''}`);
  }

  getCredentialHistory(flightNumber: string, flightDate?: string): Promise<CredentialSummaryDto[]> {
    const query = new URLSearchParams({ flightNumber });
    if (flightDate) query.set('flightDate', flightDate);
    return this.request<CredentialSummaryDto[]>(`/credentials/history?${query.toString()}`);
  }

  executePoll(credentialId: string, experimentTag?: string): Promise<PollExecutionResultDto> {
    return this.request<PollExecutionResultDto>(`/credentials/${credentialId}/poll`, {
      method: 'POST',
      body: JSON.stringify({ experimentTag, forceBypassMinInterval: false }),
    });
  }

  revalidateCredential(credentialId: string): Promise<CredentialSummaryDto> {
    return this.request<CredentialSummaryDto>(`/credentials/${credentialId}/revalidate`, {
      method: 'POST',
    });
  }

  replaceCredential(credentialId: string, trackingPageUrl: string): Promise<CredentialSummaryDto> {
    return this.request<CredentialSummaryDto>(`/credentials/${credentialId}/replace`, {
      method: 'POST',
      body: JSON.stringify({ trackingPageUrl }),
    });
  }

  disableCredential(credentialId: string): Promise<CredentialSummaryDto> {
    return this.request<CredentialSummaryDto>(`/credentials/${credentialId}/disable`, {
      method: 'POST',
    });
  }

  listPollLogs(credentialId?: string): Promise<PollExecutionResultDto[]> {
    const qs = credentialId ? `?credentialId=${encodeURIComponent(credentialId)}` : '';
    return this.request<PollExecutionResultDto[]>(`/poll-logs${qs}`);
  }

  comparePollLogs(olderPollLogId: string, newerPollLogId: string): Promise<PollDiffComparisonDto> {
    const qs = new URLSearchParams({ olderPollLogId, newerPollLogId }).toString();
    return this.request<PollDiffComparisonDto>(`/poll-logs/compare?${qs}`);
  }

  compareLatestTwoPolls(credentialId: string): Promise<PollDiffComparisonDto> {
    return this.request<PollDiffComparisonDto>(`/credentials/${credentialId}/diff-latest`);
  }

  getValidationReport(credentialId?: string): Promise<FlightDataValidationReportDto> {
    const qs = credentialId ? `?credentialId=${encodeURIComponent(credentialId)}` : '';
    return this.request<FlightDataValidationReportDto>(`/validation-report${qs}`);
  }
}

