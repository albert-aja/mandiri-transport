import 'dart:async';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geolocator/geolocator.dart';

import '../../core/network/api_client.dart';
import '../../core/notifications/fcm_push_service.dart';
import '../../core/utils/deep_links.dart';
import '../local/medan_seed_data.dart';
import '../local/offline_store.dart';
import '../models/models.dart';

final offlineStoreProvider = Provider<OfflineStore>((ref) {
  throw UnimplementedError('OfflineStore must be initialized before runApp');
});

final networkSyncEnabledProvider = Provider<bool>((ref) => true);

final fcmPushClientProvider = Provider<FcmPushClient>((ref) {
  final client = FirebaseLivePushClient();
  ref.onDispose(client.dispose);
  return client;
});

final apiClientProvider = Provider<ApiClient>((ref) {
  final store = ref.watch(offlineStoreProvider);
  final session = store.loadSession();
  return ApiClient(
    baseUrl: store.loadBaseUrl(),
    jwtToken: session?.token,
  );
});

class AppState {
  final UserSession? session;
  final String baseUrl;
  final bool deviceOnline;
  final bool forceOfflineMode;
  final bool serverReachable;
  final bool isSyncing;
  final String? lastSyncedAt;
  final List<Driver> drivers;
  final List<Vehicle> vehicles;
  final List<RouteInfo> routes;
  final List<Customer> customers;
  final List<WhatsappTemplate> whatsappTemplates;
  final List<BookingTemplate> bookingTemplates;
  final List<CostFormulaVersion> costFormulas;
  final List<Trip> trips;
  final List<CashHandover> cashHandovers;
  final List<LostAndFoundItem> lostAndFoundItems;
  final List<NotificationItem> notifications;
  final List<NotificationSettingItem> notificationSettings;
  final List<SyncMutation> outbox;
  final List<GpsPointRecord> gpsQueue;
  final List<StagedSyncConflict> stagedConflicts;
  final int ownerTabIndex;
  final int ownerFinanceSubTab;
  final ({String tripId, String previousStatus})? undoSnapshot;
  final LiveFcmState fcmState;

  const AppState({
    required this.session,
    required this.baseUrl,
    this.deviceOnline = true,
    this.forceOfflineMode = false,
    this.serverReachable = false,
    this.isSyncing = false,
    this.lastSyncedAt,
    required this.drivers,
    required this.vehicles,
    required this.routes,
    required this.customers,
    required this.whatsappTemplates,
    required this.bookingTemplates,
    required this.costFormulas,
    required this.trips,
    required this.cashHandovers,
    required this.lostAndFoundItems,
    required this.notifications,
    required this.notificationSettings,
    required this.outbox,
    required this.gpsQueue,
    required this.stagedConflicts,
    this.ownerTabIndex = 0,
    this.ownerFinanceSubTab = 0,
    this.undoSnapshot,
    this.fcmState = const LiveFcmState(),
  });

  bool get effectiveOnline => deviceOnline && !forceOfflineMode;

  double get totalDriverHeldCash =>
      drivers.fold<double>(0, (sum, d) => sum + d.cashHeldAmount);

  List<StagedSyncConflict> get unresolvedConflicts =>
      stagedConflicts.where((c) => !c.resolved).toList();

  List<Trip> get visibleTrips {
    final s = session;
    if (s == null) return const [];
    if (s.isOwner) return trips;
    return trips.where((t) => t.driverId == s.driverId).toList();
  }

  Driver? get currentDriverProfile {
    final dId = session?.driverId;
    if (dId == null) return null;
    return drivers.where((d) => d.id == dId).firstOrNull;
  }

  AppState copyWith({
    UserSession? session,
    bool clearSession = false,
    String? baseUrl,
    bool? deviceOnline,
    bool? forceOfflineMode,
    bool? serverReachable,
    bool? isSyncing,
    String? lastSyncedAt,
    List<Driver>? drivers,
    List<Vehicle>? vehicles,
    List<RouteInfo>? routes,
    List<Customer>? customers,
    List<WhatsappTemplate>? whatsappTemplates,
    List<BookingTemplate>? bookingTemplates,
    List<CostFormulaVersion>? costFormulas,
    List<Trip>? trips,
    List<CashHandover>? cashHandovers,
    List<LostAndFoundItem>? lostAndFoundItems,
    List<NotificationItem>? notifications,
    List<NotificationSettingItem>? notificationSettings,
    List<SyncMutation>? outbox,
    List<GpsPointRecord>? gpsQueue,
    List<StagedSyncConflict>? stagedConflicts,
    int? ownerTabIndex,
    int? ownerFinanceSubTab,
    ({String tripId, String previousStatus})? undoSnapshot,
    bool clearUndo = false,
    LiveFcmState? fcmState,
  }) {
    return AppState(
      session: clearSession ? null : (session ?? this.session),
      baseUrl: baseUrl ?? this.baseUrl,
      deviceOnline: deviceOnline ?? this.deviceOnline,
      forceOfflineMode: forceOfflineMode ?? this.forceOfflineMode,
      serverReachable: serverReachable ?? this.serverReachable,
      isSyncing: isSyncing ?? this.isSyncing,
      lastSyncedAt: lastSyncedAt ?? this.lastSyncedAt,
      drivers: drivers ?? this.drivers,
      vehicles: vehicles ?? this.vehicles,
      routes: routes ?? this.routes,
      customers: customers ?? this.customers,
      whatsappTemplates: whatsappTemplates ?? this.whatsappTemplates,
      bookingTemplates: bookingTemplates ?? this.bookingTemplates,
      costFormulas: costFormulas ?? this.costFormulas,
      trips: trips ?? this.trips,
      cashHandovers: cashHandovers ?? this.cashHandovers,
      lostAndFoundItems: lostAndFoundItems ?? this.lostAndFoundItems,
      notifications: notifications ?? this.notifications,
      notificationSettings: notificationSettings ?? this.notificationSettings,
      outbox: outbox ?? this.outbox,
      gpsQueue: gpsQueue ?? this.gpsQueue,
      stagedConflicts: stagedConflicts ?? this.stagedConflicts,
      ownerTabIndex: ownerTabIndex ?? this.ownerTabIndex,
      ownerFinanceSubTab: ownerFinanceSubTab ?? this.ownerFinanceSubTab,
      undoSnapshot: clearUndo ? null : (undoSnapshot ?? this.undoSnapshot),
      fcmState: fcmState ?? this.fcmState,
    );
  }
}

