import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:mandiri_transport_mobile/core/notifications/fcm_push_service.dart';
import 'package:mandiri_transport_mobile/data/local/offline_store.dart';
import 'package:mandiri_transport_mobile/data/models/models.dart';
import 'package:mandiri_transport_mobile/data/state/app_controller.dart';
import 'package:mandiri_transport_mobile/main.dart';

class FakeFcmPushClient implements FcmPushClient {
  final StreamController<String> tokenCtrl =
      StreamController<String>.broadcast();
  final StreamController<IncomingPushPayload> fgCtrl =
      StreamController<IncomingPushPayload>.broadcast();
  final StreamController<IncomingPushPayload> openCtrl =
      StreamController<IncomingPushPayload>.broadcast();

  final String initialToken;

  FakeFcmPushClient({
    this.initialToken = 'fcm-live-medan-device-token-1234567890',
  });

  @override
  Stream<String> get onTokenRefresh => tokenCtrl.stream;

  @override
  Stream<IncomingPushPayload> get onForegroundMessage => fgCtrl.stream;

  @override
  Stream<IncomingPushPayload> get onNotificationOpened => openCtrl.stream;

  @override
  Future<FcmInitOutcome> initialize({FcmRuntimeConfig? runtimeConfig}) async {
    return FcmInitOutcome(
      initialized: true,
      permissionGranted: true,
      token: initialToken,
      platform: 'ANDROID',
      deviceLabel: 'Mandiri Test Android',
      statusMessage: 'FCM Perangkat Aktif — token siap menerima push real-time.',
    );
  }

  @override
  void dispose() {
    tokenCtrl.close();
    fgCtrl.close();
    openCtrl.close();
  }
}

