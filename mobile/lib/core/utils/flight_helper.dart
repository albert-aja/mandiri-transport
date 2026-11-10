import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class FlightHelper {
  static String resolveAirlineName(String? flightNumber) {
    if (flightNumber == null || flightNumber.trim().isEmpty) {
      return 'Maskapai Penerbangan';
    }
    final norm = flightNumber.replaceAll(RegExp(r'[^A-Za-z0-9]'), '').toUpperCase();
    if (norm.startsWith('GA') || norm.startsWith('GIA')) return 'Garuda Indonesia';
    if (norm.startsWith('JT') || norm.startsWith('LNI')) return 'Lion Air';
    if (norm.startsWith('ID') || norm.startsWith('BTK')) return 'Batik Air';
    if (norm.startsWith('QG') || norm.startsWith('CTV')) return 'Citilink';
    if (norm.startsWith('IU') || norm.startsWith('SJV')) return 'Super Air Jet';
    if (norm.startsWith('QZ') || norm.startsWith('AWQ')) return 'AirAsia Indonesia';
    if (norm.startsWith('IW') || norm.startsWith('WON')) return 'Wings Air';
    if (norm.startsWith('IP') || norm.startsWith('BJV')) return 'Pelita Air';
    if (norm.startsWith('MH') || norm.startsWith('MAS')) return 'Malaysia Airlines';
    if (norm.startsWith('SQ') || norm.startsWith('SIA')) return 'Singapore Airlines';
    if (norm.startsWith('AK')) return 'AirAsia';
    if (norm.startsWith('TR') || norm.startsWith('TGW')) return 'Scoot';
    if (norm.startsWith('SJ') || norm.startsWith('SJY')) return 'Sriwijaya Air';
    if (norm.startsWith('IN') || norm.startsWith('NAM')) return 'NAM Air';
    return 'Penerbangan';
  }

  static String resolveAirportName(String code) {
    final upper = code.trim().toUpperCase();
    return switch (upper) {
      'CGK' => 'Jakarta (CGK)',
      'HLP' => 'Jakarta Halim (HLP)',
      'KNO' => 'Medan (KNO)',
      'BTH' => 'Batam (BTH)',
      'PDG' => 'Padang (PDG)',
      'PKU' => 'Pekanbaru (PKU)',
      'BTJ' => 'Banda Aceh (BTJ)',
      'SUB' => 'Surabaya (SUB)',
      'DPS' => 'Bali (DPS)',
      'YIA' => 'Yogyakarta (YIA)',
      'SIN' => 'Singapura (SIN)',
      'KUL' => 'Kuala Lumpur (KUL)',
      'PEN' => 'Penang (PEN)',
      _ => upper,
    };
  }

  static ({String origin, String destination}) resolveFlightRoute(
    String? flightNumber, {
    String? tripType,
    String? originAirport,
    String? destinationAirport,
  }) {
    if (originAirport != null && destinationAirport != null) {
      return (origin: originAirport, destination: destinationAirport);
    }
    final isPickup = tripType == 'AIRPORT_PICKUP';
    final norm = (flightNumber ?? '')
        .replaceAll(RegExp(r'[^A-Za-z0-9]'), '')
        .toUpperCase();

    if (norm.startsWith('MH')) {
      return isPickup
          ? (origin: 'KUL', destination: 'KNO')
          : (origin: 'KNO', destination: 'KUL');
    }
    if (norm.startsWith('SQ')) {
      return isPickup
          ? (origin: 'SIN', destination: 'KNO')
          : (origin: 'KNO', destination: 'SIN');
    }
    return isPickup
        ? (origin: 'CGK', destination: 'KNO')
        : (origin: 'KNO', destination: 'CGK');
  }

  static String statusLabel(String? status, {int delayMinutes = 0}) {
    final s = (status ?? 'SCHEDULED').toUpperCase();
    if (s == 'DELAYED' || delayMinutes > 0) {
      return delayMinutes > 0
          ? 'Terlambat (+$delayMinutes mnt)'
          : 'Terlambat (Delayed)';
    }
    return switch (s) {
      'LANDED' => 'Mendarat (Landed)',
      'IN_AIR' => 'Sedang Terbang (In Air)',
      'BOARDING' => 'Boarding di Asal',
      'CANCELLED' => 'Dibatalkan (Cancelled)',
      'DIVERTED' => 'Dialihkan (Diverted)',
      'SCHEDULED' => 'Tepat Waktu (On Time)',
      _ => s,
    };
  }

  static Color statusFg(String? status, {int delayMinutes = 0}) {
    final s = (status ?? 'SCHEDULED').toUpperCase();
    if (s == 'DELAYED' || delayMinutes > 0) return AppColors.amber;
    return switch (s) {
      'LANDED' => AppColors.emerald,
      'IN_AIR' || 'BOARDING' => AppColors.infoBlue,
      'CANCELLED' || 'DIVERTED' => AppColors.crimson,
      _ => AppColors.navy,
    };
  }

  static Color statusBg(String? status, {int delayMinutes = 0}) {
    final s = (status ?? 'SCHEDULED').toUpperCase();
    if (s == 'DELAYED' || delayMinutes > 0) return AppColors.amberSoft;
    return switch (s) {
      'LANDED' => AppColors.emeraldSoft,
      'IN_AIR' || 'BOARDING' => AppColors.infoSoft,
      'CANCELLED' || 'DIVERTED' => AppColors.crimsonSoft,
      _ => AppColors.slateBg,
    };
  }

  static IconData statusIcon(String? status, {int delayMinutes = 0}) {
    final s = (status ?? 'SCHEDULED').toUpperCase();
    if (s == 'DELAYED' || delayMinutes > 0) {
      return Icons.warning_amber_rounded;
    }
    return switch (s) {
      'LANDED' => Icons.flight_land_rounded,
      'IN_AIR' => Icons.airplanemode_active_rounded,
      'BOARDING' => Icons.departure_board_rounded,
      'CANCELLED' => Icons.cancel_outlined,
      'DIVERTED' => Icons.alt_route_rounded,
      _ => Icons.schedule_rounded,
    };
  }
}