final appControllerProvider = NotifierProvider<AppController, AppState>(
  AppController.new,
);

class AppController extends Notifier<AppState> {
  StreamSubscription<List<ConnectivityResult>>? _connectivitySub;
  StreamSubscription<Position>? _gpsSub;
  StreamSubscription<String>? _fcmTokenSub;
  StreamSubscription<IncomingPushPayload>? _fcmFgSub;
  StreamSubscription<IncomingPushPayload>? _fcmOpenSub;
  String _fcmPlatform = 'ANDROID';
  String _fcmDeviceLabel = 'Mandiri Flutter';
  bool _disposed = false;

  OfflineStore get _store => ref.read(offlineStoreProvider);
  ApiClient get _api => ref.read(apiClientProvider);
  bool get _networkEnabled => ref.read(networkSyncEnabledProvider);
  FcmPushClient get _fcmClient => ref.read(fcmPushClientProvider);

  @override
  AppState build() {
    _disposed = false;
    final store = ref.watch(offlineStoreProvider);
    ref.onDispose(() {
      _disposed = true;
      _connectivitySub?.cancel();
      _gpsSub?.cancel();
      _fcmTokenSub?.cancel();
      _fcmFgSub?.cancel();
      _fcmOpenSub?.cancel();
    });

    if (_networkEnabled) {
      _initConnectivityListener();
    }
    Future.microtask(initializeLiveFcm);

    return AppState(
      session: store.loadSession(),
      baseUrl: store.loadBaseUrl(),
      lastSyncedAt: store.loadLastSyncedAt(),
      drivers: store.loadDrivers(),
      vehicles: store.loadVehicles(),
      routes: store.loadRoutes(),
      customers: store.loadCustomers(),
      whatsappTemplates: store.loadWhatsappTemplates(),
      bookingTemplates: store.loadBookingTemplates(),
      costFormulas: store.loadCostFormulas(),
      trips: store.loadTrips(),
      cashHandovers: store.loadCashHandovers(),
      lostAndFoundItems: store.loadLostAndFoundItems(),
      notifications: store.loadNotifications(),
      notificationSettings: store.loadNotificationSettings(),
      outbox: store.loadOutbox(),
      gpsQueue: store.loadGpsQueue(),
      stagedConflicts: store.loadStagedConflicts(),
      fcmState: LiveFcmState(deviceToken: store.loadFcmDeviceToken()),
    );
  }

  void _initConnectivityListener() {
    Future.microtask(() async {
      try {
        final initial = await Connectivity().checkConnectivity();
        if (_disposed) return;
        final online = initial.any((r) => r != ConnectivityResult.none);
        state = state.copyWith(deviceOnline: online);
        _connectivitySub = Connectivity().onConnectivityChanged.listen((
          results,
        ) {
          if (_disposed) return;
          final isNowOnline = results.any((r) => r != ConnectivityResult.none);
          final wasOffline = !state.deviceOnline;
          state = state.copyWith(deviceOnline: isNowOnline);
          if (isNowOnline && wasOffline && state.outbox.isNotEmpty) {
            unawaited(syncNow());
          }
        });
      } on MissingPluginException {
        // Safe in unit/widget test environments without platform channels
      } catch (_) {
        // Ignore connectivity listener errors gracefully
      }
    });
  }

  // --- Live Firebase Cloud Messaging (FCM) Lifecycle ---

  Future<void> initializeLiveFcm() async {
    if (_disposed) return;
    final runtimeCfg = _store.loadFcmRuntimeConfig();
    final outcome = await _fcmClient.initialize(runtimeConfig: runtimeCfg);
    if (_disposed) return;

    _fcmPlatform = outcome.platform;
    _fcmDeviceLabel = outcome.deviceLabel;

    String? resolvedToken = _store.loadFcmDeviceToken();
    if (outcome.token != null && outcome.token!.trim().isNotEmpty) {
      resolvedToken = outcome.token!.trim();
      await _store.saveFcmDeviceToken(resolvedToken);
      if (_disposed) return;
    }

    state = state.copyWith(
      fcmState: state.fcmState.copyWith(
        clientInitialized: outcome.initialized,
        permissionGranted: outcome.permissionGranted,
        deviceToken: resolvedToken,
        statusSummary: outcome.statusMessage,
      ),
    );

    await _fcmTokenSub?.cancel();
    await _fcmFgSub?.cancel();
    await _fcmOpenSub?.cancel();
    if (_disposed) return;

    _fcmTokenSub = _fcmClient.onTokenRefresh.listen((newToken) async {
      if (_disposed || newToken.trim().isEmpty) return;
      final trimmed = newToken.trim();
      await _store.saveFcmDeviceToken(trimmed);
      if (_disposed) return;
      state = state.copyWith(
        fcmState: state.fcmState.copyWith(
          deviceToken: trimmed,
          tokenSyncedToBackend: false,
        ),
      );
      unawaited(_syncFcmTokenToBackend(trimmed));
    });

    _fcmFgSub = _fcmClient.onForegroundMessage.listen((payload) async {
      if (_disposed) return;
      await _store.prependIncomingRemoteNotification(
        id: payload.id,
        reminderType: payload.reminderType,
        title: payload.title,
        body: payload.body,
        referenceType: payload.referenceType,
        referenceId: payload.referenceId,
      );
      if (_disposed) return;
      state = state.copyWith(
        notifications: _store.loadNotifications(),
        fcmState: state.fcmState.copyWith(
          liveBannerTitle: payload.title,
          liveBannerBody: payload.body,
          liveBannerRefType: payload.referenceType ?? payload.reminderType,
          liveBannerRefId: payload.referenceId,
        ),
      );
    });

    _fcmOpenSub = _fcmClient.onNotificationOpened.listen((payload) async {
      if (_disposed) return;
      await _store.prependIncomingRemoteNotification(
        id: payload.id,
        reminderType: payload.reminderType,
        title: payload.title,
        body: payload.body,
        referenceType: payload.referenceType,
        referenceId: payload.referenceId,
      );
      if (_disposed) return;
      state = state.copyWith(
        notifications: _store.loadNotifications(),
        fcmState: state.fcmState.copyWith(clearBanner: true),
      );
      openPushTarget(
        referenceType: payload.referenceType,
        reminderType: payload.reminderType,
      );
    });

    if (state.session != null && _networkEnabled && state.effectiveOnline) {
      if (resolvedToken != null && resolvedToken.isNotEmpty) {
        await _syncFcmTokenToBackend(resolvedToken);
      }
      await refreshFcmServerStatus();
    }
  }

