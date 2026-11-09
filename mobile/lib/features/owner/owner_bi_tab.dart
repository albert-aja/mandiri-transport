import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_theme.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/shared_widgets.dart';
import '../../data/models/models.dart';
import '../../data/state/app_controller.dart';

class OwnerBiTab extends ConsumerStatefulWidget {
  const OwnerBiTab({super.key});

  @override
  ConsumerState<OwnerBiTab> createState() => _OwnerBiTabState();
}

class _OwnerBiTabState extends ConsumerState<OwnerBiTab> {
  String? _routeFilter;
  String? _vehicleFilter;
  String? _driverFilter;
  String? _customerFilter;

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(appControllerProvider);
    final controller = ref.read(appControllerProvider.notifier);

    final bi = controller.computeBiExplorer(
      routeId: _routeFilter,
      vehicleId: _vehicleFilter,
      driverId: _driverFilter,
      customerId: _customerFilter,
    );

    final pickupSetting = state.notificationSettings
        .where((s) => s.reminderType == 'UPCOMING_PICKUP')
        .firstOrNull;

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 96),
      children: [
        const Text(
          'Eksplorasi Analitik Bisnis & Smart Operations',
          style: TextStyle(fontSize: 17, fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 10),
        // Cross-Filter Bar
        Card(
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'Filter Silang Operasional',
                      style: TextStyle(
                        fontWeight: FontWeight.w700,
                        fontSize: 13.5,
                      ),
                    ),
                    if (_routeFilter != null ||
                        _vehicleFilter != null ||
                        _driverFilter != null ||
                        _customerFilter != null)
                      TextButton(
                        onPressed: () => setState(() {
                          _routeFilter = null;
                          _vehicleFilter = null;
                          _driverFilter = null;
                          _customerFilter = null;
                        }),
                        child: const Text('Reset Filter'),
                      ),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Expanded(
                      child: AppDropdownField<String?>(
                        label: 'Rute',
                        value: _routeFilter,
                        items: [
                          const DropdownMenuItem<String?>(
                            value: null,
                            child: Text('Semua Rute'),
                          ),
                          ...state.routes.map(
                            (r) => DropdownMenuItem<String?>(
                              value: r.id,
                              child: Text(r.originName == 'Medan'
                                  ? 'Mdn -> ${r.destinationName}'
                                  : r.name),
                            ),
                          ),
                        ],
                        onChanged: (v) => setState(() => _routeFilter = v),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: AppDropdownField<String?>(
                        label: 'Supir',
                        value: _driverFilter,
                        items: [
                          const DropdownMenuItem<String?>(
                            value: null,
                            child: Text('Semua Supir'),
                          ),
                          ...state.drivers.map(
                            (d) => DropdownMenuItem<String?>(
                              value: d.id,
                              child: Text(d.fullName),
                            ),
                          ),
                        ],
                        onChanged: (v) => setState(() => _driverFilter = v),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Expanded(
                      child: AppDropdownField<String?>(
                        label: 'Unit Armada',
                        value: _vehicleFilter,
                        items: [
                          const DropdownMenuItem<String?>(
                            value: null,
                            child: Text('Semua Unit'),
                          ),
                          ...state.vehicles.map(
                            (v) => DropdownMenuItem<String?>(
                              value: v.id,
                              child: Text(v.registrationNumber),
                            ),
                          ),
                        ],
                        onChanged: (v) => setState(() => _vehicleFilter = v),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: AppDropdownField<String?>(
                        label: 'Pelanggan',
                        value: _customerFilter,
                        items: [
                          const DropdownMenuItem<String?>(
                            value: null,
                            child: Text('Semua Tamu'),
                          ),
                          ...state.customers.map(
                            (c) => DropdownMenuItem<String?>(
                              value: c.id,
                              child: Text(c.name),
                            ),
                          ),
                        ],
                        onChanged: (v) => setState(() => _customerFilter = v),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 14),

        // 14.1 Financial Intelligence Grid
        GridView.count(
          crossAxisCount: 2,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          mainAxisSpacing: 8,
          crossAxisSpacing: 8,
          childAspectRatio: 1.85,
          children: [
            _BiKpiCard(
              label: 'Pendapatan Trip Selesai',
              value: AppFormatters.rupiah(bi.totalRevenue),
              color: AppColors.navy,
            ),
            _BiKpiCard(
              label: 'Laba Kotor Operasional',
              value: AppFormatters.rupiah(bi.grossOperatingProfit),
              color: AppColors.emerald,
            ),
            _BiKpiCard(
              label: 'Pengeluaran Aktual',
              value: AppFormatters.rupiah(bi.totalActualExpenses),
              color: AppColors.textPrimary,
            ),
            _BiKpiCard(
              label: 'Alokasi Aus & Komisi',
              value: AppFormatters.rupiah(bi.totalAllocatedCosts),
              color: AppColors.textSecondary,
            ),
            _BiKpiCard(
              label: 'Piutang / Belum Lunas',
              value: AppFormatters.rupiah(bi.unpaidBalances),
              color: AppColors.crimson,
            ),
            _BiKpiCard(
              label: 'Kas Tunai di Supir',
              value: AppFormatters.rupiah(bi.driverHeldCash),
              color: AppColors.amber,
            ),
          ],
        ),
        const SizedBox(height: 18),

        // 13.2 Route Performance History (with < 5 perjalanan guard!)
        const Text(
          'Performa Historis Rute (Batas Validitas >= 5 Trip)',
          style: TextStyle(fontSize: 15.5, fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 8),
        ...bi.routePerformance.map((rp) {
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
                            rp.routeName,
                            style: const TextStyle(
                              fontWeight: FontWeight.w800,
                              fontSize: 14.5,
                            ),
                          ),
                        ),
                        StatusBadge(
                          label: rp.sufficientSample
                              ? '${rp.sampleCount} perjalanan'
                              : '< 5 perjalanan (${rp.sampleCount} sampel)',
                          fg: rp.sufficientSample
                              ? AppColors.emerald
                              : AppColors.amber,
                          bg: rp.sufficientSample
                              ? AppColors.emeraldSoft
                              : AppColors.amberSoft,
                        ),
                      ],
                    ),
                    const SizedBox(height: 6),
                    Text(
                      'Rata-rata Durasi: ${rp.avgDurationMin.toStringAsFixed(0)} mnt • Jarak: ${rp.avgDistanceKm.toStringAsFixed(1)} km',
                      style: const TextStyle(fontSize: 13),
                    ),
                    Text(
                      'Rata-rata Biaya: ${AppFormatters.rupiah(rp.avgEffectiveCost)} • Rata-rata Profit: ${AppFormatters.rupiah(rp.avgActualProfit)}',
                      style: const TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w700,
                        color: AppColors.navy,
                      ),
                    ),
                    if (!rp.sufficientSample)
                      const Padding(
                        padding: EdgeInsets.only(top: 4),
                        child: Text(
                          'Catatan: Data masih awal (<5 perjalanan), gunakan rata-rata ini sebagai indikasi awal saja.',
                          style: TextStyle(
                            fontSize: 12,
                            color: AppColors.amber,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            ),
          );
        }),

        // 13.5 Customer Profitability
        const SizedBox(height: 14),
        const Text(
          'Profitabilitas Pelanggan & Kontrak Harga',
          style: TextStyle(fontSize: 15.5, fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 8),
        ...bi.customerProfitability.map((cp) {
          return Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Expanded(
                          child: Text(
                            '${cp.customerName} (${cp.tripCount} trip)',
                            style: const TextStyle(fontWeight: FontWeight.w800),
                          ),
                        ),
                        Text(
                          'Margin: ${AppFormatters.rupiah(cp.netOperatingProfit)}',
                          style: const TextStyle(
                            fontWeight: FontWeight.w800,
                            color: AppColors.emerald,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Pendapatan: ${AppFormatters.rupiah(cp.totalRevenue)} • Biaya Efektif: ${AppFormatters.rupiah(cp.totalEffectiveCost)}',
                      style: const TextStyle(fontSize: 12.5),
                    ),
                    Text(
                      'Skema: ${cp.pricingSummary}',
                      style: const TextStyle(
                        fontSize: 12,
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          );
        }),

        // 13.1 Driver Workload Distribution
        const SizedBox(height: 14),
        const Text(
          'Distribusi Beban Kerja Supir',
          style: TextStyle(fontSize: 15.5, fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 8),
        ...bi.driverWorkloads.map((dw) {
          return Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(13),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            dw.driverName,
                            style: const TextStyle(fontWeight: FontWeight.w800),
                          ),
                          Text(
                            '${dw.completedTripsCount} selesai • ${dw.upcomingTripsCount} terjadwal • ${dw.totalActiveHours} jam jalan',
                            style: const TextStyle(
                              fontSize: 12.5,
                              color: AppColors.textSecondary,
                            ),
                          ),
                        ],
                      ),
                    ),
                    StatusBadge(
                      label: OperationalLabels.driverAvailability(
                        dw.availabilityStatus,
                      ),
                      fg: dw.availabilityStatus == 'AVAILABLE'
                          ? AppColors.emerald
                          : AppColors.amber,
                      bg: dw.availabilityStatus == 'AVAILABLE'
                          ? AppColors.emeraldSoft
                          : AppColors.amberSoft,
                    ),
                  ],
                ),
              ),
            ),
          );
        }),

        // 13.7 Vehicle Replacement Intelligence
        const SizedBox(height: 14),
        const Text(
          'Evaluasi Peremajaan Armada (Biaya Servis & Downtime)',
          style: TextStyle(fontSize: 15.5, fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 8),
        ...bi.vehicleReplacementItems.map((vr) {
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
                            '${vr.registrationNumber} • ${vr.nameModel} (${vr.manufactureYear})',
                            style: const TextStyle(fontWeight: FontWeight.w800),
                          ),
                        ),
                        StatusBadge(
                          label: vr.flagForEvaluation
                              ? 'Evaluasi Peremajaan'
                              : 'Unit Sehat',
                          fg: vr.flagForEvaluation
                              ? AppColors.crimson
                              : AppColors.emerald,
                          bg: vr.flagForEvaluation
                              ? AppColors.crimsonSoft
                              : AppColors.emeraldSoft,
                        ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Odometer: ${AppFormatters.thousandNumber(vr.odometerKm)} km • Total Servis: ${AppFormatters.rupiah(vr.totalRepairCost)} • Downtime: ${vr.totalDowntimeHours.toStringAsFixed(0)} jam',
                      style: const TextStyle(fontSize: 12.5),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      vr.evaluationNote,
                      style: TextStyle(
                        fontSize: 12.5,
                        color: vr.flagForEvaluation
                            ? AppColors.crimson
                            : AppColors.textSecondary,
                        fontWeight: vr.flagForEvaluation
                            ? FontWeight.w600
                            : FontWeight.w400,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          );
        }),

        // Live Firebase Cloud Messaging (FCM) Diagnostics & Test Push
        const SizedBox(height: 14),
        const Text(
          'Live Firebase Cloud Messaging (FCM) & Push Real-Time',
          style: TextStyle(fontSize: 15.5, fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 8),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  crossAxisAlignment: WrapCrossAlignment.center,
                  children: [
                    StatusBadge(
                      label:
                          (state.fcmState.backendLiveReady ||
                              state.fcmState.clientInitialized)
                          ? 'FCM Live Aktif'
                          : 'Mode Siaga Kotak Masuk',
                      fg:
                          (state.fcmState.backendLiveReady ||
                              state.fcmState.clientInitialized)
                          ? AppColors.emerald
                          : AppColors.amber,
                      bg:
                          (state.fcmState.backendLiveReady ||
                              state.fcmState.clientInitialized)
                          ? AppColors.emeraldSoft
                          : AppColors.amberSoft,
                      icon: Icons.notifications_active_rounded,
                    ),
                    StatusBadge(
                      label:
                          'Project: ${state.fcmState.backendProjectId}',
                      fg: AppColors.infoBlue,
                      bg: AppColors.infoSoft,
                    ),
                    StatusBadge(
                      label:
                          'Perangkat Aktif: ${state.fcmState.myActiveDevicesCount} saya / ${state.fcmState.fleetActiveDevicesCount} armada',
                      fg: AppColors.navy,
                      bg: const Color(0xFFE2E8F0),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Text(
                  state.fcmState.statusSummary,
                  style: const TextStyle(
                    fontSize: 12.5,
                    color: AppColors.textSecondary,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  'Token Perangkat: ${state.fcmState.deviceToken != null && state.fcmState.deviceToken!.length > 20 ? "${state.fcmState.deviceToken!.substring(0, 12)}...${state.fcmState.deviceToken!.substring(state.fcmState.deviceToken!.length - 6)}" : (state.fcmState.deviceToken ?? "Belum tersedia (menunggu google-services.json)")}',
                  style: const TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: AppColors.navy,
                  ),
                ),
                const SizedBox(height: 10),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    FilledButton.icon(
                      style: FilledButton.styleFrom(
                        backgroundColor: AppColors.emerald,
                      ),
                      onPressed: () async {
                        final msg = await controller.sendLiveTestPush();
                        if (context.mounted && msg.isNotEmpty) {
                          ScaffoldMessenger.of(
                            context,
                          ).showSnackBar(SnackBar(content: Text(msg)));
                        }
                      },
                      icon: const Icon(Icons.send_to_mobile_rounded, size: 18),
                      label: const Text('Uji Push FCM'),
                    ),
                    OutlinedButton.icon(
                      onPressed: () async {
                        final msg = await controller
                            .triggerOperationalReminderScan();
                        if (context.mounted && msg.isNotEmpty) {
                          ScaffoldMessenger.of(
                            context,
                          ).showSnackBar(SnackBar(content: Text(msg)));
                        }
                      },
                      icon: const Icon(Icons.radar_rounded, size: 18),
                      label: const Text('Pindai Pengingat'),
                    ),
                    TextButton.icon(
                      onPressed: () => _showFirebaseOptionsDialog(context),
                      icon: const Icon(Icons.settings_suggest_rounded, size: 18),
                      label: const Text('FirebaseOptions'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),

        // Configurable Pickup Reminder Lead Time Settings
        if (pickupSetting != null) ...[
          const SizedBox(height: 14),
          const Text(
            'Pengaturan Pengingat Penjemputan (Non-Blocking)',
            style: TextStyle(fontSize: 15.5, fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 8),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Waktu Pengingat Sebelum Jemput:',
                    style: TextStyle(fontWeight: FontWeight.w700),
                  ),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      for (final opt in const [
                        (15, '15 mnt'),
                        (30, '30 mnt'),
                        (60, '1 jam'),
                        (120, '2 jam'),
                      ])
                        ChoiceChip(
                          label: Text(opt.$2),
                          selected: pickupSetting.leadTimeMinutes == opt.$1,
                          onSelected: (_) =>
                              controller.updateNotificationSetting(
                                settingId: pickupSetting.id,
                                leadTimeMinutes: opt.$1,
                              ),
                        ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  AppDropdownField<String>(
                    label: 'Penerima Pengingat',
                    value: pickupSetting.recipientScope,
                    items: const [
                      DropdownMenuItem(
                        value: 'BOTH',
                        child: Text('Pemilik & Supir Bertugas'),
                      ),
                      DropdownMenuItem(
                        value: 'DRIVER',
                        child: Text('Supir Saja'),
                      ),
                      DropdownMenuItem(
                        value: 'OWNER',
                        child: Text('Pemilik Saja'),
                      ),
                    ],
                    onChanged: (v) {
                      if (v != null) {
                        controller.updateNotificationSetting(
                          settingId: pickupSetting.id,
                          recipientScope: v,
                        );
                      }
                    },
                  ),
                ],
              ),
            ),
          ),
        ],
      ],
    );
  }

  Future<void> _showFirebaseOptionsDialog(BuildContext context) async {
    final store = ref.read(offlineStoreProvider);
    final controller = ref.read(appControllerProvider.notifier);
    final existing = store.loadFcmRuntimeConfig();

    final projectCtrl = TextEditingController(
      text: existing?.projectId ?? 'mandiri-transport-ops',
    );
    final senderCtrl = TextEditingController(
      text: existing?.messagingSenderId ?? '',
    );
    final appIdCtrl = TextEditingController(text: existing?.appId ?? '');
    final apiKeyCtrl = TextEditingController(text: existing?.apiKey ?? '');

    await showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Runtime FirebaseOptions (Opsional)'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Jika google-services.json sudah ditempatkan di mobile/android/app/, biarkan kosong. Formulir ini memungkinkan aktivasi FirebaseOptions secara runtime tanpa kompilasi ulang.',
                style: TextStyle(fontSize: 12.5, color: AppColors.textSecondary),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: projectCtrl,
                decoration: const InputDecoration(labelText: 'Project ID'),
              ),
              const SizedBox(height: 8),
              TextField(
                controller: senderCtrl,
                decoration: const InputDecoration(
                  labelText: 'Messaging Sender ID',
                ),
              ),
              const SizedBox(height: 8),
              TextField(
                controller: appIdCtrl,
                decoration: const InputDecoration(labelText: 'Firebase App ID'),
              ),
              const SizedBox(height: 8),
              TextField(
                controller: apiKeyCtrl,
                decoration: const InputDecoration(labelText: 'Firebase API Key'),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () async {
              Navigator.pop(ctx);
              final msg = await controller.saveRuntimeFirebaseConfig(null);
              if (context.mounted) {
                ScaffoldMessenger.of(
                  context,
                ).showSnackBar(SnackBar(content: Text(msg)));
              }
            },
            child: const Text('Reset ke google-services.json'),
          ),
          FilledButton(
            onPressed: () async {
              Navigator.pop(ctx);
              final cfg = FcmRuntimeConfig(
                apiKey: apiKeyCtrl.text.trim(),
                appId: appIdCtrl.text.trim(),
                messagingSenderId: senderCtrl.text.trim(),
                projectId: projectCtrl.text.trim(),
              );
              final msg = await controller.saveRuntimeFirebaseConfig(cfg);
              if (context.mounted) {
                ScaffoldMessenger.of(
                  context,
                ).showSnackBar(SnackBar(content: Text(msg)));
              }
            },
            child: const Text('Simpan & Inisialisasi'),
          ),
        ],
      ),
    );
  }
}

class _BiKpiCard extends StatelessWidget {
  final String label;
  final String value;
  final Color color;

  const _BiKpiCard({
    required this.label,
    required this.value,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              label,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                fontSize: 11.5,
                fontWeight: FontWeight.w700,
                color: AppColors.textSecondary,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              value,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                fontSize: 16.5,
                fontWeight: FontWeight.w900,
                color: color,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
