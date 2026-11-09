import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_theme.dart';
import '../../core/utils/deep_links.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/shared_widgets.dart';
import '../../data/models/models.dart';
import '../../data/state/app_controller.dart';

class OwnerCustomersTab extends ConsumerStatefulWidget {
  const OwnerCustomersTab({super.key});

  @override
  ConsumerState<OwnerCustomersTab> createState() => _OwnerCustomersTabState();
}

class _OwnerCustomersTabState extends ConsumerState<OwnerCustomersTab> {
  int _section = 0; // 0 = Customers, 1 = Lost & Found
  String _query = '';

  void _showAddCustomerDialog(BuildContext context) {
    final nameCtrl = TextEditingController();
    final phoneCtrl = TextEditingController();
    final compCtrl = TextEditingController();
    String vehType = 'MPV';

    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) {
        return StatefulBuilder(
          builder: (ctx, setModalState) {
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
                    'Tambah Pelanggan Baru',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: nameCtrl,
                    decoration: const InputDecoration(
                      labelText: 'Nama Lengkap / Kontak',
                    ),
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: phoneCtrl,
                    keyboardType: TextInputType.phone,
                    decoration: const InputDecoration(
                      labelText: 'No. HP / WhatsApp',
                      hintText: '+62812...',
                    ),
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: compCtrl,
                    decoration: const InputDecoration(
                      labelText: 'Nama Perusahaan (Opsional)',
                    ),
                  ),
                  const SizedBox(height: 10),
                  AppDropdownField<String>(
                    label: 'Preferensi Tipe Kendaraan',
                    value: vehType,
                    items: const ['MPV', 'HIACE_VAN', 'SUV', 'SEDAN']
                        .map((v) => DropdownMenuItem(value: v, child: Text(v)))
                        .toList(),
                    onChanged: (v) =>
                        setModalState(() => vehType = v ?? 'MPV'),
                  ),
                  const SizedBox(height: 16),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton(
                      onPressed: () async {
                        if (nameCtrl.text.trim().isEmpty) return;
                        await ref
                            .read(appControllerProvider.notifier)
                            .createCustomer(
                              name: nameCtrl.text.trim(),
                              phone: phoneCtrl.text.trim(),
                              companyName: compCtrl.text.trim().isEmpty
                                  ? null
                                  : compCtrl.text.trim(),
                              preferredVehicleType: vehType,
                            );
                        if (ctx.mounted) Navigator.pop(ctx);
                      },
                      child: const Text('Simpan Pelanggan'),
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

  void _showAddNoteDialog(BuildContext context, Customer customer) {
    final noteCtrl = TextEditingController();
    String category = 'PREFERENCE';
    bool visibleToDrivers = true;

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
                    'Tambah Catatan Operasional • ${customer.name}',
                    style: const TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 12),
                  AppDropdownField<String>(
                    label: 'Kategori Catatan',
                    value: category,
                    items: const [
                      DropdownMenuItem(
                        value: 'PREFERENCE',
                        child: Text('Preferensi Kenyamanan'),
                      ),
                      DropdownMenuItem(
                        value: 'LUGGAGE',
                        child: Text('Bagasi / Barang Bawaan'),
                      ),
                      DropdownMenuItem(
                        value: 'OPERATIONAL',
                        child: Text('Protokol Operasional / Korporat'),
                      ),
                      DropdownMenuItem(
                        value: 'TIMING',
                        child: Text('Ketepatan Waktu'),
                      ),
                    ],
                    onChanged: (v) =>
                        setModal(() => category = v ?? 'PREFERENCE'),
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: noteCtrl,
                    maxLines: 3,
                    decoration: const InputDecoration(
                      labelText: 'Isi Catatan Wajar & Profesional',
                      hintText:
                          'Contoh: Membawa 2 koper besar, kabin bebas bau rokok',
                    ),
                  ),
                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    value: visibleToDrivers,
                    onChanged: (v) => setModal(() => visibleToDrivers = v),
                    title: const Text(
                      'Tampilkan ke Supir yang Bertugas (Hanya Baca)',
                      style: TextStyle(
                        fontSize: 13.5,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton(
                      onPressed: () async {
                        if (noteCtrl.text.trim().isEmpty) return;
                        await ref
                            .read(appControllerProvider.notifier)
                            .addCustomerNote(
                              customerId: customer.id,
                              category: category,
                              noteText: noteCtrl.text.trim(),
                              visibleToDrivers: visibleToDrivers,
                            );
                        if (ctx.mounted) Navigator.pop(ctx);
                      },
                      child: const Text('Simpan Catatan'),
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

  void _showAddAgreementDialog(BuildContext context, Customer customer) {
    final nameCtrl = TextEditingController(
      text: 'Harga Khusus ${customer.name}',
    );
    final fareCtrl = TextEditingController(text: '220.000');
    final routes = ref.read(appControllerProvider).routes;
    String? routeId = routes.firstOrNull?.id;

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
                    'Kesepakatan Harga Khusus • ${customer.name}',
                    style: const TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: nameCtrl,
                    decoration: const InputDecoration(
                      labelText: 'Nama Kesepakatan',
                    ),
                  ),
                  const SizedBox(height: 10),
                  AppDropdownField<String>(
                    label: 'Rute Berlaku',
                    value: routeId,
                    items: routes
                        .map(
                          (r) => DropdownMenuItem(
                            value: r.id,
                            child: Text(r.name),
                          ),
                        )
                        .toList(),
                    onChanged: (v) => setModal(() => routeId = v),
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: fareCtrl,
                    keyboardType: TextInputType.number,
                    inputFormatters: [RupiahInputFormatter()],
                    decoration: const InputDecoration(
                      labelText: 'Tarif Khusus Disepakati (Rp)',
                      prefixText: 'Rp ',
                    ),
                  ),
                  const SizedBox(height: 16),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton(
                      onPressed: () async {
                        final fare = AppFormatters.parseRupiah(fareCtrl.text);
                        if (fare <= 0) return;
                        await ref
                            .read(appControllerProvider.notifier)
                            .addCustomerPricingAgreement(
                              customerId: customer.id,
                              agreementName: nameCtrl.text.trim(),
                              routeId: routeId,
                              vehicleType: customer.preferredVehicleType,
                              agreedFare: fare,
                            );
                        if (ctx.mounted) Navigator.pop(ctx);
                      },
                      child: const Text('Simpan Harga Khusus'),
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
  Widget build(BuildContext context) {
    final state = ref.watch(appControllerProvider);
    final controller = ref.read(appControllerProvider.notifier);

    final filteredCustomers = state.customers.where((c) {
      if (_query.trim().isEmpty) return true;
      final q = _query.toLowerCase();
      return c.name.toLowerCase().contains(q) ||
          c.phone.toLowerCase().contains(q) ||
          (c.companyName?.toLowerCase().contains(q) ?? false);
    }).toList();

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 96),
      children: [
        SegmentedButton<int>(
          segments: [
            ButtonSegment(
              value: 0,
              icon: const Icon(Icons.people_alt_rounded, size: 18),
              label: Text('Pelanggan (${state.customers.length})'),
            ),
            ButtonSegment(
              value: 1,
              icon: const Icon(Icons.luggage_rounded, size: 18),
              label: Text(
                'Barang Tertinggal (${state.lostAndFoundItems.length})',
              ),
            ),
          ],
          selected: {_section},
          onSelectionChanged: (s) => setState(() => _section = s.first),
        ),
        const SizedBox(height: 14),
        if (_section == 0) ...[
          Row(
            children: [
              Expanded(
                child: TextField(
                  onChanged: (v) => setState(() => _query = v),
                  decoration: const InputDecoration(
                    hintText: 'Cari nama pelanggan / perusahaan...',
                    prefixIcon: Icon(Icons.search),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              FilledButton.icon(
                onPressed: () => _showAddCustomerDialog(context),
                icon: const Icon(Icons.person_add_alt_1),
                label: const Text('Baru'),
              ),
            ],
          ),
          const SizedBox(height: 12),
          ...filteredCustomers.map((cust) {
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
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  cust.name,
                                  style: const TextStyle(
                                    fontWeight: FontWeight.w800,
                                    fontSize: 16,
                                  ),
                                ),
                                Text(
                                  '${cust.phone}${cust.companyName != null ? " • ${cust.companyName}" : ""}',
                                  style: const TextStyle(
                                    fontSize: 13,
                                    color: AppColors.textSecondary,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          if (cust.preferredVehicleType != null)
                            StatusBadge(
                              label: 'Unit ${cust.preferredVehicleType}',
                              fg: AppColors.navy,
                              bg: AppColors.slateBg,
                            ),
                        ],
                      ),
                      if (cust.addresses.isNotEmpty) ...[
                        const SizedBox(height: 10),
                        ...cust.addresses.map((addr) {
                          return Padding(
                            padding: const EdgeInsets.only(bottom: 4),
                            child: InkWell(
                              onTap: () => DeepLinks.launchExternal(
                                addr.googleMapsUrl.isNotEmpty
                                    ? addr.googleMapsUrl
                                    : DeepLinks.buildGoogleMapsSearchUrl(
                                        query: addr.addressText,
                                      ),
                              ),
                              child: Row(
                                children: [
                                  const Icon(
                                    Icons.place_outlined,
                                    size: 16,
                                    color: AppColors.emerald,
                                  ),
                                  const SizedBox(width: 6),
                                  Expanded(
                                    child: Text(
                                      '${addr.label}: ${addr.addressText}',
                                      style: const TextStyle(
                                        fontSize: 12.5,
                                        decoration: TextDecoration.underline,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          );
                        }),
                      ],
                      if (cust.pricingAgreements.isNotEmpty) ...[
                        const SizedBox(height: 8),
                        Wrap(
                          spacing: 6,
                          runSpacing: 6,
                          children: cust.pricingAgreements.map((ag) {
                            final text = ag.pricingMode == 'FIXED_FARE'
                                ? '${ag.agreementName}: ${AppFormatters.rupiah(ag.agreedFare)}'
                                : '${ag.agreementName} (${ag.discountPercent?.toStringAsFixed(0)}%)';
                            return StatusBadge(
                              label: text,
                              fg: AppColors.emerald,
                              bg: AppColors.emeraldSoft,
                            );
                          }).toList(),
                        ),
                      ],
                      if (cust.notes.isNotEmpty) ...[
                        const SizedBox(height: 10),
                        Container(
                          padding: const EdgeInsets.all(10),
                          decoration: BoxDecoration(
                            color: AppColors.slateBg,
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: cust.notes.map((n) {
                              return Padding(
                                padding: const EdgeInsets.only(bottom: 4),
                                child: Text(
                                  '• [${n.category}] ${n.noteText}${n.visibleToDrivers ? " (Dibaca Supir)" : " (Internal)"}',
                                  style: const TextStyle(fontSize: 12.5),
                                ),
                              );
                            }).toList(),
                          ),
                        ),
                      ],
                      const SizedBox(height: 12),
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: [
                          OutlinedButton.icon(
                            onPressed: () =>
                                _showAddNoteDialog(context, cust),
                            icon: const Icon(Icons.note_add_outlined, size: 18),
                            label: const Text('+ Catatan'),
                          ),
                          OutlinedButton.icon(
                            onPressed: () =>
                                _showAddAgreementDialog(context, cust),
                            icon: const Icon(Icons.sell_outlined, size: 18),
                            label: const Text('+ Harga Khusus'),
                          ),
                          FilledButton.icon(
                            style: FilledButton.styleFrom(
                              backgroundColor: AppColors.emerald,
                            ),
                            onPressed: () => showWhatsappTemplatePickerSheet(
                              context: context,
                              templates: state.whatsappTemplates,
                              recipientPhone: cust.whatsappPhone,
                              customerName: cust.name,
                            ),
                            icon: const Icon(Icons.chat_rounded, size: 18),
                            label: const Text('Kirim WA'),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            );
          }),
        ] else ...[
          // Lost & Found Tracker
          ...state.lostAndFoundItems.map((item) {
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
                          Expanded(
                            child: Text(
                              '${item.tripCode} • ${item.customerName}',
                              style: const TextStyle(
                                fontWeight: FontWeight.w800,
                                fontSize: 14.5,
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                          StatusBadge(
                            label: OperationalLabels.lostFoundStatus(
                              item.status,
                            ),
                            fg: item.status == 'RETURNED'
                                ? AppColors.emerald
                                : AppColors.amber,
                            bg: item.status == 'RETURNED'
                                ? AppColors.emeraldSoft
                                : AppColors.amberSoft,
                          ),
                        ],
                      ),
                      const SizedBox(height: 6),
                      Text(
                        item.itemDescription,
                        style: const TextStyle(
                          fontWeight: FontWeight.w700,
                          fontSize: 15,
                        ),
                      ),
                      if (item.notes != null && item.notes!.isNotEmpty)
                        Text(
                          'Catatan: ${item.notes}',
                          style: const TextStyle(
                            fontSize: 12.5,
                            color: AppColors.textSecondary,
                          ),
                        ),
                      const SizedBox(height: 10),
                      AppDropdownField<String>(
                        label: 'Perbarui Status Penanganan',
                        value: item.status,
                        items: const [
                          'FOUND',
                          'CONTACTING_CUSTOMER',
                          'STORED',
                          'RETURNED',
                          'UNRESOLVED',
                        ]
                            .map(
                              (s) => DropdownMenuItem(
                                value: s,
                                child: Text(
                                  OperationalLabels.lostFoundStatus(s),
                                ),
                              ),
                            )
                            .toList(),
                        onChanged: (v) {
                          if (v != null) {
                            controller.updateLostAndFoundStatus(
                              itemId: item.id,
                              newStatus: v,
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
        ],
      ],
    );
  }
}