  Future<void> _syncFcmTokenToBackend(String token) async {
    if (_disposed ||
        !_networkEnabled ||
        !state.effectiveOnline ||
        state.session == null ||
        token.trim().isEmpty) {
      return;
    }
    try {
      await _api.post(
        '/auth/fcm-token',
        body: {
          'fcmToken': token.trim(),
          'platform': _fcmPlatform,
          'deviceLabel': _fcmDeviceLabel,
        },
      );
      if (_disposed) return;
      state = state.copyWith(
        serverReachable: true,
        fcmState: state.fcmState.copyWith(tokenSyncedToBackend: true),
      );
    } catch (_) {
      // Keep local token safely; will retry on next sync/login
    }
  }

  void dismissLivePushBanner() {
    state = state.copyWith(
      fcmState: state.fcmState.copyWith(clearBanner: true),
    );
  }

  void openPushTarget({String? referenceType, String? reminderType}) {
    state = state.copyWith(
      fcmState: state.fcmState.copyWith(clearBanner: true),
    );
    final session = state.session;
    if (session == null || !session.isOwner) return;

    final refUpper = (referenceType ?? '').toUpperCase();
    final remUpper = (reminderType ?? '').toUpperCase();

    if (refUpper == 'TRIP' || remUpper == 'UPCOMING_PICKUP') {
      setOwnerTab(1);
    } else if (refUpper == 'DRIVER' ||
        remUpper == 'OUTSTANDING_CASH_HANDOVER') {
      setOwnerTab(3, financeSubTab: 0);
    } else if (refUpper == 'VEHICLE_DOCUMENT' ||
        refUpper == 'MAINTENANCE_SCHEDULE' ||
        remUpper.contains('EXPIRY') ||
        remUpper == 'SCHEDULED_MAINTENANCE') {
      setOwnerTab(3, financeSubTab: 2);
    } else {
      setOwnerTab(4);
    }
  }

  Future<void> refreshFcmServerStatus() async {
    if (_disposed ||
        !_networkEnabled ||
        !state.effectiveOnline ||
        state.session == null) {
      return;
    }
    try {
      final res = await _api.get('/notifications/fcm-status');
      if (_disposed) return;
      if (res is Map<String, dynamic>) {
        state = state.copyWith(
          serverReachable: true,
          fcmState: state.fcmState.copyWith(
            backendLiveReady: res['liveCredentialsReady'] == true,
            backendProjectId:
                (res['projectId'] as String?) ?? state.fcmState.backendProjectId,
            myActiveDevicesCount:
                (res['myActiveDevicesCount'] as num?)?.toInt() ?? 0,
            fleetActiveDevicesCount:
                (res['fleetActiveDevicesCount'] as num?)?.toInt() ?? 0,
            statusSummary:
                (res['statusSummary'] as String?) ??
                state.fcmState.statusSummary,
          ),
        );
      }
    } catch (_) {}
  }

  Future<String> sendLiveTestPush({String? title, String? body}) async {
    final resolvedTitle =
        title ?? 'Uji Push Live — Mandiri Transport';
    final resolvedBody =
        body ??
        'Koneksi Firebase Cloud Messaging & kotak masuk operasional berfungsi normal.';

    if (_networkEnabled && state.effectiveOnline && state.session != null) {
      try {
        final token = state.fcmState.deviceToken;
        if (token != null &&
            token.isNotEmpty &&
            !state.fcmState.tokenSyncedToBackend) {
          await _syncFcmTokenToBackend(token);
        }
        final res = await _api.post(
          '/notifications/test-push',
          body: {'title': resolvedTitle, 'body': resolvedBody},
        );
        if (_disposed) return '';
        if (res is Map<String, dynamic>) {
          final notifId =
              (res['notificationId'] as String?) ??
              'test-${DateTime.now().millisecondsSinceEpoch}';
          final diag =
              (res['diagnosticMessage'] as String?) ??
              'Uji push berhasil diproses server.';
          await _store.prependIncomingRemoteNotification(
            id: notifId,
            reminderType: 'FCM_TEST_PUSH',
            title: resolvedTitle,
            body: resolvedBody,
            referenceType: 'SYSTEM_FCM_TEST',
            referenceId: notifId,
          );
          if (_disposed) return diag;
          await refreshFcmServerStatus();
          if (_disposed) return diag;
          state = state.copyWith(
            notifications: _store.loadNotifications(),
            fcmState: state.fcmState.copyWith(
              liveBannerTitle: resolvedTitle,
              liveBannerBody: resolvedBody,
              liveBannerRefType: 'SYSTEM_FCM_TEST',
              liveBannerRefId: notifId,
            ),
          );
          return diag;
        }
      } catch (_) {
        // Fallback to local inbox test push when backend is offline
      }
    }

    final fallbackId = 'local-test-${DateTime.now().millisecondsSinceEpoch}';
    await _store.prependIncomingRemoteNotification(
      id: fallbackId,
      reminderType: 'FCM_TEST_PUSH',
      title: resolvedTitle,
      body: resolvedBody,
      referenceType: 'SYSTEM_FCM_TEST',
      referenceId: fallbackId,
    );
    if (!_disposed) {
      state = state.copyWith(
        notifications: _store.loadNotifications(),
        fcmState: state.fcmState.copyWith(
          liveBannerTitle: resolvedTitle,
          liveBannerBody: resolvedBody,
          liveBannerRefType: 'SYSTEM_FCM_TEST',
          liveBannerRefId: fallbackId,
        ),
      );
    }
    return 'Mode Offline/Lokal: Notifikasi uji masuk ke kotak masuk & banner aplikasi.';
  }

  Future<String> triggerOperationalReminderScan() async {
    if (_networkEnabled && state.effectiveOnline && state.session != null) {
      try {
        final res = await _api.post('/notifications/scan-reminders');
        if (_disposed) return '';
        final count = (res is Map<String, dynamic>)
            ? ((res['generatedCount'] as num?)?.toInt() ?? 0)
            : 0;
        await syncNow();
        return 'Pemindaian pengingat selesai ($count pengingat baru dikirim).';
      } catch (_) {}
    }
    return 'Pemindaian server belum terjangkau — pengingat lokal tetap aktif.';
  }

