-- ============================================================================
-- V2: Realistic Medan Operational Seed Data (KNO, Berastagi, Kuala Tanjung)
-- ============================================================================

-- 1. Users (Owner + 4 Drivers)
INSERT INTO app_users (id, supabase_uid, username, full_name, phone, role, is_active) VALUES
('usr-owner-01', 'sb-owner-01', 'hendra', 'Pak Hendra Wijaya', '+628116001001', 'OWNER', TRUE),
('usr-drv-budi', 'sb-drv-budi', 'budi', 'Bang Budi Siregar', '+628126002001', 'DRIVER', TRUE),
('usr-drv-andi', 'sb-drv-andi', 'andi', 'Bang Andi Nasution', '+628126002002', 'DRIVER', TRUE),
('usr-drv-rudi', 'sb-drv-rudi', 'rudi', 'Bang Rudi Ginting', '+628126002003', 'DRIVER', TRUE),
('usr-drv-dedi', 'sb-drv-dedi', 'dedi', 'Bang Dedi Tarigan', '+628126002004', 'DRIVER', TRUE);

-- 2. Drivers
INSERT INTO drivers (id, user_id, driver_code, full_name, phone, license_number, license_expiry_date, availability_status, max_daily_hours, notes) VALUES
('drv-budi', 'usr-drv-budi', 'DRV-001', 'Bang Budi Siregar', '+628126002001', 'SIM-A-UMUM-120188', DATE '2028-05-15', 'AVAILABLE', 10.0, 'Berpengalaman rute Bandara KNO & tamu korporat.'),
('drv-andi', 'usr-drv-andi', 'DRV-002', 'Bang Andi Nasution', '+628126002002', 'SIM-A-UMUM-120299', DATE '2027-11-30', 'AVAILABLE', 10.0, 'Hafal jalur Berastagi, Danau Toba & Kuala Tanjung.'),
('drv-rudi', 'usr-drv-rudi', 'DRV-003', 'Bang Rudi Ginting', '+628126002003', 'SIM-B1-UMUM-120377', DATE '2027-08-20', 'AVAILABLE', 10.0, 'Spesialis Hiace rombongan keluarga & wisata.'),
('drv-dedi', 'usr-drv-dedi', 'DRV-004', 'Bang Dedi Tarigan', '+628126002004', 'SIM-A-UMUM-120455', DATE '2026-12-10', 'ON_LEAVE', 9.0, 'Sedang izin acara keluarga di Kabanjahe.');

-- 3. Vehicles
INSERT INTO vehicles (id, registration_number, name_model, brand, manufacture_year, vehicle_type, passenger_capacity, luggage_capacity_bags, status, current_odometer_km, fuel_type, suitability_tags, notes) VALUES
('veh-innova-01', 'BK 1188 MT', 'Kijang Innova Reborn 2.4 G AT', 'Toyota', 2022, 'MPV', 6, 5, 'ACTIVE', 84250.0, 'SOLAR_DEX', 'AIRPORT,INTERCITY,CHARTER,VIP', 'Unit favorit pelanggan tetap & korporat.'),
('veh-avanza-01', 'BK 2299 MT', 'Avanza Veloz 1.5 Q CVT', 'Toyota', 2023, 'MPV', 6, 4, 'ACTIVE', 62100.0, 'BENSIN', 'AIRPORT,LOCAL,INTERCITY', 'Irit BBM untuk antar-jemput KNO & dalam kota.'),
('veh-hiace-01', 'BK 7788 MT', 'Hiace Commuter 2.5 MT', 'Toyota', 2021, 'HIACE_VAN', 14, 12, 'ACTIVE', 112400.0, 'SOLAR_DEX', 'AIRPORT,INTERCITY,CHARTER,GROUP', 'Unit rombongan besar & charter Berastagi/Parapat.'),
('veh-fortuner-01', 'BK 4455 MT', 'Fortuner 2.4 VRZ AT', 'Toyota', 2018, 'SUV', 6, 5, 'IN_MAINTENANCE', 168900.0, 'SOLAR_DEX', 'INTERCITY,VIP,CHARTER', 'Sedang servis kaki-kaki & evaluasi biaya perawatan tinggi.');

-- Vehicle Documents
INSERT INTO vehicle_documents (id, vehicle_id, doc_type, doc_number, issue_date, expiry_date, reminder_days_before, notes) VALUES
('vdoc-01', 'veh-innova-01', 'ANNUAL_TAX_STNK', 'STNK-BK1188MT', DATE '2026-01-20', DATE '2026-11-28', 30, 'Pajak tahunan jatuh tempo akhir bulan ini'),
('vdoc-02', 'veh-innova-01', 'INSURANCE', 'ALLRISK-INV-2026', DATE '2026-03-01', DATE '2027-03-01', 30, 'Asuransi All-Risk Garda Oto'),
('vdoc-03', 'veh-avanza-01', 'ANNUAL_TAX_STNK', 'STNK-BK2299MT', DATE '2026-04-10', DATE '2027-04-10', 30, 'Pajak aktif'),
('vdoc-04', 'veh-hiace-01', 'KIR', 'KIR-MDN-7788', DATE '2026-06-01', DATE '2026-12-01', 21, 'Uji KIR 6 bulanan angkutan sewa'),
('vdoc-05', 'veh-fortuner-01', 'INSURANCE', 'TLO-FTN-2026', DATE '2025-11-20', DATE '2026-11-20', 30, 'Perlu evaluasi perpanjangan asuransi');

-- Vehicle Maintenance Schedules & Historical Logs
INSERT INTO vehicle_maintenance_schedules (id, vehicle_id, maintenance_type, interval_km, interval_months, last_done_odometer_km, last_done_date, next_due_odometer_km, next_due_date, estimated_cost) VALUES
('vms-01', 'veh-innova-01', 'OIL_CHANGE', 10000, 6, 80000, DATE '2026-08-15', 90000, DATE '2027-02-15', 950000),
('vms-02', 'veh-avanza-01', 'OIL_CHANGE', 10000, 6, 55000, DATE '2026-07-10', 65000, DATE '2027-01-10', 750000),
('vms-03', 'veh-fortuner-01', 'GENERAL_SERVICE', 10000, 6, 160000, DATE '2026-06-01', 170000, DATE '2026-11-15', 3200000);

