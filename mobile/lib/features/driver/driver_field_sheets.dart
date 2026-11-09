import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_theme.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/shared_widgets.dart';
import '../../data/models/models.dart';
import '../../data/state/app_controller.dart';

Future<void> showDriverInspectionSheet(
  BuildContext context,
  WidgetRef ref,
  Trip trip,
) async {
  final selectedIssues = <String>{};
  final notesCtrl = TextEditingController();

  await showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.white,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(22)),
    ),
    builder: (ctx) {
      return StatefulBuilder(
        builder: (ctx, setModal) {
          return Padding(
            padding: EdgeInsets.fromLTRB(
              20,
              20,
              20,
              MediaQuery.of(ctx).viewInsets.bottom + 24,
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Inspeksi Cepat Kendaraan • ${trip.vehiclePlate ?? ""}',
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 6),
                const Text(
                  'Opsional & cepat. Ketuk "Semua Aman" bila seluruh kondisi prima.',
                  style: TextStyle(
                    fontSize: 13,
                    color: AppColors.textSecondary,
                  ),
                ),
                const SizedBox(height: 14),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    style: FilledButton.styleFrom(
                      backgroundColor: AppColors.emerald,
                      minimumSize: const Size.fromHeight(56),
                    ),
                    onPressed: () async {
                      await ref
                          .read(appControllerProvider.notifier)
                          .recordVehicleInspection(
                            tripId: trip.id,
                            allPassed: true,
                          );
                      if (ctx.mounted) Navigator.pop(ctx);
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text(
                              'Inspeksi kendaraan tercatat: Semua Aman.',
                            ),
                          ),
                        );
                      }
                    },
                    icon: const Icon(Icons.verified_rounded),
                    label: const Text('Semua Aman (1-Tap Siap Jalan)'),
                  ),
                ),
                const Divider(height: 26),
                const Text(
                  'Atau tandai bagian yang perlu perhatian:',
                  style: TextStyle(fontWeight: FontWeight.w700, fontSize: 13.5),
                ),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: const [
                    'Ban',
                    'Rem',
                    'AC',
                    'Lampu',
                    'Kebersihan',
                  ].map((item) {
                    final active = selectedIssues.contains(item);
                    return FilterChip(
                      label: Text(item),
                      selected: active,
                      onSelected: (val) {
                        setModal(() {
                          if (val) {
                            selectedIssues.add(item);
                          } else {
                            selectedIssues.remove(item);
                          }
                        });
                      },
                    );
                  }).toList(),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: notesCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Catatan Kondisi (Opsional)',
                    hintText: 'Contoh: Tekanan ban kiri belakang perlu ditambah',
                  ),
                ),
                const SizedBox(height: 14),
                if (selectedIssues.isNotEmpty ||
                    notesCtrl.text.trim().isNotEmpty)
                  SizedBox(
                    width: double.infinity,
                    child: OutlinedButton.icon(
                      onPressed: () async {
                        await ref
                            .read(appControllerProvider.notifier)
                            .recordVehicleInspection(
                              tripId: trip.id,
                              allPassed: selectedIssues.isEmpty,
                              flaggedItems: selectedIssues.toList(),
                              notes: notesCtrl.text.trim(),
                            );
                        if (ctx.mounted) Navigator.pop(ctx);
                      },
                      icon: const Icon(Icons.report_problem_outlined),
                      label: const Text('Simpan Temuan Inspeksi'),
                    ),
                  ),
              ],
            ),
          );
        },
      );
    },
  );
}