  Future<String> saveRuntimeFirebaseConfig(FcmRuntimeConfig? config) async {
    await _store.saveFcmRuntimeConfig(config);
    await initializeLiveFcm();
    return config != null && config.isComplete
        ? 'Konfigurasi FirebaseOptions disimpan & diinisialisasi ulang.'
        : 'Konfigurasi runtime dibersihkan (kembali ke google-services.json bawaan).';
  }

  void _reloadFromStore() {
    state = state.copyWith(
      drivers: _store.loadDrivers(),
      vehicles: _store.loadVehicles(),
      routes: _store.loadRoutes(),
      customers: _store.loadCustomers(),
      whatsappTemplates: _store.loadWhatsappTemplates(),
      bookingTemplates: _store.loadBookingTemplates(),
      costFormulas: _store.loadCostFormulas(),
      trips: _store.loadTrips(),
      cashHandovers: _store.loadCashHandovers(),
      lostAndFoundItems: _store.loadLostAndFoundItems(),
      notifications: _store.loadNotifications(),
      notificationSettings: _store.loadNotificationSettings(),
      outbox: _store.loadOutbox(),
      gpsQueue: _store.loadGpsQueue(),
      stagedConflicts: _store.loadStagedConflicts(),
      lastSyncedAt: _store.loadLastSyncedAt(),
    );
  }

  // --- Navigation ---

  void setOwnerTab(int index, {int? financeSubTab}) {
    state = state.copyWith(
      ownerTabIndex: index,
      ownerFinanceSubTab: financeSubTab ?? state.ownerFinanceSubTab,
    );
  }

  // --- Auth & Server Configuration ---

  Future<void> updateServerUrl(String url) async {
    final cleaned = url.trim();
    if (cleaned.isEmpty) return;
    await _store.saveBaseUrl(cleaned);
    _api.updateConfig(baseUrl: cleaned, jwtToken: state.session?.token);
    state = state.copyWith(baseUrl: cleaned);
  }

  void toggleForceOfflineMode(bool forceOffline) {
    state = state.copyWith(forceOfflineMode: forceOffline);
    if (!forceOffline && state.effectiveOnline && state.outbox.isNotEmpty) {
      unawaited(syncNow());
    }
  }

  Future<void> loginDemoAccount(String username) async {
    UserSession? resolvedSession;
    if (_networkEnabled && state.effectiveOnline) {
      try {
        final data = await _api.post(
          '/auth/dev-login',
          body: {'username': username},
        );
        if (data is Map<String, dynamic>) {
          resolvedSession = UserSession.fromJson(data);
          state = state.copyWith(serverReachable: true);
        }
      } catch (_) {
        state = state.copyWith(serverReachable: false);
      }
    }

    resolvedSession ??= MedanSeedData.demoAccounts().firstWhere(
      (a) => a.username == username,
      orElse: () => MedanSeedData.demoAccounts().first,
    );

    await _store.saveSession(resolvedSession);
    _api.updateConfig(jwtToken: resolvedSession.token);
    state = state.copyWith(
      session: resolvedSession,
      ownerTabIndex: 0,
      ownerFinanceSubTab: 0,
    );

    if (_networkEnabled && state.effectiveOnline) {
      final token = state.fcmState.deviceToken;
      if (token != null && token.isNotEmpty) {
        unawaited(_syncFcmTokenToBackend(token));
      }
      unawaited(refreshFcmServerStatus());
      unawaited(syncNow());
    }
  }

  Future<void> loginWithTokenOrUsername({
    required String credential,
    String fallbackRole = 'OWNER',
  }) async {
    final trimmed = credential.trim();
    if (trimmed.isEmpty) return;

    final knownDemo = MedanSeedData.demoAccounts()
        .where((a) => a.username.toLowerCase() == trimmed.toLowerCase())
        .firstOrNull;
    if (knownDemo != null) {
      await loginDemoAccount(knownDemo.username);
      return;
    }

    // Treat as Supabase JWT token
    _api.updateConfig(jwtToken: trimmed);
    UserSession? session;
    if (_networkEnabled && state.effectiveOnline) {
      try {
        final me = await _api.get('/auth/me');
        if (me is Map<String, dynamic>) {
          session = UserSession.fromJson(me, tokenOverride: trimmed);
          state = state.copyWith(serverReachable: true);
        }
      } catch (_) {
        state = state.copyWith(serverReachable: false);
      }
    }

    session ??= UserSession(
      id: 'usr-supabase-jwt',
      username: fallbackRole == 'OWNER' ? 'owner' : 'driver',
      fullName: fallbackRole == 'OWNER'
          ? 'Dispatcher Mandiri (JWT)'
          : 'Supir Mandiri (JWT)',
      phone: '+628116001001',
      role: fallbackRole,
      driverId: fallbackRole == 'DRIVER' ? 'drv-budi' : null,
      token: trimmed,
    );

    await _store.saveSession(session);
    state = state.copyWith(session: session, ownerTabIndex: 0);
    if (_networkEnabled && state.effectiveOnline) {
      final token = state.fcmState.deviceToken;
      if (token != null && token.isNotEmpty) {
        unawaited(_syncFcmTokenToBackend(token));
      }
      unawaited(refreshFcmServerStatus());
    }
  }

  Future<void> logout() async {
    await _gpsSub?.cancel();
    _gpsSub = null;
    final token = state.fcmState.deviceToken;
    if (_networkEnabled &&
        state.effectiveOnline &&
        state.session != null &&
        token != null &&
        token.trim().isNotEmpty) {
      try {
        await _api.post(
          '/auth/fcm-token/unregister',
          body: {'fcmToken': token.trim()},
        );
      } catch (_) {}
    }
    _api.clearToken();
    await _store.saveSession(null);
    state = state.copyWith(
      clearSession: true,
      clearUndo: true,
      fcmState: state.fcmState.copyWith(
        tokenSyncedToBackend: false,
        clearBanner: true,
      ),
    );
  }

  // --- Offline Sync & Outbox Flushing ---

