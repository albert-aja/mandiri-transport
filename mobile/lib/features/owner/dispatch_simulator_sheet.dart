import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_theme.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/shared_widgets.dart';
import '../../data/models/models.dart';
import '../../data/state/app_controller.dart';

Future<void> showDispatchSimulatorSheet(BuildContext context, Trip trip) async {
  await showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    backgroundColor: Colors.white,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(22)),
    ),
    builder: (_) => DispatchSimulatorSheet(trip: trip),
  );
}

class DispatchSimulatorSheet extends ConsumerStatefulWidget {
  final Trip trip;

  const DispatchSimulatorSheet({super.key, required this.trip});

  @override
  ConsumerState<DispatchSimulatorSheet> createState() =>
      _DispatchSimulatorSheetState();
}

class _DispatchSimulatorSheetState
    extends ConsumerState<DispatchSimulatorSheet> {
  String? _driverId;
  String? _vehicleId;
  bool _forceOverride = false;
  final _overrideReasonCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    final state = ref.read(appControllerProvider);
    _driverId =
        widget.trip.driverId ??
        state.drivers
            .where((d) => d.availabilityStatus == 'AVAILABLE')
            .firstOrNull
            ?.id ??
        state.drivers.firstOrNull?.id;
    _vehicleId =
        widget.trip.vehicleId ??
        state.vehicles
            .where(
              (v) =>
                  v.status == 'ACTIVE' &&
                  v.passengerCapacity >= widget.trip.passengerCount,
            )
            .firstOrNull
            ?.id ??
        state.vehicles.firstOrNull?.id;
  }

  @override
  void dispose() {
    _overrideReasonCtrl.dispose();
    super.dispose();
  }

  Future<void> _confirmAssignment(ConflictSimulationResult sim) async {
    if (_driverId == null || _vehicleId == null) return;
    if (sim.hasAnyConflict &&
        (!_forceOverride || _overrideReasonCtrl.text.trim().isEmpty)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'Terdapat bentrok jadwal! Pilih supir/unit lain atau isi alasan override.',
          ),
        ),
      );
      return;
    }

    final conflict = await ref
        .read(appControllerProvider.notifier)
        .assignTrip(
          trip: widget.trip,
          driverId: _driverId!,
          vehicleId: _vehicleId!,
          forceOverrideConfirmed: _forceOverride,
          overrideReason: _overrideReasonCtrl.text.trim(),
        );

    if (!mounted) return;
    if (conflict != null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Konflik jadwal terdeteksi — silakan tinjau peringatan.'),
        ),
      );
      return;
    }

    Navigator.pop(context);
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          'Penugasan ${widget.trip.tripCode} berhasil disimpan secara manual.',
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final appState = ref.watch(appControllerProvider);
    final sim = ref
        .read(appControllerProvider.notifier)
        .simulateDispatch(
          trip: widget.trip,
          candidateDriverId: _driverId,
          candidateVehicleId: _vehicleId,
        );

    return DraggableScrollableSheet(
      expand: false,
      initialChildSize: 0.9,
      minChildSize: 0.6,
      maxChildSize: 0.96,
      builder: (context, scrollCtrl) {
        return ListView(
          controller: scrollCtrl,
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 28),
          children: [
            Row(
              children: [
                const Icon(Icons.radar_rounded, color: AppColors.navy),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    'Dispatch & Simulasi Bentrok • ${widget.trip.tripCode}',
                    style: const TextStyle(
                      fontSize: 17.5,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
                IconButton(
                  onPressed: () => Navigator.pop(context),
                  icon: const Icon(Icons.close),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppColors.slateBg,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.cardBorder),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '${widget.trip.customerName} • ${widget.trip.passengerCount} Penumpang, ${widget.trip.luggageCount} Koper',
                    style: const TextStyle(
                      fontWeight: FontWeight.w800,
                      fontSize: 14.5,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Jendela Reservasi Penuh: ${AppFormatters.dateTimeWib(sim.reservedStartAt)} s/d ${AppFormatters.timeWib(sim.reservedEndAt)}',
                    style: const TextStyle(
                      fontSize: 13,
                      color: AppColors.navy,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Rincian Buffer: Persiapan ${widget.trip.prepBufferMin}m + Jalan ${widget.trip.estimatedDurationMin}m + Tunggu Bandara ${widget.trip.arrivalBufferMin}m + Reposisi ${widget.trip.repositionBufferMin}m',
                    style: const TextStyle(
                      fontSize: 12,
                      color: AppColors.textSecondary,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 14),
            AppDropdownField<String>(
              label: 'Pilih Supir (Cek Bentrok Independen)',
              value: _driverId,
              items: appState.drivers
                  .map(
                    (d) => DropdownMenuItem(
                      value: d.id,
                      child: Text(
                        '${d.fullName} (${OperationalLabels.driverAvailability(d.availabilityStatus)})',
                      ),
                    ),
                  )
                  .toList(),
              onChanged: (v) => setState(() => _driverId = v),
            ),
            const SizedBox(height: 10),
            AppDropdownField<String>(
              label: 'Pilih Unit Kendaraan (Cek Bentrok & Kapasitas)',
              value: _vehicleId,
              items: appState.vehicles
                  .map(
                    (v) => DropdownMenuItem(
                      value: v.id,
                      child: Text(
                        '${v.registrationNumber} • ${v.nameModel} (${v.passengerCapacity} kursi)',
                      ),
                    ),
                  )
                  .toList(),
              onChanged: (v) => setState(() => _vehicleId = v),
            ),
            const SizedBox(height: 14),
            if (sim.hasAnyConflict)
              Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: AppColors.crimsonSoft,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: AppColors.crimson.withValues(alpha: 0.4),
                  ),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Row(
                      children: [
                        Icon(
                          Icons.warning_amber_rounded,
                          color: AppColors.crimson,
                        ),
                        SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            'PERINGATAN BENTROK JADWAL / UNIT TIDAK SIAP',
                            style: TextStyle(
                              fontWeight: FontWeight.w800,
                              color: AppColors.crimson,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    ...[...sim.driverConflicts, ...sim.vehicleConflicts].map(
                      (c) => Padding(
                        padding: const EdgeInsets.only(bottom: 4),
                        child: Text(
                          '• ${c.explanation}',
                          style: const TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w600,
                            color: AppColors.textPrimary,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 8),
                    CheckboxListTile(
                      contentPadding: EdgeInsets.zero,
                      value: _forceOverride,
                      onChanged: (v) =>
                          setState(() => _forceOverride = v ?? false),
                      title: const Text(
                        'Paksa Tugaskan Manual (Override Dispatcher)',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                    if (_forceOverride)
                      TextField(
                        controller: _overrideReasonCtrl,
                        decoration: const InputDecoration(
                          labelText: 'Alasan Override (Wajib Diisi)',
                          hintText: 'Contoh: Pesawat delay, unit pengganti siap',
                        ),
                      ),
                  ],
                ),
              )
            else
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.emeraldSoft,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Row(
                  children: [
                    Icon(Icons.verified_rounded, color: AppColors.emerald),
                    SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        'Jadwal Aman: Supir & Unit Kendaraan bebas bentrok pada jendela waktu ini.',
                        style: TextStyle(
                          color: AppColors.emerald,
                          fontWeight: FontWeight.w700,
                          fontSize: 13,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            const SizedBox(height: 16),
            const Text(
              'Rekomendasi Pemerataan Beban Kerja Supir:',
              style: TextStyle(fontWeight: FontWeight.w800, fontSize: 14.5),
            ),
            const SizedBox(height: 8),
            ...sim.driverRecommendations.map((rec) {
              final selected = rec.driverId == _driverId;
              return Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: InkWell(
                  onTap: () => setState(() => _driverId = rec.driverId),
                  borderRadius: BorderRadius.circular(12),
                  child: Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: selected
                          ? AppColors.navy.withValues(alpha: 0.06)
                          : Colors.white,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: selected ? AppColors.navy : AppColors.cardBorder,
                        width: selected ? 1.8 : 1,
                      ),
                    ),
                    child: Row(
                      children: [
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  Flexible(
                                    child: Text(
                                      rec.driverName,
                                      overflow: TextOverflow.ellipsis,
                                      style: const TextStyle(
                                        fontWeight: FontWeight.w800,
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 8),
                                  if (rec.recommended)
                                    const StatusBadge(
                                      label: 'Direkomendasikan',
                                      fg: AppColors.emerald,
                                      bg: AppColors.emeraldSoft,
                                    ),
                                ],
                              ),
                              const SizedBox(height: 4),
                              Text(
                                rec.explanation,
                                style: const TextStyle(
                                  fontSize: 12.5,
                                  color: AppColors.textSecondary,
                                ),
                              ),
                            ],
                          ),
                        ),
                        Icon(
                          selected
                              ? Icons.check_circle
                              : Icons.circle_outlined,
                          color: selected
                              ? AppColors.navy
                              : AppColors.textSecondary,
                        ),
                      ],
                    ),
                  ),
                ),
              );
            }),
            const SizedBox(height: 10),
            const Text(
              'Rekomendasi Kecocokan Unit Kendaraan:',
              style: TextStyle(fontWeight: FontWeight.w800, fontSize: 14.5),
            ),
            const SizedBox(height: 8),
            ...sim.vehicleRecommendations.map((vrec) {
              final selected = vrec.vehicleId == _vehicleId;
              return Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: InkWell(
                  onTap: () => setState(() => _vehicleId = vrec.vehicleId),
                  borderRadius: BorderRadius.circular(12),
                  child: Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: selected
                          ? AppColors.navy.withValues(alpha: 0.06)
                          : Colors.white,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: selected ? AppColors.navy : AppColors.cardBorder,
                        width: selected ? 1.8 : 1,
                      ),
                    ),
                    child: Row(
                      children: [
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  Expanded(
                                    child: Text(
                                      '${vrec.registrationNumber} • ${vrec.nameModel}',
                                      style: const TextStyle(
                                        fontWeight: FontWeight.w800,
                                      ),
                                    ),
                                  ),
                                  StatusBadge(
                                    label: 'Skor ${vrec.suitabilityScore}',
                                    fg: vrec.suitable
                                        ? AppColors.emerald
                                        : AppColors.crimson,
                                    bg: vrec.suitable
                                        ? AppColors.emeraldSoft
                                        : AppColors.crimsonSoft,
                                  ),
                                ],
                              ),
                              const SizedBox(height: 4),
                              Text(
                                vrec.explanation,
                                style: const TextStyle(
                                  fontSize: 12.5,
                                  color: AppColors.textSecondary,
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(width: 8),
                        Icon(
                          selected
                              ? Icons.check_circle
                              : Icons.circle_outlined,
                          color: selected
                              ? AppColors.navy
                              : AppColors.textSecondary,
                        ),
                      ],
                    ),
                  ),
                ),
              );
            }),
            const SizedBox(height: 16),
            FilledButton.icon(
              onPressed: () => _confirmAssignment(sim),
              icon: const Icon(Icons.assignment_turned_in_rounded),
              label: const Text('Konfirmasi Penugasan Supir & Unit'),
            ),
          ],
        );
      },
    );
  }
}