Future<void> showDriverPaymentSheet(
  BuildContext context,
  WidgetRef ref,
  Trip trip,
) async {
  final defaultAmount = trip.remainingUnpaidAmount > 0
      ? trip.remainingUnpaidAmount
      : trip.agreedFareSnapshot;
  final amtCtrl = TextEditingController(
    text: AppFormatters.thousandNumber(defaultAmount),
  );
  final notesCtrl = TextEditingController();
  String method = 'CASH';

  await showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.white,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(22)),
    ),
    builder: (ctx) {
      return StatefulBuilder(
        builder: (ctx, setModal) {
          return Padding(
            padding: EdgeInsets.fromLTRB(
              20,
              20,
              20,
              MediaQuery.of(ctx).viewInsets.bottom + 24,
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Terima Pembayaran Tamu • ${trip.tripCode}',
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 12),
                SegmentedButton<String>(
                  segments: const [
                    ButtonSegment(
                      value: 'CASH',
                      icon: Icon(Icons.payments_rounded),
                      label: Text('Tunai (Cash)'),
                    ),
                    ButtonSegment(
                      value: 'BANK_TRANSFER',
                      icon: Icon(Icons.account_balance_rounded),
                      label: Text('Transfer Rekening'),
                    ),
                  ],
                  selected: {method},
                  onSelectionChanged: (s) => setModal(() => method = s.first),
                ),
                const SizedBox(height: 12),
                if (method == 'CASH')
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: AppColors.amberSoft,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: AppColors.amber.withValues(alpha: 0.4),
                      ),
                    ),
                    child: const Row(
                      children: [
                        Icon(
                          Icons.account_balance_wallet_rounded,
                          color: AppColors.amber,
                        ),
                        SizedBox(width: 10),
                        Expanded(
                          child: Text(
                            'Tunai dipegang supir — wajib disetor ke pemilik',
                            style: TextStyle(
                              fontWeight: FontWeight.w800,
                              color: AppColors.textPrimary,
                              fontSize: 13.5,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                const SizedBox(height: 12),
                TextField(
                  controller: amtCtrl,
                  keyboardType: TextInputType.number,
                  inputFormatters: [RupiahInputFormatter()],
                  decoration: const InputDecoration(
                    labelText: 'Nominal Pembayaran Diterima (Rp)',
                    prefixText: 'Rp ',
                  ),
                ),
                const SizedBox(height: 8),
                RupiahQuickChips(
                  controller: amtCtrl,
                  referenceFare: defaultAmount,
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: notesCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Catatan Pembayaran (Opsional)',
                    hintText: 'Contoh: Dibayar uang pas di bandara',
                  ),
                ),
                const SizedBox(height: 16),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    style: FilledButton.styleFrom(
                      backgroundColor: AppColors.emerald,
                    ),
                    onPressed: () async {
                      final amt = AppFormatters.parseRupiah(amtCtrl.text);
                      if (amt <= 0) return;
                      await ref
                          .read(appControllerProvider.notifier)
                          .recordTripPayment(
                            tripId: trip.id,
                            amount: amt,
                            paymentMethod: method,
                            notes: notesCtrl.text.trim(),
                          );
                      if (ctx.mounted) Navigator.pop(ctx);
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: Text(
                              method == 'CASH'
                                  ? 'Pembayaran tunai ${AppFormatters.rupiah(amt)} tercatat di kas supir.'
                                  : 'Pembayaran transfer ${AppFormatters.rupiah(amt)} tercatat.',
                            ),
                          ),
                        );
                      }
                    },
                    icon: const Icon(Icons.save_rounded),
                    label: const Text('Simpan Pembayaran'),
                  ),
                ),
              ],
            ),
          );
        },
      );
    },
  );
}

Future<void> showDriverExpenseSheet(
  BuildContext context,
  WidgetRef ref,
  Trip trip,
) async {
  String category = 'FUEL';
  final amtCtrl = TextEditingController(text: '50.000');
  final literCtrl = TextEditingController(text: '5.0');
  final notesCtrl = TextEditingController();

  await showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.white,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(22)),
    ),
    builder: (ctx) {
      return StatefulBuilder(
        builder: (ctx, setModal) {
          return Padding(
            padding: EdgeInsets.fromLTRB(
              20,
              20,
              20,
              MediaQuery.of(ctx).viewInsets.bottom + 24,
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Catat Pengeluaran Jalan • ${trip.tripCode}',
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 12),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: const [
                    ('FUEL', 'BBM'),
                    ('TOLL', 'Tol'),
                    ('PARKING', 'Parkir'),
                    ('DRIVER_MEAL', 'Makan'),
                  ].map((e) {
                    return ChoiceChip(
                      label: Text(e.$2),
                      selected: category == e.$1,
                      onSelected: (_) => setModal(() => category = e.$1),
                    );
                  }).toList(),
                ),
                const SizedBox(height: 12),
                if (category == 'FUEL') ...[
                  TextField(
                    controller: literCtrl,
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                    decoration: const InputDecoration(
                      labelText: 'Jumlah Liter BBM (Opsional)',
                      suffixText: 'Liter',
                    ),
                  ),
                  const SizedBox(height: 10),
                ],
                TextField(
                  controller: amtCtrl,
                  keyboardType: TextInputType.number,
                  inputFormatters: [RupiahInputFormatter()],
                  decoration: const InputDecoration(
                    labelText: 'Nominal Pengeluaran (Rp)',
                    prefixText: 'Rp ',
                  ),
                ),
                const SizedBox(height: 8),
                RupiahQuickChips(controller: amtCtrl),
                const SizedBox(height: 10),
                TextField(
                  controller: notesCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Keterangan (SPBU / Gerbang Tol)',
                  ),
                ),
                const SizedBox(height: 16),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton(
                    onPressed: () async {
                      final amt = AppFormatters.parseRupiah(amtCtrl.text);
                      if (amt <= 0) return;
                      await ref
                          .read(appControllerProvider.notifier)
                          .recordTripExpense(
                            tripId: trip.id,
                            category: category,
                            amount: amt,
                            fuelLiters: category == 'FUEL'
                                ? double.tryParse(literCtrl.text)
                                : null,
                            notes: notesCtrl.text.trim(),
                          );
                      if (ctx.mounted) Navigator.pop(ctx);
                    },
                    child: const Text('Simpan Pengeluaran'),
                  ),
                ),
              ],
            ),
          );
        },
      );
    },
  );
}

