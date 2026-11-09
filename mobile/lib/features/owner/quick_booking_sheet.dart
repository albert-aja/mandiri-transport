import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_theme.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/shared_widgets.dart';
import '../../data/models/models.dart';
import '../../data/state/app_controller.dart';

Future<void> showQuickBookingSheet(BuildContext context) async {
  await showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    backgroundColor: Colors.white,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(22)),
    ),
    builder: (_) => const QuickBookingSheet(),
  );
}

class QuickBookingSheet extends ConsumerStatefulWidget {
  const QuickBookingSheet({super.key});

  @override
  ConsumerState<QuickBookingSheet> createState() => _QuickBookingSheetState();
}

class _QuickBookingSheetState extends ConsumerState<QuickBookingSheet> {
  String? _selectedCustomerId = 'cust-ratna';
  bool _createNewCustomer = false;
  final _newCustNameCtrl = TextEditingController();
  final _newCustPhoneCtrl = TextEditingController();

  String _tripType = 'AIRPORT_DROPOFF';
  String? _routeId = 'rte-mdn-kno';
  String _vehicleType = 'MPV';
  int _passengerCount = 2;
  int _luggageCount = 2;

  final _pickupCtrl = TextEditingController(
    text: 'Jl. Kartini No. 18, Madras Hulu, Medan Polonia',
  );
  final _destCtrl = TextEditingController(
    text: 'Bandara Internasional Kualanamu (KNO)',
  );
  final _flightCtrl = TextEditingController();
  final _pickupTimeCtrl = TextEditingController(text: '2026-11-10T08:00:00+07:00');
  final _returnTimeCtrl = TextEditingController(text: '2026-11-10T17:00:00+07:00');
  final _fareCtrl = TextEditingController(text: '220.000');
  final _returnFareCtrl = TextEditingController(text: '230.000');
  final List<TextEditingController> _stopControllers = [];

