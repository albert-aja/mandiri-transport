import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_theme.dart';
import '../../core/utils/deep_links.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/shared_widgets.dart';
import '../../data/models/models.dart';
import '../../data/state/app_controller.dart';
import 'airport_signboard_screen.dart';
import 'driver_field_sheets.dart';

class DriverShell extends ConsumerStatefulWidget {
  const DriverShell({super.key});

  @override
  ConsumerState<DriverShell> createState() => _DriverShellState();
}

class _DriverShellState extends ConsumerState<DriverShell> {
  int _tabIndex = 0;

  ({String nextStatus, String buttonLabel})? _nextLifecycleAction(
    String currentStatus,
  ) {
    return switch (currentStatus) {
      'ASSIGNED' => (nextStatus: 'ACCEPTED', buttonLabel: 'Terima Tugas'),
      'ACCEPTED' => (nextStatus: 'ON_THE_WAY', buttonLabel: 'Berangkat Jemput'),
      'ON_THE_WAY' => (nextStatus: 'ARRIVED', buttonLabel: 'Tiba di Lokasi'),
      'ARRIVED' => (
        nextStatus: 'PASSENGER_ONBOARD',
        buttonLabel: 'Penumpang Naik',
      ),
      'PASSENGER_ONBOARD' => (
        nextStatus: 'COMPLETED',
        buttonLabel: 'Selesai Perjalanan',
      ),
      _ => null,
    };
  }

