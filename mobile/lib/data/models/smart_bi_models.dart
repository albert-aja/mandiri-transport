import 'domain_models.dart';

class CostFormulaComponent {
  final String id;
  final String componentCode;
  final String name;
  final String calcType; // PER_KM | FIXED_PER_TRIP | FORMULA_ALLOCATION | MONTHLY_ALLOCATION | PERCENT_OF_FARE
  final String reconciliationPolicy; // ACTUAL_OVERRIDES_ESTIMATE | ACTUAL_EXPENSE_ONLY | ALLOCATED_ONLY
  final String? expenseCategoryLink;
  final double? fixedAmount;
  final double? rateValue;
  final double? monthlyAmount;
  final int? assumedMonthlyTrips;
  final String? expressionFormula;
  final Map<String, dynamic>? parametersJson;
  final int sortOrder;

  const CostFormulaComponent({
    required this.id,
    required this.componentCode,
    required this.name,
    required this.calcType,
    required this.reconciliationPolicy,
    this.expenseCategoryLink,
    this.fixedAmount,
    this.rateValue,
    this.monthlyAmount,
    this.assumedMonthlyTrips,
    this.expressionFormula,
    this.parametersJson,
    this.sortOrder = 1,
  });

  factory CostFormulaComponent.fromJson(Map<String, dynamic> json) {
    Map<String, dynamic>? parsedParams;
    final rawParams = json['parametersJson'];
    if (rawParams is Map<String, dynamic>) {
      parsedParams = rawParams;
    }
    return CostFormulaComponent(
      id: (json['id'] as String?) ?? '',
      componentCode: (json['componentCode'] as String?) ?? '',
      name: (json['name'] as String?) ?? '',
      calcType: (json['calcType'] as String?) ?? 'PER_KM',
      reconciliationPolicy:
          (json['reconciliationPolicy'] as String?) ?? 'ALLOCATED_ONLY',
      expenseCategoryLink: json['expenseCategoryLink'] as String?,
      fixedAmount: asNullableDouble(json['fixedAmount']),
      rateValue: asNullableDouble(json['rateValue']),
      monthlyAmount: asNullableDouble(json['monthlyAmount']),
      assumedMonthlyTrips: asNullableInt(json['assumedMonthlyTrips']),
      expressionFormula: json['expressionFormula'] as String?,
      parametersJson: parsedParams,
      sortOrder: asInt(json['sortOrder'], 1),
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'componentCode': componentCode,
    'name': name,
    'calcType': calcType,
    'reconciliationPolicy': reconciliationPolicy,
    'expenseCategoryLink': expenseCategoryLink,
    'fixedAmount': fixedAmount,
    'rateValue': rateValue,
    'monthlyAmount': monthlyAmount,
    'assumedMonthlyTrips': assumedMonthlyTrips,
    'expressionFormula': expressionFormula,
    'parametersJson': parametersJson,
    'sortOrder': sortOrder,
  };
}

class CostFormulaVersion {
  final String id;
  final int versionNumber;
  final String name;
  final String effectiveFrom;
  final String? effectiveTo;
  final bool isActive;
  final String? notes;
  final List<CostFormulaComponent> components;

  const CostFormulaVersion({
    required this.id,
    required this.versionNumber,
    required this.name,
    required this.effectiveFrom,
    this.effectiveTo,
    required this.isActive,
    this.notes,
    this.components = const [],
  });

  CostFormulaVersion copyWith({bool? isActive, String? effectiveTo}) =>
      CostFormulaVersion(
        id: id,
        versionNumber: versionNumber,
        name: name,
        effectiveFrom: effectiveFrom,
        effectiveTo: effectiveTo ?? this.effectiveTo,
        isActive: isActive ?? this.isActive,
        notes: notes,
        components: components,
      );