void main() {
  testWidgets(
    'End-to-end operational flow: Owner Dashboard & Quick Booking -> Driver KNO Pickup, Read-Only Notes, 1-Tap Inspection & Cash Custody',
    (WidgetTester tester) async {
      // Set spacious mobile viewport so all operational actions render cleanly
      tester.view.physicalSize = const Size(1080, 2400);
      tester.view.devicePixelRatio = 2.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      SharedPreferences.setMockInitialValues({});
      final store = await OfflineStore.create();
      final fakeFcm = FakeFcmPushClient();

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            offlineStoreProvider.overrideWithValue(store),
            networkSyncEnabledProvider.overrideWithValue(false),
            fcmPushClientProvider.overrideWithValue(fakeFcm),
          ],
          child: const MandiriTransportApp(),
        ),
      );
      await tester.pumpAndSettle();

      // 1. Verify Login Screen with Medan Role Account Switcher
      expect(find.text('MANDIRI TRANSPORT'), findsOneWidget);
      expect(find.text('Pak Hendra — Pemilik/Dispatcher'), findsOneWidget);
      expect(find.text('Bang Budi — Supir'), findsOneWidget);

      // 2. Switch to Owner (Pak Hendra)
      await tester.tap(find.text('Pak Hendra — Pemilik/Dispatcher'));
      await tester.pumpAndSettle();

      // Verify Owner Dashboard ("Uang Tunai di Supir" = Rp 870.000)
      expect(find.text('Uang Tunai di Supir'), findsOneWidget);
      expect(find.text('Rp 870.000'), findsOneWidget);

      // Open Quick Booking (30 Detik) modal and verify Customer negotiated pricing
      await tester.tap(find.text('+ Booking Cepat (30 Detik)'));
      await tester.pumpAndSettle();
      expect(find.text('Booking Cepat (30 Detik)'), findsOneWidget);
      expect(
        find.textContaining('Harga Khusus Pelanggan Lama Medan-KNO'),
        findsWidgets,
      );

      // Close Quick Booking modal
      await tester.tap(find.byIcon(Icons.close));
      await tester.pumpAndSettle();

      // 3. Switch Role to Driver (Bang Budi)
      await tester.tap(find.text('Ganti Akun'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Bang Budi — Supir'));
      await tester.pumpAndSettle();

      // 4. Verify Assigned KNO Pickup Trip Card (GA-190, Ibu Ratna Kusuma) & Read-Only Notes
      expect(find.text('Ibu Ratna Kusuma'), findsOneWidget);
      expect(find.text('Penerbangan GA-190'), findsOneWidget);
      expect(find.text('Catatan Pelanggan (Hanya Baca)'), findsOneWidget);
      expect(
        find.textContaining('bebas bau rokok'),
        findsOneWidget,
      );

      // 5. Perform 1-Tap Vehicle Inspection ("Semua Aman")
      await tester.scrollUntilVisible(
        find.text('Inspeksi Mobil'),
        250,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.tap(find.text('Inspeksi Mobil'));
      await tester.pumpAndSettle();
      expect(find.text('Semua Aman (1-Tap Siap Jalan)'), findsOneWidget);

      await tester.tap(find.text('Semua Aman (1-Tap Siap Jalan)'));
      await tester.pumpAndSettle();
      expect(find.text('Inspeksi Mobil (Tercatat)'), findsOneWidget);

      // 6. Record Cash Payment & verify driver custody warning + updated balance
      await tester.scrollUntilVisible(
        find.text('Terima Pembayaran'),
        250,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.tap(find.text('Terima Pembayaran'));
      await tester.pumpAndSettle();

      expect(
        find.text('Tunai dipegang supir — wajib disetor ke pemilik'),
        findsOneWidget,
      );
      await tester.tap(find.text('Simpan Pembayaran'));
      await tester.pumpAndSettle();

      // Bang Budi held Rp 220.000 initially + Rp 230.000 KNO pickup = Rp 450.000!
      expect(find.text('Setoran (Rp 450.000)'), findsOneWidget);
    },
  );

  testWidgets(
    'All Owner 5 tabs (Dispatch Simulator, Customers, Finance Formulas, BI) & Driver Signboard + 5s Undo render cleanly',
    (WidgetTester tester) async {
      tester.view.physicalSize = const Size(1080, 2400);
      tester.view.devicePixelRatio = 2.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      SharedPreferences.setMockInitialValues({});
      final store = await OfflineStore.create();
      final fakeFcm = FakeFcmPushClient();

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            offlineStoreProvider.overrideWithValue(store),
            networkSyncEnabledProvider.overrideWithValue(false),
            fcmPushClientProvider.overrideWithValue(fakeFcm),
          ],
          child: const MandiriTransportApp(),
        ),
      );
      await tester.pumpAndSettle();

      // Login as Owner
      await tester.tap(find.text('Pak Hendra — Pemilik/Dispatcher'));
      await tester.pumpAndSettle();

      // Tab 2: Jadwal & Dispatch Simulator
      await tester.tap(find.text('Jadwal'));
      await tester.pumpAndSettle();
      expect(find.textContaining('Ko Ahong'), findsWidgets);

      await tester.tap(find.text('Tugaskan Supir & Unit').first);
      await tester.pumpAndSettle();
      expect(
        find.text('Rekomendasi Pemerataan Beban Kerja Supir:'),
        findsOneWidget,
      );
      await tester.tap(find.byIcon(Icons.close));
      await tester.pumpAndSettle();

      // Tab 3: Pelanggan & Lost and Found
      await tester.tap(find.text('Pelanggan'));
      await tester.pumpAndSettle();
      expect(find.text('PT Sawit Makmur Nusantara'), findsOneWidget);

      // Tab 4: Keuangan & Armada (all 3 sub-tabs)
      await tester.tap(find.text('Keuangan'));
      await tester.pumpAndSettle();
      expect(
        find.text('Total Uang Tunai Pelanggan di Tangan Supir'),
        findsOneWidget,
      );

      await tester.tap(find.text('Biaya & Formula'));
      await tester.pumpAndSettle();
      expect(
        find.text('Formula Biaya Operasional Berversi'),
        findsOneWidget,
      );

      await tester.tap(find.text('Armada & Supir'));
      await tester.pumpAndSettle();
      await tester.scrollUntilVisible(
        find.textContaining('BK 4455 MT'),
        300,
        scrollable: find.byType(Scrollable).first,
      );
      expect(find.textContaining('BK 4455 MT'), findsOneWidget);

      // Tab 5: Analitik BI (<5 perjalanan badge & Fortuner evaluation)
      await tester.tap(find.text('Analitik BI'));
      await tester.pumpAndSettle();
      expect(
        find.text('Eksplorasi Analitik Bisnis & Smart Operations'),
        findsOneWidget,
      );
      expect(find.textContaining('< 5 perjalanan'), findsWidgets);

      // Switch to Driver & test Airport Signboard + Single-Tap Lifecycle Undo
      await tester.tap(find.text('Ganti Akun'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Bang Budi — Supir'));
      await tester.pumpAndSettle();

      await tester.tap(find.text('Layar Papan Nama Jemput Bandara (KNO)'));
      await tester.pumpAndSettle();
      expect(find.text('IBU RATNA KUSUMA'), findsOneWidget);
      await tester.tap(find.text('Tutup Papan'));
      await tester.pumpAndSettle();

      // Single-tap lifecycle: ASSIGNED -> ACCEPTED ("Terima Tugas"), then Undo!
      await tester.tap(find.text('Terima Tugas'));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 500));
      expect(find.text('Berangkat Jemput'), findsOneWidget);
      expect(find.text('URUNGKAN (5 dtk)'), findsOneWidget);

      await tester.tap(find.text('URUNGKAN (5 dtk)'));
      await tester.pumpAndSettle();
      expect(find.text('Terima Tugas'), findsOneWidget);
    },
  );

  testWidgets(
    'Live Firebase Cloud Messaging: token registration, foreground push banner, tap routing & Owner Uji Push FCM',
    (WidgetTester tester) async {
      tester.view.physicalSize = const Size(1080, 2400);
      tester.view.devicePixelRatio = 2.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      SharedPreferences.setMockInitialValues({});
      final store = await OfflineStore.create();
      final fakeFcm = FakeFcmPushClient(
        initialToken: 'fcm-kno-dispatch-token-9876543210abcdef',
      );

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            offlineStoreProvider.overrideWithValue(store),
            networkSyncEnabledProvider.overrideWithValue(false),
            fcmPushClientProvider.overrideWithValue(fakeFcm),
          ],
          child: const MandiriTransportApp(),
        ),
      );
      await tester.pumpAndSettle();

      // Login as Owner (Pak Hendra)
      await tester.tap(find.text('Pak Hendra — Pemilik/Dispatcher'));
      await tester.pumpAndSettle();

      // Token initialized & persisted locally
      expect(
        store.loadFcmDeviceToken(),
        'fcm-kno-dispatch-token-9876543210abcdef',
      );

      // Simulate incoming live foreground FCM push for an upcoming KNO pickup
      fakeFcm.fgCtrl.add(
        const IncomingPushPayload(
          id: 'push-live-kno-99',
          reminderType: 'UPCOMING_PICKUP',
          title: 'Tugas Jemput Bandara GA-190',
          body: 'Ibu Ratna Kusuma tiba di KNO dalam 30 menit.',
          referenceType: 'TRIP',
          referenceId: 'trp-201',
        ),
      );
      await tester.pumpAndSettle();

      // Verify non-blocking LivePushBanner renders at top of OwnerShell & inbox persisted
      expect(find.text('Tugas Jemput Bandara GA-190'), findsWidgets);
      expect(
        store.loadNotifications().any((n) => n.id == 'push-live-kno-99'),
        isTrue,
      );

      // Tap "Lihat" on LivePushBanner -> clears banner & routes Owner to Tab 2 (Jadwal)
      await tester.tap(find.text('Lihat'));
      await tester.pumpAndSettle();
      expect(find.textContaining('Ko Ahong'), findsWidgets);

      // Navigate to Tab 5 (Analitik BI) -> verify Live FCM card & trigger "Uji Push FCM"
      await tester.tap(find.text('Analitik BI'));
      await tester.pumpAndSettle();

      await tester.scrollUntilVisible(
        find.text('Uji Push FCM'),
        300,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.ensureVisible(find.text('Uji Push FCM'));
      await tester.pumpAndSettle();
      expect(find.text('FCM Live Aktif'), findsOneWidget);
      expect(find.text('Uji Push FCM'), findsOneWidget);

      await tester.tap(find.text('Uji Push FCM'));
      await tester.pumpAndSettle();
      expect(find.text('Uji Push Live — Mandiri Transport'), findsWidgets);
    },
  );
}
