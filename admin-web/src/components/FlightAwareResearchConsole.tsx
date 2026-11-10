import React, { useEffect, useState } from 'react';
import { FlightAwareResearchApiClient } from '../api/flightAwareResearchApi';
import {
  CredentialSummaryDto,
  FlightDataValidationReportDto,
  PollDiffComparisonDto,
  PollExecutionResultDto,
  TokenExtractionSummaryDto,
} from '../types/flightAwareResearch';

interface Props {
  apiClient: FlightAwareResearchApiClient;
}

/**
 * Admin UI — FlightAware Research Console (Section 7)
 * Navigation path: Integrations -> Flight Tracking -> FlightAware Research
 *
 * Security Invariant:
 * - Never displays or copies decrypted tokens.
 * - Only displays boolean presence indicators and non-reversible SHA-256 fingerprints.
 */
export const FlightAwareResearchConsole: React.FC<Props> = ({ apiClient }) => {
  const [activeTab, setActiveTab] = useState<
    'EXTRACTION' | 'POLLING' | 'HISTORY_DIFF' | 'CREDENTIALS' | 'VALIDATION'
  >('EXTRACTION');

  const [trackingUrl, setTrackingUrl] = useState(
    'https://www.flightaware.com/live/flight/GIA190',
  );
  const [flightNumber, setFlightNumber] = useState('GA-190');
  const [flightDate, setFlightDate] = useState('2026-11-10');
  const [validateImmediately, setValidateImmediately] = useState(false);

  const [extractionResult, setExtractionResult] =
    useState<TokenExtractionSummaryDto | null>(null);
  const [credentials, setCredentials] = useState<CredentialSummaryDto[]>([]);
  const [selectedCredentialId, setSelectedCredentialId] = useState<string>('');
  const [latestPoll, setLatestPoll] = useState<PollExecutionResultDto | null>(
    null,
  );
  const [pollLogs, setPollLogs] = useState<PollExecutionResultDto[]>([]);
  const [diffResult, setDiffResult] = useState<PollDiffComparisonDto | null>(
    null,
  );
  const [validationReport, setValidationReport] =
    useState<FlightDataValidationReportDto | null>(null);
  const [replacementUrl, setReplacementUrl] = useState('');
  const [errorBanner, setErrorBanner] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const refreshAll = async (credId?: string) => {
    try {
      const creds = await apiClient.listCredentials();
      setCredentials(creds);
      const targetId = credId || selectedCredentialId || creds[0]?.id || '';
      if (targetId && !selectedCredentialId) {
        setSelectedCredentialId(targetId);
      }
      const logs = await apiClient.listPollLogs(targetId || undefined);
      setPollLogs(logs);
      const report = await apiClient.getValidationReport(targetId || undefined);
      setValidationReport(report);
    } catch (e: unknown) {
      setErrorBanner(e instanceof Error ? e.message : String(e));
    }
  };

  useEffect(() => {
    void refreshAll();
  }, []);

  const handleExtract = async () => {
    setLoading(true);
    setErrorBanner(null);
    try {
      const res = await apiClient.extractToken({
        trackingPageUrl: trackingUrl,
        flightNumber,
        flightDate,
        persistCredential: true,
        validateImmediately,
      });
      setExtractionResult(res);
      const newCredId = res.persistedCredential?.id;
      if (newCredId) {
        setSelectedCredentialId(newCredId);
      }
      await refreshAll(newCredId);
    } catch (e: unknown) {
      setErrorBanner(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  };

  const handlePoll = async () => {
    if (!selectedCredentialId) return;
    setLoading(true);
    setErrorBanner(null);
    try {
      const res = await apiClient.executePoll(
        selectedCredentialId,
        'ADMIN_CONSOLE_POLL',
      );
      setLatestPoll(res);
      await refreshAll(selectedCredentialId);
    } catch (e: unknown) {
      setErrorBanner(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  };

  const handleCompareLatest = async () => {
    if (!selectedCredentialId) return;
    setLoading(true);
    setErrorBanner(null);
    try {
      const diff = await apiClient.compareLatestTwoPolls(selectedCredentialId);
      setDiffResult(diff);
    } catch (e: unknown) {
      setErrorBanner(e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ fontFamily: 'system-ui, sans-serif', padding: 20 }}>
      <nav style={{ fontSize: 13, color: '#475569', marginBottom: 6 }}>
        Integrations &rarr; Flight Tracking &rarr;{' '}
        <strong>FlightAware Research</strong>
      </nav>
      <h2 style={{ margin: '0 0 12px 0' }}>
        FlightAware Research Console &amp; Persistent Token Management
      </h2>

      {errorBanner && (
        <div
          style={{
            background: '#FEF2F2',
            border: '1px solid #DC2626',
            color: '#991B1B',
            padding: 12,
            borderRadius: 8,
            marginBottom: 16,
          }}
        >
          {errorBanner}
        </div>
      )}

      <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
        {(
          [
            ['EXTRACTION', 'A. Token Extraction'],
            ['POLLING', 'B. Polling Tester'],
            ['HISTORY_DIFF', 'C. Response History & Diff'],
            ['CREDENTIALS', 'D. Credential Management'],
            ['VALIDATION', 'E. Flight Data Validation & Experiments'],
          ] as const
        ).map(([key, label]) => (
          <button
            key={key}
            onClick={() => setActiveTab(key)}
            style={{
              padding: '8px 14px',
              borderRadius: 6,
              border: '1px solid #CBD5E1',
              background: activeTab === key ? '#0F172A' : '#FFFFFF',
              color: activeTab === key ? '#FFFFFF' : '#0F172A',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            {label}
          </button>
        ))}
      </div>

      {activeTab === 'EXTRACTION' && (
        <section>
          <h3>A. Token Extraction (trackpollGlobals)</h3>
          <div style={{ display: 'grid', gap: 10, maxWidth: 680 }}>
            <label>
              FlightAware Tracking Page URL
              <input
                type="url"
                value={trackingUrl}
                onChange={(e) => setTrackingUrl(e.target.value)}
                style={{ width: '100%', padding: 8 }}
              />
            </label>
            <div style={{ display: 'flex', gap: 12 }}>
              <label>
                Flight Number
                <input
                  type="text"
                  value={flightNumber}
                  onChange={(e) => setFlightNumber(e.target.value)}
                  style={{ padding: 8, marginLeft: 8 }}
                />
              </label>
              <label>
                Scheduled Departure Date
                <input
                  type="date"
                  value={flightDate}
                  onChange={(e) => setFlightDate(e.target.value)}
                  style={{ padding: 8, marginLeft: 8 }}
                />
              </label>
            </div>
            <label>
              <input
                type="checkbox"
                checked={validateImmediately}
                onChange={(e) => setValidateImmediately(e.target.checked)}
              />{' '}
              Validate immediately against trackpoll.rvt after extraction
            </label>
            <button
              disabled={loading}
              onClick={handleExtract}
              style={{
                padding: '10px 16px',
                background: '#059669',
                color: '#fff',
                border: 'none',
                borderRadius: 6,
                fontWeight: 700,
              }}
            >
              Extract Token
            </button>
          </div>

          {extractionResult && (
            <div
              style={{
                marginTop: 16,
                padding: 14,
                border: '1px solid #CBD5E1',
                borderRadius: 8,
              }}
            >
              <p>
                <strong>Extraction Status:</strong>{' '}
                {extractionResult.extractionStatus} (HTTP{' '}
                {extractionResult.httpStatus})
              </p>
              <p>
                <strong>TOKEN Presence:</strong>{' '}
                {extractionResult.tokenPresent
                  ? `Present (Encrypted — ${extractionResult.tokenFingerprint})`
                  : 'Missing'}
              </p>
              <p>
                <strong>INTERVAL:</strong>{' '}
                {extractionResult.pollingIntervalSeconds ?? 'N/A'} seconds
              </p>
              <p>
                <strong>SINGLE_FLIGHT:</strong>{' '}
                {String(extractionResult.singleFlight ?? 'N/A')}
              </p>
              <p>
                <strong>USERTOKEN Presence:</strong>{' '}
                {extractionResult.userTokenPresent ? 'Present' : 'Not Present'}
              </p>
              <p>
                <strong>Extraction Timestamp:</strong>{' '}
                {extractionResult.extractedAt}
              </p>
              <p>{extractionResult.diagnosticMessage}</p>
            </div>
          )}
        </section>
      )}

      {activeTab === 'POLLING' && (
        <section>
          <h3>B. Polling Tester (trackpoll.rvt)</h3>
          <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
            <select
              value={selectedCredentialId}
              onChange={(e) => setSelectedCredentialId(e.target.value)}
              style={{ padding: 8 }}
            >
              <option value="">-- Select Credential --</option>
              {credentials.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.flightNumber} ({c.flightDate}) — [{c.tokenStatus}] —{' '}
                  {c.tokenFingerprint}
                </option>
              ))}
            </select>
            <button
              disabled={loading || !selectedCredentialId}
              onClick={handlePoll}
              style={{
                padding: '8px 14px',
                background: '#2563EB',
                color: '#fff',
                border: 'none',
                borderRadius: 6,
                fontWeight: 700,
              }}
            >
              Execute Poll Request
            </button>
          </div>

          {latestPoll && (
            <div style={{ marginTop: 14 }}>
              <p>
                <strong>HTTP Status:</strong> {latestPoll.httpStatus} |{' '}
                <strong>Parsing Status:</strong> {latestPoll.parsingStatus} |{' '}
                <strong>Duration:</strong> {latestPoll.responseDurationMs} ms |{' '}
                <strong>Timestamp:</strong> {latestPoll.responseTimestamp}
              </p>
              {latestPoll.errorMessage && (
                <p style={{ color: '#DC2626' }}>
                  <strong>Error:</strong> {latestPoll.errorMessage}
                </p>
              )}
              <details open>
                <summary style={{ fontWeight: 700, cursor: 'pointer' }}>
                  Normalized Flight Data
                </summary>
                <pre
                  style={{
                    background: '#F8FAFC',
                    padding: 12,
                    overflowX: 'auto',
                  }}
                >
                  {JSON.stringify(latestPoll.normalizedFlight, null, 2)}
                </pre>
              </details>
              <details>
                <summary style={{ fontWeight: 700, cursor: 'pointer' }}>
                  Parsed JSON Tree (Expandable)
                </summary>
                <pre
                  style={{
                    background: '#F8FAFC',
                    padding: 12,
                    maxHeight: 320,
                    overflow: 'auto',
                  }}
                >
                  {JSON.stringify(latestPoll.parsedResponse, null, 2)}
                </pre>
              </details>
              <details>
                <summary style={{ fontWeight: 700, cursor: 'pointer' }}>
                  Raw Response Payload
                </summary>
                <pre
                  style={{
                    background: '#0F172A',
                    color: '#E2E8F0',
                    padding: 12,
                    maxHeight: 260,
                    overflow: 'auto',
                  }}
                >
                  {latestPoll.rawResponse}
                </pre>
              </details>
            </div>
          )}
        </section>
      )}

      {activeTab === 'HISTORY_DIFF' && (
        <section>
          <h3>C. Response History &amp; Successive Diff Comparison</h3>
          <button
            disabled={loading || !selectedCredentialId}
            onClick={handleCompareLatest}
            style={{
              padding: '8px 14px',
              background: '#0F172A',
              color: '#fff',
              borderRadius: 6,
              marginBottom: 12,
            }}
          >
            Compare Latest 2 Polls for Selected Credential
          </button>

          {diffResult && (
            <div
              style={{
                padding: 12,
                border: '1px solid #94A3B8',
                borderRadius: 8,
                marginBottom: 16,
              }}
            >
              <p>
                <strong>{diffResult.summaryText}</strong>
              </p>
              <h4>
                Operationally Significant Changes (
                {diffResult.operationallySignificantChanges.length})
              </h4>
              <ul>
                {diffResult.operationallySignificantChanges.map((item) => (
                  <li key={item.jsonPath}>
                    <strong>[{item.operationalCategory}]</strong>{' '}
                    <code>{item.jsonPath}</code>: {item.oldValue ?? 'null'}{' '}
                    &rarr; <strong>{item.newValue ?? 'null'}</strong>
                  </li>
                ))}
              </ul>
              <details>
                <summary>
                  Added ({diffResult.addedFields.length}), Removed (
                  {diffResult.removedFields.length}), Unchanged (
                  {diffResult.unchangedFieldCount})
                </summary>
                <pre style={{ fontSize: 12 }}>
                  {JSON.stringify(
                    {
                      added: diffResult.addedFields,
                      removed: diffResult.removedFields,
                      changed: diffResult.changedValues,
                    },
                    null,
                    2,
                  )}
                </pre>
              </details>
            </div>
          )}

          <h4>Recorded Poll History ({pollLogs.length})</h4>
          <table
            style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}
          >
            <thead>
              <tr style={{ background: '#F1F5F9', textAlign: 'left' }}>
                <th style={{ padding: 8 }}>Request Time</th>
                <th style={{ padding: 8 }}>HTTP</th>
                <th style={{ padding: 8 }}>Duration</th>
                <th style={{ padding: 8 }}>Parsing Status</th>
                <th style={{ padding: 8 }}>Flight Ident</th>
              </tr>
            </thead>
            <tbody>
              {pollLogs.map((log) => (
                <tr
                  key={log.pollLogId}
                  style={{ borderBottom: '1px solid #E2E8F0' }}
                >
                  <td style={{ padding: 8 }}>{log.requestTimestamp}</td>
                  <td style={{ padding: 8 }}>{log.httpStatus}</td>
                  <td style={{ padding: 8 }}>{log.responseDurationMs} ms</td>
                  <td style={{ padding: 8 }}>{log.parsingStatus}</td>
                  <td style={{ padding: 8 }}>
                    {log.flightIdentifier ?? 'N/A'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      {activeTab === 'CREDENTIALS' && (
        <section>
          <h3>D. Encrypted Credential Management</h3>
          <p style={{ fontSize: 13, color: '#475569' }}>
            Tokens are AES-256-GCM encrypted at rest. Decrypted tokens are never
            exposed or copyable in the UI.
          </p>
          <div style={{ marginBottom: 12 }}>
            <input
              type="url"
              placeholder="Replacement FlightAware URL for selected credential..."
              value={replacementUrl}
              onChange={(e) => setReplacementUrl(e.target.value)}
              style={{ width: 420, padding: 8, marginRight: 8 }}
            />
          </div>
          <table
            style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}
          >
            <thead>
              <tr style={{ background: '#F1F5F9', textAlign: 'left' }}>
                <th style={{ padding: 8 }}>Flight / Date</th>
                <th style={{ padding: 8 }}>Status</th>
                <th style={{ padding: 8 }}>Fingerprint</th>
                <th style={{ padding: 8 }}>Extracted</th>
                <th style={{ padding: 8 }}>Last Validated</th>
                <th style={{ padding: 8 }}>Last Poll</th>
                <th style={{ padding: 8 }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {credentials.map((c) => (
                <tr key={c.id} style={{ borderBottom: '1px solid #E2E8F0' }}>
                  <td style={{ padding: 8 }}>
                    <strong>{c.flightNumber}</strong> ({c.flightDate})
                  </td>
                  <td style={{ padding: 8 }}>
                    <strong>{c.tokenStatus}</strong>
                  </td>
                  <td style={{ padding: 8 }}>
                    <code>{c.tokenFingerprint}</code>
                  </td>
                  <td style={{ padding: 8 }}>{c.extractedAt}</td>
                  <td style={{ padding: 8 }}>{c.lastValidatedAt ?? '-'}</td>
                  <td style={{ padding: 8 }}>{c.lastUsedAt ?? '-'}</td>
                  <td style={{ padding: 8, display: 'flex', gap: 6 }}>
                    <button
                      onClick={() =>
                        void apiClient
                          .revalidateCredential(c.id)
                          .then(() => refreshAll(c.id))
                      }
                    >
                      Revalidate
                    </button>
                    <button
                      disabled={!replacementUrl}
                      onClick={() =>
                        void apiClient
                          .replaceCredential(c.id, replacementUrl)
                          .then(() => refreshAll(c.id))
                      }
                    >
                      Replace
                    </button>
                    <button
                      onClick={() =>
                        void apiClient
                          .disableCredential(c.id)
                          .then(() => refreshAll(c.id))
                      }
                    >
                      Disable
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      {activeTab === 'VALIDATION' && validationReport && (
        <section>
          <h3>E. Section 9 Flight Data Validation &amp; Section 10 Experiments</h3>
          <p>
            <strong>Overall Verdict:</strong>{' '}
            {validationReport.overallReadinessVerdict}
          </p>
          <h4>13 Operational Data Requirements</h4>
          <table
            style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}
          >
            <thead>
              <tr style={{ background: '#F1F5F9', textAlign: 'left' }}>
                <th style={{ padding: 8 }}>Requirement</th>
                <th style={{ padding: 8 }}>Importance</th>
                <th style={{ padding: 8 }}>Status</th>
                <th style={{ padding: 8 }}>Actual JSON Paths</th>
                <th style={{ padding: 8 }}>Type / TZ</th>
                <th style={{ padding: 8 }}>Sample</th>
                <th style={{ padding: 8 }}>Dynamic Update?</th>
              </tr>
            </thead>
            <tbody>
              {validationReport.requirements.map((r) => (
                <tr
                  key={r.requirementCode}
                  style={{ borderBottom: '1px solid #E2E8F0' }}
                >
                  <td style={{ padding: 8 }}>{r.requirementName}</td>
                  <td style={{ padding: 8 }}>{r.importance}</td>
                  <td style={{ padding: 8 }}>
                    <strong>{r.availabilityStatus}</strong>
                  </td>
                  <td style={{ padding: 8 }}>
                    <code>{r.actualFieldNames.join(', ') || '-'}</code>
                  </td>
                  <td style={{ padding: 8 }}>
                    {r.dataType} ({r.timestampFormat})
                  </td>
                  <td style={{ padding: 8 }}>{r.exampleValue ?? '-'}</td>
                  <td style={{ padding: 8 }}>
                    {r.changesObservedBetweenPolls ? 'Yes' : 'No'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <h4 style={{ marginTop: 18 }}>Experiments A–E Summary</h4>
          <ul>
            {validationReport.experiments.map((exp) => (
              <li key={exp.experimentCode} style={{ marginBottom: 8 }}>
                <strong>
                  {exp.title} [{exp.status}]
                </strong>
                : {exp.observedBehavior} <em>({exp.operationalConclusion})</em>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
};