  factory CostFormulaVersion.fromJson(Map<String, dynamic> json) =>
      CostFormulaVersion(
        id: (json['id'] as String?) ?? '',
        versionNumber: asInt(json['versionNumber'], 1),
        name: (json['name'] as String?) ?? '',
        effectiveFrom: (json['effectiveFrom'] as String?) ?? '',
        effectiveTo: json['effectiveTo'] as String?,
        isActive: asBool(json['isActive']),
        notes: json['notes'] as String?,
        components: (json['components'] as List<dynamic>? ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(CostFormulaComponent.fromJson)
            .toList(),
      );

  Map<String, dynamic> toJson() => {
    'id': id,
    'versionNumber': versionNumber,
    'name': name,
    'effectiveFrom': effectiveFrom,
    'effectiveTo': effectiveTo,
    'isActive': isActive,
    'notes': notes,
    'components': components.map((e) => e.toJson()).toList(),
  };
}

class TripProfitabilityItem {
  final String componentCode;
  final String name;
  final String reconciliationPolicy;
  final double estimatedAmount;
  final double actualAmount;
  final double effectiveAmount;

  const TripProfitabilityItem({
    required this.componentCode,
    required this.name,
    required this.reconciliationPolicy,
    required this.estimatedAmount,
    required this.actualAmount,
    required this.effectiveAmount,
  });

  factory TripProfitabilityItem.fromJson(Map<String, dynamic> json) =>
      TripProfitabilityItem(
        componentCode: (json['componentCode'] as String?) ?? '',
        name: (json['name'] as String?) ?? '',
        reconciliationPolicy:
            (json['reconciliationPolicy'] as String?) ?? 'ALLOCATED_ONLY',
        estimatedAmount: asDouble(json['estimatedAmount']),
        actualAmount: asDouble(json['actualAmount']),
        effectiveAmount: asDouble(json['effectiveAmount']),
      );

  Map<String, dynamic> toJson() => {
    'componentCode': componentCode,
    'name': name,
    'reconciliationPolicy': reconciliationPolicy,
    'estimatedAmount': estimatedAmount,
    'actualAmount': actualAmount,
    'effectiveAmount': effectiveAmount,
  };
}

class TripProfitability {
  final String tripId;
  final double agreedFareSnapshot;
  final int formulaVersionNumber;
  final double distanceKmUsed;
  final String distanceSource;
  final double estimatedOperatingCost;
  final double actualExpensesTotal;
  final double allocatedOperatingCost;
  final double totalEffectiveCost;
  final double estimatedProfit;
  final double actualProfit;
  final List<TripProfitabilityItem> items;

  const TripProfitability({
    required this.tripId,
    required this.agreedFareSnapshot,
    required this.formulaVersionNumber,
    required this.distanceKmUsed,
    required this.distanceSource,
    required this.estimatedOperatingCost,
    required this.actualExpensesTotal,
    required this.allocatedOperatingCost,
    required this.totalEffectiveCost,
    required this.estimatedProfit,
    required this.actualProfit,
    this.items = const [],
  });

  factory TripProfitability.fromJson(Map<String, dynamic> json) =>
      TripProfitability(
        tripId: (json['tripId'] as String?) ?? '',
        agreedFareSnapshot: asDouble(json['agreedFareSnapshot']),
        formulaVersionNumber: asInt(json['formulaVersionNumber'], 2),
        distanceKmUsed: asDouble(json['distanceKmUsed']),
        distanceSource: (json['distanceSource'] as String?) ?? 'ROUTE_DEFAULT',
        estimatedOperatingCost: asDouble(json['estimatedOperatingCost']),
        actualExpensesTotal: asDouble(json['actualExpensesTotal']),
        allocatedOperatingCost: asDouble(json['allocatedOperatingCost']),
        totalEffectiveCost: asDouble(json['totalEffectiveCost']),
        estimatedProfit: asDouble(json['estimatedProfit']),
        actualProfit: asDouble(json['actualProfit']),
        items: (json['items'] as List<dynamic>? ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(TripProfitabilityItem.fromJson)
            .toList(),
      );
}

class ScheduleConflictItem {
  final String type; // TRIP_OVERLAP | DRIVER_UNAVAILABLE | VEHICLE_IN_MAINTENANCE
  final String? conflictingTripCode;
  final String? windowStart;
  final String? windowEnd;
  final String explanation;

