import 'dart:async';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../data/local/offline_store.dart';
import '../../data/models/models.dart';

class IncomingPushPayload {
  final String id;
  final String reminderType;
  final String title;
  final String body;
  final String? referenceType;
  final String? referenceId;

  const IncomingPushPayload({
    required this.id,
    required this.reminderType,
    required this.title,
    required this.body,
    this.referenceType,
    this.referenceId,
  });

  factory IncomingPushPayload.fromRemoteMessage(RemoteMessage message) {
    final data = message.data;
    return IncomingPushPayload(
      id: (data['notificationId']?.toString().isNotEmpty == true)
          ? data['notificationId']!.toString()
          : (message.messageId ??
                'fcm-${DateTime.now().millisecondsSinceEpoch}'),
      reminderType:
          (data['reminderType']?.toString().isNotEmpty == true)
          ? data['reminderType']!.toString()
          : 'UPCOMING_PICKUP',
      title:
          message.notification?.title ??
          data['title']?.toString() ??
          'Mandiri Transport',
      body:
          message.notification?.body ??
          data['body']?.toString() ??
          'Pembaruan operasional diterima.',
      referenceType: data['referenceType']?.toString(),
      referenceId: data['referenceId']?.toString(),
    );
  }
}

class FcmInitOutcome {
  final bool initialized;
  final bool permissionGranted;
  final String? token;
  final String platform;
  final String deviceLabel;
  final String statusMessage;

  const FcmInitOutcome({
    required this.initialized,
    required this.permissionGranted,
    this.token,
    required this.platform,
    required this.deviceLabel,
    required this.statusMessage,
  });
}

@pragma('vm:entry-point')
Future<void> mandiriFirebaseMessagingBackgroundHandler(
  RemoteMessage message,
) async {
  try {
    if (Firebase.apps.isEmpty) {
      await Firebase.initializeApp();
    }
  } catch (_) {}

  try {
    final prefs = await SharedPreferences.getInstance();
    final store = OfflineStore(prefs);
    final payload = IncomingPushPayload.fromRemoteMessage(message);
    await store.prependIncomingRemoteNotification(
      id: payload.id,
      reminderType: payload.reminderType,
      title: payload.title,
      body: payload.body,
      referenceType: payload.referenceType,
      referenceId: payload.referenceId,
    );
  } catch (_) {}
}

abstract class FcmPushClient {
  Stream<String> get onTokenRefresh;
  Stream<IncomingPushPayload> get onForegroundMessage;
  Stream<IncomingPushPayload> get onNotificationOpened;

  Future<FcmInitOutcome> initialize({FcmRuntimeConfig? runtimeConfig});
  void dispose();
}

class FirebaseLivePushClient implements FcmPushClient {
  final StreamController<String> _tokenRefreshCtrl =
      StreamController<String>.broadcast();
  final StreamController<IncomingPushPayload> _foregroundCtrl =
      StreamController<IncomingPushPayload>.broadcast();
  final StreamController<IncomingPushPayload> _openedCtrl =
      StreamController<IncomingPushPayload>.broadcast();

  StreamSubscription<String>? _tokenSub;
  StreamSubscription<RemoteMessage>? _msgSub;
  StreamSubscription<RemoteMessage>? _openSub;
  bool _listenersAttached = false;

  @override
  Stream<String> get onTokenRefresh => _tokenRefreshCtrl.stream;

  @override
  Stream<IncomingPushPayload> get onForegroundMessage => _foregroundCtrl.stream;

  @override
  Stream<IncomingPushPayload> get onNotificationOpened => _openedCtrl.stream;

  static void registerBackgroundHandlerSafely() {
    try {
      FirebaseMessaging.onBackgroundMessage(
        mandiriFirebaseMessagingBackgroundHandler,
      );
    } catch (_) {
      // Safe on unsupported desktop/test hosts
    }
  }

  String _detectPlatformCode() {
    if (kIsWeb) return 'WEB';
    return switch (defaultTargetPlatform) {
      TargetPlatform.android => 'ANDROID',
      TargetPlatform.iOS => 'IOS',
      _ => 'OTHER',
    };
  }

