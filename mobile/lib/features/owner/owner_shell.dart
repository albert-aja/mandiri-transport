import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_theme.dart';
import '../../core/widgets/shared_widgets.dart';
import '../../data/state/app_controller.dart';
import 'owner_bi_tab.dart';
import 'owner_customers_tab.dart';
import 'owner_dashboard_tab.dart';
import 'owner_dispatch_tab.dart';
import 'owner_finance_fleet_tab.dart';
import 'quick_booking_sheet.dart';

class OwnerShell extends ConsumerWidget {
  const OwnerShell({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(appControllerProvider);
    final controller = ref.read(appControllerProvider.notifier);

    final tabs = const [
      OwnerDashboardTab(),
      OwnerDispatchTab(),
      OwnerCustomersTab(),
      OwnerFinanceFleetTab(),
      OwnerBiTab(),
    ];

    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Mandiri Transport • Dispatcher'),
            Text(
              '${state.session?.fullName ?? "Pak Hendra"} • ${state.effectiveOnline ? "Online" : "Offline"}${state.outbox.isNotEmpty ? " (${state.outbox.length} antrean)" : ""}',
              style: const TextStyle(
                fontSize: 12,
                color: Colors.white70,
                fontWeight: FontWeight.w500,
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            tooltip: 'Sinkronisasi Sekarang',
            onPressed: () async {
              final msg = await controller.syncNow();
              if (context.mounted) {
                ScaffoldMessenger.of(
                  context,
                ).showSnackBar(SnackBar(content: Text(msg)));
              }
            },
            icon: const Icon(Icons.sync_rounded),
          ),
          TextButton.icon(
            style: TextButton.styleFrom(foregroundColor: Colors.white),
            onPressed: controller.logout,
            icon: const Icon(Icons.switch_account_rounded, size: 18),
            label: const Text('Ganti Akun'),
          ),
        ],
      ),
      body: Column(
        children: [
          LivePushBanner(
            fcmState: state.fcmState,
            onDismiss: controller.dismissLivePushBanner,
            onOpen: () => controller.openPushTarget(
              referenceType: state.fcmState.liveBannerRefType,
            ),
          ),
          Expanded(child: tabs[state.ownerTabIndex]),
        ],
      ),
      floatingActionButton: state.ownerTabIndex <= 1
          ? FloatingActionButton.extended(
              backgroundColor: AppColors.emerald,
              foregroundColor: Colors.white,
              onPressed: () => showQuickBookingSheet(context),
              icon: const Icon(Icons.flash_on_rounded),
              label: const Text('+ Booking Cepat'),
            )
          : null,
      bottomNavigationBar: NavigationBar(
        selectedIndex: state.ownerTabIndex,
        onDestinationSelected: (idx) => controller.setOwnerTab(idx),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.dashboard_outlined),
            selectedIcon: Icon(Icons.dashboard_rounded),
            label: 'Beranda',
          ),
          NavigationDestination(
            icon: Icon(Icons.calendar_month_outlined),
            selectedIcon: Icon(Icons.calendar_month_rounded),
            label: 'Jadwal',
          ),
          NavigationDestination(
            icon: Icon(Icons.people_outline_rounded),
            selectedIcon: Icon(Icons.people_alt_rounded),
            label: 'Pelanggan',
          ),
          NavigationDestination(
            icon: Icon(Icons.account_balance_wallet_outlined),
            selectedIcon: Icon(Icons.account_balance_wallet_rounded),
            label: 'Keuangan',
          ),
          NavigationDestination(
            icon: Icon(Icons.analytics_outlined),
            selectedIcon: Icon(Icons.analytics_rounded),
            label: 'Analitik BI',
          ),
        ],
      ),
    );
  }
}