  const ScheduleConflictItem({
    required this.type,
    this.conflictingTripCode,
    this.windowStart,
    this.windowEnd,
    required this.explanation,
  });

  factory ScheduleConflictItem.fromJson(Map<String, dynamic> json) =>
      ScheduleConflictItem(
        type: (json['type'] as String?) ?? 'TRIP_OVERLAP',
        conflictingTripCode: json['conflictingTripCode'] as String?,
        windowStart: json['windowStart'] as String?,
        windowEnd: json['windowEnd'] as String?,
        explanation: (json['explanation'] as String?) ?? '',
      );
}

class DriverRecommendation {
  final String driverId;
  final String driverName;
  final String availabilityStatus;
  final bool conflictFree;
  final double reservedHoursTargetDay;
  final double activeHoursTargetDay;
  final bool recommended;
  final String explanation;

  const DriverRecommendation({
    required this.driverId,
    required this.driverName,
    required this.availabilityStatus,
    required this.conflictFree,
    required this.reservedHoursTargetDay,
    required this.activeHoursTargetDay,
    required this.recommended,
    required this.explanation,
  });

  factory DriverRecommendation.fromJson(Map<String, dynamic> json) =>
      DriverRecommendation(
        driverId: (json['driverId'] as String?) ?? '',
        driverName: (json['driverName'] as String?) ?? '',
        availabilityStatus:
            (json['availabilityStatus'] as String?) ?? 'AVAILABLE',
        conflictFree: asBool(json['conflictFree'], true),
        reservedHoursTargetDay: asDouble(json['reservedHoursTargetDay']),
        activeHoursTargetDay: asDouble(json['activeHoursTargetDay']),
        recommended: asBool(json['recommended']),
        explanation: (json['explanation'] as String?) ?? '',
      );
}

class VehicleRecommendation {
  final String vehicleId;
  final String registrationNumber;
  final String nameModel;
  final String vehicleType;
  final int passengerCapacity;
  final int luggageCapacityBags;
  final bool conflictFree;
  final bool suitable;
  final int suitabilityScore;
  final String explanation;

  const VehicleRecommendation({
    required this.vehicleId,
    required this.registrationNumber,
    required this.nameModel,
    required this.vehicleType,
    required this.passengerCapacity,
    required this.luggageCapacityBags,
    required this.conflictFree,
    required this.suitable,
    required this.suitabilityScore,
    required this.explanation,
  });

  factory VehicleRecommendation.fromJson(Map<String, dynamic> json) =>
      VehicleRecommendation(
        vehicleId: (json['vehicleId'] as String?) ?? '',
        registrationNumber: (json['registrationNumber'] as String?) ?? '',
        nameModel: (json['nameModel'] as String?) ?? '',
        vehicleType: (json['vehicleType'] as String?) ?? 'MPV',
        passengerCapacity: asInt(json['passengerCapacity'], 6),
        luggageCapacityBags: asInt(json['luggageCapacityBags'], 4),
        conflictFree: asBool(json['conflictFree'], true),
        suitable: asBool(json['suitable'], true),
        suitabilityScore: asInt(json['suitabilityScore'], 80),
        explanation: (json['explanation'] as String?) ?? '',
      );
}

class ConflictSimulationResult {
  final String reservedStartAt;
  final String reservedEndAt;
  final bool hasDriverConflict;
  final List<ScheduleConflictItem> driverConflicts;
  final bool hasVehicleConflict;
  final List<ScheduleConflictItem> vehicleConflicts;
  final List<DriverRecommendation> driverRecommendations;
  final List<VehicleRecommendation> vehicleRecommendations;

  const ConflictSimulationResult({
    required this.reservedStartAt,
    required this.reservedEndAt,
    required this.hasDriverConflict,
    this.driverConflicts = const [],
    required this.hasVehicleConflict,
    this.vehicleConflicts = const [],
    this.driverRecommendations = const [],
    this.vehicleRecommendations = const [],
  });