INSERT INTO vehicle_maintenance_logs (id, vehicle_id, maintenance_type, service_date, odometer_km, vendor_name, cost_amount, downtime_hours, description, recorded_by) VALUES
('vml-01', 'veh-innova-01', 'OIL_CHANGE', DATE '2026-08-15', 80000, 'Auto2000 Gatot Subroto Medan', 950000, 3.0, 'Ganti oli mesin + filter oli rutin', 'usr-owner-01'),
('vml-02', 'veh-avanza-01', 'TIRE_REPLACEMENT', DATE '2026-09-05', 58000, 'Dunlop Shop Ringroad', 3400000, 4.0, 'Ganti 4 ban + spooring balancing', 'usr-owner-01'),
('vml-03', 'veh-fortuner-01', 'REPAIR', DATE '2026-08-20', 162500, 'Bengkel Spesialis Diesel Medan', 5800000, 36.0, 'Servis injektor & pompa solar', 'usr-owner-01'),
('vml-04', 'veh-fortuner-01', 'REPAIR', DATE '2026-10-05', 166200, 'Auto2000 Amplas', 4600000, 28.0, 'Ganti shockbreaker, tie rod & kompresor AC', 'usr-owner-01'),
('vml-05', 'veh-fortuner-01', 'BATTERY_REPLACEMENT', DATE '2026-11-02', 168700, 'Shop&Drive Medan Baru', 1950000, 2.0, 'Ganti aki GS Astra MF', 'usr-owner-01');

INSERT INTO vehicle_expenses (id, vehicle_id, maintenance_log_id, category, amount, expense_date, description, recorded_by) VALUES
('vexp-01', 'veh-innova-01', 'vml-01', 'OIL', 950000, DATE '2026-08-15', 'Ganti oli rutin Innova', 'usr-owner-01'),
('vexp-02', 'veh-avanza-01', 'vml-02', 'TIRE', 3400000, DATE '2026-09-05', '4 ban baru Avanza Veloz', 'usr-owner-01'),
('vexp-03', 'veh-fortuner-01', 'vml-03', 'REPAIR', 5800000, DATE '2026-08-20', 'Servis injektor Fortuner', 'usr-owner-01'),
('vexp-04', 'veh-fortuner-01', 'vml-04', 'REPAIR', 4600000, DATE '2026-10-05', 'Kaki-kaki & AC Fortuner', 'usr-owner-01'),
('vexp-05', 'veh-fortuner-01', 'vml-05', 'BATTERY', 1950000, DATE '2026-11-02', 'Aki baru Fortuner', 'usr-owner-01');

-- 4. Medan & North Sumatra Routes (with Configurable Operational Buffers)
INSERT INTO routes (id, code, name, origin_name, destination_name, typical_distance_km, default_duration_min, default_prep_buffer_min, default_arrival_buffer_min, default_reposition_buffer_min, is_long_distance, required_rest_after_min) VALUES
('rte-mdn-kno', 'MDN_KNO', 'Medan -> Bandara Kualanamu (KNO)', 'Medan', 'Bandara Kualanamu (KNO)', 39.0, 75, 15, 0, 15, FALSE, 0),
('rte-kno-mdn', 'KNO_MDN', 'Bandara Kualanamu (KNO) -> Medan', 'Bandara Kualanamu (KNO)', 'Medan', 39.0, 75, 15, 30, 15, FALSE, 0),
('rte-mdn-btg', 'MDN_BTG', 'Medan -> Berastagi', 'Medan', 'Berastagi', 68.0, 150, 20, 0, 30, TRUE, 45),
('rte-mdn-ktj', 'MDN_KTJ', 'Medan -> Kuala Tanjung', 'Medan', 'Kuala Tanjung', 115.0, 150, 20, 0, 30, TRUE, 60),
('rte-mdn-tjm', 'MDN_TJM', 'Medan -> Tanjung Morawa', 'Medan', 'Tanjung Morawa', 18.0, 45, 15, 0, 15, FALSE, 0),
('rte-mdn-city', 'MDN_CITY', 'Dalam Kota Medan / Charter Harian', 'Medan', 'Dalam Kota Medan', 35.0, 180, 15, 0, 15, FALSE, 0);

-- 5. Standard & Route Pricing Rules
INSERT INTO standard_pricing_configs (id, vehicle_type, base_fare, per_km_rate, hourly_charter_rate, full_day_charter_rate, minimum_fare) VALUES
('spc-mpv', 'MPV', 50000, 5500, 90000, 750000, 120000),
('spc-suv', 'SUV', 80000, 7500, 140000, 1200000, 200000),
('spc-hiace', 'HIACE_VAN', 150000, 9500, 180000, 1550000, 350000),
('spc-sedan', 'SEDAN', 60000, 6500, 110000, 900000, 150000);

INSERT INTO route_pricing_rules (id, route_id, vehicle_type, trip_type, suggested_fare, notes) VALUES
('rpr-01', 'rte-mdn-kno', 'MPV', 'AIRPORT_DROPOFF', 250000, 'Tarif standar MPV Medan -> KNO (termasuk tol biasa disepakati terpisah/all-in)'),
('rpr-02', 'rte-kno-mdn', 'MPV', 'AIRPORT_PICKUP', 260000, 'Tarif standar MPV jemput KNO -> Medan'),
('rpr-03', 'rte-mdn-kno', 'HIACE_VAN', 'AIRPORT_DROPOFF', 550000, 'Tarif Hiace rombongan ke KNO'),
('rpr-04', 'rte-kno-mdn', 'HIACE_VAN', 'AIRPORT_PICKUP', 575000, 'Tarif Hiace jemput KNO'),
('rpr-05', 'rte-mdn-btg', 'MPV', 'INTERCITY', 550000, 'Tarif sekali jalan Medan -> Berastagi'),
('rpr-06', 'rte-mdn-btg', 'HIACE_VAN', 'INTERCITY', 1150000, 'Tarif Hiace Medan -> Berastagi'),
('rpr-07', 'rte-mdn-ktj', 'MPV', 'INTERCITY', 700000, 'Tarif standar Medan -> Kuala Tanjung'),
('rpr-08', 'rte-mdn-tjm', 'MPV', 'LOCAL', 150000, 'Tarif Medan -> Tanjung Morawa');