  Future<void> _handleLifecycleTap(Trip trip, String nextStatus) async {
    final controller = ref.read(appControllerProvider.notifier);
    await controller.advanceTripStatus(trip: trip, newStatus: nextStatus);
    if (!mounted) return;
    final messenger = ScaffoldMessenger.of(context);
    messenger.hideCurrentSnackBar();
    messenger.showSnackBar(
      SnackBar(
        duration: const Duration(seconds: 5),
        content: Text(
          'Status diperbarui: ${OperationalLabels.tripStatus(nextStatus)}',
        ),
        action: SnackBarAction(
          label: 'URUNGKAN (5 dtk)',
          textColor: Colors.amberAccent,
          onPressed: () => controller.undoLastTripStatus(trip.id),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(appControllerProvider);
    final controller = ref.read(appControllerProvider.notifier);
    final driverProfile = state.currentDriverProfile;
    final myTrips = state.visibleTrips;

    final activeTrips = myTrips
        .where(
          (t) => !const {'COMPLETED', 'CANCELLED', 'NO_SHOW'}.contains(t.status),
        )
        .toList();
    final completedTrips = myTrips
        .where((t) => t.status == 'COMPLETED')
        .toList();

    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(state.session?.fullName ?? 'Supir Mandiri'),
            Text(
              driverProfile != null
                  ? '${driverProfile.driverCode} • ${OperationalLabels.driverAvailability(driverProfile.availabilityStatus)}'
                  : 'Mode Operasional Supir',
              style: const TextStyle(
                fontSize: 12,
                color: Colors.white70,
                fontWeight: FontWeight.w500,
              ),
            ),
          ],
        ),
        actions: [
          TextButton.icon(
            style: TextButton.styleFrom(foregroundColor: Colors.white),
            onPressed: controller.logout,
            icon: const Icon(Icons.switch_account_rounded, size: 18),
            label: const Text('Ganti Akun'),
          ),
        ],
      ),
      body: Column(
        children: [
          LivePushBanner(
            fcmState: state.fcmState,
            onDismiss: controller.dismissLivePushBanner,
          ),
          // Persistent Top Offline & Outbox Sync Pill
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
            decoration: BoxDecoration(
              color: state.effectiveOnline
                  ? AppColors.emeraldSoft
                  : AppColors.amberSoft,
              border: Border(
                bottom: BorderSide(
                  color: state.effectiveOnline
                      ? AppColors.emerald.withValues(alpha: 0.3)
                      : AppColors.amber.withValues(alpha: 0.4),
                ),
              ),
            ),
            child: Row(
              children: [
                Icon(
                  state.effectiveOnline
                      ? Icons.cloud_done_rounded
                      : Icons.cloud_off_rounded,
                  size: 20,
                  color: state.effectiveOnline
                      ? AppColors.emerald
                      : AppColors.amber,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        state.outbox.isEmpty
                            ? (state.effectiveOnline
                                  ? 'Online • Semua data tersinkron'
                                  : 'Mode Offline • Tersimpan di HP')
                            : '${state.outbox.length} data belum sinkron',
                        style: const TextStyle(
                          fontWeight: FontWeight.w800,
                          fontSize: 13,
                        ),
                      ),
                      Text(
                        'Sinkron terakhir: ${AppFormatters.relativeSyncLabel(state.lastSyncedAt)}',
                        style: const TextStyle(
                          fontSize: 11.5,
                          color: AppColors.textSecondary,
                        ),
                      ),
                    ],
                  ),
                ),
                Switch(
                  value: !state.forceOfflineMode,
                  onChanged: (online) =>
                      controller.toggleForceOfflineMode(!online),
                ),
                TextButton(
                  onPressed: () async {
                    final msg = await controller.syncNow();
                    if (context.mounted) {
                      ScaffoldMessenger.of(
                        context,
                      ).showSnackBar(SnackBar(content: Text(msg)));
                    }
                  },
                  child: const Text('Sinkron Sekarang'),
                ),
              ],
            ),
          ),

          Expanded(
            child: _tabIndex == 0
                ? _buildActiveTasksTab(context, state, activeTrips)
                : _buildCustodyAndHistoryTab(
                    context,
                    state,
                    driverProfile,
                    myTrips,
                    completedTrips,
                  ),
          ),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _tabIndex,
        onDestinationSelected: (i) => setState(() => _tabIndex = i),
        destinations: [
          NavigationDestination(
            icon: const Icon(Icons.assignment_outlined),
            selectedIcon: const Icon(Icons.assignment_rounded),
            label: 'Tugas Saya (${activeTrips.length})',
          ),
          NavigationDestination(
            icon: const Icon(Icons.account_balance_wallet_outlined),
            selectedIcon: const Icon(Icons.account_balance_wallet_rounded),
            label:
                'Setoran (${AppFormatters.rupiah(driverProfile?.cashHeldAmount ?? 0)})',
          ),
        ],
      ),
    );
  }

  Widget _buildActiveTasksTab(
    BuildContext context,
    AppState state,
    List<Trip> activeTrips,
  ) {
    final controller = ref.read(appControllerProvider.notifier);

    if (activeTrips.isEmpty) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: Text(
            'Belum ada tugas perjalanan aktif. Silakan cek tab Setoran & Riwayat.',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 15, color: AppColors.textSecondary),
          ),
        ),
      );
    }

    final heroTrip = activeTrips.first;
    final nextAction = _nextLifecycleAction(heroTrip.status);
    final tripGpsCount = state.gpsQueue
        .where((p) => p.tripId == heroTrip.id)
        .length;

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 32),
      children: [
        // HERO ACTIVE TRIP CARD
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  crossAxisAlignment: WrapCrossAlignment.center,
                  children: [
                    StatusBadge.forTripStatus(heroTrip.status),
                    StatusBadge(
                      label: OperationalLabels.tripType(heroTrip.tripType),
                      fg: AppColors.navy,
                      bg: AppColors.slateBg,
                    ),
                    if (heroTrip.flightNumber != null)
                      StatusBadge(
                        label: 'Penerbangan ${heroTrip.flightNumber}',
                        fg: AppColors.infoBlue,
                        bg: AppColors.infoSoft,
                        icon: Icons.flight_land_rounded,
                      ),
                  ],
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            heroTrip.customerName,
                            style: const TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.w900,
                            ),
                          ),
                          Text(
                            '${heroTrip.tripCode} • Jemput ${AppFormatters.dateTimeWib(heroTrip.scheduledPickupAt)}',
                            style: const TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w700,
                              color: AppColors.navy,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Text(
                      AppFormatters.rupiah(heroTrip.agreedFareSnapshot),
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w900,
                        color: AppColors.emerald,
                      ),
                    ),
                  ],
                ),

                if (heroTrip.flightNumber != null ||
                    heroTrip.tripType == 'AIRPORT_PICKUP') ...[
                  const SizedBox(height: 10),
                  SizedBox(
                    width: double.infinity,
                    child: OutlinedButton.icon(
                      onPressed: () => Navigator.push(
                        context,
                        MaterialPageRoute<void>(
                          builder: (_) =>
                              AirportSignboardScreen(trip: heroTrip),
                        ),
                      ),
                      icon: const Icon(Icons.fullscreen_rounded),
                      label: const Text(
                        'Layar Papan Nama Jemput Bandara (KNO)',
                      ),
                    ),
                  ),
                ],

                const SizedBox(height: 12),
                Text(
                  'Jemput: ${heroTrip.pickupAddress}',
                  style: const TextStyle(
                    fontWeight: FontWeight.w700,
                    fontSize: 14,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  'Tujuan: ${heroTrip.destinationAddress}',
                  style: const TextStyle(
                    fontSize: 14,
                    color: AppColors.textSecondary,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  'Unit: ${heroTrip.vehiclePlate ?? "-"} (${heroTrip.vehicleModel ?? "-"}) • ${heroTrip.passengerCount} Penumpang, ${heroTrip.luggageCount} Koper',
                  style: const TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                  ),
                ),

                // Shared Operational Customer Notes (Read-Only for Driver!)
                if (heroTrip.customerNotes.isNotEmpty) ...[
                  const SizedBox(height: 12),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: AppColors.amberSoft,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: AppColors.amber.withValues(alpha: 0.35),
                      ),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Row(
                          children: [
                            Icon(
                              Icons.info_outline_rounded,
                              size: 16,
                              color: AppColors.amber,
                            ),
                            SizedBox(width: 6),
                            Text(
                              'Catatan Pelanggan (Hanya Baca)',
                              style: TextStyle(
                                fontWeight: FontWeight.w800,
                                fontSize: 12.5,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 6),
                        ...heroTrip.customerNotes
                            .where((n) => n.visibleToDrivers)
                            .map((n) {
                              return Padding(
                                padding: const EdgeInsets.only(bottom: 3),
                                child: Text(
                                  '• ${n.noteText}',
                                  style: const TextStyle(
                                    fontSize: 13,
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
                              );
                            }),
                      ],
                    ),
                  ),
                ],

                const SizedBox(height: 14),
                // Large Navigation & WhatsApp Buttons
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: () {
                          final targetAddr = heroTrip.status == 'PASSENGER_ONBOARD'
                              ? heroTrip.destinationAddress
                              : heroTrip.pickupAddress;
                          DeepLinks.launchExternal(
                            DeepLinks.buildGoogleMapsNavigationUrl(
                              destination: targetAddr,
                            ),
                          );
                        },
                        icon: const Icon(Icons.navigation_rounded, size: 18),
                        label: const Text('Navigasi Maps'),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: OutlinedButton.icon(
                        style: OutlinedButton.styleFrom(
                          foregroundColor: AppColors.emerald,
                          side: const BorderSide(
                            color: AppColors.emerald,
                            width: 1.4,
                          ),
                        ),
                        onPressed: () => showWhatsappTemplatePickerSheet(
                          context: context,
                          templates: state.whatsappTemplates,
                          recipientPhone: heroTrip.customerPhone,
                          customerName: heroTrip.customerName,
                          trip: heroTrip,
                        ),
                        icon: const Icon(Icons.chat_rounded, size: 18),
                        label: const Text('Hubungi WA'),
                      ),
                    ),
                  ],
                ),

                // Single-Tap Lifecycle Progression Button
                if (nextAction != null) ...[
                  const SizedBox(height: 12),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton.icon(
                      style: FilledButton.styleFrom(
                        backgroundColor: AppColors.navy,
                        minimumSize: const Size.fromHeight(56),
                      ),
                      onPressed: () =>
                          _handleLifecycleTap(heroTrip, nextAction.nextStatus),
                      icon: const Icon(Icons.play_circle_fill_rounded),
                      label: Text(
                        nextAction.buttonLabel,
                        style: const TextStyle(fontSize: 16.5),
                      ),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),

        const SizedBox(height: 14),

        // ACTIVE-TRIP-ONLY GPS TRACKING CARD
        Card(
          child: Padding(
            padding: const EdgeInsets.all(15),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Icon(
                      heroTrip.trackingActive
                          ? Icons.gps_fixed_rounded
                          : Icons.gps_off_rounded,
                      color: heroTrip.trackingActive
                          ? AppColors.emerald
                          : AppColors.textSecondary,
                    ),
                    const SizedBox(width: 8),
                    const Expanded(
                      child: Text(
                        'Pelacakan GPS Perjalanan Aktif',
                        style: TextStyle(
                          fontWeight: FontWeight.w800,
                          fontSize: 15,
                        ),
                      ),
                    ),
                    TextButton.icon(
                      onPressed: () =>
                          showDriverOdometerSheet(context, ref, heroTrip),
                      icon: const Icon(Icons.speed_rounded, size: 18),
                      label: const Text('Odometer'),
                    ),
                  ],
                ),
                Text(
                  '${AppFormatters.gpsStaleness(heroTrip.lastGpsRecordedAt)} • Titik terekam: $tripGpsCount${heroTrip.gpsDistanceKm != null ? " (${heroTrip.gpsDistanceKm} km)" : ""}',
                  style: const TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: AppColors.navy,
                  ),
                ),
                const SizedBox(height: 4),
                const Text(
                  'GPS hanya aktif selama perjalanan berjalan — otomatis mati saat perjalanan selesai.',
                  style: TextStyle(
                    fontSize: 12,
                    color: AppColors.textSecondary,
                  ),
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: FilledButton.icon(
                        style: FilledButton.styleFrom(
                          backgroundColor: heroTrip.trackingActive
                              ? AppColors.crimson
                              : AppColors.emerald,
                        ),
                        onPressed: heroTrip.isActiveExecutionState
                            ? () async {
                                final msg = heroTrip.trackingActive
                                    ? await controller.stopActiveTripGps(
                                        heroTrip.id,
                                      )
                                    : await controller.startActiveTripGps(
                                        heroTrip.id,
                                      );
                                if (context.mounted) {
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    SnackBar(content: Text(msg)),
                                  );
                                }
                              }
                            : null,
                        icon: Icon(
                          heroTrip.trackingActive
                              ? Icons.stop_circle_outlined
                              : Icons.my_location_rounded,
                        ),
                        label: Text(
                          heroTrip.trackingActive
                              ? 'Hentikan GPS'
                              : (heroTrip.isActiveExecutionState
                                    ? 'Mulai GPS Perjalanan'
                                    : 'Mulai GPS (Terima Tugas Dulu)'),
                        ),
                      ),
                    ),
                    if (heroTrip.trackingActive) ...[
                      const SizedBox(width: 8),
                      OutlinedButton(
                        onPressed: () =>
                            controller.recordGpsWaypointSample(heroTrip.id),
                        child: const Text('+ Titik'),
                      ),
                    ],
                  ],
                ),
              ],
            ),
          ),
        ),

        const SizedBox(height: 14),

        // QUICK OPTIONAL FIELD ACTIONS (Non-Blocking)
        const Text(
          'Aksi Cepat Lapangan (Opsional & Tidak Menghambat):',
          style: TextStyle(fontWeight: FontWeight.w800, fontSize: 14.5),
        ),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            ActionChip(
              avatar: Icon(
                heroTrip.inspections.isNotEmpty
                    ? Icons.verified_rounded
                    : Icons.fact_check_outlined,
                size: 18,
                color: AppColors.emerald,
              ),
              label: Text(
                heroTrip.inspections.isNotEmpty
                    ? 'Inspeksi Mobil (Tercatat)'
                    : 'Inspeksi Mobil',
              ),
              onPressed: () =>
                  showDriverInspectionSheet(context, ref, heroTrip),
            ),
            ActionChip(
              avatar: const Icon(
                Icons.payments_outlined,
                size: 18,
                color: AppColors.navy,
              ),
              label: const Text('Terima Pembayaran'),
              onPressed: () => showDriverPaymentSheet(context, ref, heroTrip),
            ),
            ActionChip(
              avatar: const Icon(
                Icons.local_gas_station_rounded,
                size: 18,
                color: AppColors.amber,
              ),
              label: const Text('Catat Pengeluaran'),
              onPressed: () => showDriverExpenseSheet(context, ref, heroTrip),
            ),
            ActionChip(
              avatar: const Icon(
                Icons.luggage_outlined,
                size: 18,
                color: AppColors.infoBlue,
              ),
              label: const Text('Barang Tertinggal'),
              onPressed: () => showDriverLostFoundSheet(context, ref, heroTrip),
            ),
            ActionChip(
              backgroundColor: AppColors.crimsonSoft,
              avatar: const Icon(
                Icons.sos_rounded,
                size: 18,
                color: AppColors.crimson,
              ),
              label: const Text(
                'Darurat (SOS)',
                style: TextStyle(
                  color: AppColors.crimson,
                  fontWeight: FontWeight.w800,
                ),
              ),
              onPressed: () async {
                final url = await controller.buildAndSendEmergencySos(heroTrip);
                await DeepLinks.launchExternal(url);
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text(
                        'Peringatan Darurat SOS disiapkan ke WhatsApp Pemilik.',
                      ),
                    ),
                  );
                }
              },
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildCustodyAndHistoryTab(
    BuildContext context,
    AppState state,
    Driver? driverProfile,
    List<Trip> myTrips,
    List<Trip> completedTrips,
  ) {
    final cashHeld = driverProfile?.cashHeldAmount ?? 0;
    final unsettledTrips = myTrips
        .where((t) => t.driverCashHeldAmount > 0)
        .toList();

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Container(
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            color: AppColors.navy,
            borderRadius: BorderRadius.circular(16),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Uang Tunai di Tangan Saya',
                style: TextStyle(color: Colors.white70, fontSize: 13.5),
              ),
              const SizedBox(height: 4),
              Text(
                AppFormatters.rupiah(cashHeld),
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 28,
                  fontWeight: FontWeight.w900,
                ),
              ),
              const SizedBox(height: 6),
              const Text(
                'Pembayaran tunai dari penumpang wajib disetorkan ke Pak Hendra.',
                style: TextStyle(color: Colors.white70, fontSize: 12.5),
              ),
              if (cashHeld > 0) ...[
                const SizedBox(height: 14),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    style: FilledButton.styleFrom(
                      backgroundColor: AppColors.amber,
                    ),
                    onPressed: () async {
                      await ref
                          .read(appControllerProvider.notifier)
                          .declareDriverHandover(
                            declaredAmount: cashHeld,
                            driverNotes:
                                'Pengajuan setoran kas tunai oleh ${driverProfile?.fullName ?? "Supir"}',
                          );
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: Text(
                              'Pengajuan setoran ${AppFormatters.rupiah(cashHeld)} dikirim ke pemilik.',
                            ),
                          ),
                        );
                      }
                    },
                    icon: const Icon(Icons.upload_rounded),
                    label: const Text('Ajukan Setoran ke Pemilik'),
                  ),
                ),
              ],
            ],
          ),
        ),
        if (unsettledTrips.isNotEmpty) ...[
          const SizedBox(height: 16),
          const Text(
            'Rincian Kas Tunai Belum Disetor:',
            style: TextStyle(fontWeight: FontWeight.w800, fontSize: 15),
          ),
          const SizedBox(height: 8),
          ...unsettledTrips.map((t) {
            return Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: Card(
                child: ListTile(
                  title: Text(
                    '${t.tripCode} • ${t.customerName}',
                    style: const TextStyle(fontWeight: FontWeight.w800),
                  ),
                  subtitle: Text(t.routeName),
                  trailing: Text(
                    AppFormatters.rupiah(t.driverCashHeldAmount),
                    style: const TextStyle(
                      fontWeight: FontWeight.w900,
                      color: AppColors.amber,
                      fontSize: 15,
                    ),
                  ),
                ),
              ),
            );
          }),
        ],
        const SizedBox(height: 18),
        Text(
          'Riwayat Perjalanan Selesai (${completedTrips.length})',
          style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 15.5),
        ),
        const SizedBox(height: 8),
        ...completedTrips.map((t) {
          return Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            '${t.tripCode} • ${t.customerName}',
                            style: const TextStyle(fontWeight: FontWeight.w800),
                          ),
                        ),
                        const SizedBox(width: 8),
                        StatusBadge.forCustody(t.cashCustodyStatus),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(
                      '${t.routeName} • Tarif ${AppFormatters.rupiah(t.agreedFareSnapshot)}',
                      style: const TextStyle(
                        fontSize: 13,
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          );
        }),
      ],
    );
  }
}
