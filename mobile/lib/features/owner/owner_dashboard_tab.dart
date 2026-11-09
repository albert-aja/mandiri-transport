import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_theme.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/shared_widgets.dart';
import '../../data/state/app_controller.dart';
import 'dispatch_simulator_sheet.dart';
import 'quick_booking_sheet.dart';

class OwnerDashboardTab extends ConsumerWidget {
  const OwnerDashboardTab({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(appControllerProvider);
    final controller = ref.read(appControllerProvider.notifier);

    final activeOrScheduled = state.trips
        .where(
          (t) => !const {'COMPLETED', 'CANCELLED', 'NO_SHOW'}.contains(t.status),
        )
        .toList();
    final unassignedTrips = state.trips.where((t) => t.isUnassigned).toList();
    final unreadNotifications = state.notifications
        .where((n) => !n.acknowledged)
        .toList();
    final anomalies = controller.computeBiExplorer().anomalyAlerts;

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
      children: [
        // Top Summary Strip
        GridView.count(
          crossAxisCount: 2,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          mainAxisSpacing: 10,
          crossAxisSpacing: 10,
          childAspectRatio: 1.55,
          children: [
            _SummaryTile(
              title: 'Trip Aktif & Terjadwal',
              value: '${activeOrScheduled.length} Trip',
              subtitle: 'Pantau pelaksanaan jalan',
              accent: AppColors.navy,
              icon: Icons.event_note_rounded,
              onTap: () => controller.setOwnerTab(1),
            ),
            _SummaryTile(
              title: 'Belum Ditugaskan',
              value: '${unassignedTrips.length} Trip',
              subtitle: unassignedTrips.isEmpty
                  ? 'Semua sudah ada supir'
                  : 'Perlu pilih supir & mobil',
              accent: unassignedTrips.isEmpty
                  ? AppColors.emerald
                  : AppColors.crimson,
              icon: Icons.person_search_rounded,
              onTap: () => controller.setOwnerTab(1),
            ),
          ],
        ),
        const SizedBox(height: 10),
        // Prominent Uang Tunai di Supir Card
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: AppColors.amberSoft,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Icon(
                    Icons.account_balance_wallet_rounded,
                    color: AppColors.amber,
                    size: 28,
                  ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Uang Tunai di Supir',
                        style: TextStyle(
                          fontSize: 13.5,
                          fontWeight: FontWeight.w700,
                          color: AppColors.textSecondary,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        AppFormatters.rupiah(state.totalDriverHeldCash),
                        style: const TextStyle(
                          fontSize: 22,
                          fontWeight: FontWeight.w900,
                          color: AppColors.textPrimary,
                        ),
                      ),
                      const Text(
                        'Pembayaran tunai tamu wajib disetor ke pemilik',
                        style: TextStyle(
                          fontSize: 12,
                          color: AppColors.textSecondary,
                        ),
                      ),
                    ],
                  ),
                ),
                FilledButton(
                  style: FilledButton.styleFrom(
                    backgroundColor: AppColors.amber,
                    minimumSize: const Size(88, 48),
                  ),
                  onPressed: () =>
                      controller.setOwnerTab(3, financeSubTab: 0),
                  child: const Text('Kelola Setoran'),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),

        // Quick Action Row
        FilledButton.icon(
          style: FilledButton.styleFrom(backgroundColor: AppColors.emerald),
          onPressed: () => showQuickBookingSheet(context),
          icon: const Icon(Icons.flash_on_rounded),
          label: const Text('+ Booking Cepat (30 Detik)'),
        ),

        // Sync Conflict Review Banner (if any offline conflicts staged)
        if (state.unresolvedConflicts.isNotEmpty) ...[
          const SizedBox(height: 14),
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: AppColors.crimsonSoft,
              borderRadius: BorderRadius.circular(14),
              border: Border.all(
                color: AppColors.crimson.withValues(alpha: 0.4),
              ),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Icon(Icons.sync_problem, color: AppColors.crimson),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        '${state.unresolvedConflicts.length} Konflik Sinkronisasi Offline Perlu Ditinjau',
                        style: const TextStyle(
                          fontWeight: FontWeight.w800,
                          color: AppColors.crimson,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 6),
                const Text(
                  'Catatan pembayaran & pengeluaran lokal TIDAK pernah dihapus otomatis.',
                  style: TextStyle(fontSize: 12.5),
                ),
                const SizedBox(height: 8),
                ...state.unresolvedConflicts.map((conf) {
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: Row(
                      children: [
                        Expanded(
                          child: Text(
                            '• [${conf.mutationType}] ${conf.summaryMessage}',
                            style: const TextStyle(
                              fontSize: 12.5,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                        TextButton(
                          onPressed: () => controller.resolveConflict(conf.id),
                          child: const Text('Selesaikan'),
                        ),
                      ],
                    ),
                  );
                }),
              ],
            ),
          ),
        ],