  Future<String> syncNow() async {
    if (!state.effectiveOnline) {
      return 'Mode Offline aktif — perubahan disimpan aman di antrean HP.';
    }
    if (!_networkEnabled) {
      final now = DateTime.now().toUtc().toIso8601String();
      await _store.saveLastSyncedAt(now);
      _reloadFromStore();
      return 'Data lokal siap digunakan.';
    }

    state = state.copyWith(isSyncing: true);
    try {
      // 1. Flush pending active-trip GPS points
      final pendingGps = _store.loadGpsQueue().where((p) => !p.synced).toList();
      final byTrip = <String, List<GpsPointRecord>>{};
      for (final pt in pendingGps) {
        byTrip.putIfAbsent(pt.tripId, () => []).add(pt);
      }
      for (final entry in byTrip.entries) {
        await _api.post(
          '/trips/${entry.key}/gps-points/batch',
          body: {
            'points': entry.value
                .map(
                  (p) => {
                    'clientPointId': p.clientPointId,
                    'latitude': p.latitude,
                    'longitude': p.longitude,
                    'speedKmh': p.speedKmh,
                    'accuracyM': p.accuracyM,
                    'recordedAt': p.recordedAt,
                  },
                )
                .toList(),
          },
        );
        await _store.markGpsPointsSynced(
          entry.value.map((p) => p.clientPointId).toList(),
        );
      }

      // 2. Flush pending outbox mutations
      final outbox = _store.loadOutbox();
      if (outbox.isNotEmpty) {
        final res = await _api.post(
          '/sync/mutations',
          body: {'mutations': outbox.map((m) => m.toJson()).toList()},
        );
        if (res is Map<String, dynamic>) {
          final results = (res['results'] as List<dynamic>? ?? const [])
              .whereType<Map<String, dynamic>>()
              .toList();
          await _store.applySyncOutcomes(results);
        }
      }

      // 3. Pull fresh operational snapshots
      final session = state.session;
      if (session != null && session.isDriver) {
        final bootstrap = await _api.get('/sync/driver-bootstrap');
        if (bootstrap is Map<String, dynamic>) {
          final rawTrips = bootstrap['trips'] as List<dynamic>?;
          if (rawTrips != null) {
            final serverTrips = rawTrips
                .whereType<Map<String, dynamic>>()
                .map(Trip.fromJson)
                .toList();
            if (serverTrips.isNotEmpty) {
              await _mergeServerTripsPreservingDriverScope(serverTrips);
            }
          }
        }
      } else if (session != null && session.isOwner) {
        final rawTrips = await _api.get('/trips');
        if (rawTrips is List<dynamic> && rawTrips.isNotEmpty) {
          await _store.saveTrips(
            rawTrips.whereType<Map<String, dynamic>>().map(Trip.fromJson).toList(),
          );
        }
        final rawDrivers = await _api.get('/drivers');
        if (rawDrivers is List<dynamic> && rawDrivers.isNotEmpty) {
          await _store.saveDrivers(
            rawDrivers
                .whereType<Map<String, dynamic>>()
                .map(Driver.fromJson)
                .toList(),
          );
        }
      }

      if (session != null) {
        final token = state.fcmState.deviceToken;
        if (token != null &&
            token.isNotEmpty &&
            !state.fcmState.tokenSyncedToBackend) {
          await _syncFcmTokenToBackend(token);
        }
        try {
          final rawNotifs = await _api.get('/notifications');
          if (rawNotifs is Map<String, dynamic>) {
            final list = rawNotifs['notifications'] as List<dynamic>?;
            if (list != null && list.isNotEmpty) {
              final serverItems = list
                  .whereType<Map<String, dynamic>>()
                  .map(NotificationItem.fromJson)
                  .toList();
              final mergedById = <String, NotificationItem>{
                for (final n in _store.loadNotifications()) n.id: n,
              };
              for (final s in serverItems) {
                mergedById[s.id] = s;
              }
              await _store.saveNotifications(mergedById.values.toList());
            }
          }
        } catch (_) {}
        await refreshFcmServerStatus();
      }

      final nowIso = DateTime.now().toUtc().toIso8601String();
      await _store.saveLastSyncedAt(nowIso);
      state = state.copyWith(isSyncing: false, serverReachable: true);
      _reloadFromStore();
      return 'Sinkronisasi ke server berhasil.';
    } catch (_) {
      state = state.copyWith(isSyncing: false, serverReachable: false);
      _reloadFromStore();
      return 'Server belum terjangkau — data tetap tersimpan aman secara lokal.';
    }
  }

  Future<void> _mergeServerTripsPreservingDriverScope(
    List<Trip> serverDriverTrips,
  ) async {
    final existing = _store.loadTrips();
    final mapById = {for (final t in existing) t.id: t};
    for (final st in serverDriverTrips) {
      mapById[st.id] = st;
    }
    await _store.saveTrips(mapById.values.toList());
  }

  Future<void> resolveConflict(String conflictId) async {
    await _store.resolveStagedConflict(conflictId);
    _reloadFromStore();
  }

  // --- Driver Execution, Undo & Active-Trip-Only GPS ---

  Future<void> advanceTripStatus({
    required Trip trip,
    required String newStatus,
    double? odometerStartKm,
    double? odometerEndKm,
    String? completionNotes,
  }) async {
    final prevStatus = trip.status;
    if (const {'COMPLETED', 'CANCELLED', 'NO_SHOW'}.contains(newStatus)) {
      await _gpsSub?.cancel();
      _gpsSub = null;
    }

    await _store.applyTripStatusLocal(
      tripId: trip.id,
      newStatus: newStatus,
      odometerStartKm: odometerStartKm,
      odometerEndKm: odometerEndKm,
      completionNotes: completionNotes,
    );

    state = state.copyWith(
      undoSnapshot: (tripId: trip.id, previousStatus: prevStatus),
    );
    _reloadFromStore();

    if (_networkEnabled && state.effectiveOnline) {
      unawaited(syncNow());
    }
  }

  Future<void> undoLastTripStatus(String tripId) async {
    final snap = state.undoSnapshot;
    if (snap == null || snap.tripId != tripId) return;
    await _store.applyTripStatusLocal(
      tripId: tripId,
      newStatus: snap.previousStatus,
    );
    state = state.copyWith(clearUndo: true);
    _reloadFromStore();
  }