-- 6. Customers, Saved Addresses, Shared Notes & Special Pricing Agreements
INSERT INTO customers (id, name, phone, whatsapp_phone, company_name, preferred_vehicle_type, created_by) VALUES
('cust-ratna', 'Ibu Ratna Kusuma', '+628116100101', '+628116100101', NULL, 'MPV', 'usr-owner-01'),
('cust-sawit', 'PT Sawit Makmur Nusantara', '+628116100202', '+628116100202', 'PT Sawit Makmur Nusantara', 'MPV', 'usr-owner-01'),
('cust-ahong', 'Ko Ahong (Keluarga Cemara Asri)', '+628126100303', '+628126100303', NULL, 'HIACE_VAN', 'usr-owner-01'),
('cust-marriott', 'Concierge Tamu VIP Merdeka Walk', '+628136100404', '+628136100404', 'Mitra Perhotelan Medan', 'MPV', 'usr-owner-01');

INSERT INTO customer_addresses (id, customer_id, label, address_text, latitude, longitude, google_maps_url, is_default) VALUES
('caddr-01', 'cust-ratna', 'Rumah Polonia', 'Jl. Kartini No. 18, Madras Hulu, Medan Polonia', 3.5752000, 98.6745000, 'https://www.google.com/maps/search/?api=1&query=3.5752,98.6745', TRUE),
('caddr-02', 'cust-sawit', 'Kantor Pusat Diponegoro', 'Jl. Pangeran Diponegoro No. 42, Medan Petisah', 3.5811000, 98.6738000, 'https://www.google.com/maps/search/?api=1&query=3.5811,98.6738', TRUE),
('caddr-03', 'cust-sawit', 'Pabrik Kuala Tanjung', 'Kawasan Industri Kuala Tanjung, Batu Bara', 3.3625000, 99.4422000, 'https://www.google.com/maps/search/?api=1&query=3.3625,99.4422', FALSE),
('caddr-04', 'cust-ahong', 'Rumah Cemara Asri', 'Komp. Cemara Asri Jl. Boulevard Raya No. 88, Medan', 3.6331000, 98.7049000, 'https://www.google.com/maps/search/?api=1&query=3.6331,98.7049', TRUE);

INSERT INTO customer_notes (id, customer_id, category, note_text, visible_to_drivers, created_by) VALUES
('cnote-01', 'cust-ratna', 'PREFERENCE', 'Pelanggan lama keluarga. Lebih suka kabin dingin sedang, bebas bau rokok, dan tidak ngebut.', TRUE, 'usr-owner-01'),
('cnote-02', 'cust-ratna', 'LUGGAGE', 'Biasanya membawa 2 koper besar + 1 kotak oleh-oleh Bika Ambon.', TRUE, 'usr-owner-01'),
('cnote-03', 'cust-sawit', 'OPERATIONAL', 'Tamu direksi korporat. Harap berpakaian rapi berkerah dan siapkan struk tol/parkir bila diminta.', TRUE, 'usr-owner-01'),
('cnote-04', 'cust-ahong', 'TIMING', 'Biasanya berangkat pagi jam 07.00 bersama keluarga besar, harap tiba 15 menit lebih awal.', TRUE, 'usr-owner-01');

INSERT INTO customer_pricing_agreements (id, customer_id, agreement_name, route_id, vehicle_type, pricing_mode, agreed_fare, discount_percent, effective_from, effective_to, conditions_note, created_by) VALUES
('cpa-01', 'cust-ratna', 'Harga Khusus Pelanggan Lama Medan-KNO', 'rte-mdn-kno', 'MPV', 'FIXED_FARE', 220000, NULL, DATE '2026-01-01', DATE '2027-12-31', 'Harga langganan keluarga Ibu Ratna (belum termasuk tol bila lewat tol).', 'usr-owner-01'),
('cpa-02', 'cust-ratna', 'Harga Khusus Jemput KNO-Medan', 'rte-kno-mdn', 'MPV', 'FIXED_FARE', 230000, NULL, DATE '2026-01-01', DATE '2027-12-31', 'Sudah termasuk tunggu bagasi wajar 30 menit.', 'usr-owner-01'),
('cpa-03', 'cust-sawit', 'Kontrak Korporat Medan-Kuala Tanjung', 'rte-mdn-ktj', 'MPV', 'FIXED_FARE', 650000, NULL, DATE '2026-01-01', DATE '2026-12-31', 'Tarif kontrak korporat PT Sawit Makmur (Innova Reborn).', 'usr-owner-01'),
('cpa-04', 'cust-sawit', 'Diskon Umum Korporat 10%', NULL, NULL, 'DISCOUNT_PERCENT', NULL, 10.0, DATE '2026-01-01', DATE '2026-12-31', 'Berlaku untuk rute selain yang sudah ada harga khusus.', 'usr-owner-01');

-- 7. Reusable WhatsApp Message Templates & Booking Templates
INSERT INTO whatsapp_templates (id, code, title, category, body_template) VALUES
('wat-01', 'KONFIRMASI_BOOKING', 'Konfirmasi Pesanan Perjalanan', 'BOOKING', 'Selamat siang Bapak/Ibu {customer_name}, pesanan Mandiri Transport untuk tanggal {pickup_time} dari {pickup_address} tujuan {destination_address} telah tercatat dengan tarif disepakati {agreed_fare}. Terima kasih.'),
('wat-02', 'INFO_SUPIR_BERANGKAT', 'Info Supir & Mobil Menuju Lokasi', 'DISPATCH', 'Halo Bapak/Ibu {customer_name}, supir kami {driver_name} ({driver_phone}) dengan unit {vehicle_model} ({vehicle_plate}) sedang menuju lokasi jemput di {pickup_address}.'),
('wat-03', 'JEMPUT_KNO', 'Titik Jemput Bandara Kualanamu (KNO)', 'AIRPORT', 'Selamat datang di Bandara Kualanamu Bapak/Ibu {customer_name}. Supir kami {driver_name} ({vehicle_plate}) sudah standby. Silakan kabari setelah pengambilan bagasi selesai agar mobil langsung merapat ke pintu kedatangan.'),
('wat-04', 'KONFIRMASI_PEMBAYARAN', 'Tanda Terima Pembayaran Perjalanan', 'FINANCE', 'Terima kasih Bapak/Ibu {customer_name}, pembayaran perjalanan tujuan {destination_address} sebesar {agreed_fare} telah kami terima. Sampai jumpa di perjalanan berikutnya.');

