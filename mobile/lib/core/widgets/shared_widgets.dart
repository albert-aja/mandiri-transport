import 'package:flutter/material.dart';

import '../../data/models/models.dart';
import '../theme/app_theme.dart';
import '../utils/deep_links.dart';
import '../utils/formatters.dart';

class OperationalLabels {
  static String tripStatus(String status) => switch (status) {
    'UNASSIGNED' => 'Belum Ditugaskan',
    'ASSIGNED' => 'Ditugaskan',
    'ACCEPTED' => 'Tugas Diterima',
    'ON_THE_WAY' => 'Menuju Lokasi Jemput',
    'ARRIVED' => 'Tiba di Lokasi',
    'PASSENGER_ONBOARD' => 'Penumpang di Mobil',
    'COMPLETED' => 'Selesai',
    'CANCELLED' => 'Dibatalkan',
    'NO_SHOW' => 'Penumpang Tidak Hadir',
    _ => status,
  };

  static String tripType(String type) => switch (type) {
    'AIRPORT_PICKUP' => 'Jemput Bandara (KNO)',
    'AIRPORT_DROPOFF' => 'Antar Bandara (KNO)',
    'LOCAL' => 'Dalam Kota Medan',
    'INTERCITY' => 'Antar Kota (Sumut)',
    'HOURLY_CHARTER' => 'Charter Per Jam',
    'FULL_DAY_CHARTER' => 'Charter Harian',
    'ROUND_TRIP' => 'Pulang-Pergi (PP)',
    'MULTI_STOP_CHARTER' => 'Charter Multi-Titik',
    _ => type,
  };

  static String paymentStatus(String status) => switch (status) {
    'PAID' => 'Lunas',
    'PARTIALLY_PAID' => 'DP / Sebagian',
    'UNPAID' => 'Belum Bayar',
    'OVERPAID' => 'Lebih Bayar',
    'REFUNDED' => 'Refund',
    _ => status,
  };

  static String cashCustody(String status) => switch (status) {
    'HELD_BY_DRIVER' => 'Tunai di Supir',
    'PARTIALLY_HANDED_OVER' => 'Disetor Sebagian',
    'HANDED_OVER_CONFIRMED' => 'Setoran Diterima',
    'DISPUTED' => 'Selisih / Ditinjau',
    'NO_DRIVER_CASH_DUE' => 'Tidak Ada Kas Supir',
    _ => status,
  };

  static String driverAvailability(String status) => switch (status) {
    'AVAILABLE' => 'Siap Jalan',
    'ON_LEAVE' => 'Izin / Cuti',
    'UNAVAILABLE' => 'Tidak Aktif',
    'TEMPORARILY_UNAVAILABLE' => 'Istirahat Sementara',
    _ => status,
  };

  static String lostFoundStatus(String status) => switch (status) {
    'FOUND' => 'Baru Ditemukan',
    'CONTACTING_CUSTOMER' => 'Menghubungi Tamu',
    'RETURNED' => 'Sudah Dikembalikan',
    'STORED' => 'Disimpan di Garasi',
    'UNRESOLVED' => 'Belum Selesai',
    _ => status,
  };
}

class StatusBadge extends StatelessWidget {
  final String label;
  final Color fg;
  final Color bg;
  final IconData? icon;

  const StatusBadge({
    super.key,
    required this.label,
    required this.fg,
    required this.bg,
    this.icon,
  });

  factory StatusBadge.forTripStatus(String status) {
    final (fg, bg) = switch (status) {
      'UNASSIGNED' => (AppColors.crimson, AppColors.crimsonSoft),
      'ASSIGNED' => (AppColors.amber, AppColors.amberSoft),
      'ACCEPTED' ||
      'ON_THE_WAY' ||
      'ARRIVED' ||
      'PASSENGER_ONBOARD' => (AppColors.infoBlue, AppColors.infoSoft),
      'COMPLETED' => (AppColors.emerald, AppColors.emeraldSoft),
      _ => (AppColors.textSecondary, const Color(0xFFE2E8F0)),
    };
    return StatusBadge(
      label: OperationalLabels.tripStatus(status),
      fg: fg,
      bg: bg,
    );
  }

