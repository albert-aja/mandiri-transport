import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_theme.dart';
import '../../core/utils/deep_links.dart';
import '../../core/utils/flight_helper.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/shared_widgets.dart';
import '../../data/models/models.dart';
import '../../data/state/app_controller.dart';
import '../driver/airport_signboard_screen.dart';

class TodayFlightsHighlightSection extends ConsumerWidget {
  final bool isOwnerView;

  const TodayFlightsHighlightSection({
    super.key,
    this.isOwnerView = false,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(appControllerProvider);
    final session = state.session;

    // Filter relevant today's trips with flight numbers in local WIB timezone
    final todayFlights = state.trips.where((t) {
      if (t.flightNumber == null || t.flightNumber!.trim().isEmpty) {
        return false;
      }
      if (!isOwnerView && session != null && session.isDriver) {
        if (t.driverId != session.driverId) return false;
      }
      return AppFormatters.isTodayWib(t.scheduledPickupAt);
    }).toList();

    // Sort by scheduled pickup time
    todayFlights.sort(
      (a, b) => a.scheduledPickupAt.compareTo(b.scheduledPickupAt),
    );

    if (todayFlights.isEmpty) {
      return const SizedBox.shrink();
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Container(
              padding: const EdgeInsets.all(7),
              decoration: BoxDecoration(
                color: AppColors.infoSoft,
                borderRadius: BorderRadius.circular(8),
              ),
              child: const Icon(
                Icons.flight_takeoff_rounded,
                size: 20,
                color: AppColors.infoBlue,
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    isOwnerView
                        ? 'Jadwal Penerbangan Hari Ini (${todayFlights.length})'
                        : 'Penerbangan Tugas Saya Hari Ini (${todayFlights.length})',
                    style: const TextStyle(
                      fontSize: 16.5,
                      fontWeight: FontWeight.w900,
                      color: AppColors.textPrimary,
                    ),
                  ),
                  const Text(
                    'Waktu Lokal Medan (WIB) • Pantau perubahan jadwal & pendaratan',
                    style: TextStyle(
                      fontSize: 12,
                      color: AppColors.textSecondary,
                    ),
                  ),
                ],
              ),
            ),
            IconButton(
              tooltip: 'Perbarui Status Penerbangan',
              icon: const Icon(Icons.refresh_rounded, size: 20),
              onPressed: () async {
                await ref
                    .read(appControllerProvider.notifier)
                    .refreshTodayFlightSchedules();
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Jadwal penerbangan diperbarui.'),
                      duration: Duration(seconds: 2),
                    ),
                  );
                }
              },
            ),
          ],
        ),
        const SizedBox(height: 10),
        ...todayFlights.map(
          (trip) => Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: _TodayFlightCard(trip: trip, isOwnerView: isOwnerView),
          ),
        ),
      ],
    );
  }
}

class _TodayFlightCard extends ConsumerWidget {
  final Trip trip;
  final bool isOwnerView;

