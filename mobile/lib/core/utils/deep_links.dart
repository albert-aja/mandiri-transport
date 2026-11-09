import 'package:url_launcher/url_launcher.dart';

class DeepLinks {
  /// Normalizes Indonesian phone numbers (`0812...`, `+62812...`, `62 812-...`) into `62812...` for `wa.me`.
  static String normalizeIndonesianPhone(String rawPhone) {
    final cleaned = rawPhone.replaceAll(RegExp(r'[^0-9+]'), '');
    if (cleaned.isEmpty) return '';
    if (cleaned.startsWith('+62')) {
      return cleaned.substring(1);
    }
    if (cleaned.startsWith('62')) {
      return cleaned;
    }
    if (cleaned.startsWith('0')) {
      return '62${cleaned.substring(1)}';
    }
    return '62$cleaned';
  }

  /// Zero-cost Google Maps turn-by-turn navigation deep link.
  static String buildGoogleMapsNavigationUrl({
    required String destination,
    double? latitude,
    double? longitude,
  }) {
    final target = (latitude != null && longitude != null)
        ? '$latitude,$longitude'
        : destination.trim();
    final encoded = Uri.encodeComponent(target);
    return 'https://www.google.com/maps/dir/?api=1&destination=$encoded';
  }

  /// Zero-cost Google Maps location search/pin deep link.
  static String buildGoogleMapsSearchUrl({
    required String query,
    double? latitude,
    double? longitude,
  }) {
    final target = (latitude != null && longitude != null)
        ? '$latitude,$longitude'
        : query.trim();
    final encoded = Uri.encodeComponent(target);
    return 'https://www.google.com/maps/search/?api=1&query=$encoded';
  }

  /// Zero-cost WhatsApp click-to-chat URL (`https://wa.me/628...?text=...`).
  /// Strictly opens chat draft; never auto-sends messages.
  static String buildWhatsappUrl({
    required String phone,
    String? message,
  }) {
    final normalized = normalizeIndonesianPhone(phone);
    if (message == null || message.trim().isEmpty) {
      return 'https://wa.me/$normalized';
    }
    final encodedText = Uri.encodeComponent(message.trim());
    return 'https://wa.me/$normalized?text=$encodedText';
  }

  /// Renders template placeholders (`{customer_name}`, `{pickup_time}`, etc.).
  static String renderWhatsappTemplate(
    String templateBody,
    Map<String, String> variables,
  ) {
    var output = templateBody;
    variables.forEach((key, value) {
      output = output.replaceAll('{$key}', value);
    });
    return output;
  }

  /// Opens external browser/app for Google Maps or WhatsApp safely.
  static Future<bool> launchExternal(String url) async {
    try {
      final uri = Uri.parse(url);
      return await launchUrl(uri, mode: LaunchMode.externalApplication);
    } catch (_) {
      return false;
    }
  }
}
