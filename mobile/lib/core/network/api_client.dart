import 'dart:convert';
import 'package:http/http.dart' as http;

class ApiException implements Exception {
  final int statusCode;
  final String message;
  final String? errorCode;
  final dynamic data;

  const ApiException({
    required this.statusCode,
    required this.message,
    this.errorCode,
    this.data,
  });

  bool get isScheduleConflict =>
      statusCode == 409 || errorCode == 'SCHEDULE_CONFLICT';

  @override
  String toString() =>
      'ApiException($statusCode, code: $errorCode, message: $message)';
}

class ApiClient {
  static const String defaultBaseUrl = 'http://localhost:8080/api/v1';

  String _baseUrl;
  String? jwtToken;
  http.Client? httpClient;
  final Duration timeout;

  ApiClient({
    String baseUrl = defaultBaseUrl,
    this.jwtToken,
    this.httpClient,
    this.timeout = const Duration(seconds: 4),
  }) : _baseUrl = _normalizeBaseUrl(baseUrl);

  String get baseUrl => _baseUrl;
  http.Client get _client => httpClient ??= http.Client();

  void updateConfig({String? baseUrl, String? jwtToken}) {
    if (baseUrl != null && baseUrl.trim().isNotEmpty) {
      _baseUrl = _normalizeBaseUrl(baseUrl);
    }
    this.jwtToken = jwtToken;
  }

  void clearToken() {
    jwtToken = null;
  }

  static String _normalizeBaseUrl(String raw) {
    final trimmed = raw.trim();
    if (trimmed.endsWith('/')) {
      return trimmed.substring(0, trimmed.length - 1);
    }
    return trimmed;
  }

  Uri _buildUri(String path, [Map<String, String?>? queryParameters]) {
    final cleanPath = path.startsWith('/') ? path : '/$path';
    final uri = Uri.parse('$_baseUrl$cleanPath');
    if (queryParameters == null || queryParameters.isEmpty) {
      return uri;
    }
    final filtered = <String, String>{};
    queryParameters.forEach((key, value) {
      if (value != null && value.isNotEmpty) {
        filtered[key] = value;
      }
    });
    if (filtered.isEmpty) return uri;
    return uri.replace(queryParameters: filtered);
  }

  Map<String, String> _headers({String? clientMutationId}) {
    final headers = <String, String>{
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };
    if (jwtToken != null && jwtToken!.isNotEmpty) {
      headers['Authorization'] = 'Bearer $jwtToken';
    }
    if (clientMutationId != null && clientMutationId.isNotEmpty) {
      headers['X-Client-Mutation-Id'] = clientMutationId;
    }
    return headers;
  }

  Future<dynamic> get(
    String path, {
    Map<String, String?>? queryParameters,
  }) async {
    final uri = _buildUri(path, queryParameters);
    final response = await _client
        .get(uri, headers: _headers())
        .timeout(timeout);
    return _parseResponse(response);
  }

  Future<dynamic> post(
    String path, {
    Object? body,
    String? clientMutationId,
  }) async {
    final uri = _buildUri(path);
    final response = await _client
        .post(
          uri,
          headers: _headers(clientMutationId: clientMutationId),
          body: jsonEncode(body ?? const <String, dynamic>{}),
        )
        .timeout(timeout);
    return _parseResponse(response);
  }

  Future<dynamic> put(
    String path, {
    Object? body,
    String? clientMutationId,
  }) async {
    final uri = _buildUri(path);
    final response = await _client
        .put(
          uri,
          headers: _headers(clientMutationId: clientMutationId),
          body: jsonEncode(body ?? const <String, dynamic>{}),
        )
        .timeout(timeout);
    return _parseResponse(response);
  }

  dynamic _parseResponse(http.Response response) {
    Map<String, dynamic>? decoded;
    if (response.body.isNotEmpty) {
      final raw = jsonDecode(utf8.decode(response.bodyBytes));
      if (raw is Map<String, dynamic>) {
        decoded = raw;
      }
    }

    final isHttpSuccess =
        response.statusCode >= 200 && response.statusCode < 300;
    final apiSuccess = decoded?['success'] != false;

    if (isHttpSuccess && apiSuccess) {
      return decoded != null && decoded.containsKey('data')
          ? decoded['data']
          : decoded;
    }

    throw ApiException(
      statusCode: response.statusCode,
      message:
          (decoded?['message'] as String?) ??
          'Permintaan gagal (HTTP ${response.statusCode})',
      errorCode: decoded?['errorCode'] as String?,
      data: decoded?['data'],
    );
  }
}
