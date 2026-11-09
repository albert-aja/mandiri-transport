import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_theme.dart';
import '../../core/utils/deep_links.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/shared_widgets.dart';
import '../../data/models/models.dart';
import '../../data/state/app_controller.dart';
import 'dispatch_simulator_sheet.dart';
import 'quick_booking_sheet.dart';

class OwnerDispatchTab extends ConsumerStatefulWidget {
  const OwnerDispatchTab({super.key});

  @override
  ConsumerState<OwnerDispatchTab> createState() => _OwnerDispatchTabState();
}

class _OwnerDispatchTabState extends ConsumerState<OwnerDispatchTab> {
  String _filter = 'ALL';

  void _showTripProfitabilityDialog(BuildContext context, Trip trip) {
    final prof = ref
        .read(appControllerProvider.notifier)
        .computeTripProfitability(trip);
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 18, 20, 24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Profitabilitas Perjalanan • ${trip.tripCode}',
                  style: const TextStyle(
                    fontSize: 17.5,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                Text(
                  'Formula v${prof.formulaVersionNumber} • Jarak: ${prof.distanceKmUsed.toStringAsFixed(1)} km (${prof.distanceSource})',
                  style: const TextStyle(
                    fontSize: 12.5,
                    color: AppColors.textSecondary,
                  ),
                ),
                const Divider(height: 20),
                _RowVal(
                  label: 'Tarif Disepakati',
                  val: AppFormatters.rupiah(prof.agreedFareSnapshot),
                  bold: true,
                ),
                _RowVal(
                  label: 'Total Pengeluaran Aktual (BBM/Tol/Parkir)',
                  val: AppFormatters.rupiah(prof.actualExpensesTotal),
                ),
                _RowVal(
                  label: 'Alokasi Biaya Operasional (Ban/Oli/Servis/Komisi)',
                  val: AppFormatters.rupiah(prof.allocatedOperatingCost),
                ),
                _RowVal(
                  label: 'Total Biaya Efektif',
                  val: AppFormatters.rupiah(prof.totalEffectiveCost),
                  bold: true,
                ),
                const Divider(height: 18),
                _RowVal(
                  label: 'Estimasi Profit Awal',
                  val: AppFormatters.rupiah(prof.estimatedProfit),
                ),
                _RowVal(
                  label: 'Profit Operasional Aktual',
                  val: AppFormatters.rupiah(prof.actualProfit),
                  bold: true,
                  color: prof.actualProfit >= 0
                      ? AppColors.emerald
                      : AppColors.crimson,
                ),
                const SizedBox(height: 12),
                Flexible(
                  child: ListView.builder(
                    shrinkWrap: true,
                    itemCount: prof.items.length,
                    itemBuilder: (_, idx) {
                      final item = prof.items[idx];
                      return Padding(
                        padding: const EdgeInsets.symmetric(vertical: 3),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Expanded(
                              child: Text(
                                '${item.name} [${item.reconciliationPolicy}]',
                                style: const TextStyle(fontSize: 12),
                              ),
                            ),
                            Text(
                              AppFormatters.rupiah(item.effectiveAmount),
                              style: const TextStyle(
                                fontSize: 12,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                          ],
                        ),
                      );
                    },
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(appControllerProvider);
    final trips = state.trips.where((t) {
      return switch (_filter) {
        'UNASSIGNED' => t.isUnassigned,
        'ACTIVE' => !const {'COMPLETED', 'CANCELLED', 'NO_SHOW'}.contains(
          t.status,
        ),
        'COMPLETED' => t.status == 'COMPLETED',
        _ => true,
      };
    }).toList();

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 96),
      children: [
        Row(
          children: [
            Expanded(
              child: FilledButton.icon(
                style: FilledButton.styleFrom(
                  backgroundColor: AppColors.emerald,
                ),
                onPressed: () => showQuickBookingSheet(context),
                icon: const Icon(Icons.add_circle_outline),
                label: const Text('+ Booking Cepat'),
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(
            children: [
              for (final entry in const [
                ('ALL', 'Semua'),
                ('UNASSIGNED', 'Belum Ditugaskan'),
                ('ACTIVE', 'Aktif & Terjadwal'),
                ('COMPLETED', 'Selesai'),
              ])
                Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: ChoiceChip(
                    label: Text(entry.$2),
                    selected: _filter == entry.$1,
                    onSelected: (_) => setState(() => _filter = entry.$1),
                  ),
                ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        ...trips.map((trip) {
          return Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(15),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Wrap(
                      spacing: 8,
                      runSpacing: 6,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: [
                        StatusBadge.forTripStatus(trip.status),
                        StatusBadge(
                          label: OperationalLabels.tripType(trip.tripType),
                          fg: AppColors.navy,
                          bg: AppColors.slateBg,
                        ),
                        if (trip.flightNumber != null)
                          StatusBadge(
                            label: 'Pesawat ${trip.flightNumber}',
                            fg: AppColors.infoBlue,
                            bg: AppColors.infoSoft,
                            icon: Icons.flight_land_rounded,
                          ),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Expanded(
                          child: Text(
                            '${trip.tripCode} • ${trip.customerName}',
                            style: const TextStyle(
                              fontWeight: FontWeight.w800,
                              fontSize: 15.5,
                            ),
                          ),
                        ),
                        Text(
                          AppFormatters.rupiah(trip.agreedFareSnapshot),
                          style: const TextStyle(
                            fontWeight: FontWeight.w900,
                            fontSize: 16,
                            color: AppColors.emerald,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Jemput: ${AppFormatters.dateTimeWib(trip.scheduledPickupAt)} (Reservasi ${AppFormatters.timeWib(trip.reservedStartAt)}–${AppFormatters.timeWib(trip.reservedEndAt)})',
                      style: const TextStyle(
                        fontSize: 12.5,
                        fontWeight: FontWeight.w600,
                        color: AppColors.navy,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      '${trip.pickupAddress} -> ${trip.destinationAddress}',
                      style: const TextStyle(
                        fontSize: 13,
                        color: AppColors.textSecondary,
                      ),
                    ),
                    if (trip.stops.isNotEmpty) ...[
                      const SizedBox(height: 4),
                      Text(
                        'Singgah (${trip.stops.length}): ${trip.stops.map((s) => s.address).join(" -> ")}',
                        style: const TextStyle(
                          fontSize: 12,
                          color: AppColors.infoBlue,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                    const SizedBox(height: 8),
                    Text(
                      trip.driverName != null
                          ? 'Supir: ${trip.driverName} • Unit: ${trip.vehiclePlate ?? "-"} (${trip.vehicleModel ?? "-"})'
                          : 'Supir & Unit: BELUM DITUGASKAN (${trip.passengerCount} org, ${trip.luggageCount} tas)',
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w700,
                        color: trip.driverName != null
                            ? AppColors.textPrimary
                            : AppColors.crimson,
                      ),
                    ),
                    const SizedBox(height: 12),
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: [
                        if (trip.status != 'COMPLETED')
                          OutlinedButton.icon(
                            onPressed: () =>
                                showDispatchSimulatorSheet(context, trip),
                            icon: const Icon(Icons.radar_rounded, size: 18),
                            label: Text(
                              trip.driverId == null
                                  ? 'Tugaskan Supir & Unit'
                                  : 'Ubah / Simulasi Bentrok',
                            ),
                          ),
                        OutlinedButton.icon(
                          onPressed: () =>
                              _showTripProfitabilityDialog(context, trip),
                          icon: const Icon(
                            Icons.calculate_outlined,
                            size: 18,
                          ),
                          label: const Text('Rincian Profit'),
                        ),
                        OutlinedButton.icon(
                          onPressed: () => showWhatsappTemplatePickerSheet(
                            context: context,
                            templates: state.whatsappTemplates,
                            recipientPhone: trip.customerPhone,
                            customerName: trip.customerName,
                            trip: trip,
                          ),
                          icon: const Icon(Icons.chat_outlined, size: 18),
                          label: const Text('WA Tamu'),
                        ),
                        IconButton(
                          tooltip: 'Buka Google Maps',
                          onPressed: () => DeepLinks.launchExternal(
                            DeepLinks.buildGoogleMapsNavigationUrl(
                              destination: trip.destinationAddress,
                            ),
                          ),
                          icon: const Icon(
                            Icons.map_outlined,
                            color: AppColors.navy,
                          ),
                        ),
                      ],
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

class _RowVal extends StatelessWidget {
  final String label;
  final String val;
  final bool bold;
  final Color? color;

  const _RowVal({
    required this.label,
    required this.val,
    this.bold = false,
    this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Expanded(
            child: Text(
              label,
              style: TextStyle(
                fontSize: 13.5,
                fontWeight: bold ? FontWeight.w800 : FontWeight.w500,
              ),
            ),
          ),
          Text(
            val,
            style: TextStyle(
              fontSize: 14,
              fontWeight: bold ? FontWeight.w900 : FontWeight.w700,
              color: color ?? AppColors.textPrimary,
            ),
          ),
        ],
      ),
    );
  }
}
