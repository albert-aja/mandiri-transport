import 'dart:convert';
import 'dart:math' as math;
import 'package:shared_preferences/shared_preferences.dart';
import 'package:uuid/uuid.dart';

import '../../core/network/api_client.dart';
import '../../core/utils/formatters.dart';
import '../models/models.dart';
import 'medan_seed_data.dart';

class OfflineStore {
  static const _kBaseUrl = 'mt_base_url_v1';
  static const _kSession = 'mt_session_v1';
  static const _kDrivers = 'mt_drivers_v1';
  static const _kVehicles = 'mt_vehicles_v1';
  static const _kRoutes = 'mt_routes_v1';
  static const _kCustomers = 'mt_customers_v1';
  static const _kWaTemplates = 'mt_wa_templates_v1';
  static const _kBookingTemplates = 'mt_booking_templates_v1';
  static const _kCostFormulas = 'mt_cost_formulas_v1';
  static const _kTrips = 'mt_trips_v1';
  static const _kHandovers = 'mt_handovers_v1';
  static const _kLostAndFound = 'mt_lost_found_v1';
  static const _kNotifications = 'mt_notifications_v1';
  static const _kNotificationSettings = 'mt_notification_settings_v1';
  static const _kOutbox = 'mt_outbox_mutations_v1';
  static const _kAppliedMutationIds = 'mt_applied_mutation_ids_v1';
  static const _kGpsQueue = 'mt_gps_queue_v1';
  static const _kStagedConflicts = 'mt_staged_conflicts_v1';
  static const _kLastSyncedAt = 'mt_last_synced_at_v1';
  static const _kFcmDeviceToken = 'mt_fcm_device_token_v1';
  static const _kFcmRuntimeConfig = 'mt_fcm_runtime_config_v1';

  final SharedPreferences _prefs;
  final Uuid _uuid;

  OfflineStore(this._prefs, {Uuid? uuid}) : _uuid = uuid ?? const Uuid();

  static Future<OfflineStore> create({SharedPreferences? prefs}) async {
    final resolved = prefs ?? await SharedPreferences.getInstance();
    final store = OfflineStore(resolved);
    await store.ensureSeeded();
    return store;
  }

  Future<void> ensureSeeded() async {
    if (!_prefs.containsKey(_kTrips)) {
      await saveDrivers(MedanSeedData.drivers());
      await saveVehicles(MedanSeedData.vehicles());
      await saveRoutes(MedanSeedData.routes());
      await saveCustomers(MedanSeedData.customers());
      await saveWhatsappTemplates(MedanSeedData.whatsappTemplates());
      await saveBookingTemplates(MedanSeedData.bookingTemplates());
      await saveCostFormulas(MedanSeedData.costFormulas());
      await saveTrips(MedanSeedData.trips());
      await saveCashHandovers(MedanSeedData.cashHandovers());
      await saveLostAndFoundItems(MedanSeedData.lostAndFoundItems());
      await saveNotifications(MedanSeedData.notifications());
      await saveNotificationSettings(MedanSeedData.notificationSettings());
    }
  }

  // --- Base URL & Session ---

  String loadBaseUrl() =>
      _prefs.getString(_kBaseUrl) ?? ApiClient.defaultBaseUrl;

  Future<void> saveBaseUrl(String url) async {
    await _prefs.setString(_kBaseUrl, url.trim());
  }

  UserSession? loadSession() {
    final raw = _prefs.getString(_kSession);
    if (raw == null || raw.isEmpty) return null;
    return UserSession.fromJson(jsonDecode(raw) as Map<String, dynamic>);
  }

  Future<void> saveSession(UserSession? session) async {
    if (session == null) {
      await _prefs.remove(_kSession);
    } else {
      await _prefs.setString(_kSession, jsonEncode(session.toJson()));
    }
  }

  String? loadLastSyncedAt() => _prefs.getString(_kLastSyncedAt);

  Future<void> saveLastSyncedAt(String isoTimestamp) async {
    await _prefs.setString(_kLastSyncedAt, isoTimestamp);
  }

  String? loadFcmDeviceToken() => _prefs.getString(_kFcmDeviceToken);

  Future<void> saveFcmDeviceToken(String? token) async {
    if (token == null || token.trim().isEmpty) {
      await _prefs.remove(_kFcmDeviceToken);
    } else {
      await _prefs.setString(_kFcmDeviceToken, token.trim());
    }
  }

  FcmRuntimeConfig? loadFcmRuntimeConfig() {
    final raw = _prefs.getString(_kFcmRuntimeConfig);
    if (raw == null || raw.isEmpty) return null;
    return FcmRuntimeConfig.fromJson(jsonDecode(raw) as Map<String, dynamic>);
  }

  Future<void> saveFcmRuntimeConfig(FcmRuntimeConfig? config) async {
    if (config == null || !config.isComplete) {
      await _prefs.remove(_kFcmRuntimeConfig);
    } else {
      await _prefs.setString(_kFcmRuntimeConfig, jsonEncode(config.toJson()));
    }
  }

  Future<NotificationItem> prependIncomingRemoteNotification({
    required String id,
    required String reminderType,
    required String title,
    required String body,
    String? referenceType,
    String? referenceId,
  }) async {
    final list = loadNotifications();
    final existingIdx = list.indexWhere((n) => n.id == id);
    final item = NotificationItem(
      id: id,
      reminderType: reminderType,
      title: title,
      message: body,
      createdAt: DateTime.now().toUtc().toIso8601String(),
      acknowledged: false,
      referenceType: referenceType,
      referenceId: referenceId,
      fcmDeliveryStatus: 'PUSH_SENT',
    );
    if (existingIdx >= 0) {
      list[existingIdx] = item;
    } else {
      list.insert(0, item);
    }
    await saveNotifications(list);
    return item;
  }

  // --- Entity Loaders & Savers ---

  List<T> _loadList<T>(
    String key,
    T Function(Map<String, dynamic>) fromJson,
    List<T> Function() fallback,
  ) {
    final raw = _prefs.getString(key);
    if (raw == null || raw.isEmpty) return fallback();
    final list = jsonDecode(raw) as List<dynamic>;
    return list.whereType<Map<String, dynamic>>().map(fromJson).toList();
  }

  Future<void> _saveList<T>(
    String key,
    List<T> items,
    Map<String, dynamic> Function(T) toJson,
  ) async {
    await _prefs.setString(key, jsonEncode(items.map(toJson).toList()));
  }

  List<Driver> loadDrivers() =>
      _loadList(_kDrivers, Driver.fromJson, MedanSeedData.drivers);
  Future<void> saveDrivers(List<Driver> items) =>
      _saveList(_kDrivers, items, (e) => e.toJson());

  List<Vehicle> loadVehicles() =>
      _loadList(_kVehicles, Vehicle.fromJson, MedanSeedData.vehicles);
  Future<void> saveVehicles(List<Vehicle> items) =>
      _saveList(_kVehicles, items, (e) => e.toJson());

  List<RouteInfo> loadRoutes() =>
      _loadList(_kRoutes, RouteInfo.fromJson, MedanSeedData.routes);
  Future<void> saveRoutes(List<RouteInfo> items) =>
      _saveList(_kRoutes, items, (e) => e.toJson());

  List<Customer> loadCustomers() =>
      _loadList(_kCustomers, Customer.fromJson, MedanSeedData.customers);
  Future<void> saveCustomers(List<Customer> items) =>
      _saveList(_kCustomers, items, (e) => e.toJson());

  List<WhatsappTemplate> loadWhatsappTemplates() => _loadList(
    _kWaTemplates,
    WhatsappTemplate.fromJson,
    MedanSeedData.whatsappTemplates,
  );
  Future<void> saveWhatsappTemplates(List<WhatsappTemplate> items) =>
      _saveList(_kWaTemplates, items, (e) => e.toJson());

  List<BookingTemplate> loadBookingTemplates() => _loadList(
    _kBookingTemplates,
    BookingTemplate.fromJson,
    MedanSeedData.bookingTemplates,
  );
  Future<void> saveBookingTemplates(List<BookingTemplate> items) =>
      _saveList(_kBookingTemplates, items, (e) => e.toJson());

  List<CostFormulaVersion> loadCostFormulas() => _loadList(
    _kCostFormulas,
    CostFormulaVersion.fromJson,
    MedanSeedData.costFormulas,
  );
  Future<void> saveCostFormulas(List<CostFormulaVersion> items) =>
      _saveList(_kCostFormulas, items, (e) => e.toJson());

  List<Trip> loadTrips() =>
      _loadList(_kTrips, Trip.fromJson, MedanSeedData.trips);
  Future<void> saveTrips(List<Trip> items) =>
      _saveList(_kTrips, items, (e) => e.toJson());

  Future<void> updateTripFlightInfo(
    String tripId,
    FlightScheduleInfo info,
  ) async {
    final trips = loadTrips();
    final updated = trips.map((t) {
      if (t.id == tripId) {
        return t.copyWith(flightInfo: info);
      }
      return t;
    }).toList();
    await saveTrips(updated);
  }

  List<CashHandover> loadCashHandovers() => _loadList(
    _kHandovers,
    CashHandover.fromJson,
    MedanSeedData.cashHandovers,
  );
  Future<void> saveCashHandovers(List<CashHandover> items) =>
      _saveList(_kHandovers, items, (e) => e.toJson());

  List<LostAndFoundItem> loadLostAndFoundItems() => _loadList(
    _kLostAndFound,
    LostAndFoundItem.fromJson,
    MedanSeedData.lostAndFoundItems,
  );
  Future<void> saveLostAndFoundItems(List<LostAndFoundItem> items) =>
      _saveList(_kLostAndFound, items, (e) => e.toJson());

  List<NotificationItem> loadNotifications() => _loadList(
    _kNotifications,
    NotificationItem.fromJson,
    MedanSeedData.notifications,
  );
  Future<void> saveNotifications(List<NotificationItem> items) =>
      _saveList(_kNotifications, items, (e) => e.toJson());

  List<NotificationSettingItem> loadNotificationSettings() => _loadList(
    _kNotificationSettings,
    NotificationSettingItem.fromJson,
    MedanSeedData.notificationSettings,
  );
  Future<void> saveNotificationSettings(List<NotificationSettingItem> items) =>
      _saveList(_kNotificationSettings, items, (e) => e.toJson());

  // --- Persistent Idempotent Outbox Queue & Conflict Staging ---

  List<SyncMutation> loadOutbox() =>
      _loadList(_kOutbox, SyncMutation.fromJson, () => const []);

