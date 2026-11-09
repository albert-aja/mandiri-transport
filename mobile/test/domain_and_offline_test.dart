import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:mandiri_transport_mobile/core/utils/deep_links.dart';
import 'package:mandiri_transport_mobile/core/utils/formatters.dart';
import 'package:mandiri_transport_mobile/data/local/offline_store.dart';
import 'package:mandiri_transport_mobile/data/models/models.dart';

void main() {
  group('Formatters & Zero-Cost Deep Links', () {
    test('Rupiah formatter & thousand input formatter handle Medan fares', () {
      expect(AppFormatters.rupiah(220000), 'Rp 220.000');
      expect(AppFormatters.rupiah(870000), 'Rp 870.000');
      expect(AppFormatters.rupiah(0), 'Rp 0');
      expect(AppFormatters.rupiah(-50000), '-Rp 50.000');

      final inputFormatter = RupiahInputFormatter();
      final edited = inputFormatter.formatEditUpdate(
        TextEditingValue.empty,
        const TextEditingValue(text: '250000'),
      );
      expect(edited.text, '250.000');
      expect(AppFormatters.parseRupiah('Rp 250.000'), 250000);
    });

    test('GPS staleness formatter produces clear Bahasa Indonesia labels', () {
      final now = DateTime.utc(2026, 11, 9, 14, 0, 0);
      final twelveMinAgo = now
          .subtract(const Duration(minutes: 12))
          .toIso8601String();
      expect(
        AppFormatters.gpsStaleness(twelveMinAgo, nowOverride: now),
        'Update GPS: 12 mnt lalu',
      );
      expect(
        AppFormatters.gpsStaleness(null, nowOverride: now),
        'Belum ada sinyal GPS tercatat',
      );
    });

    test('Google Maps & WhatsApp wa.me builders normalize phone & URLs', () {
      final mapsUrl = DeepLinks.buildGoogleMapsNavigationUrl(
        destination: 'Bandara Kualanamu (KNO)',
      );
      expect(
        mapsUrl,
        'https://www.google.com/maps/dir/?api=1&destination=Bandara%20Kualanamu%20(KNO)',
      );

      expect(DeepLinks.normalizeIndonesianPhone('0812-6002-001'), '628126002001');
      expect(DeepLinks.normalizeIndonesianPhone('+628116100101'), '628116100101');

      final waUrl = DeepLinks.buildWhatsappUrl(
        phone: '08116100101',
        message: 'Halo Ibu Ratna',
      );
      expect(waUrl, 'https://wa.me/628116100101?text=Halo%20Ibu%20Ratna');
    });
  });

  group('OfflineStore Outbox Idempotency, GPS Gate & Financial Preservation', () {
    setUp(() {
      SharedPreferences.setMockInitialValues({});
    });

    test('clientMutationId is idempotent and prevents duplicate outbox entries', () async {
      final store = await OfflineStore.create();
      expect(store.loadOutbox(), isEmpty);

      await store.enqueueMutation(
        clientMutationId: 'mut-fixed-001',
        mutationType: 'TRIP_STATUS',
        entityId: 'trp-201',
        expectedVersion: 1,
        payload: {'newStatus': 'ACCEPTED'},
      );
      // Duplicate call with same clientMutationId must not create a second item
      await store.enqueueMutation(
        clientMutationId: 'mut-fixed-001',
        mutationType: 'TRIP_STATUS',
        entityId: 'trp-201',
        expectedVersion: 1,
        payload: {'newStatus': 'ACCEPTED'},
      );

      expect(store.loadOutbox().length, 1);
      expect(store.loadOutbox().first.clientMutationId, 'mut-fixed-001');
    });

    test('Offline status + cash payment queuing & conflict staging never deletes financial records', () async {
      final store = await OfflineStore.create();

      // 1. Driver accepts KNO pickup trip offline
      await store.applyTripStatusLocal(
        tripId: 'trp-201',
        newStatus: 'ACCEPTED',
        clientMutationId: 'mut-status-201',
      );

      // 2. Driver collects Rp 230.000 CASH offline & logs Rp 28.000 toll expense
      await store.recordTripPaymentLocal(
        tripId: 'trp-201',
        amount: 230000,
        paymentMethod: 'CASH',
        collectedByRole: 'DRIVER',
        driverId: 'drv-budi',
        clientMutationId: 'mut-pay-201',
      );
      await store.recordTripExpenseLocal(
        tripId: 'trp-201',
        category: 'TOLL',
        amount: 28000,
        clientMutationId: 'mut-exp-201',
      );

      expect(store.loadOutbox().length, 3);
      final budiBeforeSync = store
          .loadDrivers()
          .firstWhere((d) => d.id == 'drv-budi');
      // Bang Budi had Rp 220.000 seed + Rp 230.000 new cash = Rp 450.000
      expect(budiBeforeSync.cashHeldAmount, 450000);

      // 3. Server reports CONFLICT_STAGED on TRIP_STATUS (e.g. dispatcher edited schedule)
      await store.applySyncOutcomes([
        {
          'clientMutationId': 'mut-status-201',
          'outcomeStatus': 'CONFLICT_STAGED',
          'message': 'Jadwal diubah oleh dispatcher saat supir offline.',
        },
        {
          'clientMutationId': 'mut-pay-201',
          'outcomeStatus': 'APPLIED',
        },
        {
          'clientMutationId': 'mut-exp-201',
          'outcomeStatus': 'APPLIED',
        },
      ]);

      // Outbox drained, 1 conflict staged for dispatcher review
      expect(store.loadOutbox(), isEmpty);
      expect(store.loadStagedConflicts().length, 1);

      // CRITICAL: Local payment & expense records on trp-201 are 100% preserved!
      final tripAfterConflict = store
          .loadTrips()
          .firstWhere((t) => t.id == 'trp-201');
      expect(tripAfterConflict.payments.length, 1);
      expect(tripAfterConflict.payments.first.amount, 230000);
      expect(tripAfterConflict.cashCustodyStatus, 'HELD_BY_DRIVER');
      expect(tripAfterConflict.expenses.length, 1);
      expect(tripAfterConflict.expenses.first.amount, 28000);
    });

    test('Active-trip-only GPS gate rejects coordinates outside active tracking', () async {
      final store = await OfflineStore.create();

      // Trip trp-201 starts as ASSIGNED & trackingActive == false -> must reject GPS!
      final rejected = await store.recordActiveTripGpsPoint(
        tripId: 'trp-201',
        latitude: 3.6382,
        longitude: 98.8775,
      );
      expect(rejected, isNull);

      // Move to ACCEPTED and explicitly start tracking -> accepts GPS!
      await store.applyTripStatusLocal(tripId: 'trp-201', newStatus: 'ACCEPTED');
      await store.setTripTrackingState(tripId: 'trp-201', active: true);

      final recorded = await store.recordActiveTripGpsPoint(
        tripId: 'trp-201',
        latitude: 3.6382,
        longitude: 98.8775,
      );
      expect(recorded, isNotNull);

      // Completing trip hard-stops trackingActive -> rejects subsequent GPS!
      await store.applyTripStatusLocal(tripId: 'trp-201', newStatus: 'COMPLETED');
      final afterComplete = await store.recordActiveTripGpsPoint(
        tripId: 'trp-201',
        latitude: 3.5752,
        longitude: 98.6745,
      );
      expect(afterComplete, isNull);
    });

    test('Pricing engine, Dispatch simulator & BI <5 sample guard work accurately', () async {
      final store = await OfflineStore.create();

      // Ibu Ratna negotiated Medan -> KNO agreement = Rp 220.000
      final ratnaPrice = store.resolvePricingLocal(
        customerId: 'cust-ratna',
        routeId: 'rte-mdn-kno',
        vehicleType: 'MPV',
      );
      expect(ratnaPrice.suggestedFare, 220000);
      expect(ratnaPrice.pricingTierUsed, 'CUSTOMER_ROUTE_AGREEMENT');

      // Ko Ahong Berastagi 11 pax trip: Hiace suitable, Fortuner in maintenance
      final ahongTrip = store.loadTrips().firstWhere((t) => t.id == 'trp-203');
      final sim = store.simulateDispatchLocal(
        trip: ahongTrip,
        candidateDriverId: 'drv-dedi', // ON_LEAVE -> conflict!
        candidateVehicleId: 'veh-fortuner-01', // IN_MAINTENANCE -> conflict!
      );
      expect(sim.hasDriverConflict, isTrue);
      expect(sim.hasVehicleConflict, isTrue);
      expect(sim.vehicleRecommendations.first.vehicleId, 'veh-hiace-01');

      // BI Route Performance: Medan->KNO has 5 samples (sufficient), Medan->KTJ has 1 (<5)
      final bi = store.computeBiExplorer();
      final knoPerf = bi.routePerformance.firstWhere(
        (r) => r.routeId == 'rte-mdn-kno',
      );
      final ktjPerf = bi.routePerformance.firstWhere(
        (r) => r.routeId == 'rte-mdn-ktj',
      );
      expect(knoPerf.sufficientSample, isTrue);
      expect(ktjPerf.sufficientSample, isFalse);
    });

    test('OfflineStore persists FCM device token, runtime FirebaseOptions & deduplicates incoming remote notifications', () async {
      final store = await OfflineStore.create();
      expect(store.loadFcmDeviceToken(), isNull);

      await store.saveFcmDeviceToken('fcm-live-token-medan-001');
      expect(store.loadFcmDeviceToken(), 'fcm-live-token-medan-001');

      const runtimeCfg = FcmRuntimeConfig(
        apiKey: 'AIzaSyTestKeyMedan001',
        appId: '1:9988776655:android:aabbccddeeff',
        messagingSenderId: '9988776655',
        projectId: 'mandiri-transport-ops',
      );
      await store.saveFcmRuntimeConfig(runtimeCfg);
      expect(store.loadFcmRuntimeConfig()?.projectId, 'mandiri-transport-ops');
      expect(store.loadFcmRuntimeConfig()?.isComplete, isTrue);

      final initialCount = store.loadNotifications().length;
      await store.prependIncomingRemoteNotification(
        id: 'notif-fcm-live-1',
        reminderType: 'UPCOMING_PICKUP',
        title: 'Penjemputan KNO Segera',
        body: 'GA-190 mendarat 30 menit lagi',
        referenceType: 'TRIP',
        referenceId: 'trp-201',
      );
      // Duplicate ID must update in place without duplicating inbox entries
      await store.prependIncomingRemoteNotification(
        id: 'notif-fcm-live-1',
        reminderType: 'UPCOMING_PICKUP',
        title: 'Penjemputan KNO Segera (Diperbarui)',
        body: 'GA-190 mendarat 25 menit lagi',
        referenceType: 'TRIP',
        referenceId: 'trp-201',
      );
      final afterList = store.loadNotifications();
      expect(afterList.length, initialCount + 1);
      expect(afterList.first.title, 'Penjemputan KNO Segera (Diperbarui)');
      expect(afterList.first.fcmDeliveryStatus, 'PUSH_SENT');
    });
  });
}