  factory StatusBadge.forCustody(String status) {
    final (fg, bg) = switch (status) {
      'HELD_BY_DRIVER' ||
      'PARTIALLY_HANDED_OVER' => (AppColors.amber, AppColors.amberSoft),
      'DISPUTED' => (AppColors.crimson, AppColors.crimsonSoft),
      'HANDED_OVER_CONFIRMED' => (AppColors.emerald, AppColors.emeraldSoft),
      _ => (AppColors.textSecondary, const Color(0xFFE2E8F0)),
    };
    return StatusBadge(
      label: OperationalLabels.cashCustody(status),
      fg: fg,
      bg: bg,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: fg.withValues(alpha: 0.25)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, size: 14, color: fg),
            const SizedBox(width: 4),
          ],
          Flexible(
            child: Text(
              label,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                color: fg,
                fontSize: 12,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class AppDropdownField<T> extends StatelessWidget {
  final String label;
  final T? value;
  final List<DropdownMenuItem<T>> items;
  final ValueChanged<T?> onChanged;

  const AppDropdownField({
    super.key,
    required this.label,
    required this.value,
    required this.items,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final validValues = items.map((e) => e.value).toSet();
    final safeValue = validValues.contains(value) ? value : null;
    return InputDecorator(
      decoration: InputDecoration(
        labelText: label,
        contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 4),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<T>(
          value: safeValue,
          isExpanded: true,
          items: items,
          onChanged: onChanged,
        ),
      ),
    );
  }
}

class RupiahQuickChips extends StatelessWidget {
  final double? referenceFare;
  final TextEditingController controller;
  final VoidCallback? onChanged;

  const RupiahQuickChips({
    super.key,
    required this.controller,
    this.referenceFare,
    this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final presets = DenominationPresets.buildOptions(
      referenceFare: referenceFare,
    );
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: presets.map((opt) {
        return ActionChip(
          backgroundColor: AppColors.emeraldSoft,
          side: BorderSide(color: AppColors.emerald.withValues(alpha: 0.35)),
          label: Text(
            opt.label,
            style: const TextStyle(
              color: AppColors.emerald,
              fontWeight: FontWeight.w700,
              fontSize: 12.5,
            ),
          ),
          onPressed: () {
            final current = AppFormatters.parseRupiah(controller.text);
            final next = opt.computeNewAmount(current, referenceFare ?? 0);
            controller.text = AppFormatters.thousandNumber(next);
            onChanged?.call();
          },
        );
      }).toList(),
    );
  }
}

Future<void> showWhatsappTemplatePickerSheet({
  required BuildContext context,
  required List<WhatsappTemplate> templates,
  required String recipientPhone,
  required String customerName,
  Trip? trip,
}) async {
  await showModalBottomSheet<void>(
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
              Row(
                children: [
                  const Icon(Icons.chat_rounded, color: AppColors.emerald),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      'Kirim Pesan WhatsApp ke $customerName',
                      style: const TextStyle(
                        fontSize: 17,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ),
                  IconButton(
                    onPressed: () => Navigator.pop(ctx),
                    icon: const Icon(Icons.close),
                  ),
                ],
              ),
              const SizedBox(height: 4),
              Text(
                'Pilih template pesan. Aplikasi hanya membuka draf WhatsApp (${recipientPhone.isEmpty ? "-" : recipientPhone}) dan tidak pernah mengirim otomatis.',
                style: const TextStyle(
                  fontSize: 13,
                  color: AppColors.textSecondary,
                ),
              ),
              const SizedBox(height: 14),
              Flexible(
                child: ListView.separated(
                  shrinkWrap: true,
                  itemCount: templates.length,
                  separatorBuilder: (_, _) => const SizedBox(height: 10),
                  itemBuilder: (_, index) {
                    final tpl = templates[index];
                    final rendered = DeepLinks.renderWhatsappTemplate(
                      tpl.bodyTemplate,
                      {
                        'customer_name': customerName,
                        'pickup_time': trip != null
                            ? AppFormatters.dateTimeWib(trip.scheduledPickupAt)
                            : 'sesuai jadwal',
                        'pickup_address': trip?.pickupAddress ?? 'titik jemput',
                        'destination_address':
                            trip?.destinationAddress ?? 'tujuan',
                        'agreed_fare': trip != null
                            ? AppFormatters.rupiah(trip.agreedFareSnapshot)
                            : '-',
                        'driver_name': trip?.driverName ?? 'Supir Mandiri',
                        'driver_phone': trip?.driverPhone ?? '-',
                        'vehicle_model': trip?.vehicleModel ?? 'Unit Mandiri',
                        'vehicle_plate': trip?.vehiclePlate ?? '-',
                      },
                    );
                    final waUrl = DeepLinks.buildWhatsappUrl(
                      phone: recipientPhone,
                      message: rendered,
                    );
                    return Card(
                      child: Padding(
                        padding: const EdgeInsets.all(14),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              tpl.title,
                              style: const TextStyle(
                                fontWeight: FontWeight.w700,
                                fontSize: 14.5,
                              ),
                            ),
                            const SizedBox(height: 6),
                            Text(
                              rendered,
                              style: const TextStyle(
                                fontSize: 13,
                                color: AppColors.textSecondary,
                              ),
                            ),
                            const SizedBox(height: 10),
                            SizedBox(
                              width: double.infinity,
                              child: FilledButton.icon(
                                style: FilledButton.styleFrom(
                                  backgroundColor: AppColors.emerald,
                                ),
                                onPressed: () async {
                                  Navigator.pop(ctx);
                                  await DeepLinks.launchExternal(waUrl);
                                  if (context.mounted) {
                                    ScaffoldMessenger.of(context).showSnackBar(
                                      SnackBar(
                                        content: Text(
                                          'Membuka WhatsApp: ${tpl.title}',
                                        ),
                                      ),
                                    );
                                  }
                                },
                                icon: const Icon(Icons.open_in_new, size: 18),
                                label: const Text('Buka Chat WhatsApp'),
                              ),
                            ),
                          ],
                        ),
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

class LivePushBanner extends StatelessWidget {
  final LiveFcmState fcmState;
  final VoidCallback onDismiss;
  final VoidCallback? onOpen;

  const LivePushBanner({
    super.key,
    required this.fcmState,
    required this.onDismiss,
    this.onOpen,
  });

  @override
  Widget build(BuildContext context) {
    if (!fcmState.hasLiveBanner) return const SizedBox.shrink();
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: AppColors.infoSoft,
        border: Border(
          bottom: BorderSide(color: AppColors.infoBlue.withValues(alpha: 0.35)),
        ),
      ),
      child: Row(
        children: [
          const Icon(
            Icons.notifications_active_rounded,
            color: AppColors.infoBlue,
            size: 20,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  fcmState.liveBannerTitle ?? 'Notifikasi Baru',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontWeight: FontWeight.w800,
                    fontSize: 13,
                    color: AppColors.navy,
                  ),
                ),
                if (fcmState.liveBannerBody != null &&
                    fcmState.liveBannerBody!.isNotEmpty)
                  Text(
                    fcmState.liveBannerBody!,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 12,
                      color: AppColors.textSecondary,
                    ),
                  ),
              ],
            ),
          ),
          if (onOpen != null)
            TextButton(
              onPressed: onOpen,
              child: const Text('Lihat'),
            ),
          IconButton(
            tooltip: 'Tutup Banner',
            visualDensity: VisualDensity.compact,
            onPressed: onDismiss,
            icon: const Icon(Icons.close_rounded, size: 18),
          ),
        ],
      ),
    );
  }
}

