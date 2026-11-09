import 'package:flutter/services.dart';

class AppFormatters {
  static const List<String> _monthsShortId = [
    'Jan',
    'Feb',
    'Mar',
    'Apr',
    'Mei',
    'Jun',
    'Jul',
    'Agu',
    'Sep',
    'Okt',
    'Nov',
    'Des',
  ];

  static const List<String> _daysShortId = [
    'Sen',
    'Sel',
    'Rab',
    'Kam',
    'Jum',
    'Sab',
    'Min',
  ];

  /// Formats number as Indonesian Rupiah, e.g. `Rp 220.000` or `-Rp 50.000`.
  static String rupiah(num? amount) {
    if (amount == null) return 'Rp 0';
    final rounded = amount.round();
    final isNegative = rounded < 0;
    final digits = rounded.abs().toString();
    final formatted = _insertThousandDots(digits);
    return isNegative ? '-Rp $formatted' : 'Rp $formatted';
  }

  /// Formats integer with dot thousand separators, e.g. `220.000`.
  static String thousandNumber(num? value) {
    if (value == null) return '0';
    final rounded = value.round();
    final isNegative = rounded < 0;
    final formatted = _insertThousandDots(rounded.abs().toString());
    return isNegative ? '-$formatted' : formatted;
  }

  /// Parses Rupiah or dotted string (`Rp 220.000` or `220.000`) into double.
  static double parseRupiah(String raw) {
    final digitsOnly = raw.replaceAll(RegExp(r'[^0-9]'), '');
    if (digitsOnly.isEmpty) return 0;
    return double.tryParse(digitsOnly) ?? 0;
  }

  static String _insertThousandDots(String digits) {
    final buffer = StringBuffer();
    final length = digits.length;
    for (int i = 0; i < length; i++) {
      if (i > 0 && (length - i) % 3 == 0) {
        buffer.write('.');
      }
      buffer.write(digits[i]);
    }
    return buffer.toString();
  }

  /// Formats ISO-8601 timestamp or DateTime into WIB display, e.g. `Sen, 9 Nov 2026 • 21:00 WIB`.
  static String dateTimeWib(String? isoString) {
    if (isoString == null || isoString.isEmpty) return '-';
    final parsed = DateTime.tryParse(isoString);
    if (parsed == null) return isoString;
    final wib = parsed.toUtc().add(const Duration(hours: 7));
    final dayName = _daysShortId[wib.weekday - 1];
    final monthName = _monthsShortId[wib.month - 1];
    final hh = wib.hour.toString().padLeft(2, '0');
    final mm = wib.minute.toString().padLeft(2, '0');
    return '$dayName, ${wib.day} $monthName ${wib.year} • $hh:$mm WIB';
  }

  /// Formats ISO-8601 timestamp into short date, e.g. `9 Nov 2026`.
  static String shortDateId(String? isoOrDateString) {
    if (isoOrDateString == null || isoOrDateString.isEmpty) return '-';
    final parsed = DateTime.tryParse(isoOrDateString);
    if (parsed == null) return isoOrDateString;
    final monthName = _monthsShortId[parsed.month - 1];
    return '${parsed.day} $monthName ${parsed.year}';
  }

  /// Formats ISO-8601 timestamp into `21:00 WIB`.
  static String timeWib(String? isoString) {
    if (isoString == null || isoString.isEmpty) return '-';
    final parsed = DateTime.tryParse(isoString);
    if (parsed == null) return isoString;
    final wib = parsed.toUtc().add(const Duration(hours: 7));
    final hh = wib.hour.toString().padLeft(2, '0');
    final mm = wib.minute.toString().padLeft(2, '0');
    return '$hh:$mm WIB';
  }

  /// Relative staleness indicator for GPS or Sync, e.g. `Update GPS: 12 mnt lalu`.
  static String gpsStaleness(String? isoString, {DateTime? nowOverride}) {
    if (isoString == null || isoString.isEmpty) {
      return 'Belum ada sinyal GPS tercatat';
    }
    final recorded = DateTime.tryParse(isoString);
    if (recorded == null) return 'Update GPS: $isoString';
    final now = (nowOverride ?? DateTime.now()).toUtc();
    final diff = now.difference(recorded.toUtc());
    if (diff.inSeconds < 60) {
      return 'Update GPS: Baru saja';
    }
    if (diff.inMinutes < 60) {
      return 'Update GPS: ${diff.inMinutes} mnt lalu';
    }
    if (diff.inHours < 24) {
      return 'Update GPS: ${diff.inHours} jam lalu';
    }
    return 'Update GPS: ${diff.inDays} hari lalu';
  }

  static String relativeSyncLabel(String? isoString, {DateTime? nowOverride}) {
    if (isoString == null || isoString.isEmpty) {
      return 'Belum pernah sinkron';
    }
    final recorded = DateTime.tryParse(isoString);
    if (recorded == null) return isoString;
    final now = (nowOverride ?? DateTime.now()).toUtc();
    final diff = now.difference(recorded.toUtc());
    if (diff.inSeconds < 60) return 'Baru saja';
    if (diff.inMinutes < 60) return '${diff.inMinutes} mnt lalu';
    if (diff.inHours < 24) return '${diff.inHours} jam lalu';
    return '${diff.inDays} hari lalu';
  }
}

/// Formats numeric text field input with Indonesian dot thousand separators (`220.000`).
class RupiahInputFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final digits = newValue.text.replaceAll(RegExp(r'[^0-9]'), '');
    if (digits.isEmpty) {
      return const TextEditingValue(
        text: '',
        selection: TextSelection.collapsed(offset: 0),
      );
    }
    final number = int.tryParse(digits) ?? 0;
    final formatted = AppFormatters.thousandNumber(number);
    return TextEditingValue(
      text: formatted,
      selection: TextSelection.collapsed(offset: formatted.length),
    );
  }
}

class QuickDenominationOption {
  final String label;
  final double Function(double currentVal, double baseFare) computeNewAmount;

  const QuickDenominationOption({
    required this.label,
    required this.computeNewAmount,
  });
}

class DenominationPresets {
  static List<QuickDenominationOption> buildOptions({double? referenceFare}) {
    return [
      if (referenceFare != null && referenceFare > 0)
        QuickDenominationOption(
          label: 'Pas Sesuai Tarif (${AppFormatters.rupiah(referenceFare)})',
          computeNewAmount: (_, baseFare) => baseFare,
        ),
      QuickDenominationOption(
        label: '+Rp50rb',
        computeNewAmount: (current, _) => current + 50000,
      ),
      QuickDenominationOption(
        label: '+Rp100rb',
        computeNewAmount: (current, _) => current + 100000,
      ),
      QuickDenominationOption(
        label: 'Rp220rb',
        computeNewAmount: (_, _) => 220000,
      ),
      QuickDenominationOption(
        label: 'Rp250rb',
        computeNewAmount: (_, _) => 250000,
      ),
    ];
  }
}
