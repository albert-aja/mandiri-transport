import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/api_client.dart';
import '../../core/theme/app_theme.dart';
import '../../data/state/app_controller.dart';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _tokenController = TextEditingController();
  String _selectedJwtRole = 'OWNER';
  bool _showJwtInput = false;

  @override
  void dispose() {
    _tokenController.dispose();
    super.dispose();
  }

  void _openServerConfigSheet(BuildContext context, String currentUrl) {
    final urlCtrl = TextEditingController(text: currentUrl);
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
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Konfigurasi Server API Backend',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800),
              ),
              const SizedBox(height: 6),
              const Text(
                ' Aplikasi tetap berfungsi penuh secara Offline jika server lokal belum dinyalakan.',
                style: TextStyle(fontSize: 13, color: AppColors.textSecondary),
              ),
              const SizedBox(height: 14),
              TextField(
                controller: urlCtrl,
                decoration: const InputDecoration(
                  labelText: 'Alamat Base URL (/api/v1)',
                  hintText: 'http://localhost:8080/api/v1',
                  prefixIcon: Icon(Icons.dns_rounded),
                ),
              ),
              const SizedBox(height: 10),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  ActionChip(
                    label: const Text('Localhost (Desktop/Web)'),
                    onPressed: () => urlCtrl.text = ApiClient.defaultBaseUrl,
                  ),
                  ActionChip(
                    label: const Text('Android Emulator (10.0.2.2)'),
                    onPressed: () =>
                        urlCtrl.text = 'http://10.0.2.2:8080/api/v1',
                  ),
                ],
              ),
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: () async {
                    await ref
                        .read(appControllerProvider.notifier)
                        .updateServerUrl(urlCtrl.text);
                    if (ctx.mounted) Navigator.pop(ctx);
                  },
                  icon: const Icon(Icons.save_rounded),
                  label: const Text('Simpan Alamat Server'),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final appState = ref.watch(appControllerProvider);
    final controller = ref.read(appControllerProvider.notifier);

    final quickAccounts = [
      (
        username: 'hendra',
        title: 'Pak Hendra — Pemilik/Dispatcher',
        subtitle: 'Jadwal, Simulasi Bentrok, Kas Supir, Armada & Analitik BI',
        badge: 'PEMILIK',
        color: AppColors.navy,
        icon: Icons.admin_panel_settings_rounded,
      ),
      (
        username: 'budi',
        title: 'Bang Budi — Supir',
        subtitle: 'Tugas Aktif: Jemput Bandara KNO (GA-190 • Ibu Ratna)',
        badge: 'SUPIR',
        color: AppColors.emerald,
        icon: Icons.directions_car_filled_rounded,
      ),
      (
        username: 'andi',
        title: 'Bang Andi — Supir',
        subtitle: 'Tugas Terjadwal: Medan -> Kuala Tanjung (PT Sawit Makmur)',
        badge: 'SUPIR',
        color: AppColors.emerald,
        icon: Icons.route_rounded,
      ),
      (
        username: 'rudi',
        title: 'Bang Rudi — Supir',
        subtitle: 'Spesialis Toyota Hiace Rombongan & Wisata Berastagi',
        badge: 'SUPIR',
        color: AppColors.emerald,
        icon: Icons.airport_shuttle_rounded,
      ),
    ];

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 560),
            child: ListView(
              padding: const EdgeInsets.all(20),
              children: [
                Container(
                  padding: const EdgeInsets.all(22),
                  decoration: BoxDecoration(
                    gradient: const LinearGradient(
                      colors: [AppColors.navy, AppColors.navyDark],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                    borderRadius: BorderRadius.circular(18),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Container(
                            padding: const EdgeInsets.all(10),
                            decoration: BoxDecoration(
                              color: Colors.white.withValues(alpha: 0.14),
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: const Icon(
                              Icons.local_taxi_rounded,
                              color: Colors.white,
                              size: 28,
                            ),
                          ),
                          const SizedBox(width: 12),
                          const Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  'MANDIRI TRANSPORT',
                                  style: TextStyle(
                                    color: Colors.white,
                                    fontSize: 20,
                                    fontWeight: FontWeight.w900,
                                    letterSpacing: 0.5,
                                  ),
                                ),
                                Text(
                                  'Sistem Operasional, Dispatch & Kas Armada Medan',
                                  style: TextStyle(
                                    color: Colors.white70,
                                    fontSize: 13,
                                    fontWeight: FontWeight.w500,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 14),
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: [
                          _HeaderPill(
                            icon: Icons.offline_bolt_rounded,
                            text: appState.effectiveOnline
                                ? 'Siap Sinkron (${appState.baseUrl})'
                                : 'Mode Offline Lokal Aktif',
                          ),
                          const _HeaderPill(
                            icon: Icons.shield_outlined,
                            text: 'Murni Manajemen Operasional & Kas',
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 20),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Expanded(
                      child: Text(
                        'Pilih Akun Operasional (1-Tap Masuk)',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w800,
                          color: AppColors.textPrimary,
                        ),
                      ),
                    ),
                    TextButton.icon(
                      onPressed: () =>
                          _openServerConfigSheet(context, appState.baseUrl),
                      icon: const Icon(Icons.settings_ethernet, size: 18),
                      label: const Text('Server URL'),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                ...quickAccounts.map((acc) {
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 10),
                    child: Card(
                      child: InkWell(
                        borderRadius: BorderRadius.circular(14),
                        onTap: () => controller.loginDemoAccount(acc.username),
                        child: Padding(
                          padding: const EdgeInsets.all(15),
                          child: Row(
                            children: [
                              CircleAvatar(
                                radius: 24,
                                backgroundColor: acc.color.withValues(
                                  alpha: 0.12,
                                ),
                                child: Icon(acc.icon, color: acc.color),
                              ),
                              const SizedBox(width: 14),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      acc.title,
                                      style: const TextStyle(
                                        fontSize: 15.5,
                                        fontWeight: FontWeight.w800,
                                        color: AppColors.textPrimary,
                                      ),
                                    ),
                                    const SizedBox(height: 3),
                                    Text(
                                      acc.subtitle,
                                      style: const TextStyle(
                                        fontSize: 12.5,
                                        color: AppColors.textSecondary,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              const SizedBox(width: 8),
                              const Icon(
                                Icons.chevron_right_rounded,
                                color: AppColors.navy,
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                  );
                }),
                const SizedBox(height: 8),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(14),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        InkWell(
                          onTap: () =>
                              setState(() => _showJwtInput = !_showJwtInput),
                          child: Row(
                            children: [
                              const Icon(
                                Icons.vpn_key_outlined,
                                size: 20,
                                color: AppColors.navy,
                              ),
                              const SizedBox(width: 10),
                              const Expanded(
                                child: Text(
                                  'Masuk dengan Token Supabase JWT / Username',
                                  style: TextStyle(
                                    fontWeight: FontWeight.w700,
                                    fontSize: 14,
                                  ),
                                ),
                              ),
                              Icon(
                                _showJwtInput
                                    ? Icons.expand_less
                                    : Icons.expand_more,
                              ),
                            ],
                          ),
                        ),
                        if (_showJwtInput) ...[
                          const SizedBox(height: 12),
                          TextField(
                            controller: _tokenController,
                            decoration: const InputDecoration(
                              labelText: 'Username atau Bearer JWT Token',
                              hintText: 'Contoh: hendra / eyJhbGciOi...',
                            ),
                          ),
                          const SizedBox(height: 10),
                          SegmentedButton<String>(
                            segments: const [
                              ButtonSegment(
                                value: 'OWNER',
                                label: Text('Pemilik / Dispatcher'),
                              ),
                              ButtonSegment(
                                value: 'DRIVER',
                                label: Text('Supir'),
                              ),
                            ],
                            selected: {_selectedJwtRole},
                            onSelectionChanged: (s) =>
                                setState(() => _selectedJwtRole = s.first),
                          ),
                          const SizedBox(height: 12),
                          SizedBox(
                            width: double.infinity,
                            child: FilledButton(
                              onPressed: () =>
                                  controller.loginWithTokenOrUsername(
                                    credential: _tokenController.text,
                                    fallbackRole: _selectedJwtRole,
                                  ),
                              child: const Text('Masuk Sekarang'),
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _HeaderPill extends StatelessWidget {
  final IconData icon;
  final String text;

  const _HeaderPill({required this.icon, required this.text});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: Colors.white),
          const SizedBox(width: 6),
          Flexible(
            child: Text(
              text,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 12,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