  Future<void> saveOutbox(List<SyncMutation> mutations) =>
      _saveList(_kOutbox, mutations, (e) => e.toJson());

  Set<String> loadAppliedMutationIds() {
    final list = _prefs.getStringList(_kAppliedMutationIds) ?? const [];
    return list.toSet();
  }

  Future<void> _recordAppliedMutationId(String id) async {
    final current = loadAppliedMutationIds()..add(id);
    await _prefs.setStringList(_kAppliedMutationIds, current.toList());
  }

  /// Enqueues an offline mutation idempotently by `clientMutationId`.
  /// Returns the queued `SyncMutation` (or existing one if duplicate `clientMutationId`).
  Future<SyncMutation> enqueueMutation({
    String? clientMutationId,
    required String mutationType,
    required String entityId,
    int? expectedVersion,
    required Map<String, dynamic> payload,
    String? recordedAt,
  }) async {
    final id = (clientMutationId != null && clientMutationId.isNotEmpty)
        ? clientMutationId
        : _uuid.v4();

    final outbox = loadOutbox();
    for (final existing in outbox) {
      if (existing.clientMutationId == id) {
        return existing;
      }
    }
    if (loadAppliedMutationIds().contains(id)) {
      return SyncMutation(
        clientMutationId: id,
        mutationType: mutationType,
        entityId: entityId,
        expectedVersion: expectedVersion,
        recordedAt: recordedAt ?? DateTime.now().toUtc().toIso8601String(),
        payload: payload,
      );
    }

    final mutation = SyncMutation(
      clientMutationId: id,
      mutationType: mutationType,
      entityId: entityId,
      expectedVersion: expectedVersion,
      recordedAt: recordedAt ?? DateTime.now().toUtc().toIso8601String(),
      payload: payload,
    );
    await saveOutbox([...outbox, mutation]);
    return mutation;
  }

  Future<void> removeMutationFromOutbox(String clientMutationId) async {
    final updated = loadOutbox()
        .where((m) => m.clientMutationId != clientMutationId)
        .toList();
    await saveOutbox(updated);
    await _recordAppliedMutationId(clientMutationId);
  }

  List<StagedSyncConflict> loadStagedConflicts() => _loadList(
    _kStagedConflicts,
    StagedSyncConflict.fromJson,
    () => const [],
  );

  Future<void> saveStagedConflicts(List<StagedSyncConflict> conflicts) =>
      _saveList(_kStagedConflicts, conflicts, (e) => e.toJson());

  /// Processes server sync outcomes (`APPLIED`, `DUPLICATE_IGNORED`, `CONFLICT_STAGED`).
  /// CRITICAL RULE: Never deletes locally recorded payments, expenses, inspections, or notes!
  Future<void> applySyncOutcomes(List<Map<String, dynamic>> results) async {
    final outbox = loadOutbox();
    final remainingOutbox = <SyncMutation>[];
    final conflicts = List<StagedSyncConflict>.from(loadStagedConflicts());
    final resultById = <String, Map<String, dynamic>>{};
    for (final r in results) {
      final id = r['clientMutationId'] as String?;
      if (id != null) resultById[id] = r;
    }

    for (final mutation in outbox) {
      final outcome = resultById[mutation.clientMutationId];
      if (outcome == null) {
        remainingOutbox.add(mutation);
        continue;
      }
      final status = (outcome['outcomeStatus'] as String?) ?? 'APPLIED';
      if (status == 'APPLIED' || status == 'DUPLICATE_IGNORED') {
        await _recordAppliedMutationId(mutation.clientMutationId);
      } else if (status == 'CONFLICT_STAGED') {
        await _recordAppliedMutationId(mutation.clientMutationId);
        final alreadyStaged = conflicts.any(
          (c) => c.clientMutationId == mutation.clientMutationId,
        );
        if (!alreadyStaged) {
          conflicts.insert(
            0,
            StagedSyncConflict(
              id: 'conf-${mutation.clientMutationId}',
              clientMutationId: mutation.clientMutationId,
              mutationType: mutation.mutationType,
              entityId: mutation.entityId,
              summaryMessage:
                  (outcome['message'] as String?) ??
                  'Perubahan status bertabrakan dengan pembaruan dispatcher. Catatan pembayaran & pengeluaran lokal tetap aman tersimpan.',
              stagedAt: DateTime.now().toUtc().toIso8601String(),
              localPayload: mutation.payload,
            ),
          );
        }
      } else {
        remainingOutbox.add(mutation);
      }
    }

    await saveOutbox(remainingOutbox);
    await saveStagedConflicts(conflicts);
  }

  Future<void> resolveStagedConflict(String conflictId) async {
    final updated = loadStagedConflicts()
        .map((c) => c.id == conflictId ? c.copyWith(resolved: true) : c)
        .toList();
    await saveStagedConflicts(updated);
  }

  // --- Active-Trip-Only GPS Queue ---

  List<GpsPointRecord> loadGpsQueue() =>
      _loadList(_kGpsQueue, GpsPointRecord.fromJson, () => const []);

  Future<void> saveGpsQueue(List<GpsPointRecord> points) =>
      _saveList(_kGpsQueue, points, (e) => e.toJson());

  List<GpsPointRecord> pendingGpsPointsForTrip(String tripId) =>
      loadGpsQueue().where((p) => p.tripId == tripId && !p.synced).toList();

  List<GpsPointRecord> allGpsPointsForTrip(String tripId) =>
      loadGpsQueue().where((p) => p.tripId == tripId).toList();

  Future<GpsPointRecord?> recordActiveTripGpsPoint({
    required String tripId,
    required double latitude,
    required double longitude,
    double speedKmh = 38.0,
    double accuracyM = 8.0,
    String? clientPointId,
    String? recordedAt,
  }) async {
    final trips = loadTrips();
    final idx = trips.indexWhere((t) => t.id == tripId);
    if (idx < 0) return null;
    final trip = trips[idx];
    // Strict privacy & operational gate: ONLY record when active execution & trackingActive == true
    if (!trip.isActiveExecutionState || !trip.trackingActive) {
      return null;
    }

    final pointId = clientPointId ?? _uuid.v4();
    final existingQueue = loadGpsQueue();
    for (final existing in existingQueue) {
      if (existing.clientPointId == pointId) return existing;
    }

    final timestamp = recordedAt ?? DateTime.now().toUtc().toIso8601String();
    final point = GpsPointRecord(
      clientPointId: pointId,
      tripId: tripId,
      latitude: latitude,
      longitude: longitude,
      speedKmh: speedKmh,
      accuracyM: accuracyM,
      recordedAt: timestamp,
    );

    final updatedQueue = [...existingQueue, point];
    await saveGpsQueue(updatedQueue);

    final tripPoints = updatedQueue.where((p) => p.tripId == tripId).toList();
    final computedDist = _computePolylineKm(tripPoints);

    trips[idx] = trip.copyWith(
      lastGpsRecordedAt: timestamp,
      lastGpsLat: latitude,
      lastGpsLng: longitude,
      gpsDistanceKm: computedDist > 0 ? computedDist : trip.gpsDistanceKm,
    );
    await saveTrips(trips);
    return point;
  }

  Future<void> markGpsPointsSynced(List<String> clientPointIds) async {
    final idSet = clientPointIds.toSet();
    final updated = loadGpsQueue()
        .map((p) => idSet.contains(p.clientPointId) ? p.copyWith(synced: true) : p)
        .toList();
    await saveGpsQueue(updated);
  }

  double _computePolylineKm(List<GpsPointRecord> pts) {
    if (pts.length < 2) return 0;
    var sum = 0.0;
    for (int i = 1; i < pts.length; i++) {
      sum += _haversineKm(
        pts[i - 1].latitude,
        pts[i - 1].longitude,
        pts[i].latitude,
        pts[i].longitude,
      );
    }
    return double.parse(sum.toStringAsFixed(2));
  }