  Future<String> startActiveTripGps(String tripId) async {
    final trip = state.trips.where((t) => t.id == tripId).firstOrNull;
    if (trip == null) return 'Data perjalanan tidak ditemukan.';
    if (!trip.isActiveExecutionState) {
      return 'GPS hanya dapat diaktifkan saat perjalanan sedang berjalan (setelah Terima Tugas).';
    }

    await _store.setTripTrackingState(tripId: tripId, active: true);
    // Record initial Medan/KNO corridor coordinate immediately so tracking is visible
    await _store.recordActiveTripGpsPoint(
      tripId: tripId,
      latitude: 3.6382,
      longitude: 98.8775,
      speedKmh: 42.0,
    );
    _reloadFromStore();

    if (_networkEnabled && state.effectiveOnline) {
      try {
        await _api.post('/trips/$tripId/tracking/start');
      } catch (_) {}
    }

    // Attempt live hardware GPS stream (graceful fallback on desktop/tests)
    if (_networkEnabled) {
      unawaited(_attachHardwareGpsStream(tripId));
    }

    return 'Pelacakan GPS perjalanan diaktifkan.';
  }

  Future<void> _attachHardwareGpsStream(String tripId) async {
    try {
      await _gpsSub?.cancel();
      final serviceEnabled = await Geolocator.isLocationServiceEnabled();
      if (!serviceEnabled) return;
      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      if (permission == LocationPermission.denied ||
          permission == LocationPermission.deniedForever) {
        return;
      }
      _gpsSub = Geolocator.getPositionStream(
        locationSettings: const LocationSettings(
          accuracy: LocationAccuracy.high,
          distanceFilter: 50,
        ),
      ).listen((pos) async {
        await _store.recordActiveTripGpsPoint(
          tripId: tripId,
          latitude: pos.latitude,
          longitude: pos.longitude,
          speedKmh: (pos.speed * 3.6).clamp(0, 140),
          accuracyM: pos.accuracy,
        );
        _reloadFromStore();
      });
    } catch (_) {
      // Safe fallback when desktop/emulator has no GPS hardware
    }
  }

  Future<void> recordGpsWaypointSample(String tripId) async {
    final count = _store.allGpsPointsForTrip(tripId).length;
    await _store.recordActiveTripGpsPoint(
      tripId: tripId,
      latitude: 3.6382 - (count * 0.0085),
      longitude: 98.8775 - (count * 0.0210),
      speedKmh: 54.0,
    );
    _reloadFromStore();
  }

  Future<String> stopActiveTripGps(String tripId) async {
    await _gpsSub?.cancel();
    _gpsSub = null;
    await _store.setTripTrackingState(tripId: tripId, active: false);
    _reloadFromStore();

    if (_networkEnabled && state.effectiveOnline) {
      try {
        await syncNow();
        await _api.post('/trips/$tripId/tracking/end');
      } catch (_) {}
    }
    return 'Pelacakan GPS dihentikan. Privasi supir terlindungi di luar jam perjalanan.';
  }

  // --- Field Operations: Inspection, Payment, Expense, Lost & Found, SOS ---

  Future<void> recordVehicleInspection({
    required String tripId,
    required bool allPassed,
    List<String> flaggedItems = const [],
    String? notes,
  }) async {
    await _store.recordVehicleInspectionLocal(
      tripId: tripId,
      allPassed: allPassed,
      flaggedItems: flaggedItems,
      notes: notes,
    );
    _reloadFromStore();
    if (_networkEnabled && state.effectiveOnline) {
      unawaited(syncNow());
    }
  }

  Future<void> recordTripPayment({
    required String tripId,
    required double amount,
    required String paymentMethod,
    String? referenceNumber,
    String? notes,
  }) async {
    final role = state.session?.role ?? 'DRIVER';
    await _store.recordTripPaymentLocal(
      tripId: tripId,
      amount: amount,
      paymentMethod: paymentMethod,
      collectedByRole: role,
      driverId: state.session?.driverId,
      referenceNumber: referenceNumber,
      notes: notes,
    );
    _reloadFromStore();
    if (_networkEnabled && state.effectiveOnline) {
      unawaited(syncNow());
    }
  }

  Future<void> voidTripPayment({
    required String tripId,
    required String paymentId,
    required String reason,
  }) async {
    await _store.voidTripPaymentLocal(
      tripId: tripId,
      paymentId: paymentId,
      reason: reason,
    );
    if (_networkEnabled && state.effectiveOnline) {
      try {
        await _api.post('/payments/$paymentId/void', body: {'reason': reason});
      } catch (_) {}
    }
    _reloadFromStore();
  }

  Future<void> recordTripExpense({
    required String tripId,
    required String category,
    required double amount,
    double? fuelLiters,
    String? notes,
  }) async {
    await _store.recordTripExpenseLocal(
      tripId: tripId,
      category: category,
      amount: amount,
      fuelLiters: fuelLiters,
      notes: notes,
    );
    _reloadFromStore();
    if (_networkEnabled && state.effectiveOnline) {
      unawaited(syncNow());
    }
  }

  Future<void> recordLostAndFound({
    required String tripId,
    required String itemDescription,
    String? notes,
  }) async {
    await _store.recordLostAndFoundLocal(
      tripId: tripId,
      itemDescription: itemDescription,
      notes: notes,
    );
    _reloadFromStore();
  }

  Future<void> updateLostAndFoundStatus({
    required String itemId,
    required String newStatus,
    String? notes,
  }) async {
    await _store.updateLostAndFoundStatusLocal(
      itemId: itemId,
      newStatus: newStatus,
      notes: notes,
    );
    if (_networkEnabled && state.effectiveOnline) {
      try {
        await _api.put(
          '/lost-and-found/$itemId/status',
          body: {'status': newStatus, 'notes': notes},
        );
      } catch (_) {}
    }
    _reloadFromStore();
  }

  Future<String> buildAndSendEmergencySos(Trip trip) async {
    final lat = trip.lastGpsLat ?? 3.5952;
    final lng = trip.lastGpsLng ?? 98.6722;
    final mapsPin = DeepLinks.buildGoogleMapsSearchUrl(
      query: '$lat,$lng',
      latitude: lat,
      longitude: lng,
    );
    final emergencyMsg =
        '[DARURAT SOS MANDIRI TRANSPORT] ${state.session?.fullName ?? "Supir"} membutuhkan bantuan pada trip ${trip.tripCode} (${trip.vehiclePlate ?? "-"}). Lokasi terakhir: $mapsPin';

    if (_networkEnabled && state.effectiveOnline) {
      try {
        final res = await _api.post(
          '/emergency-alerts',
          body: {
            'tripId': trip.id,
            'latitude': lat,
            'longitude': lng,
            'message': emergencyMsg,
          },
        );
        if (res is Map<String, dynamic> &&
            res['ownerWhatsappEmergencyUrl'] is String) {
          return res['ownerWhatsappEmergencyUrl'] as String;
        }
      } catch (_) {}
    }

    return DeepLinks.buildWhatsappUrl(
      phone: '+628116001001',
      message: emergencyMsg,
    );
  }