INSERT INTO booking_templates (id, name, customer_id, route_id, trip_type, booking_source, pickup_address, destination_address, default_passenger_count, default_luggage_count, luggage_notes, preferred_vehicle_type, notes, created_by) VALUES
('btpl-01', 'Ibu Ratna — Rumah Polonia ke Bandara KNO', 'cust-ratna', 'rte-mdn-kno', 'AIRPORT_DROPOFF', 'RETURNING_CUSTOMER', 'Jl. Kartini No. 18, Medan Polonia', 'Bandara Internasional Kualanamu (KNO)', 2, 2, '2 koper besar', 'MPV', 'Template cepat langganan Ibu Ratna', 'usr-owner-01'),
('btpl-02', 'PT Sawit Makmur — Kantor ke Pabrik Kuala Tanjung', 'cust-sawit', 'rte-mdn-ktj', 'INTERCITY', 'WHATSAPP', 'Jl. Pangeran Diponegoro No. 42, Medan', 'Kawasan Industri Kuala Tanjung, Batu Bara', 3, 2, 'Tas kerja direksi', 'MPV', 'Kunjungan rutin direksi', 'usr-owner-01'),
('btpl-03', 'Rombongan Cemara Asri — Wisata Berastagi', 'cust-ahong', 'rte-mdn-btg', 'INTERCITY', 'RETURNING_CUSTOMER', 'Komp. Cemara Asri Jl. Boulevard Raya No. 88, Medan', 'Mikie Holiday / Pasar Buah Berastagi', 10, 6, 'Keluarga besar', 'HIACE_VAN', 'Berangkat pagi jam 07.00 WIB', 'usr-owner-01');

-- 8. Versioned Operating Cost Formulas (v1 Historical + v2 Active)
INSERT INTO cost_formula_versions (id, version_number, name, effective_from, effective_to, notes, is_active, created_by) VALUES
('cfv-v1', 1, 'Formula Biaya Operasional Awal 2026', DATE '2026-01-01', DATE '2026-05-31', 'Formula semester 1 sebelum penyesuaian harga ban & oli.', FALSE, 'usr-owner-01'),
('cfv-v2', 2, 'Formula Biaya Operasional Semester II 2026', DATE '2026-06-01', NULL, 'Formula aktif: alokasi ban, oli, cadangan servis, penyusutan & komisi supir.', TRUE, 'usr-owner-01');

INSERT INTO cost_formula_components (id, formula_version_id, component_code, name, calc_type, reconciliation_policy, expense_category_link, fixed_amount, rate_value, monthly_amount, assumed_monthly_trips, expression_formula, parameters_json, sort_order) VALUES
-- v1 historical
('cfc-v1-fuel', 'cfv-v1', 'FUEL', 'Estimasi BBM', 'PER_KM', 'ACTUAL_OVERRIDES_ESTIMATE', 'FUEL', NULL, 1250.0000, NULL, NULL, NULL, NULL, 1),
('cfc-v1-drv', 'cfv-v1', 'DRIVER_COMPENSATION', 'Komisi Supir (20%)', 'PERCENT_OF_FARE', 'ALLOCATED_ONLY', NULL, NULL, 0.2000, NULL, NULL, NULL, NULL, 2),
-- v2 active
('cfc-v2-fuel', 'cfv-v2', 'FUEL', 'BBM (Aktual atau Estimasi Rp1.350/km)', 'PER_KM', 'ACTUAL_OVERRIDES_ESTIMATE', 'FUEL', NULL, 1350.0000, NULL, NULL, NULL, NULL, 1),
('cfc-v2-toll', 'cfv-v2', 'TOLL', 'Biaya Tol Aktual', 'FIXED_PER_TRIP', 'ACTUAL_EXPENSE_ONLY', 'TOLL', 0.00, NULL, NULL, NULL, NULL, NULL, 2),
('cfc-v2-park', 'cfv-v2', 'PARKING', 'Parkir Bandara / Lokasi Aktual', 'FIXED_PER_TRIP', 'ACTUAL_EXPENSE_ONLY', 'PARKING', 0.00, NULL, NULL, NULL, NULL, NULL, 3),
('cfc-v2-tire', 'cfv-v2', 'TIRE_WEAR', 'Alokasi Aus Ban (4 Ban / 55.000 km)', 'FORMULA_ALLOCATION', 'ALLOCATED_ONLY', NULL, NULL, NULL, NULL, NULL, '(replacement_cost / expected_lifespan_km) * distance_km', '{"replacement_cost":4400000,"expected_lifespan_km":55000}', 4),
('cfc-v2-oil', 'cfv-v2', 'ENGINE_OIL', 'Alokasi Oli & Filter (Rp850rb / 10.000 km)', 'FORMULA_ALLOCATION', 'ALLOCATED_ONLY', NULL, NULL, NULL, NULL, NULL, '(service_cost / service_interval_km) * distance_km', '{"service_cost":850000,"service_interval_km":10000}', 5),
('cfc-v2-maint', 'cfv-v2', 'MAINTENANCE_RESERVE', 'Cadangan Servis Berkala (Rp250/km)', 'PER_KM', 'ALLOCATED_ONLY', NULL, NULL, 250.0000, NULL, NULL, NULL, NULL, 6),
('cfc-v2-depr', 'cfv-v2', 'DEPRECIATION', 'Alokasi Penyusutan Unit Bulanan', 'MONTHLY_ALLOCATION', 'ALLOCATED_ONLY', NULL, NULL, NULL, 1800000.00, 60, NULL, NULL, 7),
('cfc-v2-drv', 'cfv-v2', 'DRIVER_COMPENSATION', 'Komisi / Uang Jalan Supir (20% Tarif)', 'PERCENT_OF_FARE', 'ALLOCATED_ONLY', NULL, NULL, 0.2000, NULL, NULL, NULL, NULL, 8);