  bool get hasAnyConflict => hasDriverConflict || hasVehicleConflict;

  factory ConflictSimulationResult.fromJson(Map<String, dynamic> json) =>
      ConflictSimulationResult(
        reservedStartAt: (json['reservedStartAt'] as String?) ?? '',
        reservedEndAt: (json['reservedEndAt'] as String?) ?? '',
        hasDriverConflict: asBool(json['hasDriverConflict']),
        driverConflicts: (json['driverConflicts'] as List<dynamic>? ?? const [])
            .whereType<Map<String, dynamic>>()
            .map(ScheduleConflictItem.fromJson)
            .toList(),
        hasVehicleConflict: asBool(json['hasVehicleConflict']),
        vehicleConflicts:
            (json['vehicleConflicts'] as List<dynamic>? ?? const [])
                .whereType<Map<String, dynamic>>()
                .map(ScheduleConflictItem.fromJson)
                .toList(),
        driverRecommendations:
            (json['driverRecommendations'] as List<dynamic>? ?? const [])
                .whereType<Map<String, dynamic>>()
                .map(DriverRecommendation.fromJson)
                .toList(),
        vehicleRecommendations:
            (json['vehicleRecommendations'] as List<dynamic>? ?? const [])
                .whereType<Map<String, dynamic>>()
                .map(VehicleRecommendation.fromJson)
                .toList(),
      );
}

class SyncMutation {
  final String clientMutationId;
  final String mutationType; // TRIP_STATUS | TRIP_PAYMENT | TRIP_EXPENSE | GPS_BATCH | VEHICLE_INSPECTION | LOST_AND_FOUND
  final String entityId;
  final int? expectedVersion;
  final String recordedAt;
  final Map<String, dynamic> payload;

  const SyncMutation({
    required this.clientMutationId,
    required this.mutationType,
    required this.entityId,
    this.expectedVersion,
    required this.recordedAt,
    required this.payload,
  });

  factory SyncMutation.fromJson(Map<String, dynamic> json) => SyncMutation(
    clientMutationId: (json['clientMutationId'] as String?) ?? '',
    mutationType: (json['mutationType'] as String?) ?? 'TRIP_STATUS',
    entityId: (json['entityId'] as String?) ?? '',
    expectedVersion: asNullableInt(json['expectedVersion']),
    recordedAt: (json['recordedAt'] as String?) ?? '',
    payload: (json['payload'] as Map<String, dynamic>?) ?? const {},
  );

  Map<String, dynamic> toJson() => {
    'clientMutationId': clientMutationId,
    'mutationType': mutationType,
    'entityId': entityId,
    'expectedVersion': expectedVersion,
    'recordedAt': recordedAt,
    'payload': payload,
  };
}

class StagedSyncConflict {
  final String id;
  final String clientMutationId;
  final String mutationType;
  final String entityId;
  final String summaryMessage;
  final String stagedAt;
  final Map<String, dynamic> localPayload;
  final bool resolved;

  const StagedSyncConflict({
    required this.id,
    required this.clientMutationId,
    required this.mutationType,
    required this.entityId,
    required this.summaryMessage,
    required this.stagedAt,
    required this.localPayload,
    this.resolved = false,
  });

  StagedSyncConflict copyWith({bool? resolved}) => StagedSyncConflict(
    id: id,
    clientMutationId: clientMutationId,
    mutationType: mutationType,
    entityId: entityId,
    summaryMessage: summaryMessage,
    stagedAt: stagedAt,
    localPayload: localPayload,
    resolved: resolved ?? this.resolved,
  );

  factory StagedSyncConflict.fromJson(Map<String, dynamic> json) =>
      StagedSyncConflict(
        id:
            (json['id'] as String?) ??
            (json['clientMutationId'] as String?) ??
            '',
        clientMutationId: (json['clientMutationId'] as String?) ?? '',
        mutationType: (json['mutationType'] as String?) ?? 'TRIP_STATUS',
        entityId: (json['entityId'] as String?) ?? '',
        summaryMessage:
            (json['summaryMessage'] as String?) ??
            (json['message'] as String?) ??
            'Konflik versi data terdeteksi saat sinkronisasi',
        stagedAt: (json['stagedAt'] as String?) ?? '',
        localPayload:
            (json['localPayload'] as Map<String, dynamic>?) ?? const {},
        resolved: asBool(json['resolved']),
      );

