double asDouble(dynamic value, [double fallback = 0]) {
  if (value == null) return fallback;
  if (value is num) return value.toDouble();
  if (value is String) return double.tryParse(value) ?? fallback;
  return fallback;
}

double? asNullableDouble(dynamic value) {
  if (value == null) return null;
  if (value is num) return value.toDouble();
  if (value is String) return double.tryParse(value);
  return null;
}

int asInt(dynamic value, [int fallback = 0]) {
  if (value == null) return fallback;
  if (value is int) return value;
  if (value is num) return value.round();
  if (value is String) return int.tryParse(value) ?? fallback;
  return fallback;
}

int? asNullableInt(dynamic value) {
  if (value == null) return null;
  if (value is int) return value;
  if (value is num) return value.round();
  if (value is String) return int.tryParse(value);
  return null;
}

bool asBool(dynamic value, [bool fallback = false]) {
  if (value == null) return fallback;
  if (value is bool) return value;
  if (value is String) return value.toLowerCase() == 'true';
  return fallback;
}

class UserSession {
  final String id;
  final String username;
  final String fullName;
  final String phone;
  final String role; // OWNER | DRIVER
  final String? driverId;
  final String token;

  const UserSession({
    required this.id,
    required this.username,
    required this.fullName,
    required this.phone,
    required this.role,
    this.driverId,
    required this.token,
  });

  bool get isOwner => role == 'OWNER';
  bool get isDriver => role == 'DRIVER';

  factory UserSession.fromJson(Map<String, dynamic> json, {String? tokenOverride}) {
    final userMap = json['user'] is Map<String, dynamic>
        ? json['user'] as Map<String, dynamic>
        : json;
    return UserSession(
      id: (userMap['id'] as String?) ?? '',
      username: (userMap['username'] as String?) ?? '',
      fullName: (userMap['fullName'] as String?) ?? '',
      phone: (userMap['phone'] as String?) ?? '',
      role: (userMap['role'] as String?) ?? 'DRIVER',
      driverId: userMap['driverId'] as String?,
      token: tokenOverride ?? (json['token'] as String?) ?? '',
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'username': username,
    'fullName': fullName,
    'phone': phone,
    'role': role,
    'driverId': driverId,
    'token': token,
  };
}

class CustomerAddress {
  final String id;
  final String label;
  final String addressText;
  final double? latitude;
  final double? longitude;
  final String googleMapsUrl;
  final bool isDefault;

  const CustomerAddress({
    required this.id,
    required this.label,
    required this.addressText,
    this.latitude,
    this.longitude,
    required this.googleMapsUrl,
    this.isDefault = false,
  });

  factory CustomerAddress.fromJson(Map<String, dynamic> json) => CustomerAddress(
    id: (json['id'] as String?) ?? '',
    label: (json['label'] as String?) ?? 'Alamat',
    addressText: (json['addressText'] as String?) ?? '',
    latitude: asNullableDouble(json['latitude']),
    longitude: asNullableDouble(json['longitude']),
    googleMapsUrl: (json['googleMapsUrl'] as String?) ?? '',
    isDefault: asBool(json['isDefault']),
  );

  Map<String, dynamic> toJson() => {
    'id': id,
    'label': label,
    'addressText': addressText,
    'latitude': latitude,
    'longitude': longitude,
    'googleMapsUrl': googleMapsUrl,
    'isDefault': isDefault,
  };
}

class CustomerNote {
  final String id;
  final String category; // PREFERENCE | OPERATIONAL | LUGGAGE | TIMING
  final String noteText;
  final bool visibleToDrivers;

  const CustomerNote({
    required this.id,
    required this.category,
    required this.noteText,
    this.visibleToDrivers = true,
  });

  factory CustomerNote.fromJson(Map<String, dynamic> json) => CustomerNote(
    id: (json['id'] as String?) ?? '',
    category: (json['category'] as String?) ?? 'OPERATIONAL',
    noteText: (json['noteText'] as String?) ?? '',
    visibleToDrivers: asBool(json['visibleToDrivers'], true),
  );

  Map<String, dynamic> toJson() => {
    'id': id,
    'category': category,
    'noteText': noteText,
    'visibleToDrivers': visibleToDrivers,
  };
}

class CustomerPricingAgreement {
  final String id;
  final String customerId;
  final String agreementName;
  final String? routeId;
  final String? vehicleType;
  final String pricingMode; // FIXED_FARE | DISCOUNT_PERCENT
  final double? agreedFare;
  final double? discountPercent;
  final String? effectiveFrom;
  final String? effectiveTo;
  final String? conditionsNote;

  const CustomerPricingAgreement({
    required this.id,
    required this.customerId,
    required this.agreementName,
    this.routeId,
    this.vehicleType,
    required this.pricingMode,
    this.agreedFare,
    this.discountPercent,
    this.effectiveFrom,
    this.effectiveTo,
    this.conditionsNote,
  });

  factory CustomerPricingAgreement.fromJson(Map<String, dynamic> json) =>
      CustomerPricingAgreement(
        id: (json['id'] as String?) ?? '',
        customerId: (json['customerId'] as String?) ?? '',
        agreementName: (json['agreementName'] as String?) ?? '',
        routeId: json['routeId'] as String?,
        vehicleType: json['vehicleType'] as String?,
        pricingMode: (json['pricingMode'] as String?) ?? 'FIXED_FARE',
        agreedFare: asNullableDouble(json['agreedFare']),
        discountPercent: asNullableDouble(json['discountPercent']),
        effectiveFrom: json['effectiveFrom'] as String?,
        effectiveTo: json['effectiveTo'] as String?,
        conditionsNote: json['conditionsNote'] as String?,
      );

  Map<String, dynamic> toJson() => {
    'id': id,
    'customerId': customerId,
    'agreementName': agreementName,
    'routeId': routeId,
    'vehicleType': vehicleType,
    'pricingMode': pricingMode,
    'agreedFare': agreedFare,
    'discountPercent': discountPercent,
    'effectiveFrom': effectiveFrom,
    'effectiveTo': effectiveTo,
    'conditionsNote': conditionsNote,
  };
}

class Customer {
  final String id;
  final String name;
  final String phone;
  final String whatsappPhone;
  final String? companyName;
  final String? preferredVehicleType;
  final List<CustomerAddress> addresses;
  final List<CustomerNote> notes;
  final List<CustomerPricingAgreement> pricingAgreements;

  const Customer({
    required this.id,
    required this.name,
    required this.phone,
    required this.whatsappPhone,
    this.companyName,
    this.preferredVehicleType,
    this.addresses = const [],
    this.notes = const [],
    this.pricingAgreements = const [],
  });

  CustomerAddress? get defaultAddress {
    if (addresses.isEmpty) return null;
    for (final addr in addresses) {
      if (addr.isDefault) return addr;
    }
    return addresses.first;
  }