  // --- Cash Custody Handovers ---

  Future<void> declareDriverHandover({
    required double declaredAmount,
    String? driverNotes,
  }) async {
    final dId = state.session?.driverId ?? 'drv-budi';
    final handover = await _store.declareDriverCashHandover(
      driverId: dId,
      declaredAmount: declaredAmount,
      driverNotes: driverNotes,
    );
    if (_networkEnabled && state.effectiveOnline) {
      try {
        await _api.post(
          '/cash-handovers',
          body: {
            'driverId': dId,
            'declaredAmount': declaredAmount,
            'paymentIds': handover.paymentIds,
            'driverNotes': driverNotes,
          },
        );
      } catch (_) {}
    }
    _reloadFromStore();
  }

  Future<void> confirmOwnerCashHandover({
    required String driverId,
    required double confirmedAmount,
    String? handoverId,
    String? ownerNotes,
    bool markDisputed = false,
  }) async {
    await _store.confirmDriverCashHandover(
      driverId: driverId,
      confirmedAmount: confirmedAmount,
      handoverId: handoverId,
      ownerNotes: ownerNotes,
      markDisputed: markDisputed,
    );
    if (_networkEnabled && state.effectiveOnline && handoverId != null) {
      try {
        await _api.post(
          '/cash-handovers/$handoverId/confirm',
          body: {
            'confirmedAmount': confirmedAmount,
            'ownerNotes': ownerNotes,
            'markDisputed': markDisputed,
          },
        );
      } catch (_) {}
    }
    _reloadFromStore();
  }

  // --- Owner Quick Booking, Templates & Dispatch ---

  PricingResolution resolvePricing({
    String? customerId,
    String? routeId,
    String vehicleType = 'MPV',
    String tripType = 'AIRPORT_DROPOFF',
    double? distanceKm,
    int? charterHours,
  }) {
    return _store.resolvePricingLocal(
      customerId: customerId,
      routeId: routeId,
      vehicleType: vehicleType,
      tripType: tripType,
      distanceKm: distanceKm,
      charterHours: charterHours,
    );
  }

  Future<List<Trip>> createQuickBooking({
    String? customerId,
    String? newCustomerName,
    String? newCustomerPhone,
    required String tripType,
    String? routeId,
    required String requestedPickupAt,
    String? returnPickupAt,
    double? returnAgreedFare,
    required String pickupAddress,
    required String destinationAddress,
    int passengerCount = 2,
    int luggageCount = 2,
    String? flightNumber,
    String vehicleTypePreference = 'MPV',
    required double agreedFare,
    List<TripStop> stops = const [],
  }) async {
    final created = await _store.createQuickBookingLocal(
      customerId: customerId,
      newCustomerName: newCustomerName,
      newCustomerPhone: newCustomerPhone,
      tripType: tripType,
      routeId: routeId,
      requestedPickupAt: requestedPickupAt,
      returnPickupAt: returnPickupAt,
      returnAgreedFare: returnAgreedFare,
      pickupAddress: pickupAddress,
      destinationAddress: destinationAddress,
      passengerCount: passengerCount,
      luggageCount: luggageCount,
      flightNumber: flightNumber,
      vehicleTypePreference: vehicleTypePreference,
      agreedFare: agreedFare,
      stops: stops,
    );

    if (_networkEnabled && state.effectiveOnline) {
      try {
        await _api.post(
          '/bookings/quick',
          body: {
            'customerId': customerId,
            'newCustomerName': newCustomerName,
            'newCustomerPhone': newCustomerPhone,
            'bookingSource': 'WHATSAPP',
            'tripType': tripType,
            'routeId': routeId,
            'requestedPickupAt': requestedPickupAt,
            'returnPickupAt': returnPickupAt,
            'returnAgreedFare': returnAgreedFare,
            'pickupAddress': pickupAddress,
            'destinationAddress': destinationAddress,
            'passengerCount': passengerCount,
            'luggageCount': luggageCount,
            'flightNumber': flightNumber,
            'vehicleTypePreference': vehicleTypePreference,
            'agreedFare': agreedFare,
            'stops': stops.map((s) => s.toJson()).toList(),
          },
        );
      } catch (_) {}
    }

    _reloadFromStore();
    return created;
  }

  Future<void> saveBookingTemplate({
    required String name,
    String? customerId,
    String? routeId,
    required String tripType,
    required String pickupAddress,
    required String destinationAddress,
    required int passengerCount,
    required int luggageCount,
    String? preferredVehicleType,
  }) async {
    await _store.saveBookingTemplateLocal(
      name: name,
      customerId: customerId,
      routeId: routeId,
      tripType: tripType,
      pickupAddress: pickupAddress,
      destinationAddress: destinationAddress,
      passengerCount: passengerCount,
      luggageCount: luggageCount,
      preferredVehicleType: preferredVehicleType,
    );
    _reloadFromStore();
  }

  ConflictSimulationResult simulateDispatch({
    required Trip trip,
    String? candidateDriverId,
    String? candidateVehicleId,
  }) {
    return _store.simulateDispatchLocal(
      trip: trip,
      candidateDriverId: candidateDriverId,
      candidateVehicleId: candidateVehicleId,
    );
  }