  Map<String, dynamic> toJson() => {
    'id': id,
    'clientMutationId': clientMutationId,
    'mutationType': mutationType,
    'entityId': entityId,
    'summaryMessage': summaryMessage,
    'stagedAt': stagedAt,
    'localPayload': localPayload,
    'resolved': resolved,
  };
}

class NotificationItem {
  final String id;
  final String reminderType; // UPCOMING_PICKUP | VEHICLE_TAX_EXPIRY | INSURANCE_EXPIRY | KIR_EXPIRY | SCHEDULED_MAINTENANCE | OUTSTANDING_CASH_HANDOVER | FCM_TEST_PUSH
  final String title;
  final String message;
  final String createdAt;
  final bool acknowledged;
  final String? referenceType;
  final String? referenceId;
  final String? fcmDeliveryStatus;
  final String? fcmMessageId;

  const NotificationItem({
    required this.id,
    required this.reminderType,
    required this.title,
    required this.message,
    required this.createdAt,
    this.acknowledged = false,
    this.referenceType,
    this.referenceId,
    this.fcmDeliveryStatus,
    this.fcmMessageId,
  });

  NotificationItem copyWith({bool? acknowledged}) => NotificationItem(
    id: id,
    reminderType: reminderType,
    title: title,
    message: message,
    createdAt: createdAt,
    acknowledged: acknowledged ?? this.acknowledged,
    referenceType: referenceType,
    referenceId: referenceId,
    fcmDeliveryStatus: fcmDeliveryStatus,
    fcmMessageId: fcmMessageId,
  );

  factory NotificationItem.fromJson(Map<String, dynamic> json) =>
      NotificationItem(
        id: (json['id'] as String?) ?? '',
        reminderType: (json['reminderType'] as String?) ?? 'UPCOMING_PICKUP',
        title: (json['title'] as String?) ?? '',
        message:
            (json['message'] as String?) ?? (json['body'] as String?) ?? '',
        createdAt:
            (json['createdAt'] as String?) ??
            (json['scheduledFor'] as String?) ??
            '',
        acknowledged:
            json.containsKey('acknowledged')
                ? asBool(json['acknowledged'])
                : asBool(json['isRead']),
        referenceType: json['referenceType'] as String?,
        referenceId: json['referenceId'] as String?,
        fcmDeliveryStatus: json['fcmDeliveryStatus'] as String?,
        fcmMessageId: json['fcmMessageId'] as String?,
      );

  Map<String, dynamic> toJson() => {
    'id': id,
    'reminderType': reminderType,
    'title': title,
    'message': message,
    'createdAt': createdAt,
    'acknowledged': acknowledged,
    'referenceType': referenceType,
    'referenceId': referenceId,
    'fcmDeliveryStatus': fcmDeliveryStatus,
    'fcmMessageId': fcmMessageId,
  };
}

class FcmRuntimeConfig {
  final String apiKey;
  final String appId;
  final String messagingSenderId;
  final String projectId;
  final String? vapidKey;

  const FcmRuntimeConfig({
    required this.apiKey,
    required this.appId,
    required this.messagingSenderId,
    required this.projectId,
    this.vapidKey,
  });

  bool get isComplete =>
      apiKey.trim().isNotEmpty &&
      appId.trim().isNotEmpty &&
      messagingSenderId.trim().isNotEmpty &&
      projectId.trim().isNotEmpty;

  factory FcmRuntimeConfig.fromJson(Map<String, dynamic> json) =>
      FcmRuntimeConfig(
        apiKey: (json['apiKey'] as String?) ?? '',
        appId: (json['appId'] as String?) ?? '',
        messagingSenderId: (json['messagingSenderId'] as String?) ?? '',
        projectId: (json['projectId'] as String?) ?? '',
        vapidKey: json['vapidKey'] as String?,
      );

