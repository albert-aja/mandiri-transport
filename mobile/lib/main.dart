import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/notifications/fcm_push_service.dart';
import 'core/theme/app_theme.dart';
import 'data/local/offline_store.dart';
import 'data/state/app_controller.dart';
import 'features/auth/login_screen.dart';
import 'features/driver/driver_shell.dart';
import 'features/owner/owner_shell.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  FirebaseLivePushClient.registerBackgroundHandlerSafely();
  final store = await OfflineStore.create();
  runApp(
    ProviderScope(
      overrides: [offlineStoreProvider.overrideWithValue(store)],
      child: const MandiriTransportApp(),
    ),
  );
}

class MandiriTransportApp extends ConsumerWidget {
  const MandiriTransportApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final session = ref.watch(appControllerProvider.select((s) => s.session));

    return MaterialApp(
      title: 'Mandiri Transport Medan',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      home: switch (session?.role) {
        'OWNER' => const OwnerShell(),
        'DRIVER' => const DriverShell(),
        _ => const LoginScreen(),
      },
    );
  }
}
