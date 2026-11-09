import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_theme.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/shared_widgets.dart';
import '../../data/models/models.dart';
import '../../data/state/app_controller.dart';

class OwnerFinanceFleetTab extends ConsumerWidget {
  const OwnerFinanceFleetTab({super.key});

  void _showConfirmHandoverSheet(
    BuildContext context,
    WidgetRef ref,
    Driver driver,
  ) {
    final amtCtrl = TextEditingController(
      text: AppFormatters.thousandNumber(driver.cashHeldAmount),
    );
    final notesCtrl = TextEditingController();
    bool markDisputed = false;

    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
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
                    'Konfirmasi Setoran Tunai • ${driver.fullName}',
                    style: const TextStyle(
                      fontSize: 17.5,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    'Total Kas Belum Disetor: ${AppFormatters.rupiah(driver.cashHeldAmount)}',
                    style: const TextStyle(
                      fontWeight: FontWeight.w700,
                      color: AppColors.amber,
                    ),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: amtCtrl,
                    keyboardType: TextInputType.number,
                    inputFormatters: [RupiahInputFormatter()],
                    decoration: const InputDecoration(
                      labelText: 'Nominal Diterima Pemilik (Rp)',
                      prefixText: 'Rp ',
                    ),
                  ),
                  const SizedBox(height: 8),
                  RupiahQuickChips(
                    controller: amtCtrl,
                    referenceFare: driver.cashHeldAmount,
                  ),
                  const SizedBox(height: 10),
                  CheckboxListTile(
                    contentPadding: EdgeInsets.zero,
                    value: markDisputed,
                    onChanged: (v) =>
                        setModal(() => markDisputed = v ?? false),
                    title: const Text(
                      'Tandai Selisih / Sengketa Kas (DISPUTED)',
                      style: TextStyle(
                        fontSize: 13.5,
                        fontWeight: FontWeight.w700,
                        color: AppColors.crimson,
                      ),
                    ),
                  ),
                  TextField(
                    controller: notesCtrl,
                    decoration: const InputDecoration(
                      labelText: 'Catatan Penerimaan / Selisih',
                      hintText: 'Contoh: Diterima tunai di garasi Medan',
                    ),
                  ),
                  const SizedBox(height: 16),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton.icon(
                      style: FilledButton.styleFrom(
                        backgroundColor: markDisputed
                            ? AppColors.crimson
                            : AppColors.emerald,
                      ),
                      onPressed: () async {
                        final amt = AppFormatters.parseRupiah(amtCtrl.text);
                        await ref
                            .read(appControllerProvider.notifier)
                            .confirmOwnerCashHandover(
                              driverId: driver.id,
                              confirmedAmount: amt,
                              ownerNotes: notesCtrl.text.trim(),
                              markDisputed: markDisputed,
                            );
                        if (ctx.mounted) Navigator.pop(ctx);
                      },
                      icon: const Icon(Icons.check_circle_outline),
                      label: Text(
                        markDisputed
                            ? 'Simpan Status Selisih'
                            : 'Konfirmasi Setoran Diterima',
                      ),
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

  void _showRecordOwnerPaymentSheet(
    BuildContext context,
    WidgetRef ref,
    Trip trip,
  ) {
    final amtCtrl = TextEditingController(
      text: AppFormatters.thousandNumber(trip.remainingUnpaidAmount),
    );
    final refCtrl = TextEditingController();
    String method = 'BANK_TRANSFER';

    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
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
                    'Catat Pembayaran • ${trip.tripCode}',
                    style: const TextStyle(
                      fontSize: 17.5,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 12),
                  AppDropdownField<String>(
                    label: 'Metode Pembayaran Manual',
                    value: method,
                    items: const [
                      DropdownMenuItem(
                        value: 'BANK_TRANSFER',
                        child: Text('Transfer Bank ke Rekening Pemilik'),
                      ),
                      DropdownMenuItem(
                        value: 'CASH',
                        child: Text('Tunai Diterima Langsung Pemilik'),
                      ),
                      DropdownMenuItem(
                        value: 'OTHER',
                        child: Text('Lainnya / Invoice Korporat'),
                      ),
                    ],
                    onChanged: (v) =>
                        setModal(() => method = v ?? 'BANK_TRANSFER'),
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: amtCtrl,
                    keyboardType: TextInputType.number,
                    inputFormatters: [RupiahInputFormatter()],
                    decoration: const InputDecoration(
                      labelText: 'Nominal Pembayaran (Rp)',
                      prefixText: 'Rp ',
                    ),
                  ),
                  const SizedBox(height: 8),
                  RupiahQuickChips(
                    controller: amtCtrl,
                    referenceFare: trip.remainingUnpaidAmount,
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: refCtrl,
                    decoration: const InputDecoration(
                      labelText: 'Nomor Referensi / Catatan Bukti',
                      hintText: 'Contoh: TRF-BCA-1109',
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
                            .recordTripPayment(
                              tripId: trip.id,
                              amount: amt,
                              paymentMethod: method,
                              referenceNumber: refCtrl.text.trim(),
                            );
                        if (ctx.mounted) Navigator.pop(ctx);
                      },
                      child: const Text('Simpan Pembayaran'),
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

  void _showNewFormulaVersionSheet(BuildContext context, WidgetRef ref) {
    final nameCtrl = TextEditingController(
      text: 'Formula Biaya Operasional 2027',
    );
    final dateCtrl = TextEditingController(text: '2026-12-01');
    final fuelCtrl = TextEditingController(text: '1.400');
    final maintCtrl = TextEditingController(text: '275');
    final deprCtrl = TextEditingController(text: '1.850.000');
    final tireCostCtrl = TextEditingController(text: '4.600.000');
    final tireKmCtrl = TextEditingController(text: '55000');
    final commCtrl = TextEditingController(text: '20');

    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) {
        return Padding(
          padding: EdgeInsets.fromLTRB(
            20,
            20,
            20,
            MediaQuery.of(ctx).viewInsets.bottom + 24,
          ),
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Buat Versi Baru Formula Biaya Operasional',
                  style: TextStyle(fontSize: 17.5, fontWeight: FontWeight.w800),
                ),
                const SizedBox(height: 4),
                const Text(
                  'Snapshot biaya perjalanan lama tetap terkunci pada versi formula sebelumnya.',
                  style: TextStyle(
                    fontSize: 12.5,
                    color: AppColors.textSecondary,
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: nameCtrl,
                  decoration: const InputDecoration(labelText: 'Nama Versi'),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: dateCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Berlaku Mulai Tanggal (YYYY-MM-DD)',
                  ),
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: fuelCtrl,
                        keyboardType: TextInputType.number,
                        inputFormatters: [RupiahInputFormatter()],
                        decoration: const InputDecoration(
                          labelText: 'Estimasi BBM/km',
                          prefixText: 'Rp ',
                        ),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: TextField(
                        controller: maintCtrl,
                        keyboardType: TextInputType.number,
                        inputFormatters: [RupiahInputFormatter()],
                        decoration: const InputDecoration(
                          labelText: 'Servis/km',
                          prefixText: 'Rp ',
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: tireCostCtrl,
                        keyboardType: TextInputType.number,
                        inputFormatters: [RupiahInputFormatter()],
                        decoration: const InputDecoration(
                          labelText: 'Harga 4 Ban',
                          prefixText: 'Rp ',
                        ),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: TextField(
                        controller: tireKmCtrl,
                        keyboardType: TextInputType.number,
                        decoration: const InputDecoration(
                          labelText: 'Umur Pakai Ban (km)',
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: deprCtrl,
                        keyboardType: TextInputType.number,
                        inputFormatters: [RupiahInputFormatter()],
                        decoration: const InputDecoration(
                          labelText: 'Penyusutan / Bln',
                          prefixText: 'Rp ',
                        ),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: TextField(
                        controller: commCtrl,
                        keyboardType: TextInputType.number,
                        decoration: const InputDecoration(
                          labelText: 'Komisi Supir (%)',
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton(
                    onPressed: () async {
                      await ref
                          .read(appControllerProvider.notifier)
                          .createCostFormulaVersion(
                            name: nameCtrl.text.trim(),
                            effectiveFrom: dateCtrl.text.trim(),
                            fuelPerKm: AppFormatters.parseRupiah(fuelCtrl.text),
                            maintenanceReservePerKm: AppFormatters.parseRupiah(
                              maintCtrl.text,
                            ),
                            monthlyDepreciation: AppFormatters.parseRupiah(
                              deprCtrl.text,
                            ),
                            driverCommissionRate:
                                (double.tryParse(commCtrl.text) ?? 20) / 100.0,
                            tireReplacementCost: AppFormatters.parseRupiah(
                              tireCostCtrl.text,
                            ),
                            tireLifespanKm:
                                double.tryParse(tireKmCtrl.text) ?? 55000,
                          );
                      if (ctx.mounted) Navigator.pop(ctx);
                    },
                    child: const Text('Aktifkan Versi Formula Baru'),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  void _showAddMaintenanceSheet(
    BuildContext context,
    WidgetRef ref,
    Vehicle vehicle,
  ) {
    String type = 'OIL_CHANGE';
    final odoCtrl = TextEditingController(
      text: vehicle.currentOdometerKm.toStringAsFixed(0),
    );
    final vendorCtrl = TextEditingController(text: 'Auto2000 Medan');
    final costCtrl = TextEditingController(text: '950.000');
    final downtimeCtrl = TextEditingController(text: '3');
    final descCtrl = TextEditingController(text: 'Servis berkala & cek rem');

    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
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
                    'Catat Servis • ${vehicle.registrationNumber}',
                    style: const TextStyle(
                      fontSize: 17.5,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 12),
                  AppDropdownField<String>(
                    label: 'Jenis Perawatan / Perbaikan',
                    value: type,
                    items: const [
                      DropdownMenuItem(
                        value: 'OIL_CHANGE',
                        child: Text('Ganti Oli & Filter'),
                      ),
                      DropdownMenuItem(
                        value: 'TIRE_REPLACEMENT',
                        child: Text('Ganti Ban & Spooring'),
                      ),
                      DropdownMenuItem(
                        value: 'BATTERY_REPLACEMENT',
                        child: Text('Ganti Aki'),
                      ),
                      DropdownMenuItem(
                        value: 'GENERAL_SERVICE',
                        child: Text('Servis Berkala'),
                      ),
                      DropdownMenuItem(
                        value: 'REPAIR',
                        child: Text('Perbaikan Komponen (Repair)'),
                      ),
                    ],
                    onChanged: (v) =>
                        setModal(() => type = v ?? 'OIL_CHANGE'),
                  ),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: odoCtrl,
                          keyboardType: TextInputType.number,
                          decoration: const InputDecoration(
                            labelText: 'Odometer (km)',
                          ),
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: TextField(
                          controller: downtimeCtrl,
                          keyboardType: TextInputType.number,
                          decoration: const InputDecoration(
                            labelText: 'Downtime (jam)',
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: vendorCtrl,
                    decoration: const InputDecoration(
                      labelText: 'Nama Bengkel / Vendor',
                    ),
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: costCtrl,
                    keyboardType: TextInputType.number,
                    inputFormatters: [RupiahInputFormatter()],
                    decoration: const InputDecoration(
                      labelText: 'Biaya Servis (Rp)',
                      prefixText: 'Rp ',
                    ),
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: descCtrl,
                    decoration: const InputDecoration(
                      labelText: 'Rincian Pekerjaan',
                    ),
                  ),
                  const SizedBox(height: 16),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton(
                      onPressed: () async {
                        await ref
                            .read(appControllerProvider.notifier)
                            .addVehicleMaintenanceLog(
                              vehicleId: vehicle.id,
                              maintenanceType: type,
                              odometerKm:
                                  double.tryParse(odoCtrl.text) ??
                                  vehicle.currentOdometerKm,
                              vendorName: vendorCtrl.text.trim(),
                              costAmount: AppFormatters.parseRupiah(
                                costCtrl.text,
                              ),
                              downtimeHours:
                                  double.tryParse(downtimeCtrl.text) ?? 2,
                              description: descCtrl.text.trim(),
                            );
                        if (ctx.mounted) Navigator.pop(ctx);
                      },
                      child: const Text('Simpan Riwayat Servis'),
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

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(appControllerProvider);
    final controller = ref.read(appControllerProvider.notifier);
    final subTab = state.ownerFinanceSubTab;

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 96),
      children: [
        SegmentedButton<int>(
          segments: const [
            ButtonSegment(value: 0, label: Text('Setoran & Kas')),
            ButtonSegment(value: 1, label: Text('Biaya & Formula')),
            ButtonSegment(value: 2, label: Text('Armada & Supir')),
          ],
          selected: {subTab},
          onSelectionChanged: (s) =>
              controller.setOwnerTab(3, financeSubTab: s.first),
        ),
        const SizedBox(height: 14),
        if (subTab == 0) ...[
          // 1. Cash Custody & Payments
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: AppColors.navy,
              borderRadius: BorderRadius.circular(14),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Total Uang Tunai Pelanggan di Tangan Supir',
                  style: TextStyle(color: Colors.white70, fontSize: 13),
                ),
                const SizedBox(height: 4),
                Text(
                  AppFormatters.rupiah(state.totalDriverHeldCash),
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 26,
                    fontWeight: FontWeight.w900,
                  ),
                ),
                const SizedBox(height: 4),
                const Text(
                  'Status Lunas Pelanggan dipisahkan tegas dari Status Setoran Tunai Supir.',
                  style: TextStyle(color: Colors.white70, fontSize: 12),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          const Text(
            'Saldo Kas Tunai per Supir',
            style: TextStyle(fontWeight: FontWeight.w800, fontSize: 15.5),
          ),
          const SizedBox(height: 8),
          ...state.drivers.map((drv) {
            return Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.all(14),
                  child: Row(
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              drv.fullName,
                              style: const TextStyle(
                                fontWeight: FontWeight.w800,
                                fontSize: 15,
                              ),
                            ),
                            Text(
                              drv.cashHeldAmount > 0
                                  ? 'Belum disetor: ${AppFormatters.rupiah(drv.cashHeldAmount)}'
                                  : 'Semua kas tunai sudah disetor (Rp 0)',
                              style: TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w700,
                                color: drv.cashHeldAmount > 0
                                    ? AppColors.amber
                                    : AppColors.emerald,
                              ),
                            ),
                          ],
                        ),
                      ),
                      if (drv.cashHeldAmount > 0)
                        FilledButton(
                          style: FilledButton.styleFrom(
                            backgroundColor: AppColors.emerald,
                          ),
                          onPressed: () =>
                              _showConfirmHandoverSheet(context, ref, drv),
                          child: const Text('Konfirmasi Setoran'),
                        ),
                    ],
                  ),
                ),
              ),
            );
          }),
          const SizedBox(height: 14),
          const Text(
            'Buku Pembayaran Perjalanan (Catat & Soft-Void)',
            style: TextStyle(fontWeight: FontWeight.w800, fontSize: 15.5),
          ),
          const SizedBox(height: 8),
          ...state.trips.take(6).map((t) {
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
                              style: const TextStyle(
                                fontWeight: FontWeight.w800,
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                          StatusBadge.forCustody(t.cashCustodyStatus),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'Tarif: ${AppFormatters.rupiah(t.agreedFareSnapshot)} • Terbayar: ${AppFormatters.rupiah(t.totalPaidAmount)} (${OperationalLabels.paymentStatus(t.paymentStatus)})',
                        style: const TextStyle(fontSize: 13),
                      ),
                      ...t.payments.map((p) {
                        return Padding(
                          padding: const EdgeInsets.only(top: 6),
                          child: Row(
                            children: [
                              Expanded(
                                child: Text(
                                  '• ${p.paymentMethod} ${AppFormatters.rupiah(p.amount)} (${p.collectedByRole})${p.isVoided ? " [DIBATALKAN/VOID]" : ""}',
                                  style: TextStyle(
                                    fontSize: 12.5,
                                    decoration: p.isVoided
                                        ? TextDecoration.lineThrough
                                        : null,
                                    color: p.isVoided
                                        ? AppColors.crimson
                                        : AppColors.textSecondary,
                                  ),
                                ),
                              ),
                              if (!p.isVoided)
                                TextButton(
                                  onPressed: () => controller.voidTripPayment(
                                    tripId: t.id,
                                    paymentId: p.id,
                                    reason: 'Koreksi input oleh pemilik',
                                  ),
                                  child: const Text('Void'),
                                ),
                            ],
                          ),
                        );
                      }),
                      if (t.remainingUnpaidAmount > 0) ...[
                        const SizedBox(height: 8),
                        OutlinedButton.icon(
                          onPressed: () =>
                              _showRecordOwnerPaymentSheet(context, ref, t),
                          icon: const Icon(Icons.payments_outlined, size: 18),
                          label: Text(
                            'Catat Bayar Sisa ${AppFormatters.rupiah(t.remainingUnpaidAmount)}',
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ),
            );
          }),
        ] else if (subTab == 1) ...[
          // 2. Operating Cost Formulas & Profitability
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Expanded(
                child: Text(
                  'Formula Biaya Operasional Berversi',
                  style: TextStyle(fontWeight: FontWeight.w800, fontSize: 16),
                ),
              ),
              FilledButton.icon(
                onPressed: () => _showNewFormulaVersionSheet(context, ref),
                icon: const Icon(Icons.add_chart_rounded, size: 18),
                label: const Text('Versi Baru'),
              ),
            ],
          ),
          const SizedBox(height: 10),
          ...state.costFormulas.map((formula) {
            return Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.all(15),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          StatusBadge(
                            label: formula.isActive
                                ? 'v${formula.versionNumber} AKTIF'
                                : 'v${formula.versionNumber} HISTORIS',
                            fg: formula.isActive
                                ? AppColors.emerald
                                : AppColors.textSecondary,
                            bg: formula.isActive
                                ? AppColors.emeraldSoft
                                : AppColors.slateBg,
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              formula.name,
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
                        'Berlaku mulai: ${formula.effectiveFrom}${formula.effectiveTo != null ? " s/d ${formula.effectiveTo}" : " (berjalan)"}',
                        style: const TextStyle(
                          fontSize: 12.5,
                          color: AppColors.textSecondary,
                        ),
                      ),
                      const Divider(height: 18),
                      ...formula.components.map((comp) {
                        return Padding(
                          padding: const EdgeInsets.only(bottom: 6),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceBetween,
                                children: [
                                  Expanded(
                                    child: Text(
                                      comp.name,
                                      style: const TextStyle(
                                        fontWeight: FontWeight.w700,
                                        fontSize: 13,
                                      ),
                                    ),
                                  ),
                                  Text(
                                    comp.reconciliationPolicy,
                                    style: const TextStyle(
                                      fontSize: 11,
                                      fontWeight: FontWeight.w700,
                                      color: AppColors.navy,
                                    ),
                                  ),
                                ],
                              ),
                              if (comp.expressionFormula != null)
                                Text(
                                  'Rumus: ${comp.expressionFormula}',
                                  style: const TextStyle(
                                    fontSize: 11.5,
                                    color: AppColors.textSecondary,
                                  ),
                                ),
                            ],
                          ),
                        );
                      }),
                    ],
                  ),
                ),
              ),
            );
          }),
        ] else ...[
          // 3. Fleet & Drivers
          const Text(
            'Ketersediaan Supir (4 Personel)',
            style: TextStyle(fontWeight: FontWeight.w800, fontSize: 16),
          ),
          const SizedBox(height: 8),
          ...state.drivers.map((drv) {
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
                              '${drv.driverCode} • ${drv.fullName}',
                              style: const TextStyle(
                                fontWeight: FontWeight.w800,
                                fontSize: 15,
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Text(
                            'SIM s/d ${drv.licenseExpiryDate}',
                            style: const TextStyle(
                              fontSize: 12,
                              color: AppColors.textSecondary,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      AppDropdownField<String>(
                        label: 'Status Ketersediaan Supir',
                        value: drv.availabilityStatus,
                        items: const [
                          'AVAILABLE',
                          'TEMPORARILY_UNAVAILABLE',
                          'ON_LEAVE',
                          'UNAVAILABLE',
                        ]
                            .map(
                              (s) => DropdownMenuItem(
                                value: s,
                                child: Text(
                                  OperationalLabels.driverAvailability(s),
                                ),
                              ),
                            )
                            .toList(),
                        onChanged: (v) {
                          if (v != null) {
                            controller.updateDriverAvailability(
                              driverId: drv.id,
                              availabilityStatus: v,
                            );
                          }
                        },
                      ),
                    ],
                  ),
                ),
              ),
            );
          }),
          const SizedBox(height: 16),
          const Text(
            'Armada Kendaraan, Dokumen & Riwayat Servis',
            style: TextStyle(fontWeight: FontWeight.w800, fontSize: 16),
          ),
          const SizedBox(height: 8),
          ...state.vehicles.map((veh) {
            return Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.all(15),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Expanded(
                            child: Text(
                              '${veh.registrationNumber} • ${veh.nameModel}',
                              style: const TextStyle(
                                fontWeight: FontWeight.w800,
                                fontSize: 15.5,
                              ),
                            ),
                          ),
                          StatusBadge(
                            label: veh.status == 'ACTIVE'
                                ? 'SIAP JALAN'
                                : 'DALAM SERVIS',
                            fg: veh.status == 'ACTIVE'
                                ? AppColors.emerald
                                : AppColors.crimson,
                            bg: veh.status == 'ACTIVE'
                                ? AppColors.emeraldSoft
                                : AppColors.crimsonSoft,
                          ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'Tahun ${veh.manufactureYear} • ${veh.passengerCapacity} Kursi / ${veh.luggageCapacityBags} Koper • Odometer: ${AppFormatters.thousandNumber(veh.currentOdometerKm)} km',
                        style: const TextStyle(
                          fontSize: 12.5,
                          color: AppColors.textSecondary,
                        ),
                      ),
                      if (veh.documents.isNotEmpty) ...[
                        const SizedBox(height: 8),
                        Wrap(
                          spacing: 6,
                          runSpacing: 6,
                          children: veh.documents.map((d) {
                            return StatusBadge(
                              label: '${d.docType}: ${d.expiryDate}',
                              fg: AppColors.navy,
                              bg: AppColors.slateBg,
                            );
                          }).toList(),
                        ),
                      ],
                      if (veh.recentMaintenanceLogs.isNotEmpty) ...[
                        const SizedBox(height: 8),
                        ...veh.recentMaintenanceLogs.take(2).map((log) {
                          return Text(
                            '• Servis ${log.serviceDate}: ${log.description} (${AppFormatters.rupiah(log.costAmount)}, downtime ${log.downtimeHours.toStringAsFixed(0)}j)',
                            style: const TextStyle(fontSize: 12.5),
                          );
                        }),
                      ],
                      const SizedBox(height: 10),
                      OutlinedButton.icon(
                        onPressed: () =>
                            _showAddMaintenanceSheet(context, ref, veh),
                        icon: const Icon(Icons.build_circle_outlined, size: 18),
                        label: const Text('+ Catat Servis / Ganti Oli & Ban'),
                      ),
                    ],
                  ),
                ),
              ),
            );
          }),
        ],
      ],
    );
  }
}