-- 9. Notification Settings & Anomaly Detection Rules
INSERT INTO notification_settings (id, reminder_type, enabled, lead_time_minutes, recipient_scope, acknowledgement_enabled) VALUES
('nset-01', 'UPCOMING_PICKUP', TRUE, 60, 'BOTH', TRUE),
('nset-02', 'DRIVER_ASSIGNED', TRUE, 0, 'DRIVER', FALSE),
('nset-03', 'DRIVER_NOT_STARTED', TRUE, 15, 'OWNER', FALSE),
('nset-04', 'OVERDUE_TRIP', TRUE, 30, 'OWNER', FALSE),
('nset-05', 'VEHICLE_TAX_EXPIRY', TRUE, 43200, 'OWNER', FALSE),
('nset-06', 'INSURANCE_EXPIRY', TRUE, 43200, 'OWNER', FALSE),
('nset-07', 'SCHEDULED_MAINTENANCE', TRUE, 10080, 'OWNER', FALSE),
('nset-08', 'OUTSTANDING_CASH_HANDOVER', TRUE, 1440, 'OWNER', FALSE);

INSERT INTO anomaly_rules (id, rule_code, name, enabled, threshold_multiplier, threshold_absolute, min_sample_size, description) VALUES
('arul-01', 'HIGH_FUEL_EXPENSE', 'Konsumsi BBM di Atas Normal Rute', TRUE, 1.400, NULL, 5, 'Menandai perjalanan dengan biaya BBM > 1,4x rata-rata historis rute yang sama.'),
('arul-02', 'LONG_TRIP_DURATION', 'Durasi Perjalanan Jauh Lebih Lama', TRUE, 1.500, NULL, 5, 'Menandai perjalanan dengan durasi aktual > 1,5x rata-rata historis rute.'),
('arul-03', 'UNEXPECTED_TRIP_EXPENSE', 'Pengeluaran Perjalanan Tak Terduga', TRUE, NULL, 150000.00, 1, 'Menandai pengeluaran tunggal non-BBM di atas Rp150.000 untuk ditinjau.'),
('arul-04', 'UNUSUAL_MAINTENANCE_COST', 'Biaya Perawatan Unit Tinggi', TRUE, 1.600, 4000000.00, 3, 'Menandai servis/perbaikan kendaraan yang melebihi ambang wajar.'),
('arul-05', 'AGED_DRIVER_CASH', 'Setoran Tunai Supir Belum Dikonfirmasi', TRUE, NULL, 300000.00, 1, 'Menandai uang tunai pelanggan di tangan supir > 24 jam atau > Rp300.000.'),
('arul-06', 'COST_ESTIMATE_VARIANCE', 'Selisih Besar Estimasi vs Biaya Aktual', TRUE, 1.350, NULL, 1, 'Menandai perjalanan dengan realisasi biaya > 1,35x estimasi formula.');

-- 10. Historical Completed Bookings & Trips (Rich Data for Route BI, Customer Profit & Cash Custody)
INSERT INTO bookings (id, booking_code, customer_id, contact_name, contact_phone, booking_source, trip_type, route_id, requested_pickup_at, pickup_address, destination_address, passenger_count, luggage_count, flight_number, suggested_fare, pricing_tier_used, agreed_fare, status, created_by) VALUES
('bkg-101', 'BKG-20261101-01', 'cust-ratna', 'Ibu Ratna Kusuma', '+628116100101', 'RETURNING_CUSTOMER', 'AIRPORT_DROPOFF', 'rte-mdn-kno', TIMESTAMP WITH TIME ZONE '2026-11-01T07:00:00+07:00', 'Jl. Kartini No. 18, Medan Polonia', 'Bandara Kualanamu (KNO)', 2, 2, 'GA-183', 220000, 'CUSTOMER_ROUTE_AGREEMENT', 220000, 'COMPLETED', 'usr-owner-01'),
('bkg-102', 'BKG-20261102-01', 'cust-marriott', 'Bpk. Daniel Tan', '+628136100404', 'REFERRAL', 'AIRPORT_DROPOFF', 'rte-mdn-kno', TIMESTAMP WITH TIME ZONE '2026-11-02T09:00:00+07:00', 'Merdeka Walk, Medan Barat', 'Bandara Kualanamu (KNO)', 3, 3, 'SQ-991', 250000, 'ROUTE_PRICING_RULE', 260000, 'COMPLETED', 'usr-owner-01'),
('bkg-103', 'BKG-20261103-01', 'cust-sawit', 'Direksi PT Sawit Makmur', '+628116100202', 'WHATSAPP', 'AIRPORT_DROPOFF', 'rte-mdn-kno', TIMESTAMP WITH TIME ZONE '2026-11-03T06:30:00+07:00', 'Jl. Pangeran Diponegoro No. 42, Medan', 'Bandara Kualanamu (KNO)', 2, 2, 'GA-181', 225000, 'CUSTOMER_GENERAL_AGREEMENT', 250000, 'COMPLETED', 'usr-owner-01'),
('bkg-104', 'BKG-20261104-01', 'cust-ratna', 'Ibu Ratna Kusuma', '+628116100101', 'RETURNING_CUSTOMER', 'AIRPORT_DROPOFF', 'rte-mdn-kno', TIMESTAMP WITH TIME ZONE '2026-11-04T14:00:00+07:00', 'Jl. Kartini No. 18, Medan Polonia', 'Bandara Kualanamu (KNO)', 1, 2, 'ID-6885', 220000, 'CUSTOMER_ROUTE_AGREEMENT', 220000, 'COMPLETED', 'usr-owner-01'),
('bkg-105', 'BKG-20261105-01', 'cust-ahong', 'Ko Ahong', '+628126100303', 'PHONE_CALL', 'AIRPORT_DROPOFF', 'rte-mdn-kno', TIMESTAMP WITH TIME ZONE '2026-11-05T08:00:00+07:00', 'Komp. Cemara Asri No. 88, Medan', 'Bandara Kualanamu (KNO)', 10, 8, 'MH-861', 550000, 'ROUTE_PRICING_RULE', 550000, 'COMPLETED', 'usr-owner-01'),
('bkg-106', 'BKG-20261106-01', 'cust-sawit', 'Tim Pabrik PT Sawit Makmur', '+628116100202', 'WHATSAPP', 'INTERCITY', 'rte-mdn-ktj', TIMESTAMP WITH TIME ZONE '2026-11-06T07:30:00+07:00', 'Jl. Pangeran Diponegoro No. 42, Medan', 'Kawasan Industri Kuala Tanjung', 3, 2, NULL, 650000, 'CUSTOMER_ROUTE_AGREEMENT', 650000, 'COMPLETED', 'usr-owner-01'),
-- Today / Upcoming Active Bookings (2026-11-09 & 2026-11-10)
('bkg-201', 'BKG-20261109-01', 'cust-ratna', 'Ibu Ratna Kusuma', '+628116100101', 'WHATSAPP', 'AIRPORT_PICKUP', 'rte-kno-mdn', TIMESTAMP WITH TIME ZONE '2026-11-09T21:00:00+07:00', 'Bandara Kualanamu (KNO) Pintu Kedatangan Domestik', 'Jl. Kartini No. 18, Medan Polonia', 2, 2, 'GA-190', 230000, 'CUSTOMER_ROUTE_AGREEMENT', 230000, 'CONFIRMED', 'usr-owner-01'),
('bkg-202', 'BKG-20261110-01', 'cust-sawit', 'Direksi PT Sawit Makmur', '+628116100202', 'WHATSAPP', 'INTERCITY', 'rte-mdn-ktj', TIMESTAMP WITH TIME ZONE '2026-11-10T08:00:00+07:00', 'Jl. Pangeran Diponegoro No. 42, Medan', 'Kawasan Industri Kuala Tanjung', 4, 3, NULL, 650000, 'CUSTOMER_ROUTE_AGREEMENT', 650000, 'CONFIRMED', 'usr-owner-01'),
('bkg-203', 'BKG-20261110-02', 'cust-ahong', 'Ko Ahong & Keluarga', '+628126100303', 'RETURNING_CUSTOMER', 'INTERCITY', 'rte-mdn-btg', TIMESTAMP WITH TIME ZONE '2026-11-10T09:00:00+07:00', 'Komp. Cemara Asri No. 88, Medan', 'Mikie Holiday Resort, Berastagi', 11, 7, NULL, 1150000, 'ROUTE_PRICING_RULE', 1150000, 'CONFIRMED', 'usr-owner-01');