Future<void> showDriverLostFoundSheet(
  BuildContext context,
  WidgetRef ref,
  Trip trip,
) async {
  final descCtrl = TextEditingController();
  final notesCtrl = TextEditingController();

  await showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.white,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(22)),
    ),
    builder: (ctx) {
      return Padding(
        padding: EdgeInsets.fromLTRB(
          20,
          20,
          20,
          MediaQuery.of(ctx).viewInsets.bottom + 24,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Lapor Barang Tertinggal • ${trip.tripCode}',
              style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: descCtrl,
              decoration: const InputDecoration(
                labelText: 'Deskripsi Barang yang Ditemukan',
                hintText: 'Contoh: Dompet coklat / charger HP di jok tengah',
              ),
            ),
            const SizedBox(height: 10),
            TextField(
              controller: notesCtrl,
              decoration: const InputDecoration(
                labelText: 'Tindakan / Lokasi Penyimpanan',
                hintText: 'Contoh: Diamankan supir untuk diserahkan ke kantor',
              ),
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: () async {
                  if (descCtrl.text.trim().isEmpty) return;
                  await ref
                      .read(appControllerProvider.notifier)
                      .recordLostAndFound(
                        tripId: trip.id,
                        itemDescription: descCtrl.text.trim(),
                        notes: notesCtrl.text.trim(),
                      );
                  if (ctx.mounted) Navigator.pop(ctx);
                },
                child: const Text('Simpan Laporan Barang'),
              ),
            ),
          ],
        ),
      );
    },
  );
}

Future<void> showDriverOdometerSheet(
  BuildContext context,
  WidgetRef ref,
  Trip trip,
) async {
  final startCtrl = TextEditingController(
    text: trip.odometerStartKm?.toStringAsFixed(0) ?? '62100',
  );
  final endCtrl = TextEditingController(
    text: trip.odometerEndKm?.toStringAsFixed(0) ?? '62140',
  );

  await showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.white,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(22)),
    ),
    builder: (ctx) {
      return Padding(
        padding: EdgeInsets.fromLTRB(
          20,
          20,
          20,
          MediaQuery.of(ctx).viewInsets.bottom + 24,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Catat Odometer Awal & Akhir (Opsional)',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: startCtrl,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                labelText: 'Odometer Awal (km)',
              ),
            ),
            const SizedBox(height: 10),
            TextField(
              controller: endCtrl,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                labelText: 'Odometer Akhir (km)',
              ),
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: () async {
                  await ref
                      .read(appControllerProvider.notifier)
                      .advanceTripStatus(
                        trip: trip,
                        newStatus: trip.status,
                        odometerStartKm: double.tryParse(startCtrl.text),
                        odometerEndKm: double.tryParse(endCtrl.text),
                      );
                  if (ctx.mounted) Navigator.pop(ctx);
                },
                child: const Text('Simpan Odometer'),
              ),
            ),
          ],
        ),
      );
    },
  );
}