  Map<String, dynamic> toJson() => {
    'apiKey': apiKey,
    'appId': appId,
    'messagingSenderId': messagingSenderId,
    'projectId': projectId,
    'vapidKey': vapidKey,
  };
}

class LiveFcmState {
  final bool clientInitialized;
  final bool permissionGranted;
  final String? deviceToken;
  final bool tokenSyncedToBackend;
  final bool backendLiveReady;
  final String backendProjectId;
  final int myActiveDevicesCount;
  final int fleetActiveDevicesCount;
  final String statusSummary;
  final String? liveBannerTitle;
  final String? liveBannerBody;
  final String? liveBannerRefType;
  final String? liveBannerRefId;

  const LiveFcmState({
    this.clientInitialized = false,
    this.permissionGranted = false,
    this.deviceToken,
    this.tokenSyncedToBackend = false,
    this.backendLiveReady = false,
    this.backendProjectId = 'mandiri-transport-ops',
    this.myActiveDevicesCount = 0,
    this.fleetActiveDevicesCount = 0,
    this.statusSummary =
        'Siaga: Menunggu inisialisasi google-services.json / FirebaseOptions.',
    this.liveBannerTitle,
    this.liveBannerBody,
    this.liveBannerRefType,
    this.liveBannerRefId,
  });

  bool get hasLiveBanner =>
      liveBannerTitle != null && liveBannerTitle!.trim().isNotEmpty;

  LiveFcmState copyWith({
    bool? clientInitialized,
    bool? permissionGranted,
    String? deviceToken,
    bool clearDeviceToken = false,
    bool? tokenSyncedToBackend,
    bool? backendLiveReady,
    String? backendProjectId,
    int? myActiveDevicesCount,
    int? fleetActiveDevicesCount,
    String? statusSummary,
    String? liveBannerTitle,
    String? liveBannerBody,
    String? liveBannerRefType,
    String? liveBannerRefId,
    bool clearBanner = false,
  }) {
    return LiveFcmState(
      clientInitialized: clientInitialized ?? this.clientInitialized,
      permissionGranted: permissionGranted ?? this.permissionGranted,
      deviceToken: clearDeviceToken ? null : (deviceToken ?? this.deviceToken),
      tokenSyncedToBackend: tokenSyncedToBackend ?? this.tokenSyncedToBackend,
      backendLiveReady: backendLiveReady ?? this.backendLiveReady,
      backendProjectId: backendProjectId ?? this.backendProjectId,
      myActiveDevicesCount: myActiveDevicesCount ?? this.myActiveDevicesCount,
      fleetActiveDevicesCount:
          fleetActiveDevicesCount ?? this.fleetActiveDevicesCount,
      statusSummary: statusSummary ?? this.statusSummary,
      liveBannerTitle: clearBanner
          ? null
          : (liveBannerTitle ?? this.liveBannerTitle),
      liveBannerBody: clearBanner
          ? null
          : (liveBannerBody ?? this.liveBannerBody),
      liveBannerRefType: clearBanner
          ? null
          : (liveBannerRefType ?? this.liveBannerRefType),
      liveBannerRefId: clearBanner
          ? null
          : (liveBannerRefId ?? this.liveBannerRefId),
    );
  }
}

class NotificationSettingItem {
  final String id;
  final String reminderType;
  final bool enabled;
  final int leadTimeMinutes;
  final String recipientScope; // BOTH | DRIVER | OWNER
  final bool acknowledgementEnabled;

  const NotificationSettingItem({
    required this.id,
    required this.reminderType,
    required this.enabled,
    required this.leadTimeMinutes,
    required this.recipientScope,
    required this.acknowledgementEnabled,
  });