INSERT INTO trips (id, trip_code, booking_id, leg_number, leg_label, route_id, trip_type, driver_id, vehicle_id, scheduled_pickup_at, estimated_duration_min, prep_buffer_min, arrival_buffer_min, reposition_buffer_min, reserved_start_at, reserved_end_at, pickup_address, destination_address, passenger_count, luggage_count, flight_number, agreed_fare_snapshot, status, odometer_start_km, odometer_end_km, gps_distance_km, actual_started_at, actual_completed_at, actual_duration_min, payment_status, total_paid_amount, cash_custody_status, driver_cash_held_amount, created_by) VALUES
('trp-101', 'TRP-20261101-01', 'bkg-101', 1, 'Outbound', 'rte-mdn-kno', 'AIRPORT_DROPOFF', 'drv-budi', 'veh-avanza-01', TIMESTAMP WITH TIME ZONE '2026-11-01T07:00:00+07:00', 75, 15, 0, 15, TIMESTAMP WITH TIME ZONE '2026-11-01T06:45:00+07:00', TIMESTAMP WITH TIME ZONE '2026-11-01T08:30:00+07:00', 'Jl. Kartini No. 18, Medan Polonia', 'Bandara Kualanamu (KNO)', 2, 2, 'GA-183', 220000, 'COMPLETED', 61800, 61841, 40.5, TIMESTAMP WITH TIME ZONE '2026-11-01T07:02:00+07:00', TIMESTAMP WITH TIME ZONE '2026-11-01T08:12:00+07:00', 70, 'PAID', 220000, 'HANDED_OVER_CONFIRMED', 0, 'usr-owner-01'),
('trp-102', 'TRP-20261102-01', 'bkg-102', 1, 'Outbound', 'rte-mdn-kno', 'AIRPORT_DROPOFF', 'drv-budi', 'veh-innova-01', TIMESTAMP WITH TIME ZONE '2026-11-02T09:00:00+07:00', 75, 15, 0, 15, TIMESTAMP WITH TIME ZONE '2026-11-02T08:45:00+07:00', TIMESTAMP WITH TIME ZONE '2026-11-02T10:30:00+07:00', 'Merdeka Walk, Medan Barat', 'Bandara Kualanamu (KNO)', 3, 3, 'SQ-991', 260000, 'COMPLETED', 83900, 83940, 39.8, TIMESTAMP WITH TIME ZONE '2026-11-02T09:00:00+07:00', TIMESTAMP WITH TIME ZONE '2026-11-02T10:14:00+07:00', 74, 'PAID', 260000, 'HANDED_OVER_CONFIRMED', 0, 'usr-owner-01'),
('trp-103', 'TRP-20261103-01', 'bkg-103', 1, 'Outbound', 'rte-mdn-kno', 'AIRPORT_DROPOFF', 'drv-andi', 'veh-innova-01', TIMESTAMP WITH TIME ZONE '2026-11-03T06:30:00+07:00', 75, 15, 0, 15, TIMESTAMP WITH TIME ZONE '2026-11-03T06:15:00+07:00', TIMESTAMP WITH TIME ZONE '2026-11-03T08:00:00+07:00', 'Jl. Pangeran Diponegoro No. 42, Medan', 'Bandara Kualanamu (KNO)', 2, 2, 'GA-181', 250000, 'COMPLETED', 83980, 84020, 39.5, TIMESTAMP WITH TIME ZONE '2026-11-03T06:28:00+07:00', TIMESTAMP WITH TIME ZONE '2026-11-03T07:38:00+07:00', 70, 'PAID', 250000, 'NO_DRIVER_CASH_DUE', 0, 'usr-owner-01'),
('trp-104', 'TRP-20261104-01', 'bkg-104', 1, 'Outbound', 'rte-mdn-kno', 'AIRPORT_DROPOFF', 'drv-budi', 'veh-avanza-01', TIMESTAMP WITH TIME ZONE '2026-11-04T14:00:00+07:00', 75, 15, 0, 15, TIMESTAMP WITH TIME ZONE '2026-11-04T13:45:00+07:00', TIMESTAMP WITH TIME ZONE '2026-11-04T15:30:00+07:00', 'Jl. Kartini No. 18, Medan Polonia', 'Bandara Kualanamu (KNO)', 1, 2, 'ID-6885', 220000, 'COMPLETED', 61900, 61942, 41.2, TIMESTAMP WITH TIME ZONE '2026-11-04T14:00:00+07:00', TIMESTAMP WITH TIME ZONE '2026-11-04T16:15:00+07:00', 135, 'PAID', 220000, 'HELD_BY_DRIVER', 220000, 'usr-owner-01'),
('trp-105', 'TRP-20261105-01', 'bkg-105', 1, 'Outbound', 'rte-mdn-kno', 'AIRPORT_DROPOFF', 'drv-rudi', 'veh-hiace-01', TIMESTAMP WITH TIME ZONE '2026-11-05T08:00:00+07:00', 75, 15, 0, 15, TIMESTAMP WITH TIME ZONE '2026-11-05T07:45:00+07:00', TIMESTAMP WITH TIME ZONE '2026-11-05T09:30:00+07:00', 'Komp. Cemara Asri No. 88, Medan', 'Bandara Kualanamu (KNO)', 10, 8, 'MH-861', 550000, 'COMPLETED', 112310, 112346, 35.8, TIMESTAMP WITH TIME ZONE '2026-11-05T08:00:00+07:00', TIMESTAMP WITH TIME ZONE '2026-11-05T09:05:00+07:00', 65, 'PAID', 550000, 'NO_DRIVER_CASH_DUE', 0, 'usr-owner-01'),
('trp-106', 'TRP-20261106-01', 'bkg-106', 1, 'Outbound', 'rte-mdn-ktj', 'INTERCITY', 'drv-andi', 'veh-innova-01', TIMESTAMP WITH TIME ZONE '2026-11-06T07:30:00+07:00', 150, 20, 0, 30, TIMESTAMP WITH TIME ZONE '2026-11-06T07:10:00+07:00', TIMESTAMP WITH TIME ZONE '2026-11-06T10:30:00+07:00', 'Jl. Pangeran Diponegoro No. 42, Medan', 'Kawasan Industri Kuala Tanjung', 3, 2, NULL, 650000, 'COMPLETED', 84120, 84238, 117.5, TIMESTAMP WITH TIME ZONE '2026-11-06T07:30:00+07:00', TIMESTAMP WITH TIME ZONE '2026-11-06T09:58:00+07:00', 148, 'PAID', 650000, 'HELD_BY_DRIVER', 650000, 'usr-owner-01'),
-- Active / Upcoming Trips
('trp-201', 'TRP-20261109-01', 'bkg-201', 1, 'Jemput Bandara', 'rte-kno-mdn', 'AIRPORT_PICKUP', 'drv-budi', 'veh-avanza-01', TIMESTAMP WITH TIME ZONE '2026-11-09T21:00:00+07:00', 75, 15, 30, 15, TIMESTAMP WITH TIME ZONE '2026-11-09T20:45:00+07:00', TIMESTAMP WITH TIME ZONE '2026-11-09T23:00:00+07:00', 'Bandara Kualanamu (KNO) Pintu Kedatangan Domestik', 'Jl. Kartini No. 18, Medan Polonia', 2, 2, 'GA-190', 230000, 'ASSIGNED', NULL, NULL, NULL, NULL, NULL, NULL, 'UNPAID', 0, 'NO_DRIVER_CASH_DUE', 0, 'usr-owner-01'),
('trp-202', 'TRP-20261110-01', 'bkg-202', 1, 'Outbound', 'rte-mdn-ktj', 'INTERCITY', 'drv-andi', 'veh-innova-01', TIMESTAMP WITH TIME ZONE '2026-11-10T08:00:00+07:00', 150, 20, 0, 30, TIMESTAMP WITH TIME ZONE '2026-11-10T07:40:00+07:00', TIMESTAMP WITH TIME ZONE '2026-11-10T11:00:00+07:00', 'Jl. Pangeran Diponegoro No. 42, Medan', 'Kawasan Industri Kuala Tanjung', 4, 3, NULL, 650000, 'ASSIGNED', NULL, NULL, NULL, NULL, NULL, NULL, 'PARTIALLY_PAID', 200000, 'NO_DRIVER_CASH_DUE', 0, 'usr-owner-01'),
('trp-203', 'TRP-20261110-02', 'bkg-203', 1, 'Outbound', 'rte-mdn-btg', 'INTERCITY', NULL, NULL, TIMESTAMP WITH TIME ZONE '2026-11-10T09:00:00+07:00', 150, 20, 0, 30, TIMESTAMP WITH TIME ZONE '2026-11-10T08:40:00+07:00', TIMESTAMP WITH TIME ZONE '2026-11-10T12:00:00+07:00', 'Komp. Cemara Asri No. 88, Medan', 'Mikie Holiday Resort, Berastagi', 11, 7, NULL, 1150000, 'UNASSIGNED', NULL, NULL, NULL, NULL, NULL, NULL, 'UNPAID', 0, 'NO_DRIVER_CASH_DUE', 0, 'usr-owner-01');