        // Immediate Unassigned Trip Dispatch Prompt
        if (unassignedTrips.isNotEmpty) ...[
          const SizedBox(height: 18),
          const Text(
            'Segera Tugaskan Supir & Unit',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 8),
          ...unassignedTrips.map((t) {
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
                          StatusBadge.forTripStatus(t.status),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              '${t.tripCode} • ${AppFormatters.dateTimeWib(t.scheduledPickupAt)}',
                              style: const TextStyle(
                                fontWeight: FontWeight.w700,
                                fontSize: 13,
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Text(
                        '${t.customerName} (${t.passengerCount} org, ${t.luggageCount} koper)',
                        style: const TextStyle(
                          fontWeight: FontWeight.w800,
                          fontSize: 15,
                        ),
                      ),
                      Text(
                        '${t.pickupAddress} -> ${t.destinationAddress}',
                        style: const TextStyle(
                          fontSize: 13,
                          color: AppColors.textSecondary,
                        ),
                      ),
                      const SizedBox(height: 10),
                      SizedBox(
                        width: double.infinity,
                        child: OutlinedButton.icon(
                          onPressed: () =>
                              showDispatchSimulatorSheet(context, t),
                          icon: const Icon(Icons.radar_rounded),
                          label: const Text(
                            'Pilih Supir & Mobil (Simulasi Bentrok)',
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            );
          }),
        ],

        // Active Operational Anomaly Alerts
        const SizedBox(height: 18),
        Row(
          children: [
            const Icon(Icons.insights_rounded, color: AppColors.amber, size: 20),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                'Peringatan Anomali Operasional (${anomalies.length})',
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
        ...anomalies.map((a) {
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
                        const StatusBadge(
                          label: 'Tinjauan Wajar',
                          fg: AppColors.amber,
                          bg: AppColors.amberSoft,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            a.title,
                            style: const TextStyle(
                              fontWeight: FontWeight.w800,
                              fontSize: 14.5,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(
                      a.subjectLabel,
                      style: const TextStyle(
                        fontWeight: FontWeight.w700,
                        fontSize: 13,
                        color: AppColors.navy,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Wrap(
                      spacing: 12,
                      runSpacing: 4,
                      children: [
                        Text(
                          'Teramati: ${a.observedDisplay}',
                          style: const TextStyle(
                            fontSize: 12.5,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        Text(
                          'Normal: ${a.baselineDisplay}',
                          style: const TextStyle(
                            fontSize: 12.5,
                            color: AppColors.textSecondary,
                          ),
                        ),
                        Text(
                          'Batas: ${a.thresholdDisplay}',
                          style: const TextStyle(
                            fontSize: 12.5,
                            color: AppColors.textSecondary,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 6),
                    Text(
                      'Saran: ${a.reviewHint}',
                      style: const TextStyle(
                        fontSize: 12.5,
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          );
        }),

        // Notifications & Document Reminders
        const SizedBox(height: 18),
        Text(
          'Notifikasi & Pengingat Dokumen (${unreadNotifications.length} Aktif)',
          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 8),
        ...state.notifications.map((n) {
          return Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Card(
              child: ListTile(
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: 14,
                  vertical: 6,
                ),
                leading: Icon(
                  n.acknowledged
                      ? Icons.notifications_none_rounded
                      : Icons.notifications_active_rounded,
                  color: n.acknowledged
                      ? AppColors.textSecondary
                      : AppColors.navy,
                ),
                title: Text(
                  n.title,
                  style: TextStyle(
                    fontWeight: n.acknowledged
                        ? FontWeight.w600
                        : FontWeight.w800,
                    fontSize: 14,
                  ),
                ),
                subtitle: Text(
                  n.message,
                  style: const TextStyle(fontSize: 12.5),
                ),
                trailing: n.acknowledged
                    ? const Text(
                        'Dibaca',
                        style: TextStyle(
                          fontSize: 12,
                          color: AppColors.textSecondary,
                        ),
                      )
                    : TextButton(
                        onPressed: () =>
                            controller.acknowledgeNotification(n.id),
                        child: const Text('OK'),
                      ),
              ),
            ),
          );
        }),
      ],
    );
  }
}

class _SummaryTile extends StatelessWidget {
  final String title;
  final String value;
  final String subtitle;
  final Color accent;
  final IconData icon;
  final VoidCallback onTap;

  const _SummaryTile({
    required this.title,
    required this.value,
    required this.subtitle,
    required this.accent,
    required this.icon,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        borderRadius: BorderRadius.circular(14),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  Icon(icon, color: accent, size: 18),
                  const SizedBox(width: 6),
                  Expanded(
                    child: Text(
                      title,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w700,
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ),
                ],
              ),
              Text(
                value,
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w900,
                  color: accent,
                ),
              ),
              Text(
                subtitle,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  fontSize: 11.5,
                  color: AppColors.textSecondary,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