  double _haversineKm(double lat1, double lon1, double lat2, double lon2) {
    const r = 6371.0;
    final dLat = _deg2rad(lat2 - lat1);
    final dLon = _deg2rad(lon2 - lon1);
    final a =
        math.sin(dLat / 2) * math.sin(dLat / 2) +
        math.cos(_deg2rad(lat1)) *
            math.cos(_deg2rad(lat2)) *
            math.sin(dLon / 2) *
            math.sin(dLon / 2);
    final c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a));
    return r * c;
  }

  double _deg2rad(double deg) => deg * (math.pi / 180.0);

  // --- Domain Write Operations (Offline-First Local State + Outbox) ---

  Future<Trip?> applyTripStatusLocal({
    required String tripId,
    required String newStatus,
    double? odometerStartKm,
    double? odometerEndKm,
    String? completionNotes,
    bool queueOutbox = true,
    String? clientMutationId,
  }) async {
    final trips = loadTrips();
    final idx = trips.indexWhere((t) => t.id == tripId);
    if (idx < 0) return null;

    final current = trips[idx];
    final nowIso = DateTime.now().toUtc().toIso8601String();
    final shouldHardStopGps = const {
      'COMPLETED',
      'CANCELLED',
      'NO_SHOW',
    }.contains(newStatus);

    final updated = current.copyWith(
      status: newStatus,
      trackingActive: shouldHardStopGps ? false : current.trackingActive,
      trackingEndedAt:
          (shouldHardStopGps && current.trackingActive)
          ? nowIso
          : current.trackingEndedAt,
      odometerStartKm: odometerStartKm ?? current.odometerStartKm,
      odometerEndKm: odometerEndKm ?? current.odometerEndKm,
      actualDurationMin: newStatus == 'COMPLETED'
          ? (current.actualDurationMin ?? current.estimatedDurationMin)
          : current.actualDurationMin,
      completionNotes: completionNotes ?? current.completionNotes,
      version: current.version + 1,
    );
    trips[idx] = updated;
    await saveTrips(trips);

    if (odometerEndKm != null && current.vehicleId != null) {
      final vehicles = loadVehicles();
      final vIdx = vehicles.indexWhere((v) => v.id == current.vehicleId);
      if (vIdx >= 0 && odometerEndKm > vehicles[vIdx].currentOdometerKm) {
        vehicles[vIdx] = vehicles[vIdx].copyWith(
          currentOdometerKm: odometerEndKm,
        );
        await saveVehicles(vehicles);
      }
    }

    if (queueOutbox) {
      await enqueueMutation(
        clientMutationId: clientMutationId,
        mutationType: 'TRIP_STATUS',
        entityId: tripId,
        expectedVersion: current.version,
        payload: {
          'newStatus': newStatus,
          'odometerStartKm': odometerStartKm,
          'odometerEndKm': odometerEndKm,
          'completionNotes': completionNotes,
        },
      );
    }

    return updated;
  }

  Future<Trip?> setTripTrackingState({
    required String tripId,
    required bool active,
  }) async {
    final trips = loadTrips();
    final idx = trips.indexWhere((t) => t.id == tripId);
    if (idx < 0) return null;
    final current = trips[idx];
    if (active && !current.isActiveExecutionState) {
      return null;
    }
    final nowIso = DateTime.now().toUtc().toIso8601String();
    final updated = current.copyWith(
      trackingActive: active,
      trackingStartedAt: active
          ? (current.trackingStartedAt ?? nowIso)
          : current.trackingStartedAt,
      trackingEndedAt: !active ? nowIso : current.trackingEndedAt,
    );
    trips[idx] = updated;
    await saveTrips(trips);
    return updated;
  }

  Future<Trip?> recordTripPaymentLocal({
    required String tripId,
    required double amount,
    required String paymentMethod, // CASH | BANK_TRANSFER | OTHER
    required String collectedByRole, // DRIVER | OWNER
    String? driverId,
    String? referenceNumber,
    String? notes,
    bool queueOutbox = true,
    String? clientMutationId,
  }) async {
    final trips = loadTrips();
    final idx = trips.indexWhere((t) => t.id == tripId);
    if (idx < 0) return null;
    final trip = trips[idx];

    final mutationId = clientMutationId ?? _uuid.v4();
    if (trip.payments.any((p) => p.id == 'pay-$mutationId')) {
      return trip;
    }

    final isDriverCash =
        paymentMethod == 'CASH' && collectedByRole == 'DRIVER';
    final effectiveDriverId = driverId ?? trip.driverId;
    final nowIso = DateTime.now().toUtc().toIso8601String();

    final payment = TripPayment(
      id: 'pay-$mutationId',
      tripId: tripId,
      amount: amount,
      paymentMethod: paymentMethod,
      collectedByRole: collectedByRole,
      heldByDriverId: isDriverCash ? effectiveDriverId : null,
      handedOverAmount: 0,
      custodySettled: !isDriverCash,
      paidAt: nowIso,
      referenceNumber: referenceNumber,
      notes: notes,
    );

    final updatedPayments = [...trip.payments, payment];
    final recalculatedTrip = _recalculateTripFinancials(
      trip.copyWith(payments: updatedPayments),
    );
    trips[idx] = recalculatedTrip;
    await saveTrips(trips);
    await _recomputeAllDriverCashBalances(trips);

    if (queueOutbox) {
      await enqueueMutation(
        clientMutationId: mutationId,
        mutationType: 'TRIP_PAYMENT',
        entityId: tripId,
        expectedVersion: trip.version,
        payload: {
          'amount': amount,
          'paymentMethod': paymentMethod,
          'isRefund': false,
          'collectedByRole': collectedByRole,
          'paidAt': nowIso,
          'referenceNumber': referenceNumber,
          'notes': notes,
        },
      );
    }

    return recalculatedTrip;
  }

  Future<Trip?> voidTripPaymentLocal({
    required String tripId,
    required String paymentId,
    required String reason,
  }) async {
    final trips = loadTrips();
    final idx = trips.indexWhere((t) => t.id == tripId);
    if (idx < 0) return null;
    final trip = trips[idx];
    final updatedPayments = trip.payments
        .map(
          (p) => p.id == paymentId
              ? p.copyWith(isVoided: true, voidReason: reason)
              : p,
        )
        .toList();
    final updatedTrip = _recalculateTripFinancials(
      trip.copyWith(payments: updatedPayments),
    );
    trips[idx] = updatedTrip;
    await saveTrips(trips);
    await _recomputeAllDriverCashBalances(trips);
    return updatedTrip;
  }

  Trip _recalculateTripFinancials(Trip trip) {
    var paidSum = 0.0;
    var driverHeldSum = 0.0;
    var anyPartialHandover = false;
    var allDriverCashConfirmed = true;
    var hasAnyDriverCash = false;

    for (final p in trip.payments) {
      if (p.isVoided) continue;
      if (p.isRefund) {
        paidSum -= p.amount;
      } else {
        paidSum += p.amount;
      }
      if (p.paymentMethod == 'CASH' && p.collectedByRole == 'DRIVER') {
        hasAnyDriverCash = true;
        final unsettled = p.unsettledDriverCash;
        driverHeldSum += unsettled;
        if (unsettled > 0) {
          allDriverCashConfirmed = false;
          if (p.handedOverAmount > 0) {
            anyPartialHandover = true;
          }
        }
      }
    }

    String payStatus;
    if (paidSum <= 0) {
      payStatus = 'UNPAID';
    } else if (paidSum < trip.agreedFareSnapshot) {
      payStatus = 'PARTIALLY_PAID';
    } else if (paidSum == trip.agreedFareSnapshot) {
      payStatus = 'PAID';
    } else {
      payStatus = 'OVERPAID';
    }

    String custodyStatus;
    if (!hasAnyDriverCash) {
      custodyStatus = 'NO_DRIVER_CASH_DUE';
    } else if (allDriverCashConfirmed) {
      custodyStatus = 'HANDED_OVER_CONFIRMED';
    } else if (anyPartialHandover) {
      custodyStatus = 'PARTIALLY_HANDED_OVER';
    } else {
      custodyStatus = 'HELD_BY_DRIVER';
    }

    return trip.copyWith(
      paymentStatus: payStatus,
      totalPaidAmount: paidSum < 0 ? 0 : paidSum,
      cashCustodyStatus: custodyStatus,
      driverCashHeldAmount: driverHeldSum,
    );
  }

  Future<void> _recomputeAllDriverCashBalances(List<Trip> trips) async {
    final drivers = loadDrivers();
    final totals = <String, double>{};
    for (final t in trips) {
      for (final p in t.payments) {
        final dId = p.heldByDriverId ?? t.driverId;
        if (dId != null) {
          totals[dId] = (totals[dId] ?? 0) + p.unsettledDriverCash;
        }
      }
    }
    final updatedDrivers = drivers
        .map((d) => d.copyWith(cashHeldAmount: totals[d.id] ?? 0))
        .toList();
    await saveDrivers(updatedDrivers);
  }

  Future<CashHandover> declareDriverCashHandover({
    required String driverId,
    required double declaredAmount,
    String? driverNotes,
  }) async {
    final drivers = loadDrivers();
    final drv = drivers.firstWhere(
      (d) => d.id == driverId,
      orElse: () => drivers.first,
    );
    final trips = loadTrips();
    final unsettledPaymentIds = <String>[];
    for (final t in trips) {
      for (final p in t.payments) {
        if ((p.heldByDriverId ?? t.driverId) == driverId &&
            p.unsettledDriverCash > 0) {
          unsettledPaymentIds.add(p.id);
        }
      }
    }

    final nowIso = DateTime.now().toUtc().toIso8601String();
    final handover = CashHandover(
      id: 'cho-${_uuid.v4().substring(0, 8)}',
      handoverCode:
          'CHO-${DateTime.now().millisecondsSinceEpoch.toString().substring(7)}',
      driverId: driverId,
      driverName: drv.fullName,
      declaredAmount: declaredAmount,
      status: 'PENDING',
      driverNotes: driverNotes,
      handedOverAt: nowIso,
      paymentIds: unsettledPaymentIds,
    );

    final handovers = [handover, ...loadCashHandovers()];
    await saveCashHandovers(handovers);
    return handover;
  }

  Future<void> confirmDriverCashHandover({
    required String driverId,
    required double confirmedAmount,
    String? handoverId,
    String? ownerNotes,
    bool markDisputed = false,
  }) async {
    final trips = loadTrips();
    var remainingToSettle = markDisputed ? 0.0 : confirmedAmount;

    for (int i = 0; i < trips.length; i++) {
      final t = trips[i];
      var changed = false;
      final newPayments = <TripPayment>[];
      for (final p in t.payments) {
        final pDriver = p.heldByDriverId ?? t.driverId;
        if (pDriver == driverId && p.unsettledDriverCash > 0 && !markDisputed) {
          final due = p.unsettledDriverCash;
          final applied = math.min(due, remainingToSettle);
          if (applied > 0) {
            remainingToSettle -= applied;
            final newHanded = p.handedOverAmount + applied;
            final settled = newHanded >= p.amount;
            newPayments.add(
              p.copyWith(handedOverAmount: newHanded, custodySettled: settled),
            );
            changed = true;
            continue;
          }
        }
        newPayments.add(p);
      }
      if (changed) {
        trips[i] = _recalculateTripFinancials(t.copyWith(payments: newPayments));
      } else if (markDisputed && t.driverId == driverId && t.driverCashHeldAmount > 0) {
        trips[i] = t.copyWith(cashCustodyStatus: 'DISPUTED');
      }
    }

    await saveTrips(trips);
    await _recomputeAllDriverCashBalances(trips);

    final nowIso = DateTime.now().toUtc().toIso8601String();
    final handovers = List<CashHandover>.from(loadCashHandovers());
    final drv = loadDrivers().firstWhere(
      (d) => d.id == driverId,
      orElse: () => loadDrivers().first,
    );

    final status = markDisputed
        ? 'DISPUTED'
        : (drv.cashHeldAmount > 0
              ? 'PARTIALLY_HANDED_OVER'
              : 'HANDED_OVER_CONFIRMED');

    if (handoverId != null) {
      final hIdx = handovers.indexWhere((h) => h.id == handoverId);
      if (hIdx >= 0) {
        handovers[hIdx] = handovers[hIdx].copyWith(
          confirmedAmount: confirmedAmount,
          status: status,
          ownerNotes: ownerNotes,
          confirmedAt: nowIso,
        );
        await saveCashHandovers(handovers);
        return;
      }
    }

    handovers.insert(
      0,
      CashHandover(
        id: 'cho-${_uuid.v4().substring(0, 8)}',
        handoverCode:
            'CHO-${DateTime.now().millisecondsSinceEpoch.toString().substring(7)}',
        driverId: driverId,
        driverName: drv.fullName,
        declaredAmount: confirmedAmount,
        confirmedAmount: confirmedAmount,
        status: status,
        ownerNotes: ownerNotes,
        handedOverAt: nowIso,
        confirmedAt: nowIso,
      ),
    );
    await saveCashHandovers(handovers);
  }

  Future<Trip?> recordTripExpenseLocal({
    required String tripId,
    required String category,
    required double amount,
    double? fuelLiters,
    String? notes,
    bool queueOutbox = true,
    String? clientMutationId,
  }) async {
    final trips = loadTrips();
    final idx = trips.indexWhere((t) => t.id == tripId);
    if (idx < 0) return null;
    final trip = trips[idx];
    final mutationId = clientMutationId ?? _uuid.v4();
    final nowIso = DateTime.now().toUtc().toIso8601String();

    final expense = TripExpense(
      id: 'texp-$mutationId',
      tripId: tripId,
      category: category,
      amount: amount,
      fuelLiters: fuelLiters,
      notes: notes,
      incurredAt: nowIso,
    );

    final updated = trip.copyWith(expenses: [...trip.expenses, expense]);
    trips[idx] = updated;
    await saveTrips(trips);

    if (queueOutbox) {
      await enqueueMutation(
        clientMutationId: mutationId,
        mutationType: 'TRIP_EXPENSE',
        entityId: tripId,
        expectedVersion: trip.version,
        payload: {
          'category': category,
          'amount': amount,
          'fuelLiters': fuelLiters,
          'notes': notes,
          'incurredAt': nowIso,
        },
      );
    }
    return updated;
  }

  Future<Trip?> recordVehicleInspectionLocal({
    required String tripId,
    String stage = 'PRE_TRIP',
    required bool allPassed,
    List<String> flaggedItems = const [],
    String? notes,
    bool queueOutbox = true,
    String? clientMutationId,
  }) async {
    final trips = loadTrips();
    final idx = trips.indexWhere((t) => t.id == tripId);
    if (idx < 0) return null;
    final trip = trips[idx];
    final mutationId = clientMutationId ?? _uuid.v4();
    final nowIso = DateTime.now().toUtc().toIso8601String();

    final inspection = VehicleInspection(
      id: 'insp-$mutationId',
      tripId: tripId,
      stage: stage,
      allPassed: allPassed,
      flaggedItems: flaggedItems,
      notes: notes,
      recordedAt: nowIso,
    );

    final updated = trip.copyWith(
      inspections: [inspection, ...trip.inspections],
    );
    trips[idx] = updated;
    await saveTrips(trips);

    if (queueOutbox) {
      await enqueueMutation(
        clientMutationId: mutationId,
        mutationType: 'VEHICLE_INSPECTION',
        entityId: tripId,
        expectedVersion: trip.version,
        payload: {
          'stage': stage,
          'allPassed': allPassed,
          'flaggedItems': flaggedItems,
          'notes': notes,
        },
      );
    }
    return updated;
  }

  Future<LostAndFoundItem> recordLostAndFoundLocal({
    required String tripId,
    required String itemDescription,
    String? notes,
    bool queueOutbox = true,
  }) async {
    final trips = loadTrips();
    final trip = trips.firstWhere(
      (t) => t.id == tripId,
      orElse: () => trips.first,
    );
    final mutationId = _uuid.v4();
    final today = DateTime.now().toIso8601String().split('T').first;
    final item = LostAndFoundItem(
      id: 'laf-${mutationId.substring(0, 8)}',
      tripId: trip.id,
      tripCode: trip.tripCode,
      customerName: trip.customerName,
      itemDescription: itemDescription,
      foundDate: today,
      status: 'FOUND',
      notes: notes,
    );

    await saveLostAndFoundItems([item, ...loadLostAndFoundItems()]);
    if (queueOutbox) {
      await enqueueMutation(
        clientMutationId: mutationId,
        mutationType: 'LOST_AND_FOUND',
        entityId: trip.id,
        payload: {
          'itemDescription': itemDescription,
          'foundDate': today,
          'status': 'FOUND',
          'notes': notes,
        },
      );
    }
    return item;
  }

  Future<void> updateLostAndFoundStatusLocal({
    required String itemId,
    required String newStatus,
    String? notes,
  }) async {
    final items = loadLostAndFoundItems()
        .map(
          (i) => i.id == itemId
              ? i.copyWith(status: newStatus, notes: notes ?? i.notes)
              : i,
        )
        .toList();
    await saveLostAndFoundItems(items);
  }

  // --- Pricing Engine (5-Tier Priority) ---

  PricingResolution resolvePricingLocal({
    String? customerId,
    String? routeId,
    String vehicleType = 'MPV',
    String tripType = 'AIRPORT_DROPOFF',
    double? distanceKm,
    int? charterHours,
  }) {
    final routes = loadRoutes();
    final route = routes.where((r) => r.id == routeId).firstOrNull;
    final dist = distanceKm ?? route?.typicalDistanceKm ?? 39.0;

    Customer? customer;
    if (customerId != null) {
      customer = loadCustomers().where((c) => c.id == customerId).firstOrNull;
    }

    // 1. CUSTOMER_ROUTE_AGREEMENT
    if (customer != null && routeId != null) {
      for (final ag in customer.pricingAgreements) {
        if (ag.routeId == routeId &&
            (ag.vehicleType == null || ag.vehicleType == vehicleType) &&
            ag.pricingMode == 'FIXED_FARE' &&
            ag.agreedFare != null) {
          return PricingResolution(
            suggestedFare: ag.agreedFare!,
            pricingTierUsed: 'CUSTOMER_ROUTE_AGREEMENT',
            explanation:
                '${ag.agreementName} (${AppFormatters.rupiah(ag.agreedFare)})',
            matchedAgreementName: ag.agreementName,
          );
        }
      }
    }

    // Standard route rule lookup helper
    final routeStandardFare = _lookupRouteRuleFare(
      routeId: routeId,
      vehicleType: vehicleType,
      tripType: tripType,
    );

    // 2. CUSTOMER_GENERAL_AGREEMENT (Discount %)
    if (customer != null) {
      for (final ag in customer.pricingAgreements) {
        if (ag.routeId == null &&
            ag.pricingMode == 'DISCOUNT_PERCENT' &&
            ag.discountPercent != null) {
          final base =
              routeStandardFare ??
              _standardCalcFare(
                vehicleType: vehicleType,
                tripType: tripType,
                distanceKm: dist,
                charterHours: charterHours,
              );
          final discounted = (base * (1.0 - ag.discountPercent! / 100.0))
              .roundToDouble();
          return PricingResolution(
            suggestedFare: discounted,
            pricingTierUsed: 'CUSTOMER_GENERAL_AGREEMENT',
            explanation:
                '${ag.agreementName}: Diskon ${ag.discountPercent!.toStringAsFixed(0)}% dari ${AppFormatters.rupiah(base)}',
            matchedAgreementName: ag.agreementName,
          );
        }
      }
    }

    // 3. ROUTE_PRICING_RULE
    if (routeStandardFare != null) {
      return PricingResolution(
        suggestedFare: routeStandardFare,
        pricingTierUsed: 'ROUTE_PRICING_RULE',
        explanation:
            'Tarif standar rute ${route?.name ?? ""} ($vehicleType)',
      );
    }

    // 4. STANDARD_CALCULATION
    final calc = _standardCalcFare(
      vehicleType: vehicleType,
      tripType: tripType,
      distanceKm: dist,
      charterHours: charterHours,
    );
    return PricingResolution(
      suggestedFare: calc,
      pricingTierUsed: 'STANDARD_CALCULATION',
      explanation:
          'Kalkulasi standar $vehicleType (${dist.toStringAsFixed(0)} km)',
    );
  }

  double? _lookupRouteRuleFare({
    String? routeId,
    required String vehicleType,
    required String tripType,
  }) {
    if (routeId == 'rte-mdn-kno' && vehicleType == 'MPV') return 250000;
    if (routeId == 'rte-kno-mdn' && vehicleType == 'MPV') return 260000;
    if (routeId == 'rte-mdn-kno' && vehicleType == 'HIACE_VAN') return 550000;
    if (routeId == 'rte-kno-mdn' && vehicleType == 'HIACE_VAN') return 575000;
    if (routeId == 'rte-mdn-btg' && vehicleType == 'MPV') return 550000;
    if (routeId == 'rte-mdn-btg' && vehicleType == 'HIACE_VAN') return 1150000;
    if (routeId == 'rte-mdn-ktj' && vehicleType == 'MPV') return 700000;
    if (routeId == 'rte-mdn-tjm' && vehicleType == 'MPV') return 150000;
    return null;
  }

  double _standardCalcFare({
    required String vehicleType,
    required String tripType,
    required double distanceKm,
    int? charterHours,
  }) {
    final (base, perKm, hourly, fullDay, minFare) = switch (vehicleType) {
      'HIACE_VAN' => (150000.0, 9500.0, 180000.0, 1550000.0, 350000.0),
      'SUV' => (80000.0, 7500.0, 140000.0, 1200000.0, 200000.0),
      'SEDAN' => (60000.0, 6500.0, 110000.0, 900000.0, 150000.0),
      _ => (50000.0, 5500.0, 90000.0, 750000.0, 120000.0),
    };
    if (tripType == 'FULL_DAY_CHARTER') return fullDay;
    if (tripType == 'HOURLY_CHARTER') {
      final hrs = charterHours ?? 4;
      return math.max(minFare, hourly * hrs);
    }
    final raw = base + (perKm * distanceKm);
    final rounded = ((raw / 5000).round() * 5000).toDouble();
    return math.max(minFare, rounded);
  }

  // --- Quick Booking & Booking Templates ---

  Future<List<Trip>> createQuickBookingLocal({
    String? customerId,
    String? newCustomerName,
    String? newCustomerPhone,
    String bookingSource = 'WHATSAPP',
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
    final customers = List<Customer>.from(loadCustomers());
    Customer? customer;
    if (customerId != null && customerId.isNotEmpty) {
      customer = customers.where((c) => c.id == customerId).firstOrNull;
    } else if (newCustomerName != null && newCustomerName.trim().isNotEmpty) {
      final newCust = Customer(
        id: 'cust-${_uuid.v4().substring(0, 6)}',
        name: newCustomerName.trim(),
        phone: (newCustomerPhone ?? '').trim(),
        whatsappPhone: (newCustomerPhone ?? '').trim(),
        preferredVehicleType: vehicleTypePreference,
      );
      customers.insert(0, newCust);
      await saveCustomers(customers);
      customer = newCust;
    }

    final route = loadRoutes().where((r) => r.id == routeId).firstOrNull;
    final durationMin = route?.defaultDurationMin ?? 75;
    final prepMin = route?.defaultPrepBufferMin ?? 15;
    final arrMin = tripType == 'AIRPORT_PICKUP'
        ? math.max(30, route?.defaultArrivalBufferMin ?? 30)
        : (route?.defaultArrivalBufferMin ?? 0);
    final repoMin = route?.defaultRepositionBufferMin ?? 15;

    final pickupDt =
        DateTime.tryParse(requestedPickupAt) ??
        DateTime.now().add(const Duration(hours: 2));
    final resStart = pickupDt.subtract(Duration(minutes: prepMin));
    final resEnd = pickupDt.add(
      Duration(minutes: arrMin + durationMin + repoMin),
    );

    final suffix = DateTime.now().millisecondsSinceEpoch
        .toString()
        .substring(8);
    final bookingId = 'bkg-$suffix';
    final bookingCode = 'BKG-20261109-$suffix';

    final leg1 = Trip(
      id: 'trp-$suffix-1',
      tripCode: 'TRP-20261109-$suffix',
      bookingId: bookingId,
      bookingCode: bookingCode,
      customerId: customer?.id,
      customerName: customer?.name ?? newCustomerName ?? 'Pelanggan Baru',
      customerPhone: customer?.phone ?? newCustomerPhone ?? '',
      customerNotes: customer?.notes ?? const [],
      legNumber: 1,
      legLabel: tripType == 'AIRPORT_PICKUP' ? 'Jemput Bandara' : 'Outbound',
      routeId: route?.id,
      routeName: route?.name ?? '$pickupAddress -> $destinationAddress',
      tripType: tripType,
      scheduledPickupAt: pickupDt.toIso8601String(),
      estimatedDurationMin: durationMin,
      prepBufferMin: prepMin,
      arrivalBufferMin: arrMin,
      repositionBufferMin: repoMin,
      reservedStartAt: resStart.toIso8601String(),
      reservedEndAt: resEnd.toIso8601String(),
      pickupAddress: pickupAddress,
      destinationAddress: destinationAddress,
      passengerCount: passengerCount,
      luggageCount: luggageCount,
      flightNumber: flightNumber,
      agreedFareSnapshot: agreedFare,
      status: 'UNASSIGNED',
      stops: stops,
    );

    final created = <Trip>[leg1];

    if (tripType == 'ROUND_TRIP' ||
        (returnPickupAt != null && returnPickupAt.isNotEmpty)) {
      final retDt =
          DateTime.tryParse(returnPickupAt ?? '') ??
          pickupDt.add(const Duration(hours: 6));
      final retStart = retDt.subtract(Duration(minutes: prepMin));
      final retEnd = retDt.add(Duration(minutes: durationMin + repoMin));
      created.add(
        Trip(
          id: 'trp-$suffix-2',
          tripCode: 'TRP-20261109-$suffix-R',
          bookingId: bookingId,
          bookingCode: bookingCode,
          customerId: customer?.id,
          customerName: leg1.customerName,
          customerPhone: leg1.customerPhone,
          customerNotes: leg1.customerNotes,
          legNumber: 2,
          legLabel: 'Perjalanan Pulang (Return)',
          routeId: route?.id,
          routeName: '$destinationAddress -> $pickupAddress',
          tripType: 'ROUND_TRIP',
          scheduledPickupAt: retDt.toIso8601String(),
          estimatedDurationMin: durationMin,
          prepBufferMin: prepMin,
          arrivalBufferMin: 0,
          repositionBufferMin: repoMin,
          reservedStartAt: retStart.toIso8601String(),
          reservedEndAt: retEnd.toIso8601String(),
          pickupAddress: destinationAddress,
          destinationAddress: pickupAddress,
          passengerCount: passengerCount,
          luggageCount: luggageCount,
          agreedFareSnapshot: returnAgreedFare ?? agreedFare,
          status: 'UNASSIGNED',
        ),
      );
    }

    final allTrips = [...created, ...loadTrips()];
    await saveTrips(allTrips);
    return created;
  }

  Future<BookingTemplate> saveBookingTemplateLocal({
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
    final tpl = BookingTemplate(
      id: 'btpl-${_uuid.v4().substring(0, 6)}',
      name: name,
      customerId: customerId,
      routeId: routeId,
      tripType: tripType,
      pickupAddress: pickupAddress,
      destinationAddress: destinationAddress,
      defaultPassengerCount: passengerCount,
      defaultLuggageCount: luggageCount,
      preferredVehicleType: preferredVehicleType,
    );
    await saveBookingTemplates([tpl, ...loadBookingTemplates()]);
    return tpl;
  }

  // --- Dispatch Conflict Simulator & Workload / Suitability Engine ---

  ConflictSimulationResult simulateDispatchLocal({
    required Trip trip,
    String? candidateDriverId,
    String? candidateVehicleId,
  }) {
    final allTrips = loadTrips();
    final drivers = loadDrivers();
    final vehicles = loadVehicles();

    final startDt =
        DateTime.tryParse(trip.reservedStartAt) ??
        DateTime.tryParse(trip.scheduledPickupAt) ??
        DateTime.now();
    final endDt =
        DateTime.tryParse(trip.reservedEndAt) ??
        startDt.add(Duration(minutes: trip.estimatedDurationMin + 30));

    List<ScheduleConflictItem> checkDriver(Driver drv) {
      final conflicts = <ScheduleConflictItem>[];
      if (drv.availabilityStatus != 'AVAILABLE') {
        conflicts.add(
          ScheduleConflictItem(
            type: 'DRIVER_UNAVAILABLE',
            explanation:
                '${drv.fullName} berstatus ${drv.availabilityStatus} (${drv.notes ?? "Tidak tersedia"}).',
          ),
        );
      }
      for (final other in allTrips) {
        if (other.id == trip.id) continue;
        if (other.driverId != drv.id) continue;
        if (const {'CANCELLED', 'NO_SHOW', 'COMPLETED'}.contains(other.status)) {
          continue;
        }
        final oStart = DateTime.tryParse(other.reservedStartAt);
        final oEnd = DateTime.tryParse(other.reservedEndAt);
        if (oStart != null && oEnd != null) {
          if (startDt.isBefore(oEnd) && endDt.isAfter(oStart)) {
            conflicts.add(
              ScheduleConflictItem(
                type: 'TRIP_OVERLAP',
                conflictingTripCode: other.tripCode,
                windowStart: other.reservedStartAt,
                windowEnd: other.reservedEndAt,
                explanation:
                    'Bentrok jadwal supir dengan ${other.tripCode} (${AppFormatters.timeWib(other.reservedStartAt)} – ${AppFormatters.timeWib(other.reservedEndAt)}).',
              ),
            );
          }
        }
      }
      return conflicts;
    }

    List<ScheduleConflictItem> checkVehicle(Vehicle veh) {
      final conflicts = <ScheduleConflictItem>[];
      if (veh.status != 'ACTIVE') {
        conflicts.add(
          ScheduleConflictItem(
            type: 'VEHICLE_IN_MAINTENANCE',
            explanation:
                'Unit ${veh.registrationNumber} sedang berstatus ${veh.status} (${veh.notes ?? "Servis"}).',
          ),
        );
      }
      for (final other in allTrips) {
        if (other.id == trip.id) continue;
        if (other.vehicleId != veh.id) continue;
        if (const {'CANCELLED', 'NO_SHOW', 'COMPLETED'}.contains(other.status)) {
          continue;
        }
        final oStart = DateTime.tryParse(other.reservedStartAt);
        final oEnd = DateTime.tryParse(other.reservedEndAt);
        if (oStart != null && oEnd != null) {
          if (startDt.isBefore(oEnd) && endDt.isAfter(oStart)) {
            conflicts.add(
              ScheduleConflictItem(
                type: 'TRIP_OVERLAP',
                conflictingTripCode: other.tripCode,
                windowStart: other.reservedStartAt,
                windowEnd: other.reservedEndAt,
                explanation:
                    'Bentrok jadwal unit ${veh.registrationNumber} dengan ${other.tripCode} (${AppFormatters.timeWib(other.reservedStartAt)} – ${AppFormatters.timeWib(other.reservedEndAt)}).',
              ),
            );
          }
        }
      }
      return conflicts;
    }

    final selectedDriver = drivers
        .where((d) => d.id == candidateDriverId)
        .firstOrNull;
    final selectedVehicle = vehicles
        .where((v) => v.id == candidateVehicleId)
        .firstOrNull;

    final driverConflicts = selectedDriver != null
        ? checkDriver(selectedDriver)
        : const <ScheduleConflictItem>[];
    final vehicleConflicts = selectedVehicle != null
        ? checkVehicle(selectedVehicle)
        : const <ScheduleConflictItem>[];

    // Driver workload recommendations
    final targetDay = startDt.toUtc().add(const Duration(hours: 7));
    final driverRecs = <DriverRecommendation>[];
    for (final drv in drivers) {
      final dConf = checkDriver(drv);
      var reservedMin = 0;
      var activeMin = 0;
      for (final other in allTrips) {
        if (other.driverId != drv.id || other.id == trip.id) continue;
        if (other.status == 'CANCELLED' || other.status == 'NO_SHOW') continue;
        final oDt = DateTime.tryParse(other.scheduledPickupAt)
            ?.toUtc()
            .add(const Duration(hours: 7));
        if (oDt != null &&
            oDt.year == targetDay.year &&
            oDt.month == targetDay.month &&
            oDt.day == targetDay.day) {
          activeMin += other.estimatedDurationMin;
          reservedMin +=
              other.prepBufferMin +
              other.arrivalBufferMin +
              other.estimatedDurationMin +
              other.repositionBufferMin;
        }
      }
      final resHours = double.parse((reservedMin / 60.0).toStringAsFixed(1));
      final actHours = double.parse((activeMin / 60.0).toStringAsFixed(1));
      final conflictFree = dConf.isEmpty;
      driverRecs.add(
        DriverRecommendation(
          driverId: drv.id,
          driverName: drv.fullName,
          availabilityStatus: drv.availabilityStatus,
          conflictFree: conflictFree,
          reservedHoursTargetDay: resHours,
          activeHoursTargetDay: actHours,
          recommended: conflictFree && resHours <= 5.0,
          explanation: conflictFree
              ? 'Bebas bentrok • Beban hari itu $resHours jam / maks ${drv.maxDailyHours.toStringAsFixed(0)} jam'
              : dConf.first.explanation,
        ),
      );
    }
    driverRecs.sort((a, b) {
      if (a.conflictFree != b.conflictFree) return a.conflictFree ? -1 : 1;
      return a.reservedHoursTargetDay.compareTo(b.reservedHoursTargetDay);
    });

    // Customer preferred vehicle
    final custPref = trip.customerId != null
        ? loadCustomers()
              .where((c) => c.id == trip.customerId)
              .firstOrNull
              ?.preferredVehicleType
        : null;

    final vehicleRecs = <VehicleRecommendation>[];
    for (final veh in vehicles) {
      final vConf = checkVehicle(veh);
      final conflictFree = vConf.isEmpty;
      final fitsPax = veh.passengerCapacity >= trip.passengerCount;
      final fitsBags = veh.luggageCapacityBags >= trip.luggageCount;
      final suitable = conflictFree && fitsPax && fitsBags;
      var score = 60;
      if (conflictFree) score += 20;
      if (fitsPax && fitsBags) score += 15;
      if (custPref != null && veh.vehicleType == custPref) score += 5;
      if (!fitsPax || !fitsBags) score -= 35;

      String reason;
      if (!conflictFree) {
        reason = vConf.first.explanation;
      } else if (!fitsPax || !fitsBags) {
        reason =
            'Kapasitas kurang (${veh.passengerCapacity} kursi / ${veh.luggageCapacityBags} tas vs kebutuhan ${trip.passengerCount} org / ${trip.luggageCount} tas)';
      } else if (custPref == veh.vehicleType) {
        reason =
            'Sesuai preferensi pelanggan ($custPref) • Kapasitas ${veh.passengerCapacity} kursi / ${veh.luggageCapacityBags} tas';
      } else {
        reason =
            'Siap jalan • Kapasitas ${veh.passengerCapacity} kursi / ${veh.luggageCapacityBags} tas';
      }

      vehicleRecs.add(
        VehicleRecommendation(
          vehicleId: veh.id,
          registrationNumber: veh.registrationNumber,
          nameModel: veh.nameModel,
          vehicleType: veh.vehicleType,
          passengerCapacity: veh.passengerCapacity,
          luggageCapacityBags: veh.luggageCapacityBags,
          conflictFree: conflictFree,
          suitable: suitable,
          suitabilityScore: score.clamp(0, 100),
          explanation: reason,
        ),
      );
    }
    vehicleRecs.sort((a, b) => b.suitabilityScore.compareTo(a.suitabilityScore));

    return ConflictSimulationResult(
      reservedStartAt: startDt.toIso8601String(),
      reservedEndAt: endDt.toIso8601String(),
      hasDriverConflict: driverConflicts.isNotEmpty,
      driverConflicts: driverConflicts,
      hasVehicleConflict: vehicleConflicts.isNotEmpty,
      vehicleConflicts: vehicleConflicts,
      driverRecommendations: driverRecs,
      vehicleRecommendations: vehicleRecs,
    );
  }

  Future<Trip?> assignTripLocal({
    required String tripId,
    required String driverId,
    required String vehicleId,
  }) async {
    final trips = loadTrips();
    final idx = trips.indexWhere((t) => t.id == tripId);
    if (idx < 0) return null;
    final drv = loadDrivers().firstWhere((d) => d.id == driverId);
    final veh = loadVehicles().firstWhere((v) => v.id == vehicleId);

    final current = trips[idx];
    final nextStatus = current.status == 'UNASSIGNED'
        ? 'ASSIGNED'
        : current.status;
    final updated = current.copyWith(
      driverId: drv.id,
      driverName: drv.fullName,
      driverPhone: drv.phone,
      vehicleId: veh.id,
      vehiclePlate: veh.registrationNumber,
      vehicleModel: veh.nameModel,
      status: nextStatus,
      version: current.version + 1,
    );
    trips[idx] = updated;
    await saveTrips(trips);
    return updated;
  }

  // --- Cost Formula & Trip Profitability Engine ---

  TripProfitability computeTripProfitability(Trip trip) {
    final formulas = loadCostFormulas();
    final activeFormula = formulas.firstWhere(
      (f) => f.isActive,
      orElse: () => formulas.first,
    );

    double distanceKm = 39.0;
    String distanceSource = 'ESTIMASI_RUTE';
    if (trip.odometerStartKm != null &&
        trip.odometerEndKm != null &&
        trip.odometerEndKm! > trip.odometerStartKm!) {
      distanceKm = trip.odometerEndKm! - trip.odometerStartKm!;
      distanceSource = 'ODOMETER';
    } else if (trip.gpsDistanceKm != null && trip.gpsDistanceKm! > 0) {
      distanceKm = trip.gpsDistanceKm!;
      distanceSource = 'GPS_TRACK';
    } else {
      final route = loadRoutes().where((r) => r.id == trip.routeId).firstOrNull;
      if (route != null) {
        distanceKm = route.typicalDistanceKm;
      }
    }

    final expenseByCategory = <String, double>{};
    var actualExpensesTotal = 0.0;
    for (final exp in trip.expenses) {
      expenseByCategory[exp.category] =
          (expenseByCategory[exp.category] ?? 0) + exp.amount;
      actualExpensesTotal += exp.amount;
    }

    final items = <TripProfitabilityItem>[];
    var estimatedOperatingCost = 0.0;
    var allocatedOperatingCost = 0.0;
    var totalEffectiveCost = 0.0;

    for (final comp in activeFormula.components) {
      var est = 0.0;
      switch (comp.calcType) {
        case 'PER_KM':
          est = (comp.rateValue ?? 0) * distanceKm;
          break;
        case 'FIXED_PER_TRIP':
          est = comp.fixedAmount ?? 0;
          break;
        case 'MONTHLY_ALLOCATION':
          final tripsPerMonth = comp.assumedMonthlyTrips ?? 60;
          est = (comp.monthlyAmount ?? 0) / math.max(1, tripsPerMonth);
          break;
        case 'PERCENT_OF_FARE':
          est = trip.agreedFareSnapshot * (comp.rateValue ?? 0.20);
          break;
        case 'FORMULA_ALLOCATION':
          final params = comp.parametersJson ?? const {};
          if (params.containsKey('replacement_cost')) {
            final rc = asDouble(params['replacement_cost'], 4400000);
            final life = asDouble(params['expected_lifespan_km'], 55000);
            est = (rc / math.max(1, life)) * distanceKm;
          } else if (params.containsKey('service_cost')) {
            final sc = asDouble(params['service_cost'], 850000);
            final interval = asDouble(params['service_interval_km'], 10000);
            est = (sc / math.max(1, interval)) * distanceKm;
          }
          break;
      }

      final linkedCat = comp.expenseCategoryLink;
      final actual = linkedCat != null
          ? (expenseByCategory[linkedCat] ?? 0.0)
          : 0.0;

      double effective;
      if (comp.reconciliationPolicy == 'ACTUAL_OVERRIDES_ESTIMATE') {
        effective = actual > 0 ? actual : est;
        if (actual == 0) allocatedOperatingCost += est;
      } else if (comp.reconciliationPolicy == 'ACTUAL_EXPENSE_ONLY') {
        effective = actual;
      } else {
        effective = est;
        allocatedOperatingCost += est;
      }

      estimatedOperatingCost += est;
      totalEffectiveCost += effective;

      items.add(
        TripProfitabilityItem(
          componentCode: comp.componentCode,
          name: comp.name,
          reconciliationPolicy: comp.reconciliationPolicy,
          estimatedAmount: est.roundToDouble(),
          actualAmount: actual.roundToDouble(),
          effectiveAmount: effective.roundToDouble(),
        ),
      );
    }

    // Add uncategorized DRIVER_MEAL / OTHER expenses into effective cost
    for (final entry in expenseByCategory.entries) {
      if (!const {'FUEL', 'TOLL', 'PARKING'}.contains(entry.key)) {
        totalEffectiveCost += entry.value;
        items.add(
          TripProfitabilityItem(
            componentCode: entry.key,
            name: 'Pengeluaran Aktual (${entry.key})',
            reconciliationPolicy: 'ACTUAL_EXPENSE_ONLY',
            estimatedAmount: 0,
            actualAmount: entry.value,
            effectiveAmount: entry.value,
          ),
        );
      }
    }

    return TripProfitability(
      tripId: trip.id,
      agreedFareSnapshot: trip.agreedFareSnapshot,
      formulaVersionNumber: activeFormula.versionNumber,
      distanceKmUsed: distanceKm,
      distanceSource: distanceSource,
      estimatedOperatingCost: estimatedOperatingCost.roundToDouble(),
      actualExpensesTotal: actualExpensesTotal.roundToDouble(),
      allocatedOperatingCost: allocatedOperatingCost.roundToDouble(),
      totalEffectiveCost: totalEffectiveCost.roundToDouble(),
      estimatedProfit:
          (trip.agreedFareSnapshot - estimatedOperatingCost).roundToDouble(),
      actualProfit:
          (trip.agreedFareSnapshot - totalEffectiveCost).roundToDouble(),
      items: items,
    );
  }

  Future<CostFormulaVersion> createCostFormulaVersionLocal({
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
    final existing = loadCostFormulas();
    final maxVer = existing.fold<int>(
      0,
      (prev, e) => math.max(prev, e.versionNumber),
    );
    final deactivated = existing
        .map(
          (f) => f.isActive
              ? f.copyWith(isActive: false, effectiveTo: effectiveFrom)
              : f,
        )
        .toList();

    final newVer = CostFormulaVersion(
      id: 'cfv-v${maxVer + 1}',
      versionNumber: maxVer + 1,
      name: name,
      effectiveFrom: effectiveFrom,
      isActive: true,
      notes: notes,
      components: [
        CostFormulaComponent(
          id: 'cfc-v${maxVer + 1}-fuel',
          componentCode: 'FUEL',
          name:
              'BBM (Aktual atau Estimasi ${AppFormatters.rupiah(fuelPerKm)}/km)',
          calcType: 'PER_KM',
          reconciliationPolicy: 'ACTUAL_OVERRIDES_ESTIMATE',
          expenseCategoryLink: 'FUEL',
          rateValue: fuelPerKm,
          sortOrder: 1,
        ),
        const CostFormulaComponent(
          id: 'cfc-toll',
          componentCode: 'TOLL',
          name: 'Biaya Tol Aktual',
          calcType: 'FIXED_PER_TRIP',
          reconciliationPolicy: 'ACTUAL_EXPENSE_ONLY',
          expenseCategoryLink: 'TOLL',
          fixedAmount: 0,
          sortOrder: 2,
        ),
        const CostFormulaComponent(
          id: 'cfc-park',
          componentCode: 'PARKING',
          name: 'Parkir Aktual',
          calcType: 'FIXED_PER_TRIP',
          reconciliationPolicy: 'ACTUAL_EXPENSE_ONLY',
          expenseCategoryLink: 'PARKING',
          fixedAmount: 0,
          sortOrder: 3,
        ),
        CostFormulaComponent(
          id: 'cfc-v${maxVer + 1}-tire',
          componentCode: 'TIRE_WEAR',
          name:
              'Alokasi Aus Ban (${AppFormatters.rupiah(tireReplacementCost)} / ${AppFormatters.thousandNumber(tireLifespanKm)} km)',
          calcType: 'FORMULA_ALLOCATION',
          reconciliationPolicy: 'ALLOCATED_ONLY',
          expressionFormula:
              '(replacement_cost / expected_lifespan_km) * distance_km',
          parametersJson: {
            'replacement_cost': tireReplacementCost,
            'expected_lifespan_km': tireLifespanKm,
          },
          sortOrder: 4,
        ),
        const CostFormulaComponent(
          id: 'cfc-oil',
          componentCode: 'ENGINE_OIL',
          name: 'Alokasi Oli & Filter (Rp850rb / 10.000 km)',
          calcType: 'FORMULA_ALLOCATION',
          reconciliationPolicy: 'ALLOCATED_ONLY',
          expressionFormula:
              '(service_cost / service_interval_km) * distance_km',
          parametersJson: {
            'service_cost': 850000,
            'service_interval_km': 10000,
          },
          sortOrder: 5,
        ),
        CostFormulaComponent(
          id: 'cfc-v${maxVer + 1}-maint',
          componentCode: 'MAINTENANCE_RESERVE',
          name:
              'Cadangan Servis Berkala (${AppFormatters.rupiah(maintenanceReservePerKm)}/km)',
          calcType: 'PER_KM',
          reconciliationPolicy: 'ALLOCATED_ONLY',
          rateValue: maintenanceReservePerKm,
          sortOrder: 6,
        ),
        CostFormulaComponent(
          id: 'cfc-v${maxVer + 1}-depr',
          componentCode: 'DEPRECIATION',
          name: 'Alokasi Penyusutan Unit Bulanan',
          calcType: 'MONTHLY_ALLOCATION',
          reconciliationPolicy: 'ALLOCATED_ONLY',
          monthlyAmount: monthlyDepreciation,
          assumedMonthlyTrips: 60,
          sortOrder: 7,
        ),
        CostFormulaComponent(
          id: 'cfc-v${maxVer + 1}-drv',
          componentCode: 'DRIVER_COMPENSATION',
          name:
              'Komisi Supir (${(driverCommissionRate * 100).toStringAsFixed(0)}% Tarif)',
          calcType: 'PERCENT_OF_FARE',
          reconciliationPolicy: 'ALLOCATED_ONLY',
          rateValue: driverCommissionRate,
          sortOrder: 8,
        ),
      ],
    );

    await saveCostFormulas([newVer, ...deactivated]);
    return newVer;
  }

  // --- Customer, Fleet & Settings Helpers ---

  Future<Customer> addCustomerNoteLocal({
    required String customerId,
    required String category,
    required String noteText,
    bool visibleToDrivers = true,
  }) async {
    final customers = loadCustomers();
    final idx = customers.indexWhere((c) => c.id == customerId);
    final note = CustomerNote(
      id: 'cnote-${_uuid.v4().substring(0, 6)}',
      category: category,
      noteText: noteText,
      visibleToDrivers: visibleToDrivers,
    );
    final updated = customers[idx].copyWith(
      notes: [note, ...customers[idx].notes],
    );
    customers[idx] = updated;
    await saveCustomers(customers);
    return updated;
  }

  Future<Customer> addCustomerAddressLocal({
    required String customerId,
    required String label,
    required String addressText,
    bool isDefault = false,
  }) async {
    final customers = loadCustomers();
    final idx = customers.indexWhere((c) => c.id == customerId);
    final encoded = Uri.encodeComponent(addressText);
    final addr = CustomerAddress(
      id: 'caddr-${_uuid.v4().substring(0, 6)}',
      label: label,
      addressText: addressText,
      googleMapsUrl: 'https://www.google.com/maps/search/?api=1&query=$encoded',
      isDefault: isDefault,
    );
    final updated = customers[idx].copyWith(
      addresses: [addr, ...customers[idx].addresses],
    );
    customers[idx] = updated;
    await saveCustomers(customers);
    return updated;
  }

  Future<Customer> addCustomerPricingAgreementLocal({
    required String customerId,
    required String agreementName,
    String? routeId,
    String? vehicleType,
    required double agreedFare,
    String? conditionsNote,
  }) async {
    final customers = loadCustomers();
    final idx = customers.indexWhere((c) => c.id == customerId);
    final ag = CustomerPricingAgreement(
      id: 'cpa-${_uuid.v4().substring(0, 6)}',
      customerId: customerId,
      agreementName: agreementName,
      routeId: routeId,
      vehicleType: vehicleType,
      pricingMode: 'FIXED_FARE',
      agreedFare: agreedFare,
      effectiveFrom: DateTime.now().toIso8601String().split('T').first,
      conditionsNote: conditionsNote,
    );
    final updated = customers[idx].copyWith(
      pricingAgreements: [ag, ...customers[idx].pricingAgreements],
    );
    customers[idx] = updated;
    await saveCustomers(customers);
    return updated;
  }

  Future<Customer> createCustomerLocal({
    required String name,
    required String phone,
    String? companyName,
    String preferredVehicleType = 'MPV',
  }) async {
    final cust = Customer(
      id: 'cust-${_uuid.v4().substring(0, 6)}',
      name: name,
      phone: phone,
      whatsappPhone: phone,
      companyName: companyName,
      preferredVehicleType: preferredVehicleType,
    );
    await saveCustomers([cust, ...loadCustomers()]);
    return cust;
  }

  Future<void> updateDriverAvailabilityLocal({
    required String driverId,
    required String availabilityStatus,
    String? notes,
  }) async {
    final drivers = loadDrivers()
        .map(
          (d) => d.id == driverId
              ? d.copyWith(
                  availabilityStatus: availabilityStatus,
                  notes: notes ?? d.notes,
                )
              : d,
        )
        .toList();
    await saveDrivers(drivers);
  }

  Future<void> addVehicleMaintenanceLogLocal({
    required String vehicleId,
    required String maintenanceType,
    required double odometerKm,
    required String vendorName,
    required double costAmount,
    required double downtimeHours,
    required String description,
  }) async {
    final vehicles = loadVehicles();
    final idx = vehicles.indexWhere((v) => v.id == vehicleId);
    if (idx < 0) return;
    final log = VehicleMaintenanceLog(
      id: 'vml-${_uuid.v4().substring(0, 6)}',
      maintenanceType: maintenanceType,
      serviceDate: DateTime.now().toIso8601String().split('T').first,
      odometerKm: odometerKm,
      vendorName: vendorName,
      costAmount: costAmount,
      downtimeHours: downtimeHours,
      description: description,
    );
    final veh = vehicles[idx];
    vehicles[idx] = veh.copyWith(
      currentOdometerKm: math.max(veh.currentOdometerKm, odometerKm),
      recentMaintenanceLogs: [log, ...veh.recentMaintenanceLogs],
    );
    await saveVehicles(vehicles);
  }

  Future<void> addVehicleDocumentLocal({
    required String vehicleId,
    required String docType,
    required String docNumber,
    required String expiryDate,
    String? notes,
  }) async {
    final vehicles = loadVehicles();
    final idx = vehicles.indexWhere((v) => v.id == vehicleId);
    if (idx < 0) return;
    final doc = VehicleDocument(
      id: 'vdoc-${_uuid.v4().substring(0, 6)}',
      docType: docType,
      docNumber: docNumber,
      issueDate: DateTime.now().toIso8601String().split('T').first,
      expiryDate: expiryDate,
      notes: notes,
    );
    vehicles[idx] = vehicles[idx].copyWith(
      documents: [doc, ...vehicles[idx].documents],
    );
    await saveVehicles(vehicles);
  }

  Future<void> acknowledgeNotificationLocal(String notificationId) async {
    final updated = loadNotifications()
        .map(
          (n) => n.id == notificationId ? n.copyWith(acknowledged: true) : n,
        )
        .toList();
    await saveNotifications(updated);
  }

  Future<void> updateNotificationSettingLocal({
    required String settingId,
    int? leadTimeMinutes,
    String? recipientScope,
    bool? enabled,
    bool? acknowledgementEnabled,
  }) async {
    final updated = loadNotificationSettings()
        .map(
          (s) => s.id == settingId
              ? s.copyWith(
                  leadTimeMinutes: leadTimeMinutes,
                  recipientScope: recipientScope,
                  enabled: enabled,
                  acknowledgementEnabled: acknowledgementEnabled,
                )
              : s,
        )
        .toList();
    await saveNotificationSettings(updated);
  }

  // --- Smart Operations & Cross-Filtered Business Intelligence Explorer ---

  BiExplorerData computeBiExplorer({
    String? routeId,
    String? vehicleId,
    String? driverId,
    String? customerId,
  }) {
    final allTrips = loadTrips();
    final routes = loadRoutes();
    final customers = loadCustomers();
    final drivers = loadDrivers();
    final vehicles = loadVehicles();

    final filteredTrips = allTrips.where((t) {
      if (routeId != null && routeId.isNotEmpty && t.routeId != routeId) {
        return false;
      }
      if (vehicleId != null &&
          vehicleId.isNotEmpty &&
          t.vehicleId != vehicleId) {
        return false;
      }
      if (driverId != null && driverId.isNotEmpty && t.driverId != driverId) {
        return false;
      }
      if (customerId != null &&
          customerId.isNotEmpty &&
          t.customerId != customerId) {
        return false;
      }
      return true;
    }).toList();

    final completedTrips = filteredTrips
        .where((t) => t.status == 'COMPLETED')
        .toList();

    var totalRevenue = 0.0;
    var totalActualExpenses = 0.0;
    var totalAllocatedCosts = 0.0;
    var grossOperatingProfit = 0.0;
    var unpaidBalances = 0.0;

    for (final t in completedTrips) {
      final prof = computeTripProfitability(t);
      totalRevenue += t.agreedFareSnapshot;
      totalActualExpenses += prof.actualExpensesTotal;
      totalAllocatedCosts += prof.allocatedOperatingCost;
      grossOperatingProfit += prof.actualProfit;
    }

    for (final t in filteredTrips) {
      if (t.status != 'CANCELLED' && t.status != 'NO_SHOW') {
        unpaidBalances += t.remainingUnpaidAmount;
      }
    }

    final driverHeldCash = drivers.fold<double>(
      0,
      (sum, d) =>
          (driverId == null || driverId.isEmpty || d.id == driverId)
          ? sum + d.cashHeldAmount
          : sum,
    );

    // Route performance with minSampleSize = 5 guard
    final routePerf = <RoutePerformanceItem>[];
    for (final r in routes) {
      if (routeId != null && routeId.isNotEmpty && r.id != routeId) continue;
      final rTrips = completedTrips.where((t) => t.routeId == r.id).toList();
      if (rTrips.isEmpty) continue;
      final count = rTrips.length;
      final avgDur =
          rTrips.fold<double>(
            0,
            (s, t) => s + (t.actualDurationMin ?? t.estimatedDurationMin),
          ) /
          count;
      final avgDist =
          rTrips.fold<double>(
            0,
            (s, t) => s + computeTripProfitability(t).distanceKmUsed,
          ) /
          count;
      final avgCost =
          rTrips.fold<double>(
            0,
            (s, t) => s + computeTripProfitability(t).totalEffectiveCost,
          ) /
          count;
      final avgProfit =
          rTrips.fold<double>(
            0,
            (s, t) => s + computeTripProfitability(t).actualProfit,
          ) /
          count;
      routePerf.add(
        RoutePerformanceItem(
          routeId: r.id,
          routeName: r.name,
          sampleCount: count,
          minSampleRequired: 5,
          sufficientSample: count >= 5,
          avgDurationMin: avgDur,
          avgDistanceKm: avgDist,
          avgEffectiveCost: avgCost,
          avgActualProfit: avgProfit,
        ),
      );
    }

    // Customer profitability
    final custProf = <CustomerProfitabilityItem>[];
    for (final c in customers) {
      if (customerId != null && customerId.isNotEmpty && c.id != customerId) {
        continue;
      }
      final cTrips = completedTrips.where((t) => t.customerId == c.id).toList();
      if (cTrips.isEmpty) continue;
      var rev = 0.0;
      var cost = 0.0;
      var net = 0.0;
      for (final t in cTrips) {
        final p = computeTripProfitability(t);
        rev += t.agreedFareSnapshot;
        cost += p.totalEffectiveCost;
        net += p.actualProfit;
      }
      final summary = c.pricingAgreements.isNotEmpty
          ? c.pricingAgreements.first.agreementName
          : 'Tarif Standar Rute';
      custProf.add(
        CustomerProfitabilityItem(
          customerId: c.id,
          customerName: c.name,
          tripCount: cTrips.length,
          totalRevenue: rev,
          totalEffectiveCost: cost,
          netOperatingProfit: net,
          pricingSummary: summary,
        ),
      );
    }

    // Driver workload
    final workloads = <DriverWorkloadItem>[];
    for (final d in drivers) {
      if (driverId != null && driverId.isNotEmpty && d.id != driverId) continue;
      final dCompleted = completedTrips
          .where((t) => t.driverId == d.id)
          .toList();
      final dUpcoming = filteredTrips
          .where(
            (t) =>
                t.driverId == d.id &&
                !const {'COMPLETED', 'CANCELLED', 'NO_SHOW'}.contains(t.status),
          )
          .toList();
      final activeMinutes = dCompleted.fold<int>(
        0,
        (s, t) => s + (t.actualDurationMin ?? t.estimatedDurationMin),
      );
      workloads.add(
        DriverWorkloadItem(
          driverId: d.id,
          driverName: d.fullName,
          availabilityStatus: d.availabilityStatus,
          completedTripsCount: dCompleted.length,
          upcomingTripsCount: dUpcoming.length,
          totalActiveHours: double.parse(
            (activeMinutes / 60.0).toStringAsFixed(1),
          ),
          maxDailyHours: d.maxDailyHours,
        ),
      );
    }

    // Vehicle Replacement Intelligence
    final replacementItems = <VehicleReplacementItem>[];
    for (final v in vehicles) {
      if (vehicleId != null && vehicleId.isNotEmpty && v.id != vehicleId) {
        continue;
      }
      final repairSum = v.recentMaintenanceLogs.fold<double>(
        0,
        (s, l) => s + l.costAmount,
      );
      final downtimeSum = v.recentMaintenanceLogs.fold<double>(
        0,
        (s, l) => s + l.downtimeHours,
      );
      final costPerKm = v.currentOdometerKm > 0
          ? repairSum / v.currentOdometerKm
          : 0.0;
      final flagged =
          repairSum >= 8000000 ||
          downtimeSum >= 48 ||
          v.currentOdometerKm >= 150000;
      replacementItems.add(
        VehicleReplacementItem(
          vehicleId: v.id,
          registrationNumber: v.registrationNumber,
          nameModel: v.nameModel,
          manufactureYear: v.manufactureYear,
          odometerKm: v.currentOdometerKm,
          totalRepairCost: repairSum,
          totalDowntimeHours: downtimeSum,
          repairCostPerKm: double.parse(costPerKm.toStringAsFixed(1)),
          flagForEvaluation: flagged,
          evaluationNote: flagged
              ? 'Biaya perbaikan ${AppFormatters.rupiah(repairSum)} & downtime ${downtimeSum.toStringAsFixed(0)} jam tinggi pada odometer ${AppFormatters.thousandNumber(v.currentOdometerKm)} km — disarankan evaluasi peremajaan unit.'
              : 'Kondisi operasional sehat & biaya perawatan terkendali.',
        ),
      );
    }
    replacementItems.sort(
      (a, b) => b.totalRepairCost.compareTo(a.totalRepairCost),
    );

    return BiExplorerData(
      totalRevenue: totalRevenue,
      totalActualExpenses: totalActualExpenses,
      totalAllocatedCosts: totalAllocatedCosts,
      grossOperatingProfit: grossOperatingProfit,
      unpaidBalances: unpaidBalances,
      driverHeldCash: driverHeldCash,
      routePerformance: routePerf,
      customerProfitability: custProf,
      driverWorkloads: workloads,
      vehicleReplacementItems: replacementItems,
      anomalyAlerts: computeAnomalyAlerts(),
    );
  }

  List<AnomalyAlert> computeAnomalyAlerts() {
    final alerts = <AnomalyAlert>[];
    final trips = loadTrips();
    final knoTrips = trips
        .where((t) => t.routeId == 'rte-mdn-kno' && t.status == 'COMPLETED')
        .toList();

    if (knoTrips.length >= 5) {
      final avgFuel =
          knoTrips.fold<double>(0, (s, t) {
            final f = t.expenses
                .where((e) => e.category == 'FUEL')
                .fold<double>(0, (a, b) => a + b.amount);
            return s + f;
          }) /
          knoTrips.length;
      final avgDur =
          knoTrips.fold<double>(
            0,
            (s, t) => s + (t.actualDurationMin ?? t.estimatedDurationMin),
          ) /
          knoTrips.length;

      for (final t in knoTrips) {
        final fuelAmt = t.expenses
            .where((e) => e.category == 'FUEL')
            .fold<double>(0, (a, b) => a + b.amount);
        if (fuelAmt > avgFuel * 1.4) {
          alerts.add(
            AnomalyAlert(
              id: 'anom-fuel-${t.id}',
              ruleCode: 'HIGH_FUEL_EXPENSE',
              title: 'Konsumsi BBM di Atas Rata-Rata Rute',
              subjectLabel: '${t.tripCode} • ${t.routeName}',
              observedDisplay: AppFormatters.rupiah(fuelAmt),
              baselineDisplay: AppFormatters.rupiah(avgFuel),
              thresholdDisplay:
                  '> ${AppFormatters.rupiah(avgFuel * 1.4)} (1,4x)',
              explanation:
                  'Biaya BBM perjalanan ini tercatat lebih tinggi dari rata-rata historis rute Medan -> KNO.',
              reviewHint:
                  'Tinjau catatan lalu lintas/kemacetan atau struk SPBU bersama ${t.driverName ?? "supir"} secara wajar.',
            ),
          );
        }
        final dur = (t.actualDurationMin ?? t.estimatedDurationMin).toDouble();
        if (dur > avgDur * 1.5) {
          alerts.add(
            AnomalyAlert(
              id: 'anom-dur-${t.id}',
              ruleCode: 'LONG_TRIP_DURATION',
              title: 'Durasi Perjalanan Lebih Lama dari Biasanya',
              subjectLabel: '${t.tripCode} • ${t.routeName}',
              observedDisplay: '${dur.toStringAsFixed(0)} mnt',
              baselineDisplay: '${avgDur.toStringAsFixed(0)} mnt',
              thresholdDisplay: '> ${(avgDur * 1.5).toStringAsFixed(0)} mnt (1,5x)',
              explanation:
                  'Durasi aktual melebihi 1,5x rata-rata historis rute Medan -> KNO.',
              reviewHint:
                  'Periksa apakah terdapat penutupan jalan arteri atau antrean panjang bandara pada jam tersebut.',
            ),
          );
        }
      }
    }

    for (final d in loadDrivers()) {
      if (d.cashHeldAmount > 300000) {
        alerts.add(
          AnomalyAlert(
            id: 'anom-cash-${d.id}',
            ruleCode: 'AGED_DRIVER_CASH',
            title: 'Uang Tunai di Tangan Supir > Rp 300.000',
            subjectLabel: d.fullName,
            observedDisplay: AppFormatters.rupiah(d.cashHeldAmount),
            baselineDisplay: 'Rp 0 (Disetor Harian)',
            thresholdDisplay: 'Rp 300.000',
            explanation:
                'Terdapat pembayaran tunai pelanggan di ${d.fullName} yang belum dikonfirmasi setorannya.',
            reviewHint:
                'Jadwalkan konfirmasi setoran tunai saat supir kembali ke garasi.',
          ),
        );
      }
    }

    for (final v in loadVehicles()) {
      for (final log in v.recentMaintenanceLogs) {
        if (log.costAmount >= 4000000) {
          alerts.add(
            AnomalyAlert(
              id: 'anom-maint-${log.id}',
              ruleCode: 'UNUSUAL_MAINTENANCE_COST',
              title: 'Biaya Perbaikan Unit Tinggi',
              subjectLabel: '${v.registrationNumber} (${v.nameModel})',
              observedDisplay: AppFormatters.rupiah(log.costAmount),
              baselineDisplay: 'Rp 1.500.000',
              thresholdDisplay: 'Rp 4.000.000',
              explanation: '${log.description} di ${log.vendorName}.',
              reviewHint:
                  'Tinjau riwayat komponen & kelayakan peremajaan unit pada tab Analitik BI.',
            ),
          );
          break;
        }
      }
    }

    return alerts;
  }
}