-- Payments & Cash Custody Seed Records
INSERT INTO trip_payments (id, trip_id, amount, payment_method, collected_by_user_id, collected_by_role, held_by_driver_id, handed_over_amount, custody_settled, paid_at, reference_number, notes) VALUES
('pay-101', 'trp-101', 220000, 'CASH', 'usr-drv-budi', 'DRIVER', 'drv-budi', 220000, TRUE, TIMESTAMP WITH TIME ZONE '2026-11-01T08:15:00+07:00', NULL, 'Tunai diterima Bang Budi & sudah disetor ke Pak Hendra'),
('pay-102', 'trp-102', 260000, 'CASH', 'usr-drv-budi', 'DRIVER', 'drv-budi', 260000, TRUE, TIMESTAMP WITH TIME ZONE '2026-11-02T10:15:00+07:00', NULL, 'Tunai tamu Merdeka Walk'),
('pay-103', 'trp-103', 250000, 'BANK_TRANSFER', 'usr-owner-01', 'OWNER', NULL, 0, TRUE, TIMESTAMP WITH TIME ZONE '2026-11-03T11:00:00+07:00', 'TRF-BCA-99012', 'Transfer langsung ke rekening Pak Hendra'),
('pay-104', 'trp-104', 220000, 'CASH', 'usr-drv-budi', 'DRIVER', 'drv-budi', 0, FALSE, TIMESTAMP WITH TIME ZONE '2026-11-04T16:16:00+07:00', NULL, 'Tunai Ibu Ratna — masih dipegang Bang Budi'),
('pay-105', 'trp-105', 550000, 'BANK_TRANSFER', 'usr-owner-01', 'OWNER', NULL, 0, TRUE, TIMESTAMP WITH TIME ZONE '2026-11-05T09:30:00+07:00', 'TRF-MDR-88210', 'Transfer Ko Ahong'),
('pay-106', 'trp-106', 650000, 'CASH', 'usr-drv-andi', 'DRIVER', 'drv-andi', 0, FALSE, TIMESTAMP WITH TIME ZONE '2026-11-06T10:05:00+07:00', NULL, 'Tunai perjalanan Kuala Tanjung — masih dipegang Bang Andi'),
('pay-202', 'trp-202', 200000, 'BANK_TRANSFER', 'usr-owner-01', 'OWNER', NULL, 0, TRUE, TIMESTAMP WITH TIME ZONE '2026-11-09T15:00:00+07:00', 'DP-SAWIT-1110', 'DP 200rb perjalanan besok');