  NotificationSettingItem copyWith({
    bool? enabled,
    int? leadTimeMinutes,
    String? recipientScope,
    bool? acknowledgementEnabled,
  }) => NotificationSettingItem(
    id: id,
    reminderType: reminderType,
    enabled: enabled ?? this.enabled,
    leadTimeMinutes: leadTimeMinutes ?? this.leadTimeMinutes,
    recipientScope: recipientScope ?? this.recipientScope,
    acknowledgementEnabled:
        acknowledgementEnabled ?? this.acknowledgementEnabled,
  );

  factory NotificationSettingItem.fromJson(Map<String, dynamic> json) =>
      NotificationSettingItem(
        id: (json['id'] as String?) ?? '',
        reminderType: (json['reminderType'] as String?) ?? 'UPCOMING_PICKUP',
        enabled: asBool(json['enabled'], true),
        leadTimeMinutes: asInt(json['leadTimeMinutes'], 60),
        recipientScope: (json['recipientScope'] as String?) ?? 'BOTH',
        acknowledgementEnabled: asBool(json['acknowledgementEnabled'], true),
      );

  Map<String, dynamic> toJson() => {
    'id': id,
    'reminderType': reminderType,
    'enabled': enabled,
    'leadTimeMinutes': leadTimeMinutes,
    'recipientScope': recipientScope,
    'acknowledgementEnabled': acknowledgementEnabled,
  };
}

class AnomalyAlert {
  final String id;
  final String ruleCode;
  final String title;
  final String subjectLabel;
  final String observedDisplay;
  final String baselineDisplay;
  final String thresholdDisplay;
  final String explanation;
  final String reviewHint;

  const AnomalyAlert({
    required this.id,
    required this.ruleCode,
    required this.title,
    required this.subjectLabel,
    required this.observedDisplay,
    required this.baselineDisplay,
    required this.thresholdDisplay,
    required this.explanation,
    required this.reviewHint,
  });

  factory AnomalyAlert.fromJson(Map<String, dynamic> json) => AnomalyAlert(
    id: (json['id'] as String?) ?? '',
    ruleCode: (json['ruleCode'] as String?) ?? '',
    title:
        (json['title'] as String?) ?? (json['name'] as String?) ?? 'Anomali',
    subjectLabel:
        (json['subjectLabel'] as String?) ??
        (json['tripCode'] as String?) ??
        '',
    observedDisplay:
        (json['observedDisplay'] as String?) ??
        '${json['observed'] ?? '-'}',
    baselineDisplay:
        (json['baselineDisplay'] as String?) ??
        '${json['baseline'] ?? '-'}',
    thresholdDisplay:
        (json['thresholdDisplay'] as String?) ??
        '${json['threshold'] ?? '-'}',
    explanation: (json['explanation'] as String?) ?? '',
    reviewHint:
        (json['reviewHint'] as String?) ??
        'Silakan periksa catatan perjalanan atau struk terkait secara wajar.',
  );
}

class RoutePerformanceItem {
  final String routeId;
  final String routeName;
  final int sampleCount;
  final int minSampleRequired;
  final bool sufficientSample;
  final double avgDurationMin;
  final double avgDistanceKm;
  final double avgEffectiveCost;
  final double avgActualProfit;

  const RoutePerformanceItem({
    required this.routeId,
    required this.routeName,
    required this.sampleCount,
    this.minSampleRequired = 5,
    required this.sufficientSample,
    required this.avgDurationMin,
    required this.avgDistanceKm,
    required this.avgEffectiveCost,
    required this.avgActualProfit,
  });

  factory RoutePerformanceItem.fromJson(Map<String, dynamic> json) {
    final count = asInt(json['sampleCount']);
    final minReq = asInt(json['minSampleRequired'], 5);
    return RoutePerformanceItem(
      routeId: (json['routeId'] as String?) ?? '',
      routeName: (json['routeName'] as String?) ?? '',
      sampleCount: count,
      minSampleRequired: minReq,
      sufficientSample: asBool(json['sufficientSample'], count >= minReq),
      avgDurationMin: asDouble(json['avgDurationMin']),
      avgDistanceKm: asDouble(json['avgDistanceKm']),
      avgEffectiveCost: asDouble(json['avgEffectiveCost']),
      avgActualProfit: asDouble(json['avgActualProfit']),
    );
  }
}

class CustomerProfitabilityItem {
  final String customerId;
  final String customerName;
  final int tripCount;
  final double totalRevenue;
  final double totalEffectiveCost;
  final double netOperatingProfit;
  final String pricingSummary;