  Customer copyWith({
    String? name,
    String? phone,
    String? whatsappPhone,
    String? companyName,
    String? preferredVehicleType,
    List<CustomerAddress>? addresses,
    List<CustomerNote>? notes,
    List<CustomerPricingAgreement>? pricingAgreements,
  }) {
    return Customer(
      id: id,
      name: name ?? this.name,
      phone: phone ?? this.phone,
      whatsappPhone: whatsappPhone ?? this.whatsappPhone,
      companyName: companyName ?? this.companyName,
      preferredVehicleType: preferredVehicleType ?? this.preferredVehicleType,
      addresses: addresses ?? this.addresses,
      notes: notes ?? this.notes,
      pricingAgreements: pricingAgreements ?? this.pricingAgreements,
    );
  }

  factory Customer.fromJson(Map<String, dynamic> json) => Customer(
    id: (json['id'] as String?) ?? '',
    name: (json['name'] as String?) ?? '',
    phone: (json['phone'] as String?) ?? '',
    whatsappPhone:
        (json['whatsappPhone'] as String?) ?? (json['phone'] as String?) ?? '',
    companyName: json['companyName'] as String?,
    preferredVehicleType: json['preferredVehicleType'] as String?,
    addresses: (json['addresses'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>()
        .map(CustomerAddress.fromJson)
        .toList(),
    notes: (json['notes'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>()
        .map(CustomerNote.fromJson)
        .toList(),
    pricingAgreements: (json['pricingAgreements'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>()
        .map(CustomerPricingAgreement.fromJson)
        .toList(),
  );

  Map<String, dynamic> toJson() => {
    'id': id,
    'name': name,
    'phone': phone,
    'whatsappPhone': whatsappPhone,
    'companyName': companyName,
    'preferredVehicleType': preferredVehicleType,
    'addresses': addresses.map((e) => e.toJson()).toList(),
    'notes': notes.map((e) => e.toJson()).toList(),
    'pricingAgreements': pricingAgreements.map((e) => e.toJson()).toList(),
  };
}

class WhatsappTemplate {
  final String id;
  final String code;
  final String title;
  final String category;
  final String bodyTemplate;

  const WhatsappTemplate({
    required this.id,
    required this.code,
    required this.title,
    required this.category,
    required this.bodyTemplate,
  });

  factory WhatsappTemplate.fromJson(Map<String, dynamic> json) =>
      WhatsappTemplate(
        id: (json['id'] as String?) ?? '',
        code: (json['code'] as String?) ?? '',
        title: (json['title'] as String?) ?? '',
        category: (json['category'] as String?) ?? 'GENERAL',
        bodyTemplate: (json['bodyTemplate'] as String?) ?? '',
      );

  Map<String, dynamic> toJson() => {
    'id': id,
    'code': code,
    'title': title,
    'category': category,
    'bodyTemplate': bodyTemplate,
  };
}

class RouteInfo {
  final String id;
  final String code;
  final String name;
  final String originName;
  final String destinationName;
  final double typicalDistanceKm;
  final int defaultDurationMin;
  final int defaultPrepBufferMin;
  final int defaultArrivalBufferMin;
  final int defaultRepositionBufferMin;
  final bool isLongDistance;
  final int requiredRestAfterMin;

  const RouteInfo({
    required this.id,
    required this.code,
    required this.name,
    required this.originName,
    required this.destinationName,
    required this.typicalDistanceKm,
    required this.defaultDurationMin,
    required this.defaultPrepBufferMin,
    required this.defaultArrivalBufferMin,
    required this.defaultRepositionBufferMin,
    this.isLongDistance = false,
    this.requiredRestAfterMin = 0,
  });

  factory RouteInfo.fromJson(Map<String, dynamic> json) => RouteInfo(
    id: (json['id'] as String?) ?? '',
    code: (json['code'] as String?) ?? '',
    name: (json['name'] as String?) ?? '',
    originName: (json['originName'] as String?) ?? '',
    destinationName: (json['destinationName'] as String?) ?? '',
    typicalDistanceKm: asDouble(json['typicalDistanceKm'], 35),
    defaultDurationMin: asInt(json['defaultDurationMin'], 60),
    defaultPrepBufferMin: asInt(json['defaultPrepBufferMin'], 15),
    defaultArrivalBufferMin: asInt(json['defaultArrivalBufferMin'], 0),
    defaultRepositionBufferMin: asInt(json['defaultRepositionBufferMin'], 15),
    isLongDistance: asBool(json['isLongDistance']),
    requiredRestAfterMin: asInt(json['requiredRestAfterMin']),
  );

  Map<String, dynamic> toJson() => {
    'id': id,
    'code': code,
    'name': name,
    'originName': originName,
    'destinationName': destinationName,
    'typicalDistanceKm': typicalDistanceKm,
    'defaultDurationMin': defaultDurationMin,
    'defaultPrepBufferMin': defaultPrepBufferMin,
    'defaultArrivalBufferMin': defaultArrivalBufferMin,
    'defaultRepositionBufferMin': defaultRepositionBufferMin,
    'isLongDistance': isLongDistance,
    'requiredRestAfterMin': requiredRestAfterMin,
  };
}

class Driver {
  final String id;
  final String? userId;
  final String driverCode;
  final String fullName;
  final String phone;
  final String licenseNumber;
  final String licenseExpiryDate;
  final String availabilityStatus; // AVAILABLE | UNAVAILABLE | ON_LEAVE | TEMPORARILY_UNAVAILABLE
  final double maxDailyHours;
  final String? notes;
  final double cashHeldAmount;

  const Driver({
    required this.id,
    this.userId,
    required this.driverCode,
    required this.fullName,
    required this.phone,
    required this.licenseNumber,
    required this.licenseExpiryDate,
    required this.availabilityStatus,
    this.maxDailyHours = 10.0,
    this.notes,
    this.cashHeldAmount = 0,
  });

  Driver copyWith({
    String? availabilityStatus,
    String? notes,
    double? cashHeldAmount,
  }) {
    return Driver(
      id: id,
      userId: userId,
      driverCode: driverCode,
      fullName: fullName,
      phone: phone,
      licenseNumber: licenseNumber,
      licenseExpiryDate: licenseExpiryDate,
      availabilityStatus: availabilityStatus ?? this.availabilityStatus,
      maxDailyHours: maxDailyHours,
      notes: notes ?? this.notes,
      cashHeldAmount: cashHeldAmount ?? this.cashHeldAmount,
    );
  }

  factory Driver.fromJson(Map<String, dynamic> json) => Driver(
    id: (json['id'] as String?) ?? '',
    userId: json['userId'] as String?,
    driverCode: (json['driverCode'] as String?) ?? '',
    fullName: (json['fullName'] as String?) ?? '',
    phone: (json['phone'] as String?) ?? '',
    licenseNumber: (json['licenseNumber'] as String?) ?? '',
    licenseExpiryDate: (json['licenseExpiryDate'] as String?) ?? '',
    availabilityStatus: (json['availabilityStatus'] as String?) ?? 'AVAILABLE',
    maxDailyHours: asDouble(json['maxDailyHours'], 10.0),
    notes: json['notes'] as String?,
    cashHeldAmount: asDouble(json['cashHeldAmount']),
  );

  Map<String, dynamic> toJson() => {
    'id': id,
    'userId': userId,
    'driverCode': driverCode,
    'fullName': fullName,
    'phone': phone,
    'licenseNumber': licenseNumber,
    'licenseExpiryDate': licenseExpiryDate,
    'availabilityStatus': availabilityStatus,
    'maxDailyHours': maxDailyHours,
    'notes': notes,
    'cashHeldAmount': cashHeldAmount,
  };
}

class VehicleDocument {
  final String id;
  final String docType; // ANNUAL_TAX_STNK | FIVE_YEAR_PLATE | INSURANCE | KIR
  final String docNumber;
  final String? issueDate;
  final String expiryDate;
  final int reminderDaysBefore;
  final String? notes;

  const VehicleDocument({
    required this.id,
    required this.docType,
    required this.docNumber,
    this.issueDate,
    required this.expiryDate,
    this.reminderDaysBefore = 30,
    this.notes,
  });

  factory VehicleDocument.fromJson(Map<String, dynamic> json) =>
      VehicleDocument(
        id: (json['id'] as String?) ?? '',
        docType: (json['docType'] as String?) ?? 'ANNUAL_TAX_STNK',
        docNumber: (json['docNumber'] as String?) ?? '',
        issueDate: json['issueDate'] as String?,
        expiryDate: (json['expiryDate'] as String?) ?? '',
        reminderDaysBefore: asInt(json['reminderDaysBefore'], 30),
        notes: json['notes'] as String?,
      );

  Map<String, dynamic> toJson() => {
    'id': id,
    'docType': docType,
    'docNumber': docNumber,
    'issueDate': issueDate,
    'expiryDate': expiryDate,
    'reminderDaysBefore': reminderDaysBefore,
    'notes': notes,
  };
}

class VehicleMaintenanceSchedule {
  final String id;
  final String maintenanceType;
  final int intervalKm;
  final int intervalMonths;
  final double lastDoneOdometerKm;
  final String? lastDoneDate;
  final double nextDueOdometerKm;
  final String? nextDueDate;
  final double estimatedCost;

  const VehicleMaintenanceSchedule({
    required this.id,
    required this.maintenanceType,
    required this.intervalKm,
    required this.intervalMonths,
    required this.lastDoneOdometerKm,
    this.lastDoneDate,
    required this.nextDueOdometerKm,
    this.nextDueDate,
    required this.estimatedCost,
  });

  factory VehicleMaintenanceSchedule.fromJson(Map<String, dynamic> json) =>
      VehicleMaintenanceSchedule(
        id: (json['id'] as String?) ?? '',
        maintenanceType: (json['maintenanceType'] as String?) ?? 'OIL_CHANGE',
        intervalKm: asInt(json['intervalKm'], 10000),
        intervalMonths: asInt(json['intervalMonths'], 6),
        lastDoneOdometerKm: asDouble(json['lastDoneOdometerKm']),
        lastDoneDate: json['lastDoneDate'] as String?,
        nextDueOdometerKm: asDouble(json['nextDueOdometerKm']),
        nextDueDate: json['nextDueDate'] as String?,
        estimatedCost: asDouble(json['estimatedCost']),
      );

  Map<String, dynamic> toJson() => {
    'id': id,
    'maintenanceType': maintenanceType,
    'intervalKm': intervalKm,
    'intervalMonths': intervalMonths,
    'lastDoneOdometerKm': lastDoneOdometerKm,
    'lastDoneDate': lastDoneDate,
    'nextDueOdometerKm': nextDueOdometerKm,
    'nextDueDate': nextDueDate,
    'estimatedCost': estimatedCost,
  };
}

class VehicleMaintenanceLog {
  final String id;
  final String maintenanceType; // OIL_CHANGE | TIRE_REPLACEMENT | BATTERY_REPLACEMENT | GENERAL_SERVICE | REPAIR
  final String serviceDate;
  final double odometerKm;
  final String vendorName;
  final double costAmount;
  final double downtimeHours;
  final String description;

  const VehicleMaintenanceLog({
    required this.id,
    required this.maintenanceType,
    required this.serviceDate,
    required this.odometerKm,
    required this.vendorName,
    required this.costAmount,
    required this.downtimeHours,
    required this.description,
  });

  factory VehicleMaintenanceLog.fromJson(Map<String, dynamic> json) =>
      VehicleMaintenanceLog(
        id: (json['id'] as String?) ?? '',
        maintenanceType:
            (json['maintenanceType'] as String?) ?? 'GENERAL_SERVICE',
        serviceDate: (json['serviceDate'] as String?) ?? '',
        odometerKm: asDouble(json['odometerKm']),
        vendorName: (json['vendorName'] as String?) ?? '',
        costAmount: asDouble(json['costAmount']),
        downtimeHours: asDouble(json['downtimeHours']),
        description: (json['description'] as String?) ?? '',
      );

  Map<String, dynamic> toJson() => {
    'id': id,
    'maintenanceType': maintenanceType,
    'serviceDate': serviceDate,
    'odometerKm': odometerKm,
    'vendorName': vendorName,
    'costAmount': costAmount,
    'downtimeHours': downtimeHours,
    'description': description,
  };
}

class Vehicle {
  final String id;
  final String registrationNumber;
  final String nameModel;
  final String brand;
  final int manufactureYear;
  final String vehicleType; // MPV | SUV | HIACE_VAN | SEDAN
  final int passengerCapacity;
  final int luggageCapacityBags;
  final String status; // ACTIVE | IN_MAINTENANCE | INACTIVE
  final double currentOdometerKm;
  final String fuelType;
  final String suitabilityTags;
  final String? notes;
  final List<VehicleDocument> documents;
  final List<VehicleMaintenanceSchedule> maintenanceSchedules;
  final List<VehicleMaintenanceLog> recentMaintenanceLogs;

  const Vehicle({
    required this.id,
    required this.registrationNumber,
    required this.nameModel,
    required this.brand,
    required this.manufactureYear,
    required this.vehicleType,
    required this.passengerCapacity,
    required this.luggageCapacityBags,
    required this.status,
    required this.currentOdometerKm,
    required this.fuelType,
    this.suitabilityTags = '',
    this.notes,
    this.documents = const [],
    this.maintenanceSchedules = const [],
    this.recentMaintenanceLogs = const [],
  });

  Vehicle copyWith({
    String? status,
    double? currentOdometerKm,
    List<VehicleDocument>? documents,
    List<VehicleMaintenanceLog>? recentMaintenanceLogs,
  }) {
    return Vehicle(
      id: id,
      registrationNumber: registrationNumber,
      nameModel: nameModel,
      brand: brand,
      manufactureYear: manufactureYear,
      vehicleType: vehicleType,
      passengerCapacity: passengerCapacity,
      luggageCapacityBags: luggageCapacityBags,
      status: status ?? this.status,
      currentOdometerKm: currentOdometerKm ?? this.currentOdometerKm,
      fuelType: fuelType,
      suitabilityTags: suitabilityTags,
      notes: notes,
      documents: documents ?? this.documents,
      maintenanceSchedules: maintenanceSchedules,
      recentMaintenanceLogs:
          recentMaintenanceLogs ?? this.recentMaintenanceLogs,
    );
  }

  factory Vehicle.fromJson(Map<String, dynamic> json) => Vehicle(
    id: (json['id'] as String?) ?? '',
    registrationNumber: (json['registrationNumber'] as String?) ?? '',
    nameModel: (json['nameModel'] as String?) ?? '',
    brand: (json['brand'] as String?) ?? 'Toyota',
    manufactureYear: asInt(json['manufactureYear'], 2022),
    vehicleType: (json['vehicleType'] as String?) ?? 'MPV',
    passengerCapacity: asInt(json['passengerCapacity'], 6),
    luggageCapacityBags: asInt(json['luggageCapacityBags'], 4),
    status: (json['status'] as String?) ?? 'ACTIVE',
    currentOdometerKm: asDouble(json['currentOdometerKm']),
    fuelType: (json['fuelType'] as String?) ?? 'BENSIN',
    suitabilityTags: (json['suitabilityTags'] as String?) ?? '',
    notes: json['notes'] as String?,
    documents: (json['documents'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>()
        .map(VehicleDocument.fromJson)
        .toList(),
    maintenanceSchedules:
        (json['maintenanceSchedules'] as List<dynamic>? ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(VehicleMaintenanceSchedule.fromJson)
            .toList(),
    recentMaintenanceLogs:
        (json['recentMaintenanceLogs'] as List<dynamic>? ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(VehicleMaintenanceLog.fromJson)
            .toList(),
  );

  Map<String, dynamic> toJson() => {
    'id': id,
    'registrationNumber': registrationNumber,
    'nameModel': nameModel,
    'brand': brand,
    'manufactureYear': manufactureYear,
    'vehicleType': vehicleType,
    'passengerCapacity': passengerCapacity,
    'luggageCapacityBags': luggageCapacityBags,
    'status': status,
    'currentOdometerKm': currentOdometerKm,
    'fuelType': fuelType,
    'suitabilityTags': suitabilityTags,
    'notes': notes,
    'documents': documents.map((e) => e.toJson()).toList(),
    'maintenanceSchedules': maintenanceSchedules.map((e) => e.toJson()).toList(),
    'recentMaintenanceLogs':
        recentMaintenanceLogs.map((e) => e.toJson()).toList(),
  };
}

class PricingResolution {
  final double suggestedFare;
  final String pricingTierUsed;
  final String explanation;
  final String? matchedAgreementName;

  const PricingResolution({
    required this.suggestedFare,
    required this.pricingTierUsed,
    required this.explanation,
    this.matchedAgreementName,
  });

  factory PricingResolution.fromJson(Map<String, dynamic> json) =>
      PricingResolution(
        suggestedFare: asDouble(json['suggestedFare']),
        pricingTierUsed:
            (json['pricingTierUsed'] as String?) ?? 'STANDARD_CALCULATION',
        explanation: (json['explanation'] as String?) ?? '',
        matchedAgreementName: json['matchedAgreementName'] as String?,
      );

  Map<String, dynamic> toJson() => {
    'suggestedFare': suggestedFare,
    'pricingTierUsed': pricingTierUsed,
    'explanation': explanation,
    'matchedAgreementName': matchedAgreementName,
  };
}

class BookingTemplate {
  final String id;
  final String name;
  final String? customerId;
  final String? routeId;
  final String tripType;
  final String bookingSource;
  final String pickupAddress;
  final String destinationAddress;
  final int defaultPassengerCount;
  final int defaultLuggageCount;
  final String? luggageNotes;
  final String? preferredVehicleType;
  final String? notes;

  const BookingTemplate({
    required this.id,
    required this.name,
    this.customerId,
    this.routeId,
    required this.tripType,
    this.bookingSource = 'WHATSAPP',
    required this.pickupAddress,
    required this.destinationAddress,
    this.defaultPassengerCount = 2,
    this.defaultLuggageCount = 2,
    this.luggageNotes,
    this.preferredVehicleType,
    this.notes,
  });

  factory BookingTemplate.fromJson(Map<String, dynamic> json) =>
      BookingTemplate(
        id: (json['id'] as String?) ?? '',
        name: (json['name'] as String?) ?? '',
        customerId: json['customerId'] as String?,
        routeId: json['routeId'] as String?,
        tripType: (json['tripType'] as String?) ?? 'AIRPORT_DROPOFF',
        bookingSource: (json['bookingSource'] as String?) ?? 'WHATSAPP',
        pickupAddress: (json['pickupAddress'] as String?) ?? '',
        destinationAddress: (json['destinationAddress'] as String?) ?? '',
        defaultPassengerCount: asInt(json['defaultPassengerCount'], 2),
        defaultLuggageCount: asInt(json['defaultLuggageCount'], 2),
        luggageNotes: json['luggageNotes'] as String?,
        preferredVehicleType: json['preferredVehicleType'] as String?,
        notes: json['notes'] as String?,
      );

  Map<String, dynamic> toJson() => {
    'id': id,
    'name': name,
    'customerId': customerId,
    'routeId': routeId,
    'tripType': tripType,
    'bookingSource': bookingSource,
    'pickupAddress': pickupAddress,
    'destinationAddress': destinationAddress,
    'defaultPassengerCount': defaultPassengerCount,
    'defaultLuggageCount': defaultLuggageCount,
    'luggageNotes': luggageNotes,
    'preferredVehicleType': preferredVehicleType,
    'notes': notes,
  };
}

class TripStop {
  final int stopOrder;
  final String address;
  final double? latitude;
  final double? longitude;
  final int plannedWaitMin;
  final String? notes;

  const TripStop({
    required this.stopOrder,
    required this.address,
    this.latitude,
    this.longitude,
    this.plannedWaitMin = 15,
    this.notes,
  });

  factory TripStop.fromJson(Map<String, dynamic> json) => TripStop(
    stopOrder: asInt(json['stopOrder'], 1),
    address: (json['address'] as String?) ?? '',
    latitude: asNullableDouble(json['latitude']),
    longitude: asNullableDouble(json['longitude']),
    plannedWaitMin: asInt(json['plannedWaitMin'], 15),
    notes: json['notes'] as String?,
  );

  Map<String, dynamic> toJson() => {
    'stopOrder': stopOrder,
    'address': address,
    'latitude': latitude,
    'longitude': longitude,
    'plannedWaitMin': plannedWaitMin,
    'notes': notes,
  };
}

class TripPayment {
  final String id;
  final String tripId;
  final double amount;
  final String paymentMethod; // CASH | BANK_TRANSFER | OTHER
  final bool isRefund;
  final String collectedByRole; // DRIVER | OWNER
  final String? heldByDriverId;
  final double handedOverAmount;
  final bool custodySettled;
  final String paidAt;
  final String? referenceNumber;
  final String? notes;
  final bool isVoided;
  final String? voidReason;

  const TripPayment({
    required this.id,
    required this.tripId,
    required this.amount,
    required this.paymentMethod,
    this.isRefund = false,
    required this.collectedByRole,
    this.heldByDriverId,
    this.handedOverAmount = 0,
    this.custodySettled = true,
    required this.paidAt,
    this.referenceNumber,
    this.notes,
    this.isVoided = false,
    this.voidReason,
  });

  double get unsettledDriverCash {
    if (isVoided || isRefund) return 0;
    if (paymentMethod != 'CASH' || collectedByRole != 'DRIVER') return 0;
    if (custodySettled) return 0;
    final remaining = amount - handedOverAmount;
    return remaining > 0 ? remaining : 0;
  }

  TripPayment copyWith({
    double? handedOverAmount,
    bool? custodySettled,
    bool? isVoided,
    String? voidReason,
  }) {
    return TripPayment(
      id: id,
      tripId: tripId,
      amount: amount,
      paymentMethod: paymentMethod,
      isRefund: isRefund,
      collectedByRole: collectedByRole,
      heldByDriverId: heldByDriverId,
      handedOverAmount: handedOverAmount ?? this.handedOverAmount,
      custodySettled: custodySettled ?? this.custodySettled,
      paidAt: paidAt,
      referenceNumber: referenceNumber,
      notes: notes,
      isVoided: isVoided ?? this.isVoided,
      voidReason: voidReason ?? this.voidReason,
    );
  }

  factory TripPayment.fromJson(Map<String, dynamic> json) => TripPayment(
    id: (json['id'] as String?) ?? '',
    tripId: (json['tripId'] as String?) ?? '',
    amount: asDouble(json['amount']),
    paymentMethod: (json['paymentMethod'] as String?) ?? 'CASH',
    isRefund: asBool(json['isRefund']),
    collectedByRole: (json['collectedByRole'] as String?) ?? 'DRIVER',
    heldByDriverId: json['heldByDriverId'] as String?,
    handedOverAmount: asDouble(json['handedOverAmount']),
    custodySettled: asBool(json['custodySettled'], true),
    paidAt: (json['paidAt'] as String?) ?? '',
    referenceNumber: json['referenceNumber'] as String?,
    notes: json['notes'] as String?,
    isVoided: asBool(json['isVoided']),
    voidReason: json['voidReason'] as String?,
  );

  Map<String, dynamic> toJson() => {
    'id': id,
    'tripId': tripId,
    'amount': amount,
    'paymentMethod': paymentMethod,
    'isRefund': isRefund,
    'collectedByRole': collectedByRole,
    'heldByDriverId': heldByDriverId,
    'handedOverAmount': handedOverAmount,
    'custodySettled': custodySettled,
    'paidAt': paidAt,
    'referenceNumber': referenceNumber,
    'notes': notes,
    'isVoided': isVoided,
    'voidReason': voidReason,
  };
}

class CashHandover {
  final String id;
  final String handoverCode;
  final String driverId;
  final String driverName;
  final double declaredAmount;
  final double? confirmedAmount;
  final String status; // PENDING | HANDED_OVER_CONFIRMED | PARTIALLY_HANDED_OVER | DISPUTED
  final String? driverNotes;
  final String? ownerNotes;
  final String handedOverAt;
  final String? confirmedAt;
  final List<String> paymentIds;

  const CashHandover({
    required this.id,
    required this.handoverCode,
    required this.driverId,
    required this.driverName,
    required this.declaredAmount,
    this.confirmedAmount,
    required this.status,
    this.driverNotes,
    this.ownerNotes,
    required this.handedOverAt,
    this.confirmedAt,
    this.paymentIds = const [],
  });

  CashHandover copyWith({
    double? confirmedAmount,
    String? status,
    String? ownerNotes,
    String? confirmedAt,
  }) {
    return CashHandover(
      id: id,
      handoverCode: handoverCode,
      driverId: driverId,
      driverName: driverName,
      declaredAmount: declaredAmount,
      confirmedAmount: confirmedAmount ?? this.confirmedAmount,
      status: status ?? this.status,
      driverNotes: driverNotes,
      ownerNotes: ownerNotes ?? this.ownerNotes,
      handedOverAt: handedOverAt,
      confirmedAt: confirmedAt ?? this.confirmedAt,
      paymentIds: paymentIds,
    );
  }

  factory CashHandover.fromJson(Map<String, dynamic> json) => CashHandover(
    id: (json['id'] as String?) ?? '',
    handoverCode: (json['handoverCode'] as String?) ?? '',
    driverId: (json['driverId'] as String?) ?? '',
    driverName: (json['driverName'] as String?) ?? '',
    declaredAmount: asDouble(json['declaredAmount']),
    confirmedAmount: asNullableDouble(json['confirmedAmount']),
    status: (json['status'] as String?) ?? 'PENDING',
    driverNotes: json['driverNotes'] as String?,
    ownerNotes: json['ownerNotes'] as String?,
    handedOverAt: (json['handedOverAt'] as String?) ?? '',
    confirmedAt: json['confirmedAt'] as String?,
    paymentIds: (json['paymentIds'] as List<dynamic>? ?? const [])
        .map((e) => e.toString())
        .toList(),
  );

  Map<String, dynamic> toJson() => {
    'id': id,
    'handoverCode': handoverCode,
    'driverId': driverId,
    'driverName': driverName,
    'declaredAmount': declaredAmount,
    'confirmedAmount': confirmedAmount,
    'status': status,
    'driverNotes': driverNotes,
    'ownerNotes': ownerNotes,
    'handedOverAt': handedOverAt,
    'confirmedAt': confirmedAt,
    'paymentIds': paymentIds,
  };
}

class TripExpense {
  final String id;
  final String tripId;
  final String category; // FUEL | TOLL | PARKING | DRIVER_MEAL | OTHER
  final double amount;
  final double? fuelLiters;
  final String? notes;
  final String incurredAt;

  const TripExpense({
    required this.id,
    required this.tripId,
    required this.category,
    required this.amount,
    this.fuelLiters,
    this.notes,
    required this.incurredAt,
  });

  factory TripExpense.fromJson(Map<String, dynamic> json) => TripExpense(
    id: (json['id'] as String?) ?? '',
    tripId: (json['tripId'] as String?) ?? '',
    category: (json['category'] as String?) ?? 'FUEL',
    amount: asDouble(json['amount']),
    fuelLiters: asNullableDouble(json['fuelLiters']),
    notes: json['notes'] as String?,
    incurredAt: (json['incurredAt'] as String?) ?? '',
  );

  Map<String, dynamic> toJson() => {
    'id': id,
    'tripId': tripId,
    'category': category,
    'amount': amount,
    'fuelLiters': fuelLiters,
    'notes': notes,
    'incurredAt': incurredAt,
  };
}

class VehicleInspection {
  final String id;
  final String tripId;
  final String stage; // PRE_TRIP | POST_TRIP
  final bool allPassed;
  final List<String> flaggedItems;
  final String? notes;
  final String recordedAt;

  const VehicleInspection({
    required this.id,
    required this.tripId,
    this.stage = 'PRE_TRIP',
    required this.allPassed,
    this.flaggedItems = const [],
    this.notes,
    required this.recordedAt,
  });

  factory VehicleInspection.fromJson(Map<String, dynamic> json) =>
      VehicleInspection(
        id: (json['id'] as String?) ?? '',
        tripId: (json['tripId'] as String?) ?? '',
        stage: (json['stage'] as String?) ?? 'PRE_TRIP',
        allPassed: asBool(json['allPassed'], true),
        flaggedItems: (json['flaggedItems'] as List<dynamic>? ?? const [])
            .map((e) => e.toString())
            .toList(),
        notes: json['notes'] as String?,
        recordedAt: (json['recordedAt'] as String?) ?? '',
      );

  Map<String, dynamic> toJson() => {
    'id': id,
    'tripId': tripId,
    'stage': stage,
    'allPassed': allPassed,
    'flaggedItems': flaggedItems,
    'notes': notes,
    'recordedAt': recordedAt,
  };
}

class LostAndFoundItem {
  final String id;
  final String tripId;
  final String tripCode;
  final String customerName;
  final String itemDescription;
  final String foundDate;
  final String status; // FOUND | CONTACTING_CUSTOMER | RETURNED | STORED | UNRESOLVED
  final String? notes;

  const LostAndFoundItem({
    required this.id,
    required this.tripId,
    required this.tripCode,
    required this.customerName,
    required this.itemDescription,
    required this.foundDate,
    required this.status,
    this.notes,
  });

  LostAndFoundItem copyWith({String? status, String? notes}) =>
      LostAndFoundItem(
        id: id,
        tripId: tripId,
        tripCode: tripCode,
        customerName: customerName,
        itemDescription: itemDescription,
        foundDate: foundDate,
        status: status ?? this.status,
        notes: notes ?? this.notes,
      );

  factory LostAndFoundItem.fromJson(Map<String, dynamic> json) =>
      LostAndFoundItem(
        id: (json['id'] as String?) ?? '',
        tripId: (json['tripId'] as String?) ?? '',
        tripCode: (json['tripCode'] as String?) ?? '',
        customerName: (json['customerName'] as String?) ?? '',
        itemDescription: (json['itemDescription'] as String?) ?? '',
        foundDate: (json['foundDate'] as String?) ?? '',
        status: (json['status'] as String?) ?? 'FOUND',
        notes: json['notes'] as String?,
      );

  Map<String, dynamic> toJson() => {
    'id': id,
    'tripId': tripId,
    'tripCode': tripCode,
    'customerName': customerName,
    'itemDescription': itemDescription,
    'foundDate': foundDate,
    'status': status,
    'notes': notes,
  };
}

class GpsPointRecord {
  final String clientPointId;
  final String tripId;
  final double latitude;
  final double longitude;
  final double speedKmh;
  final double accuracyM;
  final String recordedAt;
  final bool synced;

  const GpsPointRecord({
    required this.clientPointId,
    required this.tripId,
    required this.latitude,
    required this.longitude,
    this.speedKmh = 0,
    this.accuracyM = 10,
    required this.recordedAt,
    this.synced = false,
  });

  GpsPointRecord copyWith({bool? synced}) => GpsPointRecord(
    clientPointId: clientPointId,
    tripId: tripId,
    latitude: latitude,
    longitude: longitude,
    speedKmh: speedKmh,
    accuracyM: accuracyM,
    recordedAt: recordedAt,
    synced: synced ?? this.synced,
  );

  factory GpsPointRecord.fromJson(Map<String, dynamic> json) => GpsPointRecord(
    clientPointId: (json['clientPointId'] as String?) ?? '',
    tripId: (json['tripId'] as String?) ?? '',
    latitude: asDouble(json['latitude']),
    longitude: asDouble(json['longitude']),
    speedKmh: asDouble(json['speedKmh']),
    accuracyM: asDouble(json['accuracyM'], 10),
    recordedAt: (json['recordedAt'] as String?) ?? '',
    synced: asBool(json['synced']),
  );

  Map<String, dynamic> toJson() => {
    'clientPointId': clientPointId,
    'tripId': tripId,
    'latitude': latitude,
    'longitude': longitude,
    'speedKmh': speedKmh,
    'accuracyM': accuracyM,
    'recordedAt': recordedAt,
    'synced': synced,
  };
}

class Trip {
  final String id;
  final String tripCode;
  final String bookingId;
  final String bookingCode;
  final String? customerId;
  final String customerName;
  final String customerPhone;
  final List<CustomerNote> customerNotes;
  final int legNumber;
  final String legLabel;
  final String? routeId;
  final String routeName;
  final String tripType;
  final String? driverId;
  final String? driverName;
  final String? driverPhone;
  final String? vehicleId;
  final String? vehiclePlate;
  final String? vehicleModel;
  final String scheduledPickupAt;
  final int estimatedDurationMin;
  final int prepBufferMin;
  final int arrivalBufferMin;
  final int repositionBufferMin;
  final String reservedStartAt;
  final String reservedEndAt;
  final String pickupAddress;
  final String destinationAddress;
  final String? pickupMapsUrl;
  final String? destinationMapsUrl;
  final String? customerWhatsappUrl;
  final int passengerCount;
  final int luggageCount;
  final String? flightNumber;
  final double agreedFareSnapshot;
  final String status; // UNASSIGNED | ASSIGNED | ACCEPTED | ON_THE_WAY | ARRIVED | PASSENGER_ONBOARD | COMPLETED | CANCELLED | NO_SHOW
  final bool trackingActive;
  final String? trackingStartedAt;
  final String? trackingEndedAt;
  final String? lastGpsRecordedAt;
  final double? lastGpsLat;
  final double? lastGpsLng;
  final double? gpsDistanceKm;
  final double? odometerStartKm;
  final double? odometerEndKm;
  final int? actualDurationMin;
  final String? completionNotes;
  final String paymentStatus; // UNPAID | PARTIALLY_PAID | PAID | OVERPAID | REFUNDED
  final double totalPaidAmount;
  final String cashCustodyStatus; // NO_DRIVER_CASH_DUE | HELD_BY_DRIVER | PARTIALLY_HANDED_OVER | HANDED_OVER_CONFIRMED | DISPUTED
  final double driverCashHeldAmount;
  final int version;
  final List<TripStop> stops;
  final List<TripPayment> payments;
  final List<TripExpense> expenses;
  final List<VehicleInspection> inspections;
  final List<LostAndFoundItem> lostAndFoundItems;

  const Trip({
    required this.id,
    required this.tripCode,
    required this.bookingId,
    required this.bookingCode,
    this.customerId,
    required this.customerName,
    required this.customerPhone,
    this.customerNotes = const [],
    this.legNumber = 1,
    this.legLabel = 'Outbound',
    this.routeId,
    required this.routeName,
    required this.tripType,
    this.driverId,
    this.driverName,
    this.driverPhone,
    this.vehicleId,
    this.vehiclePlate,
    this.vehicleModel,
    required this.scheduledPickupAt,
    required this.estimatedDurationMin,
    this.prepBufferMin = 15,
    this.arrivalBufferMin = 0,
    this.repositionBufferMin = 15,
    required this.reservedStartAt,
    required this.reservedEndAt,
    required this.pickupAddress,
    required this.destinationAddress,
    this.pickupMapsUrl,
    this.destinationMapsUrl,
    this.customerWhatsappUrl,
    this.passengerCount = 2,
    this.luggageCount = 2,
    this.flightNumber,
    required this.agreedFareSnapshot,
    required this.status,
    this.trackingActive = false,
    this.trackingStartedAt,
    this.trackingEndedAt,
    this.lastGpsRecordedAt,
    this.lastGpsLat,
    this.lastGpsLng,
    this.gpsDistanceKm,
    this.odometerStartKm,
    this.odometerEndKm,
    this.actualDurationMin,
    this.completionNotes,
    this.paymentStatus = 'UNPAID',
    this.totalPaidAmount = 0,
    this.cashCustodyStatus = 'NO_DRIVER_CASH_DUE',
    this.driverCashHeldAmount = 0,
    this.version = 1,
    this.stops = const [],
    this.payments = const [],
    this.expenses = const [],
    this.inspections = const [],
    this.lostAndFoundItems = const [],
  });

  bool get isActiveExecutionState => const {
    'ACCEPTED',
    'ON_THE_WAY',
    'ARRIVED',
    'PASSENGER_ONBOARD',
  }.contains(status);

  bool get isCompleted => status == 'COMPLETED';
  bool get isUnassigned => status == 'UNASSIGNED' || driverId == null;

  double get remainingUnpaidAmount {
    final diff = agreedFareSnapshot - totalPaidAmount;
    return diff > 0 ? diff : 0;
  }

  Trip copyWith({
    String? driverId,
    String? driverName,
    String? driverPhone,
    String? vehicleId,
    String? vehiclePlate,
    String? vehicleModel,
    String? scheduledPickupAt,
    String? reservedStartAt,
    String? reservedEndAt,
    String? status,
    bool? trackingActive,
    String? trackingStartedAt,
    String? trackingEndedAt,
    String? lastGpsRecordedAt,
    double? lastGpsLat,
    double? lastGpsLng,
    double? gpsDistanceKm,
    double? odometerStartKm,
    double? odometerEndKm,
    int? actualDurationMin,
    String? completionNotes,
    String? paymentStatus,
    double? totalPaidAmount,
    String? cashCustodyStatus,
    double? driverCashHeldAmount,
    int? version,
    List<TripStop>? stops,
    List<TripPayment>? payments,
    List<TripExpense>? expenses,
    List<VehicleInspection>? inspections,
    List<LostAndFoundItem>? lostAndFoundItems,
  }) {
    return Trip(
      id: id,
      tripCode: tripCode,
      bookingId: bookingId,
      bookingCode: bookingCode,
      customerId: customerId,
      customerName: customerName,
      customerPhone: customerPhone,
      customerNotes: customerNotes,
      legNumber: legNumber,
      legLabel: legLabel,
      routeId: routeId,
      routeName: routeName,
      tripType: tripType,
      driverId: driverId ?? this.driverId,
      driverName: driverName ?? this.driverName,
      driverPhone: driverPhone ?? this.driverPhone,
      vehicleId: vehicleId ?? this.vehicleId,
      vehiclePlate: vehiclePlate ?? this.vehiclePlate,
      vehicleModel: vehicleModel ?? this.vehicleModel,
      scheduledPickupAt: scheduledPickupAt ?? this.scheduledPickupAt,
      estimatedDurationMin: estimatedDurationMin,
      prepBufferMin: prepBufferMin,
      arrivalBufferMin: arrivalBufferMin,
      repositionBufferMin: repositionBufferMin,
      reservedStartAt: reservedStartAt ?? this.reservedStartAt,
      reservedEndAt: reservedEndAt ?? this.reservedEndAt,
      pickupAddress: pickupAddress,
      destinationAddress: destinationAddress,
      pickupMapsUrl: pickupMapsUrl,
      destinationMapsUrl: destinationMapsUrl,
      customerWhatsappUrl: customerWhatsappUrl,
      passengerCount: passengerCount,
      luggageCount: luggageCount,
      flightNumber: flightNumber,
      agreedFareSnapshot: agreedFareSnapshot,
      status: status ?? this.status,
      trackingActive: trackingActive ?? this.trackingActive,
      trackingStartedAt: trackingStartedAt ?? this.trackingStartedAt,
      trackingEndedAt: trackingEndedAt ?? this.trackingEndedAt,
      lastGpsRecordedAt: lastGpsRecordedAt ?? this.lastGpsRecordedAt,
      lastGpsLat: lastGpsLat ?? this.lastGpsLat,
      lastGpsLng: lastGpsLng ?? this.lastGpsLng,
      gpsDistanceKm: gpsDistanceKm ?? this.gpsDistanceKm,
      odometerStartKm: odometerStartKm ?? this.odometerStartKm,
      odometerEndKm: odometerEndKm ?? this.odometerEndKm,
      actualDurationMin: actualDurationMin ?? this.actualDurationMin,
      completionNotes: completionNotes ?? this.completionNotes,
      paymentStatus: paymentStatus ?? this.paymentStatus,
      totalPaidAmount: totalPaidAmount ?? this.totalPaidAmount,
      cashCustodyStatus: cashCustodyStatus ?? this.cashCustodyStatus,
      driverCashHeldAmount: driverCashHeldAmount ?? this.driverCashHeldAmount,
      version: version ?? this.version,
      stops: stops ?? this.stops,
      payments: payments ?? this.payments,
      expenses: expenses ?? this.expenses,
      inspections: inspections ?? this.inspections,
      lostAndFoundItems: lostAndFoundItems ?? this.lostAndFoundItems,
    );
  }

  factory Trip.fromJson(Map<String, dynamic> json) => Trip(
    id: (json['id'] as String?) ?? '',
    tripCode: (json['tripCode'] as String?) ?? '',
    bookingId: (json['bookingId'] as String?) ?? '',
    bookingCode: (json['bookingCode'] as String?) ?? '',
    customerId: json['customerId'] as String?,
    customerName: (json['customerName'] as String?) ?? 'Pelanggan',
    customerPhone: (json['customerPhone'] as String?) ?? '',
    customerNotes: (json['customerNotes'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>()
        .map(CustomerNote.fromJson)
        .toList(),
    legNumber: asInt(json['legNumber'], 1),
    legLabel: (json['legLabel'] as String?) ?? 'Outbound',
    routeId: json['routeId'] as String?,
    routeName: (json['routeName'] as String?) ?? 'Rute Lokal',
    tripType: (json['tripType'] as String?) ?? 'LOCAL',
    driverId: json['driverId'] as String?,
    driverName: json['driverName'] as String?,
    driverPhone: json['driverPhone'] as String?,
    vehicleId: json['vehicleId'] as String?,
    vehiclePlate: json['vehiclePlate'] as String?,
    vehicleModel: json['vehicleModel'] as String?,
    scheduledPickupAt: (json['scheduledPickupAt'] as String?) ?? '',
    estimatedDurationMin: asInt(json['estimatedDurationMin'], 75),
    prepBufferMin: asInt(json['prepBufferMin'], 15),
    arrivalBufferMin: asInt(json['arrivalBufferMin'], 0),
    repositionBufferMin: asInt(json['repositionBufferMin'], 15),
    reservedStartAt:
        (json['reservedStartAt'] as String?) ??
        (json['scheduledPickupAt'] as String?) ??
        '',
    reservedEndAt:
        (json['reservedEndAt'] as String?) ??
        (json['scheduledPickupAt'] as String?) ??
        '',
    pickupAddress: (json['pickupAddress'] as String?) ?? '',
    destinationAddress: (json['destinationAddress'] as String?) ?? '',
    pickupMapsUrl: json['pickupMapsUrl'] as String?,
    destinationMapsUrl: json['destinationMapsUrl'] as String?,
    customerWhatsappUrl: json['customerWhatsappUrl'] as String?,
    passengerCount: asInt(json['passengerCount'], 2),
    luggageCount: asInt(json['luggageCount'], 2),
    flightNumber: json['flightNumber'] as String?,
    agreedFareSnapshot: asDouble(json['agreedFareSnapshot']),
    status: (json['status'] as String?) ?? 'UNASSIGNED',
    trackingActive: asBool(json['trackingActive']),
    trackingStartedAt: json['trackingStartedAt'] as String?,
    trackingEndedAt: json['trackingEndedAt'] as String?,
    lastGpsRecordedAt: json['lastGpsRecordedAt'] as String?,
    lastGpsLat: asNullableDouble(json['lastGpsLat']),
    lastGpsLng: asNullableDouble(json['lastGpsLng']),
    gpsDistanceKm: asNullableDouble(json['gpsDistanceKm']),
    odometerStartKm: asNullableDouble(json['odometerStartKm']),
    odometerEndKm: asNullableDouble(json['odometerEndKm']),
    actualDurationMin: asNullableInt(json['actualDurationMin']),
    completionNotes: json['completionNotes'] as String?,
    paymentStatus: (json['paymentStatus'] as String?) ?? 'UNPAID',
    totalPaidAmount: asDouble(json['totalPaidAmount']),
    cashCustodyStatus:
        (json['cashCustodyStatus'] as String?) ?? 'NO_DRIVER_CASH_DUE',
    driverCashHeldAmount: asDouble(json['driverCashHeldAmount']),
    version: asInt(json['version'], 1),
    stops: (json['stops'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>()
        .map(TripStop.fromJson)
        .toList(),
    payments: (json['payments'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>()
        .map(TripPayment.fromJson)
        .toList(),
    expenses: (json['expenses'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>()
        .map(TripExpense.fromJson)
        .toList(),
    inspections: (json['inspections'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>()
        .map(VehicleInspection.fromJson)
        .toList(),
    lostAndFoundItems: (json['lostAndFoundItems'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>()
        .map(LostAndFoundItem.fromJson)
        .toList(),
  );

  Map<String, dynamic> toJson() => {
    'id': id,
    'tripCode': tripCode,
    'bookingId': bookingId,
    'bookingCode': bookingCode,
    'customerId': customerId,
    'customerName': customerName,
    'customerPhone': customerPhone,
    'customerNotes': customerNotes.map((e) => e.toJson()).toList(),
    'legNumber': legNumber,
    'legLabel': legLabel,
    'routeId': routeId,
    'routeName': routeName,
    'tripType': tripType,
    'driverId': driverId,
    'driverName': driverName,
    'driverPhone': driverPhone,
    'vehicleId': vehicleId,
    'vehiclePlate': vehiclePlate,
    'vehicleModel': vehicleModel,
    'scheduledPickupAt': scheduledPickupAt,
    'estimatedDurationMin': estimatedDurationMin,
    'prepBufferMin': prepBufferMin,
    'arrivalBufferMin': arrivalBufferMin,
    'repositionBufferMin': repositionBufferMin,
    'reservedStartAt': reservedStartAt,
    'reservedEndAt': reservedEndAt,
    'pickupAddress': pickupAddress,
    'destinationAddress': destinationAddress,
    'pickupMapsUrl': pickupMapsUrl,
    'destinationMapsUrl': destinationMapsUrl,
    'customerWhatsappUrl': customerWhatsappUrl,
    'passengerCount': passengerCount,
    'luggageCount': luggageCount,
    'flightNumber': flightNumber,
    'agreedFareSnapshot': agreedFareSnapshot,
    'status': status,
    'trackingActive': trackingActive,
    'trackingStartedAt': trackingStartedAt,
    'trackingEndedAt': trackingEndedAt,
    'lastGpsRecordedAt': lastGpsRecordedAt,
    'lastGpsLat': lastGpsLat,
    'lastGpsLng': lastGpsLng,
    'gpsDistanceKm': gpsDistanceKm,
    'odometerStartKm': odometerStartKm,
    'odometerEndKm': odometerEndKm,
    'actualDurationMin': actualDurationMin,
    'completionNotes': completionNotes,
    'paymentStatus': paymentStatus,
    'totalPaidAmount': totalPaidAmount,
    'cashCustodyStatus': cashCustodyStatus,
    'driverCashHeldAmount': driverCashHeldAmount,
    'version': version,
    'stops': stops.map((e) => e.toJson()).toList(),
    'payments': payments.map((e) => e.toJson()).toList(),
    'expenses': expenses.map((e) => e.toJson()).toList(),
    'inspections': inspections.map((e) => e.toJson()).toList(),
    'lostAndFoundItems': lostAndFoundItems.map((e) => e.toJson()).toList(),
  };
}