  PricingResolution? _pricingResolution;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _refreshPricing());
  }

  @override
  void dispose() {
    _newCustNameCtrl.dispose();
    _newCustPhoneCtrl.dispose();
    _pickupCtrl.dispose();
    _destCtrl.dispose();
    _flightCtrl.dispose();
    _pickupTimeCtrl.dispose();
    _returnTimeCtrl.dispose();
    _fareCtrl.dispose();
    _returnFareCtrl.dispose();
    for (final c in _stopControllers) {
      c.dispose();
    }
    super.dispose();
  }

  void _refreshPricing({bool overwriteAgreedFare = true}) {
    final controller = ref.read(appControllerProvider.notifier);
    final res = controller.resolvePricing(
      customerId: _createNewCustomer ? null : _selectedCustomerId,
      routeId: _routeId,
      vehicleType: _vehicleType,
      tripType: _tripType,
    );
    setState(() {
      _pricingResolution = res;
      if (overwriteAgreedFare) {
        _fareCtrl.text = AppFormatters.thousandNumber(res.suggestedFare);
      }
    });
  }

  void _applyTemplate(BookingTemplate tpl) {
    final state = ref.read(appControllerProvider);
    final cust = state.customers
        .where((c) => c.id == tpl.customerId)
        .firstOrNull;
    setState(() {
      _createNewCustomer = false;
      _selectedCustomerId = tpl.customerId;
      _routeId = tpl.routeId;
      _tripType = tpl.tripType;
      _pickupCtrl.text = tpl.pickupAddress;
      _destCtrl.text = tpl.destinationAddress;
      _passengerCount = tpl.defaultPassengerCount;
      _luggageCount = tpl.defaultLuggageCount;
      _vehicleType =
          tpl.preferredVehicleType ?? cust?.preferredVehicleType ?? 'MPV';
    });
    _refreshPricing();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          'Template "${tpl.name}" diterapkan ke formulir (belum dijadwalkan).',
        ),
      ),
    );
  }

  void _onCustomerSelected(String? customerId) {
    final state = ref.read(appControllerProvider);
    final cust = state.customers.where((c) => c.id == customerId).firstOrNull;
    setState(() {
      _selectedCustomerId = customerId;
      if (cust != null) {
        if (cust.defaultAddress != null) {
          _pickupCtrl.text = cust.defaultAddress!.addressText;
        }
        if (cust.preferredVehicleType != null) {
          _vehicleType = cust.preferredVehicleType!;
        }
      }
    });
    _refreshPricing();
  }

  void _onRouteSelected(String? routeId) {
    final state = ref.read(appControllerProvider);
    final route = state.routes.where((r) => r.id == routeId).firstOrNull;
    setState(() {
      _routeId = routeId;
      if (route != null) {
        if (route.code == 'KNO_MDN') {
          _tripType = 'AIRPORT_PICKUP';
          _pickupCtrl.text = 'Bandara Kualanamu (KNO) Pintu Kedatangan';
        } else if (route.code == 'MDN_KNO') {
          _tripType = 'AIRPORT_DROPOFF';
          _destCtrl.text = 'Bandara Internasional Kualanamu (KNO)';
        } else {
          _destCtrl.text = route.destinationName;
        }
      }
    });
    _refreshPricing();
  }

  Future<void> _saveCurrentAsTemplate() async {
    final controller = ref.read(appControllerProvider.notifier);
    final state = ref.read(appControllerProvider);
    final custName = state.customers
            .where((c) => c.id == _selectedCustomerId)
            .firstOrNull
            ?.name ??
        _newCustNameCtrl.text.trim();
    final name =
        '${custName.isEmpty ? "Umum" : custName} — ${_destCtrl.text.trim()}';
    await controller.saveBookingTemplate(
      name: name,
      customerId: _createNewCustomer ? null : _selectedCustomerId,
      routeId: _routeId,
      tripType: _tripType,
      pickupAddress: _pickupCtrl.text.trim(),
      destinationAddress: _destCtrl.text.trim(),
      passengerCount: _passengerCount,
      luggageCount: _luggageCount,
      preferredVehicleType: _vehicleType,
    );
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Template "$name" disimpan tanpa membuat jadwal otomatis.',
          ),
        ),
      );
    }
  }

  Future<void> _submitQuickBooking() async {
    final fare = AppFormatters.parseRupiah(_fareCtrl.text);
    if (fare <= 0) return;

    final stops = <TripStop>[];
    for (int i = 0; i < _stopControllers.length; i++) {
      final addr = _stopControllers[i].text.trim();
      if (addr.isNotEmpty) {
        stops.add(TripStop(stopOrder: i + 1, address: addr));
      }
    }

    final createdTrips = await ref
        .read(appControllerProvider.notifier)
        .createQuickBooking(
          customerId: _createNewCustomer ? null : _selectedCustomerId,
          newCustomerName: _createNewCustomer ? _newCustNameCtrl.text : null,
          newCustomerPhone: _createNewCustomer ? _newCustPhoneCtrl.text : null,
          tripType: _tripType,
          routeId: _routeId,
          requestedPickupAt: _pickupTimeCtrl.text.trim(),
          returnPickupAt: _tripType == 'ROUND_TRIP'
              ? _returnTimeCtrl.text.trim()
              : null,
          returnAgreedFare: _tripType == 'ROUND_TRIP'
              ? AppFormatters.parseRupiah(_returnFareCtrl.text)
              : null,
          pickupAddress: _pickupCtrl.text.trim(),
          destinationAddress: _destCtrl.text.trim(),
          passengerCount: _passengerCount,
          luggageCount: _luggageCount,
          flightNumber: _flightCtrl.text.trim().isEmpty
              ? null
              : _flightCtrl.text.trim(),
          vehicleTypePreference: _vehicleType,
          agreedFare: fare,
          stops: stops,
        );

    if (mounted) {
      Navigator.pop(context);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Booking cepat tersimpan (${createdTrips.length} perjalanan dibuat: ${createdTrips.first.tripCode}).',
          ),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final appState = ref.watch(appControllerProvider);
    final selectedCustomer = _createNewCustomer
        ? null
        : appState.customers
              .where((c) => c.id == _selectedCustomerId)
              .firstOrNull;

    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
      ),
      child: DraggableScrollableSheet(
        expand: false,
        initialChildSize: 0.92,
        minChildSize: 0.6,
        maxChildSize: 0.96,
        builder: (context, scrollCtrl) {
          return ListView(
            controller: scrollCtrl,
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 28),
            children: [
              Row(
                children: [
                  const Icon(
                    Icons.flash_on_rounded,
                    color: AppColors.emerald,
                    size: 26,
                  ),
                  const SizedBox(width: 8),
                  const Expanded(
                    child: Text(
                      'Booking Cepat (30 Detik)',
                      style: TextStyle(
                        fontSize: 19,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ),
                  TextButton.icon(
                    onPressed: _saveCurrentAsTemplate,
                    icon: const Icon(Icons.bookmark_add_outlined, size: 18),
                    label: const Text('Simpan Template'),
                  ),
                  IconButton(
                    onPressed: () => Navigator.pop(context),
                    icon: const Icon(Icons.close),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              const Text(
                '1-Tap Template Langganan (Mengisi Form Cepat — Tidak Auto-Jadwal):',
                style: TextStyle(
                  fontSize: 12.5,
                  fontWeight: FontWeight.w700,
                  color: AppColors.textSecondary,
                ),
              ),
              const SizedBox(height: 8),
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: Row(
                  children: appState.bookingTemplates.map((tpl) {
                    return Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: ActionChip(
                        avatar: const Icon(Icons.bolt, size: 16),
                        label: Text(tpl.name),
                        onPressed: () => _applyTemplate(tpl),
                      ),
                    );
                  }).toList(),
                ),
              ),
              const Divider(height: 24),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text(
                    'Pelanggan',
                    style: TextStyle(fontWeight: FontWeight.w800, fontSize: 15),
                  ),
                  TextButton.icon(
                    onPressed: () => setState(
                      () => _createNewCustomer = !_createNewCustomer,
                    ),
                    icon: Icon(
                      _createNewCustomer
                          ? Icons.people_alt_outlined
                          : Icons.person_add_alt_1,
                      size: 18,
                    ),
                    label: Text(
                      _createNewCustomer
                          ? 'Pilih Pelanggan Lama'
                          : '+ Pelanggan Baru',
                    ),
                  ),
                ],
              ),
              if (_createNewCustomer) ...[
                TextField(
                  controller: _newCustNameCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Nama Pelanggan Baru',
                  ),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: _newCustPhoneCtrl,
                  keyboardType: TextInputType.phone,
                  decoration: const InputDecoration(
                    labelText: 'Nomor HP / WhatsApp',
                    hintText: '+62812...',
                  ),
                ),
              ] else ...[
                AppDropdownField<String>(
                  label: 'Pilih Pelanggan Langganan / Korporat',
                  value: _selectedCustomerId,
                  items: appState.customers
                      .map(
                        (c) => DropdownMenuItem(
                          value: c.id,
                          child: Text('${c.name} (${c.phone})'),
                        ),
                      )
                      .toList(),
                  onChanged: _onCustomerSelected,
                ),
                if (selectedCustomer != null &&
                    selectedCustomer.notes.isNotEmpty) ...[
                  const SizedBox(height: 8),
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: AppColors.amberSoft,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Text(
                      'Catatan Pelanggan: ${selectedCustomer.notes.map((n) => n.noteText).join(" • ")}',
                      style: const TextStyle(
                        fontSize: 12.5,
                        color: AppColors.textPrimary,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ],
              ],
              const SizedBox(height: 14),
              Row(
                children: [
                  Expanded(
                    child: AppDropdownField<String>(
                      label: 'Jenis Perjalanan',
                      value: _tripType,
                      items: const [
                        'AIRPORT_DROPOFF',
                        'AIRPORT_PICKUP',
                        'INTERCITY',
                        'LOCAL',
                        'ROUND_TRIP',
                        'MULTI_STOP_CHARTER',
                        'HOURLY_CHARTER',
                        'FULL_DAY_CHARTER',
                      ]
                          .map(
                            (t) => DropdownMenuItem(
                              value: t,
                              child: Text(OperationalLabels.tripType(t)),
                            ),
                          )
                          .toList(),
                      onChanged: (v) {
                        if (v == null) return;
                        setState(() => _tripType = v);
                        _refreshPricing();
                      },
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: AppDropdownField<String>(
                      label: 'Tipe Mobil',
                      value: _vehicleType,
                      items: const ['MPV', 'HIACE_VAN', 'SUV', 'SEDAN']
                          .map(
                            (v) => DropdownMenuItem(value: v, child: Text(v)),
                          )
                          .toList(),
                      onChanged: (v) {
                        if (v == null) return;
                        setState(() => _vehicleType = v);
                        _refreshPricing();
                      },
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              AppDropdownField<String>(
                label: 'Rute Operasional Medan / Sumut',
                value: _routeId,
                items: appState.routes
                    .map(
                      (r) => DropdownMenuItem(value: r.id, child: Text(r.name)),
                    )
                    .toList(),
                onChanged: _onRouteSelected,
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _pickupTimeCtrl,
                decoration: const InputDecoration(
                  labelText: 'Waktu Jemput (ISO WIB)',
                  prefixIcon: Icon(Icons.schedule_rounded),
                ),
              ),
              if (_tripType == 'ROUND_TRIP') ...[
                const SizedBox(height: 10),
                TextField(
                  controller: _returnTimeCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Waktu Jemput Pulang (Leg 2 Return)',
                    prefixIcon: Icon(Icons.u_turn_left_rounded),
                  ),
                ),
              ],
              const SizedBox(height: 12),
              TextField(
                controller: _pickupCtrl,
                decoration: const InputDecoration(
                  labelText: 'Alamat Jemput',
                  prefixIcon: Icon(Icons.my_location_rounded),
                ),
              ),
              const SizedBox(height: 10),
              TextField(
                controller: _destCtrl,
                decoration: const InputDecoration(
                  labelText: 'Alamat Tujuan',
                  prefixIcon: Icon(Icons.place_rounded),
                ),
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _flightCtrl,
                      decoration: const InputDecoration(
                        labelText: 'No. Penerbangan (Opsional)',
                        hintText: 'GA-190 / SQ-991',
                      ),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Row(
                      children: [
                        Expanded(
                          child: AppDropdownField<int>(
                            label: 'Penumpang',
                            value: _passengerCount,
                            items: List.generate(14, (i) => i + 1)
                                .map(
                                  (n) => DropdownMenuItem(
                                    value: n,
                                    child: Text('$n org'),
                                  ),
                                )
                                .toList(),
                            onChanged: (v) =>
                                setState(() => _passengerCount = v ?? 2),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: AppDropdownField<int>(
                            label: 'Koper',
                            value: _luggageCount,
                            items: List.generate(13, (i) => i)
                                .map(
                                  (n) => DropdownMenuItem(
                                    value: n,
                                    child: Text('$n tas'),
                                  ),
                                )
                                .toList(),
                            onChanged: (v) =>
                                setState(() => _luggageCount = v ?? 2),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              // Multi-Stop dynamic stops
              Row(
                children: [
                  const Expanded(
                    child: Text(
                      'Titik Singgah Tambahan (Multi-Stop)',
                      style: TextStyle(
                        fontWeight: FontWeight.w700,
                        fontSize: 13.5,
                      ),
                    ),
                  ),
                  TextButton.icon(
                    onPressed: () {
                      setState(() {
                        _stopControllers.add(TextEditingController());
                      });
                    },
                    icon: const Icon(Icons.add_location_alt_outlined, size: 18),
                    label: const Text('+ Tambah Singgah'),
                  ),
                ],
              ),
              ...List.generate(_stopControllers.length, (index) {
                return Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: _stopControllers[index],
                          decoration: InputDecoration(
                            labelText: 'Singgah #${index + 1}',
                            hintText: 'Contoh: Restoran Garuda / Toko Bika Ambon',
                          ),
                        ),
                      ),
                      IconButton(
                        onPressed: () {
                          setState(() {
                            final removed = _stopControllers.removeAt(index);
                            removed.dispose();
                          });
                        },
                        icon: const Icon(
                          Icons.remove_circle_outline,
                          color: AppColors.crimson,
                        ),
                      ),
                    ],
                  ),
                );
              }),
              const Divider(height: 24),
              if (_pricingResolution != null)
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: AppColors.infoSoft,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    children: [
                      const Icon(
                        Icons.sell_rounded,
                        color: AppColors.infoBlue,
                        size: 20,
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Saran Tarif (${_pricingResolution!.pricingTierUsed}): ${AppFormatters.rupiah(_pricingResolution!.suggestedFare)}',
                              style: const TextStyle(
                                fontWeight: FontWeight.w800,
                                color: AppColors.infoBlue,
                                fontSize: 13.5,
                              ),
                            ),
                            Text(
                              _pricingResolution!.explanation,
                              style: const TextStyle(
                                fontSize: 12,
                                color: AppColors.textPrimary,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              const SizedBox(height: 12),
              TextField(
                controller: _fareCtrl,
                keyboardType: TextInputType.number,
                inputFormatters: [RupiahInputFormatter()],
                decoration: const InputDecoration(
                  labelText: 'Tarif Disepakati (Rp)',
                  prefixText: 'Rp ',
                ),
              ),
              const SizedBox(height: 8),
              RupiahQuickChips(
                controller: _fareCtrl,
                referenceFare: _pricingResolution?.suggestedFare,
              ),
              if (_tripType == 'ROUND_TRIP') ...[
                const SizedBox(height: 12),
                TextField(
                  controller: _returnFareCtrl,
                  keyboardType: TextInputType.number,
                  inputFormatters: [RupiahInputFormatter()],
                  decoration: const InputDecoration(
                    labelText: 'Tarif Perjalanan Pulang / Leg 2 (Rp)',
                    prefixText: 'Rp ',
                  ),
                ),
              ],
              const SizedBox(height: 20),
              FilledButton.icon(
                style: FilledButton.styleFrom(
                  backgroundColor: AppColors.emerald,
                ),
                onPressed: _submitQuickBooking,
                icon: const Icon(Icons.check_circle_outline),
                label: const Text('Simpan Booking Cepat'),
              ),
            ],
          );
        },
      ),
    );
  }
}
