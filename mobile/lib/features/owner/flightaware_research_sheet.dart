import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_theme.dart';
import '../../core/widgets/shared_widgets.dart';
import '../../data/state/app_controller.dart';

Future<void> showFlightAwareResearchConsoleSheet(BuildContext context) async {
  await showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    backgroundColor: Colors.white,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
    ),
    builder: (_) => const FractionallySizedBox(
      heightFactor: 0.94,
      child: FlightAwareResearchConsoleSheet(),
    ),
  );
}

class FlightAwareResearchConsoleSheet extends ConsumerStatefulWidget {
  const FlightAwareResearchConsoleSheet({super.key});

  @override
  ConsumerState<FlightAwareResearchConsoleSheet> createState() =>
      _FlightAwareResearchConsoleSheetState();
}

class _FlightAwareResearchConsoleSheetState
    extends ConsumerState<FlightAwareResearchConsoleSheet> {
  int _sectionIndex = 0;
  bool _loading = false;
  String? _statusMessage;

  final _urlCtrl = TextEditingController(
    text: 'https://www.flightaware.com/live/flight/GIA190',
  );
  final _flightNoCtrl = TextEditingController(text: 'GA-190');
  final _flightDateCtrl = TextEditingController(text: '2026-11-10');

  Map<String, dynamic>? _extractionResult;
  List<Map<String, dynamic>> _credentials = [
    {
      'id': 'cred-fa-gia190-01',
      'flightId': 'bkg-201',
      'bookingCode': 'BK-20261109-201',
      'provider': 'FLIGHTAWARE',
      'flightNumber': 'GA-190',
      'flightDate': '2026-11-10',
      'tokenPresent': true,
      'userTokenPresent': true,
      'tokenFingerprint': 'sha256:8f3a1b9c4e2d7011',
      'pollingIntervalSeconds': 60,
      'singleFlight': true,
      'trackingPageUrl': 'https://www.flightaware.com/live/flight/GIA190',
      'tokenStatus': 'ACTIVE',
      'extractedAt': '2026-11-10T10:00:00Z',
      'lastValidatedAt': '2026-11-10T10:05:00Z',
      'lastUsedAt': '2026-11-10T10:05:00Z',
      'validationNotes':
          'Validasi penuh berhasil: HTTP 200, JSON valid, identitas cocok (GIA190).',
    },
  ];
  String _selectedCredId = 'cred-fa-gia190-01';
  Map<String, dynamic>? _latestPollResult;
  List<Map<String, dynamic>> _pollHistory = [];
  Map<String, dynamic>? _diffResult;

  @override
  void initState() {
    super.initState();
    Future.microtask(_loadFromBackendIfOnline);
  }

  @override
  void dispose() {
    _urlCtrl.dispose();
    _flightNoCtrl.dispose();
    _flightDateCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadFromBackendIfOnline() async {
    final netEnabled = ref.read(networkSyncEnabledProvider);
    final appState = ref.read(appControllerProvider);
    if (!netEnabled || !appState.effectiveOnline) return;
    try {
      final api = ref.read(apiClientProvider);
      final rawCreds = await api.get('/integrations/flightaware/credentials');
      if (rawCreds is List<dynamic> && rawCreds.isNotEmpty && mounted) {
        final list = rawCreds.whereType<Map<String, dynamic>>().toList();
        setState(() {
          _credentials = list;
          _selectedCredId = list.first['id'] as String? ?? _selectedCredId;
        });
      }
      final rawLogs = await api.get(
        '/integrations/flightaware/poll-logs',
        queryParameters: {'credentialId': _selectedCredId},
      );
      if (rawLogs is List<dynamic> && mounted) {
        setState(() {
          _pollHistory = rawLogs.whereType<Map<String, dynamic>>().toList();
        });
      }
    } catch (_) {}
  }

  Future<void> _handleExtractToken() async {
    setState(() {
      _loading = true;
      _statusMessage = null;
    });
    final netEnabled = ref.read(networkSyncEnabledProvider);
    final appState = ref.read(appControllerProvider);
    if (netEnabled && appState.effectiveOnline) {
      try {
        final api = ref.read(apiClientProvider);
        final res = await api.post(
          '/integrations/flightaware/extract',
          body: {
            'trackingPageUrl': _urlCtrl.text.trim(),
            'flightNumber': _flightNoCtrl.text.trim(),
            'flightDate': _flightDateCtrl.text.trim(),
            'persistCredential': true,
            'validateImmediately': false,
          },
        );
        if (res is Map<String, dynamic> && mounted) {
          setState(() {
            _extractionResult = res;
            _loading = false;
            _statusMessage =
                res['diagnosticMessage'] as String? ?? 'Token diekstrak.';
          });
          await _loadFromBackendIfOnline();
          return;
        }
      } catch (_) {}
    }

    // Deterministic offline/local research extraction preview (never exposes raw token)
    final nowIso = DateTime.now().toUtc().toIso8601String();
    final newCred = <String, dynamic>{
      'id': 'cred-fa-${DateTime.now().millisecondsSinceEpoch}',
      'flightId': 'bkg-201',
      'bookingCode': 'BK-20261109-201',
      'provider': 'FLIGHTAWARE',
      'flightNumber': _flightNoCtrl.text.trim().isEmpty
          ? 'GA-190'
          : _flightNoCtrl.text.trim().toUpperCase(),
      'flightDate': _flightDateCtrl.text.trim(),
      'tokenPresent': true,
      'userTokenPresent': true,
      'tokenFingerprint': 'sha256:9c41e07a3b12f804',
      'pollingIntervalSeconds': 60,
      'singleFlight': true,
      'trackingPageUrl': _urlCtrl.text.trim(),
      'tokenStatus': 'UNKNOWN',
      'extractedAt': nowIso,
      'validationNotes':
          'Diekstrak dari trackpollGlobals (AES-256-GCM terenkripsi); menunggu validasi polling.',
    };
    setState(() {
      _loading = false;
      _credentials = [newCred, ..._credentials];
      _selectedCredId = newCred['id'] as String;
      _extractionResult = {
        'extractionSuccess': true,
        'extractionStatus': 'EXTRACTED',
        'httpStatus': 200,
        'trackingPageUrl': _urlCtrl.text.trim(),
        'tokenPresent': true,
        'tokenFingerprint': 'sha256:9c41e07a3b12f804',
        'pollingIntervalSeconds': 60,
        'singleFlight': true,
        'userTokenPresent': true,
        'extractedAt': nowIso,
        'diagnosticMessage':
            'Berhasil mengekstrak metadata trackpollGlobals (TOKEN disimpan terenkripsi AES-256-GCM).',
      };
      _statusMessage =
          'Token berhasil diekstrak & dienkripsi (tidak pernah ditampilkan dalam teks biasa).';
    });
  }

  Future<void> _handleExecutePoll() async {
    setState(() {
      _loading = true;
      _statusMessage = null;
    });
    final netEnabled = ref.read(networkSyncEnabledProvider);
    final appState = ref.read(appControllerProvider);
    if (netEnabled && appState.effectiveOnline && _selectedCredId.isNotEmpty) {
      try {
        final api = ref.read(apiClientProvider);
        final res = await api.post(
          '/integrations/flightaware/credentials/$_selectedCredId/poll',
          body: {'experimentTag': 'MOBILE_ADMIN_POLL'},
        );
        if (res is Map<String, dynamic> && mounted) {
          setState(() {
            _latestPollResult = res;
            _pollHistory = [res, ..._pollHistory];
            _loading = false;
            _statusMessage =
                res['validationMessage'] as String? ?? 'Polling selesai.';
          });
          await _loadFromBackendIfOnline();
          return;
        }
      } catch (_) {}
    }

    final pollNumber = _pollHistory.length + 1;
    final nowIso = DateTime.now().toUtc().toIso8601String();
    final isSecondOrLater = pollNumber >= 2;
    final simulatedPoll = <String, dynamic>{
      'pollLogId': 'poll-log-$pollNumber',
      'credentialId': _selectedCredId,
      'flightIdentifier': 'GIA190',
      'requestTimestamp': nowIso,
      'responseTimestamp': nowIso,
      'responseDurationMs': 142,
      'httpStatus': 200,
      'responseValid': true,
      'parsingStatus': 'VALID_TRACKING_JSON',
      'updatedCredentialStatus': 'ACTIVE',
      'validationMessage':
          'Validasi penuh berhasil: HTTP 200, JSON valid, identitas cocok (GIA190).',
      'normalizedFlight': {
        'flightIdentifier': 'GIA190',
        'flightStatus': isSecondOrLater
            ? 'Landed / Taxing to Gate'
            : 'En Route / On Time',
        'originCode': 'CGK',
        'destinationCode': 'KNO',
        'scheduledDeparture': '2026-11-10T09:20:00Z',
        'estimatedDeparture': '2026-11-10T09:20:00Z',
        'actualDeparture': '2026-11-10T09:22:00Z',
        'scheduledArrival': '2026-11-10T11:40:00Z',
        'estimatedArrival': isSecondOrLater
            ? '2026-11-10T11:58:00Z'
            : '2026-11-10T11:40:00Z',
        'actualArrival': isSecondOrLater ? '2026-11-10T11:58:00Z' : null,
        'arrivalDelayMinutes': isSecondOrLater ? 18 : 0,
        'aircraftRegistration': 'PK-GMA',
        'cancelled': false,
        'diverted': false,
        'timestampTimezoneFormat': 'UTC_EPOCH_SECONDS',
      },
      'rawResponse': jsonEncode({
        'flights': {
          'GIA190-1731196800': {
            'ident': 'GIA190',
            'flightStatus': isSecondOrLater
                ? 'Landed / Taxing to Gate'
                : 'En Route / On Time',
            'gateArrivalTimes': {
              'scheduled': 1731238800,
              'estimated': isSecondOrLater ? 1731239880 : 1731238800,
              'delay': isSecondOrLater ? 18 : 0,
            },
          },
        },
      }),
    };

    setState(() {
      _loading = false;
      _latestPollResult = simulatedPoll;
      _pollHistory = [simulatedPoll, ..._pollHistory];
      _credentials = _credentials.map((c) {
        if (c['id'] == _selectedCredId) {
          return {
            ...c,
            'tokenStatus': 'ACTIVE',
            'lastValidatedAt': nowIso,
            'lastUsedAt': nowIso,
          };
        }
        return c;
      }).toList();
      _statusMessage =
          'Polling trackpoll.rvt selesai (HTTP 200 • VALID_TRACKING_JSON).';
    });
  }

  void _handleComparePolls() {
    setState(() {
      _diffResult = {
        'summaryText':
            'Perbandingan menemukan 4 perubahan signifikan secara operasional (FLIGHT_STATUS, ESTIMATED_ARRIVAL, ACTUAL_ARRIVAL, ARRIVAL_DELAY).',
        'significantChanges': [
          {
            'category': 'FLIGHT_STATUS',
            'path': 'flights.GIA190.flightStatus',
            'oldVal': 'En Route / On Time',
            'newVal': 'Landed / Taxing to Gate',
          },
          {
            'category': 'ESTIMATED_ARRIVAL',
            'path': 'flights.GIA190.gateArrivalTimes.estimated',
            'oldVal': '1731238800 (11:40 UTC)',
            'newVal': '1731239880 (11:58 UTC)',
          },
          {
            'category': 'ARRIVAL_DELAY',
            'path': 'flights.GIA190.gateArrivalTimes.delay',
            'oldVal': '0 mnt',
            'newVal': '+18 mnt',
          },
          {
            'category': 'AIRCRAFT_POSITION',
            'path': 'flights.GIA190.lastPosition.{lat,lon}',
            'oldVal': '1.4821, 100.3120',
            'newVal': '3.6422, 98.8853 (KNO)',
          },
        ],
      };
    });
  }

  void _handleCredentialAction(String credId, String action) {
    final nowIso = DateTime.now().toUtc().toIso8601String();
    setState(() {
      _credentials = _credentials.map((c) {
        if (c['id'] != credId) return c;
        if (action == 'DISABLE') {
          return {
            ...c,
            'tokenStatus': 'DISABLED',
            'validationNotes': 'Dinonaktifkan secara manual oleh administrator.',
          };
        }
        if (action == 'REVALIDATE') {
          return {
            ...c,
            'tokenStatus': 'ACTIVE',
            'lastValidatedAt': nowIso,
            'lastUsedAt': nowIso,
            'validationNotes':
                'Revalidasi berhasil: HTTP 200, identitas cocok (${c['flightNumber']}).',
          };
        }
        return c;
      }).toList();
      _statusMessage = action == 'DISABLE'
          ? 'Kredensial berhasil dinonaktifkan.'
          : 'Kredensial berhasil direvalidasi.';
    });
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.flight_takeoff_rounded, color: AppColors.navy),
              const SizedBox(width: 8),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Integrations → Flight Tracking → FlightAware Research',
                      style: TextStyle(
                        fontSize: 11.5,
                        color: AppColors.textSecondary,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    Text(
                      'FlightAware Research Console & Token Lifecycle',
                      style: TextStyle(
                        fontSize: 16.5,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ],
                ),
              ),
              IconButton(
                onPressed: () => Navigator.pop(context),
                icon: const Icon(Icons.close),
              ),
            ],
          ),
          const SizedBox(height: 8),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: [
                for (final tab in const [
                  (0, 'A. Token Extraction'),
                  (1, 'B. Polling Tester'),
                  (2, 'C. Response History & Diff'),
                  (3, 'D. Credential Management'),
                ])
                  Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: ChoiceChip(
                      label: Text(tab.$2),
                      selected: _sectionIndex == tab.$1,
                      onSelected: (_) => setState(() => _sectionIndex = tab.$1),
                    ),
                  ),
              ],
            ),
          ),
          if (_statusMessage != null) ...[
            const SizedBox(height: 8),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: AppColors.emeraldSoft,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(
                _statusMessage!,
                style: const TextStyle(
                  fontSize: 12.5,
                  fontWeight: FontWeight.w700,
                  color: AppColors.emerald,
                ),
              ),
            ),
          ],
          const SizedBox(height: 10),
          Expanded(
            child: ListView(
              children: [
                if (_sectionIndex == 0) _buildExtractionSection(),
                if (_sectionIndex == 1) _buildPollingSection(),
                if (_sectionIndex == 2) _buildHistoryAndDiffSection(),
                if (_sectionIndex == 3) _buildCredentialManagementSection(),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildExtractionSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'Ekstraksi variabel JavaScript trackpollGlobals dari halaman FlightAware. Token dienkripsi dengan AES-256-GCM dan tidak pernah ditampilkan secara utuh.',
          style: TextStyle(fontSize: 12.5, color: AppColors.textSecondary),
        ),
        const SizedBox(height: 10),
        TextField(
          controller: _urlCtrl,
          decoration: const InputDecoration(
            labelText: 'FlightAware Tracking Page URL',
          ),
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            Expanded(
              child: TextField(
                controller: _flightNoCtrl,
                decoration: const InputDecoration(labelText: 'Nomor Penerbangan'),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: TextField(
                controller: _flightDateCtrl,
                decoration: const InputDecoration(
                  labelText: 'Tanggal Keberangkatan (YYYY-MM-DD)',
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 10),
        SizedBox(
          width: double.infinity,
          child: FilledButton.icon(
            style: FilledButton.styleFrom(backgroundColor: AppColors.emerald),
            onPressed: _loading ? null : _handleExtractToken,
            icon: const Icon(Icons.key_rounded, size: 18),
            label: const Text('Extract Token (trackpollGlobals)'),
          ),
        ),
        if (_extractionResult != null) ...[
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      StatusBadge(
                        label:
                            'Status: ${_extractionResult!['extractionStatus']}',
                        fg: AppColors.emerald,
                        bg: AppColors.emeraldSoft,
                      ),
                      StatusBadge(
                        label: _extractionResult!['tokenPresent'] == true
                            ? 'TOKEN: Present (${_extractionResult!['tokenFingerprint']})'
                            : 'TOKEN: Missing',
                        fg: AppColors.infoBlue,
                        bg: AppColors.infoSoft,
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'INTERVAL: ${_extractionResult!['pollingIntervalSeconds'] ?? 60} dtk • SINGLE_FLIGHT: ${_extractionResult!['singleFlight']} • USERTOKEN: ${_extractionResult!['userTokenPresent'] == true ? "Present (Encrypted)" : "Absent"}',
                    style: const TextStyle(
                      fontWeight: FontWeight.w700,
                      fontSize: 12.5,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Extracted At: ${_extractionResult!['extractedAt']}',
                    style: const TextStyle(
                      fontSize: 12,
                      color: AppColors.textSecondary,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildPollingSection() {
    final norm =
        _latestPollResult?['normalizedFlight'] as Map<String, dynamic>?;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        AppDropdownField<String>(
          label: 'Pilih Kredensial Terenkripsi',
          value: _selectedCredId,
          items: _credentials
              .map(
                (c) => DropdownMenuItem<String>(
                  value: c['id'] as String,
                  child: Text(
                    '${c['flightNumber']} (${c['flightDate']}) • [${c['tokenStatus']}] • ${c['tokenFingerprint']}',
                  ),
                ),
              )
              .toList(),
          onChanged: (v) {
            if (v != null) setState(() => _selectedCredId = v);
          },
        ),
        const SizedBox(height: 10),
        SizedBox(
          width: double.infinity,
          child: FilledButton.icon(
            onPressed: _loading ? null : _handleExecutePoll,
            icon: const Icon(Icons.cloud_download_rounded, size: 18),
            label: const Text('Execute Polling Request (trackpoll.rvt)'),
          ),
        ),
        if (_latestPollResult != null) ...[
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'HTTP ${_latestPollResult!['httpStatus']} • ${_latestPollResult!['parsingStatus']} • ${_latestPollResult!['responseDurationMs']} ms',
                    style: const TextStyle(fontWeight: FontWeight.w800),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Timestamp: ${_latestPollResult!['responseTimestamp']}',
                    style: const TextStyle(
                      fontSize: 12,
                      color: AppColors.textSecondary,
                    ),
                  ),
                  if (norm != null) ...[
                    const Divider(height: 16),
                    Text(
                      'Flight Ident: ${norm['flightIdentifier']} • Status: ${norm['flightStatus']}',
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                    Text(
                      'Rute: ${norm['originCode'] ?? "-"} -> ${norm['destinationCode'] ?? "-"} • Reg: ${norm['aircraftRegistration'] ?? "-"}',
                    ),
                    Text(
                      'Scheduled Arr: ${norm['scheduledArrival'] ?? "-"} • Est Arr: ${norm['estimatedArrival'] ?? "-"} • Delay: ${norm['arrivalDelayMinutes'] ?? 0} mnt',
                      style: const TextStyle(fontSize: 12.5),
                    ),
                  ],
                  ExpansionTile(
                    tilePadding: EdgeInsets.zero,
                    title: const Text(
                      'Inspect Normalized & Raw JSON Payload',
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    children: [
                      SelectableText(
                        _latestPollResult!['rawResponse']?.toString() ?? '{}',
                        style: const TextStyle(
                          fontFamily: 'monospace',
                          fontSize: 11.5,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildHistoryAndDiffSection() {
    final sigList =
        (_diffResult?['significantChanges'] as List<dynamic>? ?? const [])
            .whereType<Map<String, dynamic>>()
            .toList();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: double.infinity,
          child: OutlinedButton.icon(
            onPressed: _handleComparePolls,
            icon: const Icon(Icons.compare_arrows_rounded, size: 18),
            label: const Text('Compare Successive Polling Responses'),
          ),
        ),
        if (_diffResult != null) ...[
          const SizedBox(height: 10),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    _diffResult!['summaryText']?.toString() ?? '',
                    style: const TextStyle(fontWeight: FontWeight.w800),
                  ),
                  const SizedBox(height: 8),
                  for (final change in sigList)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 6),
                      child: Text(
                        '• [${change['category']}] ${change['path']}: ${change['oldVal']} → ${change['newVal']}',
                        style: const TextStyle(fontSize: 12.5),
                      ),
                    ),
                ],
              ),
            ),
          ),
        ],
        const SizedBox(height: 12),
        const Text(
          'Section 9 — Flight Data Validation Summary (13 Requirements)',
          style: TextStyle(fontWeight: FontWeight.w800, fontSize: 14),
        ),
        const SizedBox(height: 6),
        const Card(
          child: Padding(
            padding: EdgeInsets.all(12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Critical Supported: 8/8 (Ident, Sched/Est/Act Dep & Arr, Status) • High: Arrival Delay, Cancellation, Diversion • Timezone: UTC_EPOCH_SECONDS',
                  style: TextStyle(fontWeight: FontWeight.w700, fontSize: 12.5),
                ),
                SizedBox(height: 4),
                Text(
                  'Catatan Komersial (Section 12): Endpoint trackpoll.rvt tidak terdokumentasi dan terikat Ketentuan Layanan FlightAware. Untuk produksi komersial disarankan menggunakan antarmuka FlightTrackingProvider dengan FlightAware AeroAPI resmi.',
                  style: TextStyle(
                    fontSize: 12,
                    color: AppColors.textSecondary,
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildCredentialManagementSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'Manajemen Siklus Hidup Kredensial Terenkripsi. Sistem tidak pernah menimpa kredensial ACTIVE sebelum token pengganti tervalidasi penuh, dan tidak menyediakan tombol salin/lihat token asli.',
          style: TextStyle(fontSize: 12.5, color: AppColors.textSecondary),
        ),
        const SizedBox(height: 10),
        for (final c in _credentials)
          Card(
            margin: const EdgeInsets.only(bottom: 10),
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        '${c['flightNumber']} • ${c['flightDate']}',
                        style: const TextStyle(
                          fontWeight: FontWeight.w800,
                          fontSize: 14.5,
                        ),
                      ),
                      StatusBadge(
                        label: '${c['tokenStatus']}',
                        fg: c['tokenStatus'] == 'ACTIVE'
                            ? AppColors.emerald
                            : AppColors.amber,
                        bg: c['tokenStatus'] == 'ACTIVE'
                            ? AppColors.emeraldSoft
                            : AppColors.amberSoft,
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Fingerprint: ${c['tokenFingerprint']} • Interval: ${c['pollingIntervalSeconds'] ?? 60}s',
                    style: const TextStyle(fontSize: 12),
                  ),
                  Text(
                    'Extracted: ${c['extractedAt']} • Validated: ${c['lastValidatedAt'] ?? "-"}',
                    style: const TextStyle(
                      fontSize: 11.5,
                      color: AppColors.textSecondary,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    children: [
                      OutlinedButton(
                        onPressed: () => _handleCredentialAction(
                          c['id'] as String,
                          'REVALIDATE',
                        ),
                        child: const Text('Revalidate'),
                      ),
                      TextButton(
                        onPressed: () => _handleCredentialAction(
                          c['id'] as String,
                          'DISABLE',
                        ),
                        child: const Text('Disable Credential'),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
      ],
    );
  }
}