  Future<ConflictSimulationResult?> assignTrip({
    required Trip trip,
    required String driverId,
    required String vehicleId,
    bool forceOverrideConfirmed = false,
    String? overrideReason,
  }) async {
    final sim = simulateDispatch(
      trip: trip,
      candidateDriverId: driverId,
      candidateVehicleId: vehicleId,
    );
    if (sim.hasAnyConflict && !forceOverrideConfirmed) {
      return sim;
    }

    if (_networkEnabled && state.effectiveOnline) {
      try {
        await _api.post(
          '/trips/${trip.id}/assign',
          body: {
            'driverId': driverId,
            'vehicleId': vehicleId,
            'forceOverrideConfirmed': forceOverrideConfirmed,
            'overrideReason': overrideReason,
          },
        );
      } on ApiException catch (e) {
        if (e.isScheduleConflict &&
            !forceOverrideConfirmed &&
            e.data is Map<String, dynamic>) {
          return ConflictSimulationResult.fromJson(
            e.data as Map<String, dynamic>,
          );
        }
      } catch (_) {}
    }

    await _store.assignTripLocal(
      tripId: trip.id,
      driverId: driverId,
      vehicleId: vehicleId,
    );
    _reloadFromStore();
    return null;
  }

  // --- Owner Customers, Fleet, Cost Formulas & BI ---

  TripProfitability computeTripProfitability(Trip trip) =>
      _store.computeTripProfitability(trip);

  BiExplorerData computeBiExplorer({
    String? routeId,
    String? vehicleId,
    String? driverId,
    String? customerId,
  }) => _store.computeBiExplorer(
    routeId: routeId,
    vehicleId: vehicleId,
    driverId: driverId,
    customerId: customerId,
  );

  Future<void> createCustomer({
    required String name,
    required String phone,
    String? companyName,
    String preferredVehicleType = 'MPV',
  }) async {
    await _store.createCustomerLocal(
      name: name,
      phone: phone,
      companyName: companyName,
      preferredVehicleType: preferredVehicleType,
    );
    _reloadFromStore();
  }

  Future<void> addCustomerNote({
    required String customerId,
    required String category,
    required String noteText,
    bool visibleToDrivers = true,
  }) async {
    await _store.addCustomerNoteLocal(
      customerId: customerId,
      category: category,
      noteText: noteText,
      visibleToDrivers: visibleToDrivers,
    );
    _reloadFromStore();
  }

  Future<void> addCustomerAddress({
    required String customerId,
    required String label,
    required String addressText,
    bool isDefault = false,
  }) async {
    await _store.addCustomerAddressLocal(
      customerId: customerId,
      label: label,
      addressText: addressText,
      isDefault: isDefault,
    );
    _reloadFromStore();
  }

  Future<void> addCustomerPricingAgreement({
    required String customerId,
    required String agreementName,
    String? routeId,
    String? vehicleType,
    required double agreedFare,
    String? conditionsNote,
  }) async {
    await _store.addCustomerPricingAgreementLocal(
      customerId: customerId,
      agreementName: agreementName,
      routeId: routeId,
      vehicleType: vehicleType,
      agreedFare: agreedFare,
      conditionsNote: conditionsNote,
    );
    _reloadFromStore();
  }

  Future<void> updateDriverAvailability({
    required String driverId,
    required String availabilityStatus,
    String? notes,
  }) async {
    await _store.updateDriverAvailabilityLocal(
      driverId: driverId,
      availabilityStatus: availabilityStatus,
      notes: notes,
    );
    if (_networkEnabled && state.effectiveOnline) {
      try {
        await _api.put(
          '/drivers/$driverId/availability',
          body: {'availabilityStatus': availabilityStatus, 'notes': notes},
        );
      } catch (_) {}
    }
    _reloadFromStore();
  }

  Future<void> addVehicleMaintenanceLog({
    required String vehicleId,
    required String maintenanceType,
    required double odometerKm,
    required String vendorName,
    required double costAmount,
    required double downtimeHours,
    required String description,
  }) async {
    await _store.addVehicleMaintenanceLogLocal(
      vehicleId: vehicleId,
      maintenanceType: maintenanceType,
      odometerKm: odometerKm,
      vendorName: vendorName,
      costAmount: costAmount,
      downtimeHours: downtimeHours,
      description: description,
    );
    _reloadFromStore();
  }

  Future<void> addVehicleDocument({
    required String vehicleId,
    required String docType,
    required String docNumber,
    required String expiryDate,
    String? notes,
  }) async {
    await _store.addVehicleDocumentLocal(
      vehicleId: vehicleId,
      docType: docType,
      docNumber: docNumber,
      expiryDate: expiryDate,
      notes: notes,
    );
    _reloadFromStore();
  }

  Future<void> createCostFormulaVersion({
    required String name,
    required String effectiveFrom,
    required double fuelPerKm,
    required double maintenanceReservePerKm,
    required double monthlyDepreciation,
    required double driverCommissionRate,
    required double tireReplacementCost,
    required double tireLifespanKm,
    String? notes,
  }) async {
    await _store.createCostFormulaVersionLocal(
      name: name,
      effectiveFrom: effectiveFrom,
      fuelPerKm: fuelPerKm,
      maintenanceReservePerKm: maintenanceReservePerKm,
      monthlyDepreciation: monthlyDepreciation,
      driverCommissionRate: driverCommissionRate,
      tireReplacementCost: tireReplacementCost,
      tireLifespanKm: tireLifespanKm,
      notes: notes,
    );
    _reloadFromStore();
  }

  Future<void> acknowledgeNotification(String id) async {
    await _store.acknowledgeNotificationLocal(id);
    if (_networkEnabled && state.effectiveOnline && state.session != null) {
      try {
        await _api.post('/notifications/$id/acknowledge');
      } catch (_) {}
    }
    _reloadFromStore();
  }

  Future<void> updateNotificationSetting({
    required String settingId,
    int? leadTimeMinutes,
    String? recipientScope,
    bool? enabled,
    bool? acknowledgementEnabled,
  }) async {
    await _store.updateNotificationSettingLocal(
      settingId: settingId,
      leadTimeMinutes: leadTimeMinutes,
      recipientScope: recipientScope,
      enabled: enabled,
      acknowledgementEnabled: acknowledgementEnabled,
    );
    if (_networkEnabled && state.effectiveOnline && state.session != null) {
      final updated = _store
          .loadNotificationSettings()
          .where((s) => s.id == settingId)
          .firstOrNull;
      if (updated != null) {
        try {
          await _api.put(
            '/notifications/settings',
            body: [
              {
                'reminderType': updated.reminderType,
                'enabled': updated.enabled,
                'leadTimeMinutes': updated.leadTimeMinutes,
                'recipientScope': updated.recipientScope,
                'acknowledgementEnabled': updated.acknowledgementEnabled,
              },
            ],
          );
        } catch (_) {}
      }
    }
    _reloadFromStore();
  }
}