  const _TodayFlightCard({
    required this.trip,
    required this.isOwnerView,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final flightNo = trip.flightNumber ?? '-';
    final info = trip.flightInfo;
    final airline = info?.airlineNameResolved ?? FlightHelper.resolveAirlineName(flightNo);
    final route = FlightHelper.resolveFlightRoute(
      flightNo,
      tripType: trip.tripType,
      originAirport: info?.originAirport,
      destinationAirport: info?.destinationAirport,
    );

    final status = info?.flightStatus ?? (trip.isCompleted ? 'LANDED' : 'SCHEDULED');
    final delayMinutes = info?.delayMinutes ?? 0;
    final isLanded = status == 'LANDED' || trip.isCompleted;
    final isCancelled = status == 'CANCELLED';
    final isDelayed = status == 'DELAYED' || delayMinutes > 0;

    // Time calculations
    final scheduledArrival = info?.scheduledArrivalTime ?? trip.scheduledPickupAt;
    final estimatedArrival = info?.estimatedArrivalTime;
    final actualArrival = info?.actualArrivalTime;

    // Approaching arrival check (within 60 mins of estimated/scheduled time)
    final refTime = DateTime.tryParse(estimatedArrival ?? scheduledArrival);
    final isApproaching = !isLanded &&
        !isCancelled &&
        refTime != null &&
        refTime.isAfter(DateTime.now().toUtc().add(const Duration(hours: 7)).subtract(const Duration(minutes: 5))) &&
        refTime.difference(DateTime.now().toUtc().add(const Duration(hours: 7))).inMinutes <= 60;

    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: isDelayed
              ? AppColors.amber.withValues(alpha: 0.6)
              : (isCancelled
                  ? AppColors.crimson.withValues(alpha: 0.6)
                  : (isApproaching
                      ? AppColors.infoBlue.withValues(alpha: 0.6)
                      : AppColors.cardBorder)),
          width: isDelayed || isApproaching || isCancelled ? 1.8 : 1,
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.04),
            blurRadius: 8,
            offset: const Offset(0, 3),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Aeronautical Flight Header Strip
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 11),
            decoration: BoxDecoration(
              color: FlightHelper.statusBg(status, delayMinutes: delayMinutes),
              borderRadius: const BorderRadius.vertical(top: Radius.circular(15)),
            ),
            child: Row(
              children: [
                Icon(
                  FlightHelper.statusIcon(status, delayMinutes: delayMinutes),
                  size: 20,
                  color: FlightHelper.statusFg(status, delayMinutes: delayMinutes),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Text(
                            flightNo,
                            style: const TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.w900,
                              color: AppColors.textPrimary,
                              letterSpacing: 0.2,
                            ),
                          ),
                          const SizedBox(width: 6),
                          Expanded(
                            child: Text(
                              '• $airline',
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                fontSize: 12.5,
                                fontWeight: FontWeight.w700,
                                color: AppColors.textSecondary,
                              ),
                            ),
                          ),
                        ],
                      ),
                      Text(
                        '${FlightHelper.resolveAirportName(route.origin)} → ${FlightHelper.resolveAirportName(route.destination)}',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                          color: AppColors.navy,
                        ),
                      ),
                    ],
                  ),
                ),
                StatusBadge(
                  label: FlightHelper.statusLabel(status, delayMinutes: delayMinutes),
                  fg: FlightHelper.statusFg(status, delayMinutes: delayMinutes),
                  bg: Colors.white,
                  icon: FlightHelper.statusIcon(status, delayMinutes: delayMinutes),
                ),
              ],
            ),
          ),

          Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Highlight: Approaching Arrival or Delay Banner
                if (isApproaching) ...[
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                    decoration: BoxDecoration(
                      color: AppColors.infoSoft,
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(
                        color: AppColors.infoBlue.withValues(alpha: 0.4),
                      ),
                    ),
                    child: Row(
                      children: [
                        const Icon(
                          Icons.timer_outlined,
                          size: 18,
                          color: AppColors.infoBlue,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            'Mendekati Waktu Kedatangan di KNO (~${refTime.difference(DateTime.now().toUtc().add(const Duration(hours: 7))).inMinutes.clamp(1, 60)} mnt lagi)',
                            style: const TextStyle(
                              fontSize: 12.5,
                              fontWeight: FontWeight.w800,
                              color: AppColors.infoBlue,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 10),
                ] else if (isDelayed) ...[
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                    decoration: BoxDecoration(
                      color: AppColors.amberSoft,
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(
                        color: AppColors.amber.withValues(alpha: 0.4),
                      ),
                    ),
                    child: Row(
                      children: [
                        const Icon(
                          Icons.warning_amber_rounded,
                          size: 18,
                          color: AppColors.amber,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            'Jadwal Mengalami Perubahan (Terlambat +$delayMinutes mnt)',
                            style: const TextStyle(
                              fontSize: 12.5,
                              fontWeight: FontWeight.w800,
                              color: AppColors.amber,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 10),
                ],

                // 3-Column Timing Comparison Grid
                Container(
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: AppColors.slateBg,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    children: [
                      // Column 1: Scheduled Time
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text(
                              'JADWAL RESMI',
                              style: TextStyle(
                                fontSize: 10.5,
                                fontWeight: FontWeight.w800,
                                color: AppColors.textSecondary,
                              ),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              AppFormatters.timeWib(scheduledArrival),
                              style: TextStyle(
                                fontSize: 14,
                                fontWeight: FontWeight.w800,
                                decoration: isDelayed
                                    ? TextDecoration.lineThrough
                                    : TextDecoration.none,
                                color: isDelayed
                                    ? AppColors.textSecondary
                                    : AppColors.textPrimary,
                              ),
                            ),
                          ],
                        ),
                      ),
                      // Column 2: Estimated Time
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text(
                              'ESTIMASI KINI',
                              style: TextStyle(
                                fontSize: 10.5,
                                fontWeight: FontWeight.w800,
                                color: AppColors.textSecondary,
                              ),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              estimatedArrival != null
                                  ? AppFormatters.timeWib(estimatedArrival)
                                  : AppFormatters.timeWib(scheduledArrival),
                              style: TextStyle(
                                fontSize: 14,
                                fontWeight: FontWeight.w900,
                                color: isDelayed
                                    ? AppColors.amber
                                    : AppColors.textPrimary,
                              ),
                            ),
                          ],
                        ),
                      ),
                      // Column 3: Actual Landed Time
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text(
                              'AKTUAL TIBA',
                              style: TextStyle(
                                fontSize: 10.5,
                                fontWeight: FontWeight.w800,
                                color: AppColors.textSecondary,
                              ),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              actualArrival != null
                                  ? AppFormatters.timeWib(actualArrival)
                                  : (isLanded
                                      ? AppFormatters.timeWib(scheduledArrival)
                                      : 'Belum tiba'),
                              style: TextStyle(
                                fontSize: 14,
                                fontWeight: FontWeight.w800,
                                color: isLanded
                                    ? AppColors.emerald
                                    : AppColors.textSecondary,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 10),

                // Data freshness indicator (Never presents stale data as live)
                Row(
                  children: [
                    Icon(
                      info?.isLive == true
                          ? Icons.circle_rounded
                          : (info?.isStale == true
                              ? Icons.access_time_rounded
                              : Icons.info_outline_rounded),
                      size: 13,
                      color: info?.isLive == true
                          ? AppColors.emerald
                          : (info?.isStale == true
                              ? AppColors.amber
                              : AppColors.textSecondary),
                    ),
                    const SizedBox(width: 5),
                    Expanded(
                      child: Text(
                        info?.isLive == true
                            ? 'Pelacakan FlightAware Live (${AppFormatters.flightTrackingStaleness(info?.lastTrackedAt)})'
                            : (info?.isStale == true
                                ? 'Data Tersimpan (${AppFormatters.flightTrackingStaleness(info?.lastTrackedAt)}) • Bukan Real-time'
                                : 'Jadwal Booking Asli • Pelacakan live belum tersedia'),
                        style: TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.w600,
                          color: info?.isLive == true
                              ? AppColors.emerald
                              : AppColors.textSecondary,
                        ),
                      ),
                    ),
                    if (info?.gate != null) ...[
                      Text(
                        'Pintu: ${info!.gate}',
                        style: const TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.w700,
                          color: AppColors.navy,
                        ),
                      ),
                      const SizedBox(width: 6),
                    ],
                    if (info?.terminal != null)
                      Text(
                        info!.terminal!,
                        style: const TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.w600,
                          color: AppColors.textSecondary,
                        ),
                      ),
                  ],
                ),
                const Divider(height: 18),

                // Booking & Customer Reference
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            '${trip.customerName} • ${trip.passengerCount} Penumpang (${trip.luggageCount} Koper)',
                            style: const TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            'Jemput: ${AppFormatters.dateTimeWib(trip.scheduledPickupAt)}',
                            style: const TextStyle(
                              fontSize: 12.5,
                              fontWeight: FontWeight.w700,
                              color: AppColors.navy,
                            ),
                          ),
                          Text(
                            'Tujuan: ${trip.destinationAddress}',
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontSize: 12,
                              color: AppColors.textSecondary,
                            ),
                          ),
                          if (isOwnerView && trip.driverName != null) ...[
                            const SizedBox(height: 2),
                            Text(
                              'Supir: ${trip.driverName} (${trip.vehiclePlate ?? "-"})',
                              style: const TextStyle(
                                fontSize: 12,
                                fontWeight: FontWeight.w700,
                                color: AppColors.emerald,
                              ),
                            ),
                          ],
                        ],
                      ),
                    ),
                    Text(
                      AppFormatters.rupiah(trip.agreedFareSnapshot),
                      style: const TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w900,
                        color: AppColors.emerald,
                      ),
                    ),
                  ],
                ),

                const SizedBox(height: 12),
                // Action Buttons: Detail, KNO Signboard, Maps & WA
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton.icon(
                        style: OutlinedButton.styleFrom(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
                          minimumSize: const Size(0, 44),
                        ),
                        onPressed: () => showFlightBookingDetailSheet(context, trip),
                        icon: const Icon(Icons.info_outline_rounded, size: 17),
                        label: const Text('Detail & Papan'),
                      ),
                    ),
                    const SizedBox(width: 8),
                    IconButton.outlined(
                      tooltip: 'Navigasi Maps ke Lokasi Jemput',
                      onPressed: () => DeepLinks.launchExternal(
                        DeepLinks.buildGoogleMapsNavigationUrl(
                          destination: trip.pickupAddress,
                        ),
                      ),
                      icon: const Icon(Icons.navigation_rounded, size: 18),
                    ),
                    const SizedBox(width: 8),
                    IconButton.filled(
                      style: IconButton.styleFrom(
                        backgroundColor: AppColors.emerald,
                        foregroundColor: Colors.white,
                      ),
                      tooltip: 'Hubungi Pelanggan via WhatsApp',
                      onPressed: () => DeepLinks.launchExternal(
                        DeepLinks.buildWhatsappUrl(
                          phone: trip.customerPhone,
                          message:
                              'Halo ${trip.customerName}, supir Mandiri Transport siap menjemput Anda untuk penerbangan $flightNo.',
                        ),
                      ),
                      icon: const Icon(Icons.chat_rounded, size: 18),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

Future<void> showFlightBookingDetailSheet(BuildContext context, Trip trip) async {
  await showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.white,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(22)),
    ),
    builder: (ctx) => FractionallySizedBox(
      heightFactor: 0.88,
      child: _FlightBookingDetailView(trip: trip),
    ),
  );
}

