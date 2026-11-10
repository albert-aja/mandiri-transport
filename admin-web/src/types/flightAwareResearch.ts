export interface CredentialSummaryDto {
  id: string;
  flightId?: string | null;
  bookingCode?: string | null;
  tripId?: string | null;
  provider: string;
  flightNumber: string;
  flightDate: string;
  tokenPresent: boolean;
  userTokenPresent: boolean;
  tokenFingerprint: string;
  pollingIntervalSeconds?: number | null;
  singleFlight?: boolean | null;
  trackingPageUrl: string;
  tokenStatus: 'ACTIVE' | 'EXPIRED' | 'INVALID' | 'UNKNOWN' | 'SUPERSEDED' | 'DISABLED';
  extractedAt: string;
  lastValidatedAt?: string | null;
  lastUsedAt?: string | null;
  expiresAt?: string | null;
  validationNotes?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TokenExtractionSummaryDto {
  extractionSuccess: boolean;
  extractionStatus: string;
  httpStatus: number;
  trackingPageUrl: string;
  tokenPresent: boolean;
  tokenFingerprint: string;
  pollingIntervalSeconds?: number | null;
  singleFlight?: boolean | null;
  userTokenPresent: boolean;
  additionalDiscoveredKeys: string[];
  extractedAt: string;
  diagnosticMessage: string;
  persistedCredential?: CredentialSummaryDto | null;
}

export interface DiscoveredFieldItem {
  jsonPath: string;
  dataType: string;
  sampleValue: string;
  timestampFormat: string;
  mappedRequirementCode?: string | null;
}

export interface NormalizedFlightSummary {
  flightIdentifier?: string | null;
  flightStatus?: string | null;
  originCode?: string | null;
  destinationCode?: string | null;
  scheduledDeparture?: string | null;
  estimatedDeparture?: string | null;
  actualDeparture?: string | null;
  departureDelayMinutes?: number | null;
  scheduledArrival?: string | null;
  estimatedArrival?: string | null;
  actualArrival?: string | null;
  arrivalDelayMinutes?: number | null;
  latitude?: number | null;
  longitude?: number | null;
  altitudeFeet?: number | null;
  groundSpeedKts?: number | null;
  aircraftType?: string | null;
  aircraftRegistration?: string | null;
  cancelled?: boolean | null;
  diverted?: boolean | null;
  timestampTimezoneFormat?: string | null;
  sourceFieldMap: Record<string, string>;
}

export interface PollExecutionResultDto {
  pollLogId: string;
  credentialId: string;
  flightIdentifier?: string | null;
  requestTimestamp: string;
  responseTimestamp: string;
  responseDurationMs: number;
  httpStatus: number;
  responseValid: boolean;
  parsingStatus: string;
  rawResponse?: string | null;
  parsedResponse?: Record<string, unknown> | null;
  normalizedFlight?: NormalizedFlightSummary | null;
  discoveredFields: DiscoveredFieldItem[];
  errorMessage?: string | null;
  updatedCredentialStatus?: string | null;
  validationMessage?: string | null;
}

export interface FieldChangeItem {
  jsonPath: string;
  oldValue?: string | null;
  newValue?: string | null;
  operationalCategory?: string | null;
  operationallySignificant: boolean;
}

export interface PollDiffComparisonDto {
  olderPollLogId: string;
  olderTimestamp: string;
  newerPollLogId: string;
  newerTimestamp: string;
  addedFields: FieldChangeItem[];
  removedFields: FieldChangeItem[];
  changedValues: FieldChangeItem[];
  operationallySignificantChanges: FieldChangeItem[];
  unchangedFieldCount: number;
  sampleUnchangedPaths: string[];
  summaryText: string;
}

export interface RequirementValidationItem {
  requirementCode: string;
  requirementName: string;
  importance: 'CRITICAL' | 'HIGH' | 'OPTIONAL';
  availabilityStatus: 'VERIFIED_MEANINGFUL' | 'FIELD_PRESENT_UNVERIFIED_VALUE' | 'MISSING_NOT_OBSERVED';
  actualFieldNames: string[];
  dataType: string;
  exampleValue?: string | null;
  changesObservedBetweenPolls: boolean;
  timestampFormat: string;
  verificationNotes: string;
}

export interface ExperimentFindingDto {
  experimentCode: string;
  title: string;
  status: 'VERIFIED_PASS' | 'PARTIAL_OR_CONDITIONAL' | 'FAILED_OR_RESTRICTED' | 'PENDING_EVIDENCE';
  evidenceCount: number;
  observedBehavior: string;
  operationalConclusion: string;
}

export interface FlightDataValidationReportDto {
  provider: string;
  scopeCredentialId?: string | null;
  scopeFlightNumber: string;
  totalCredentialsEvaluated: number;
  totalPollLogsEvaluated: number;
  validPollLogsCount: number;
  criticalRequirementsSupported: number;
  criticalRequirementsTotal: number;
  meetsOperationalRequirements: boolean;
  overallReadinessVerdict: string;
  requirements: RequirementValidationItem[];
  experiments: ExperimentFindingDto[];
  commercialAndTechnicalRestrictions: string[];
}