  const CustomerProfitabilityItem({
    required this.customerId,
    required this.customerName,
    required this.tripCount,
    required this.totalRevenue,
    required this.totalEffectiveCost,
    required this.netOperatingProfit,
    required this.pricingSummary,
  });

  factory CustomerProfitabilityItem.fromJson(Map<String, dynamic> json) =>
      CustomerProfitabilityItem(
        customerId: (json['customerId'] as String?) ?? '',
        customerName: (json['customerName'] as String?) ?? '',
        tripCount: asInt(json['tripCount']),
        totalRevenue: asDouble(json['totalRevenue']),
        totalEffectiveCost: asDouble(json['totalEffectiveCost']),
        netOperatingProfit: asDouble(json['netOperatingProfit']),
        pricingSummary: (json['pricingSummary'] as String?) ?? '',
      );
}

class DriverWorkloadItem {
  final String driverId;
  final String driverName;
  final String availabilityStatus;
  final int completedTripsCount;
  final int upcomingTripsCount;
  final double totalActiveHours;
  final double maxDailyHours;

  const DriverWorkloadItem({
    required this.driverId,
    required this.driverName,
    required this.availabilityStatus,
    required this.completedTripsCount,
    required this.upcomingTripsCount,
    required this.totalActiveHours,
    required this.maxDailyHours,
  });
}

class VehicleReplacementItem {
  final String vehicleId;
  final String registrationNumber;
  final String nameModel;
  final int manufactureYear;
  final double odometerKm;
  final double totalRepairCost;
  final double totalDowntimeHours;
  final double repairCostPerKm;
  final bool flagForEvaluation;
  final String evaluationNote;

  const VehicleReplacementItem({
    required this.vehicleId,
    required this.registrationNumber,
    required this.nameModel,
    required this.manufactureYear,
    required this.odometerKm,
    required this.totalRepairCost,
    required this.totalDowntimeHours,
    required this.repairCostPerKm,
    required this.flagForEvaluation,
    required this.evaluationNote,
  });

  factory VehicleReplacementItem.fromJson(Map<String, dynamic> json) =>
      VehicleReplacementItem(
        vehicleId: (json['vehicleId'] as String?) ?? '',
        registrationNumber: (json['registrationNumber'] as String?) ?? '',
        nameModel: (json['nameModel'] as String?) ?? '',
        manufactureYear: asInt(json['manufactureYear'], 2020),
        odometerKm: asDouble(json['odometerKm']),
        totalRepairCost: asDouble(json['totalRepairCost']),
        totalDowntimeHours: asDouble(json['totalDowntimeHours']),
        repairCostPerKm: asDouble(json['repairCostPerKm']),
        flagForEvaluation: asBool(json['flagForEvaluation']),
        evaluationNote: (json['evaluationNote'] as String?) ?? '',
      );
}

class BiExplorerData {
  final double totalRevenue;
  final double totalActualExpenses;
  final double totalAllocatedCosts;
  final double grossOperatingProfit;
  final double unpaidBalances;
  final double driverHeldCash;
  final List<RoutePerformanceItem> routePerformance;
  final List<CustomerProfitabilityItem> customerProfitability;
  final List<DriverWorkloadItem> driverWorkloads;
  final List<VehicleReplacementItem> vehicleReplacementItems;
  final List<AnomalyAlert> anomalyAlerts;

  const BiExplorerData({
    required this.totalRevenue,
    required this.totalActualExpenses,
    required this.totalAllocatedCosts,
    required this.grossOperatingProfit,
    required this.unpaidBalances,
    required this.driverHeldCash,
    required this.routePerformance,
    required this.customerProfitability,
    required this.driverWorkloads,
    required this.vehicleReplacementItems,
    required this.anomalyAlerts,
  });
}