class _FlightBookingDetailView extends ConsumerWidget {
  final Trip trip;

  const _FlightBookingDetailView({required this.trip});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final flightNo = trip.flightNumber ?? '-';
    final info = trip.flightInfo;
    final airline = info?.airlineNameResolved ?? FlightHelper.resolveAirlineName(flightNo);
    final route = FlightHelper.resolveFlightRoute(
      flightNo,
      tripType: trip.tripType,
      originAirport: info?.originAirport,
      destinationAirport: info?.destinationAirport,
    );
    final status = info?.flightStatus ?? (trip.isCompleted ? 'LANDED' : 'SCHEDULED');
    final delayMinutes = info?.delayMinutes ?? 0;

    return Scaffold(
      appBar: AppBar(
        title: Text('Detail Penerbangan • $flightNo'),
        leading: IconButton(
          icon: const Icon(Icons.close_rounded),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.all(18),
        children: [
          // Aeronautical header
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: AppColors.navy,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            flightNo,
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 24,
                              fontWeight: FontWeight.w900,
                            ),
                          ),
                          Text(
                            airline,
                            style: const TextStyle(
                              color: Colors.white70,
                              fontSize: 14,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ],
                      ),
                    ),
                    StatusBadge(
                      label: FlightHelper.statusLabel(status, delayMinutes: delayMinutes),
                      fg: FlightHelper.statusFg(status, delayMinutes: delayMinutes),
                      bg: Colors.white,
                      icon: FlightHelper.statusIcon(status, delayMinutes: delayMinutes),
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
                          const Text(
                            'ASAL (ORIGIN)',
                            style: TextStyle(color: Colors.white54, fontSize: 10.5),
                          ),
                          Text(
                            FlightHelper.resolveAirportName(route.origin),
                            style: const TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.w800,
                              fontSize: 14,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const Icon(Icons.arrow_forward_rounded, color: Colors.white70),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          const Text(
                            'TUJUAN (DESTINATION)',
                            style: TextStyle(color: Colors.white54, fontSize: 10.5),
                          ),
                          Text(
                            FlightHelper.resolveAirportName(route.destination),
                            style: const TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.w800,
                              fontSize: 14,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),

          // Flight Timing Table
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Rincian Waktu Kedatangan (WIB):',
                    style: TextStyle(fontSize: 15, fontWeight: FontWeight.w800),
                  ),
                  const SizedBox(height: 10),
                  _buildDetailRow(
                    'Jadwal Resmi:',
                    AppFormatters.dateTimeWib(info?.scheduledArrivalTime ?? trip.scheduledPickupAt),
                  ),
                  _buildDetailRow(
                    'Estimasi Terkini:',
                    info?.estimatedArrivalTime != null
                        ? AppFormatters.dateTimeWib(info!.estimatedArrivalTime)
                        : 'Sesuai Jadwal Resmi',
                    highlightColor: delayMinutes > 0 ? AppColors.amber : null,
                  ),
                  _buildDetailRow(
                    'Waktu Mendarat (Aktual):',
                    info?.actualArrivalTime != null
                        ? AppFormatters.dateTimeWib(info!.actualArrivalTime)
                        : (trip.isCompleted ? AppFormatters.dateTimeWib(trip.scheduledPickupAt) : 'Belum tiba'),
                    highlightColor: trip.isCompleted || status == 'LANDED' ? AppColors.emerald : null,
                  ),
                  _buildDetailRow(
                    'Pintu & Terminal:',
                    '${info?.gate != null ? "Pintu ${info!.gate} • " : ""}${info?.terminal ?? "Terminal 1"}',
                  ),
                  if (info?.baggageClaim != null)
                    _buildDetailRow('Sabuk Bagasi:', info!.baggageClaim!),
                  _buildDetailRow(
                    'Kualitas Data:',
                    info?.isLive == true
                        ? 'Live FlightAware (Aktif)'
                        : (info?.isStale == true
                            ? 'Data Tersimpan (Stale)'
                            : 'Jadwal Booking Asli'),
                  ),
                  _buildDetailRow(
                    'Pembaruan Terakhir:',
                    info?.lastTrackedAt != null
                        ? AppFormatters.dateTimeWib(info!.lastTrackedAt)
                        : 'Belum pernah dilacak',
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 14),

          // Booking Details Card
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Data Booking Mandiri Transport:',
                    style: TextStyle(fontSize: 15, fontWeight: FontWeight.w800),
                  ),
                  const SizedBox(height: 10),
                  _buildDetailRow('Kode Booking:', trip.bookingCode),
                  _buildDetailRow('Kode Trip:', trip.tripCode),
                  _buildDetailRow('Tamu / Pelanggan:', trip.customerName),
                  _buildDetailRow('Telepon:', trip.customerPhone),
                  _buildDetailRow('Tarif Disepakati:', AppFormatters.rupiah(trip.agreedFareSnapshot)),
                  _buildDetailRow('Lokasi Jemput:', trip.pickupAddress),
                  _buildDetailRow('Tujuan:', trip.destinationAddress),
                  _buildDetailRow('Armada & Supir:', '${trip.vehiclePlate ?? "-"} • ${trip.driverName ?? "Belum ditugaskan"}'),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Action buttons
          if (trip.tripType == 'AIRPORT_PICKUP' || trip.flightNumber != null) ...[
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                style: FilledButton.styleFrom(backgroundColor: AppColors.navy),
                onPressed: () => Navigator.push(
                  context,
                  MaterialPageRoute<void>(
                    builder: (_) => AirportSignboardScreen(trip: trip),
                  ),
                ),
                icon: const Icon(Icons.fullscreen_rounded),
                label: const Text('Buka Layar Papan Nama (KNO)'),
              ),
            ),
            const SizedBox(height: 10),
          ],
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              onPressed: () async {
                await ref
                    .read(appControllerProvider.notifier)
                    .refreshTodayFlightSchedules();
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Pembaruan data selesai.')),
                  );
                }
              },
              icon: const Icon(Icons.refresh_rounded),
              label: const Text('Segera Perbarui Pelacakan'),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDetailRow(String label, String value, {Color? highlightColor}) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 140,
            child: Text(
              label,
              style: const TextStyle(
                fontSize: 12.5,
                color: AppColors.textSecondary,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w700,
                color: highlightColor ?? AppColors.textPrimary,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