  @override
  Future<FcmInitOutcome> initialize({FcmRuntimeConfig? runtimeConfig}) async {
    final platformCode = _detectPlatformCode();
    final deviceLabel = 'Mandiri Flutter ($platformCode)';

    try {
      if (Firebase.apps.isEmpty) {
        if (runtimeConfig != null && runtimeConfig.isComplete) {
          await Firebase.initializeApp(
            options: FirebaseOptions(
              apiKey: runtimeConfig.apiKey.trim(),
              appId: runtimeConfig.appId.trim(),
              messagingSenderId: runtimeConfig.messagingSenderId.trim(),
              projectId: runtimeConfig.projectId.trim(),
            ),
          );
        } else {
          await Firebase.initializeApp();
        }
      }
    } catch (_) {
      return FcmInitOutcome(
        initialized: false,
        permissionGranted: false,
        platform: platformCode,
        deviceLabel: deviceLabel,
        statusMessage:
            'Mode Siaga Lokal: Tambahkan google-services.json (Android) / GoogleService-Info.plist (iOS) atau isi FirebaseOptions untuk mengaktifkan push latar belakang.',
      );
    }

    try {
      final messaging = FirebaseMessaging.instance;
      final settings = await messaging.requestPermission(
        alert: true,
        badge: true,
        sound: true,
        provisional: false,
      );
      final granted =
          settings.authorizationStatus == AuthorizationStatus.authorized ||
          settings.authorizationStatus == AuthorizationStatus.provisional;

      await messaging.setForegroundNotificationPresentationOptions(
        alert: true,
        badge: true,
        sound: true,
      );

      String? latestRegisteredId;
      final firstTokenCompleter = Completer<String?>();

      if (!_listenersAttached) {
        _listenersAttached = true;
        _tokenSub = messaging.onRegistered.listen((registeredId) {
          final cleaned = registeredId.trim();
          if (cleaned.isNotEmpty) {
            latestRegisteredId = cleaned;
            if (!firstTokenCompleter.isCompleted) {
              firstTokenCompleter.complete(cleaned);
            }
            _tokenRefreshCtrl.add(cleaned);
          }
        });
        _msgSub = FirebaseMessaging.onMessage.listen((msg) {
          _foregroundCtrl.add(IncomingPushPayload.fromRemoteMessage(msg));
        });
        _openSub = FirebaseMessaging.onMessageOpenedApp.listen((msg) {
          _openedCtrl.add(IncomingPushPayload.fromRemoteMessage(msg));
        });

        final initialMsg = await messaging.getInitialMessage();
        if (initialMsg != null) {
          Future.microtask(
            () => _openedCtrl.add(
              IncomingPushPayload.fromRemoteMessage(initialMsg),
            ),
          );
        }
      }

      final vapid = runtimeConfig?.vapidKey?.trim();
      await messaging.register(
        vapidKey: (vapid != null && vapid.isNotEmpty) ? vapid : null,
      );

      final token =
          latestRegisteredId ??
          await firstTokenCompleter.future.timeout(
            const Duration(milliseconds: 600),
            onTimeout: () => null,
          );

      return FcmInitOutcome(
        initialized: true,
        permissionGranted: granted,
        token: token,
        platform: platformCode,
        deviceLabel: deviceLabel,
        statusMessage: granted
            ? (token != null && token.isNotEmpty
                  ? 'FCM Perangkat Aktif — token siap menerima push real-time.'
                  : 'FCM Terinisialisasi — menunggu pembuatan token perangkat.')
            : 'Izin notifikasi belum diberikan pada perangkat ini.',
      );
    } catch (e) {
      return FcmInitOutcome(
        initialized: false,
        permissionGranted: false,
        platform: platformCode,
        deviceLabel: deviceLabel,
        statusMessage:
            'Inisialisasi FCM tertunda (${e.runtimeType}) — kotak masuk aplikasi tetap aktif.',
      );
    }
  }

  @override
  void dispose() {
    _tokenSub?.cancel();
    _msgSub?.cancel();
    _openSub?.cancel();
    _tokenRefreshCtrl.close();
    _foregroundCtrl.close();
    _openedCtrl.close();
  }
}