INSERT INTO cash_handovers (id, handover_code, driver_id, received_by_owner_id, declared_amount, confirmed_amount, status, driver_notes, owner_notes, handed_over_at, confirmed_at) VALUES
('cho-01', 'CHO-20261102-01', 'drv-budi', 'usr-owner-01', 480000, 480000, 'HANDED_OVER_CONFIRMED', 'Setor tunai 2 trip KNO (1 & 2 Nov)', 'Uang pas diterima di garasi', TIMESTAMP WITH TIME ZONE '2026-11-02T18:00:00+07:00', TIMESTAMP WITH TIME ZONE '2026-11-02T18:05:00+07:00');

INSERT INTO cash_handover_items (id, handover_id, trip_payment_id, trip_id, amount_applied) VALUES
('choi-01', 'cho-01', 'pay-101', 'trp-101', 220000),
('choi-02', 'cho-01', 'pay-102', 'trp-102', 260000);

-- Trip Expenses (Fuel, Toll, Parking)
INSERT INTO trip_expenses (id, trip_id, category, amount, paid_by_user_id, paid_by_role, fuel_liters, notes, incurred_at) VALUES
('texp-101-f', 'trp-101', 'FUEL', 52000, 'usr-drv-budi', 'DRIVER', 5.2, 'Pertalite SPBU Cemara', TIMESTAMP WITH TIME ZONE '2026-11-01T08:20:00+07:00'),
('texp-101-t', 'trp-101', 'TOLL', 28000, 'usr-drv-budi', 'DRIVER', NULL, 'Tol Tanjung Mulia - Kualanamu', TIMESTAMP WITH TIME ZONE '2026-11-01T07:35:00+07:00'),
('texp-102-f', 'trp-102', 'FUEL', 58000, 'usr-drv-budi', 'DRIVER', 4.2, 'Solar Dex Innova', TIMESTAMP WITH TIME ZONE '2026-11-02T10:20:00+07:00'),
('texp-103-f', 'trp-103', 'FUEL', 55000, 'usr-drv-andi', 'DRIVER', 4.0, 'Solar Dex Innova', TIMESTAMP WITH TIME ZONE '2026-11-03T07:50:00+07:00'),
('texp-104-f', 'trp-104', 'FUEL', 96000, 'usr-drv-budi', 'DRIVER', 9.6, 'Macet panjang jalan arteri & antre panjang (anomali BBM & durasi)', TIMESTAMP WITH TIME ZONE '2026-11-04T16:20:00+07:00'),
('texp-105-f', 'trp-105', 'FUEL', 65000, 'usr-drv-rudi', 'DRIVER', 4.8, 'Solar Hiace', TIMESTAMP WITH TIME ZONE '2026-11-05T09:15:00+07:00'),
('texp-105-p', 'trp-105', 'PARKING', 15000, 'usr-drv-rudi', 'DRIVER', NULL, 'Parkir drop-off Hiace KNO', TIMESTAMP WITH TIME ZONE '2026-11-05T09:05:00+07:00'),
('texp-106-f', 'trp-106', 'FUEL', 155000, 'usr-drv-andi', 'DRIVER', 11.4, 'Solar Dex Medan-Kuala Tanjung', TIMESTAMP WITH TIME ZONE '2026-11-06T10:00:00+07:00'),
('texp-106-t', 'trp-106', 'TOLL', 64000, 'usr-drv-andi', 'DRIVER', NULL, 'Tol Medan - Tebing Tinggi - Indrapura', TIMESTAMP WITH TIME ZONE '2026-11-06T08:45:00+07:00');

-- Historical Locked Cost Snapshots for Completed Trips (Formula v2)
INSERT INTO trip_cost_snapshots (id, trip_id, formula_version_id, formula_version_number, distance_km_used, distance_source, duration_hours_used, agreed_fare_snapshot, estimated_operating_cost, actual_expenses_total, allocated_operating_cost, total_effective_cost, estimated_profit, actual_profit) VALUES
('tcs-101', 'trp-101', 'cfv-v2', 2, 41.0, 'ODOMETER', 1.17, 220000, 146365, 80000, 91015, 171015, 73635, 48985),
('tcs-102', 'trp-102', 'cfv-v2', 2, 40.0, 'ODOMETER', 1.23, 260000, 152600, 58000, 98600, 156600, 107400, 103400),
('tcs-103', 'trp-103', 'cfv-v2', 2, 40.0, 'ODOMETER', 1.17, 250000, 150600, 55000, 96600, 151600, 99400, 98400),
('tcs-104', 'trp-104', 'cfv-v2', 2, 42.0, 'ODOMETER', 2.25, 220000, 148130, 96000, 91430, 187430, 71870, 32570),
('tcs-105', 'trp-105', 'cfv-v2', 2, 36.0, 'ODOMETER', 1.08, 550000, 203540, 80000, 154940, 234940, 346460, 315060),
('tcs-106', 'trp-106', 'cfv-v2', 2, 118.0, 'ODOMETER', 2.47, 650000, 368270, 219000, 208970, 427970, 281730, 222030);

