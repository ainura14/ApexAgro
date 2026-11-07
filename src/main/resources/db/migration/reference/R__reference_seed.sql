
-- Preliminary reference seed for BE1-03.
-- Sample data only. Not an official complete reference dataset.
-- Settlements are omitted until D-16 is resolved.

-- 1. Regions
INSERT INTO reference.regions (id, code, name_ky, name_ru, sort_order)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'BATKEN', 'Баткен облусу', 'Баткенская область', 1),
    ('10000000-0000-0000-0000-000000000002', 'JALAL_ABAD', 'Жалал-Абад облусу', 'Джалал-Абадская область', 2),
    ('10000000-0000-0000-0000-000000000003', 'ISSYK_KUL', 'Ысык-Көл облусу', 'Иссык-Кульская область', 3),
    ('10000000-0000-0000-0000-000000000004', 'NARYN', 'Нарын облусу', 'Нарынская область', 4),
    ('10000000-0000-0000-0000-000000000005', 'OSH', 'Ош облусу', 'Ошская область', 5),
    ('10000000-0000-0000-0000-000000000006', 'TALAS', 'Талас облусу', 'Таласская область', 6),
    ('10000000-0000-0000-0000-000000000007', 'CHUY', 'Чүй облусу', 'Чуйская область', 7),
    ('10000000-0000-0000-0000-000000000008', 'BISHKEK', 'Бишкек шаары', 'город Бишкек', 8),
    ('10000000-0000-0000-0000-000000000009', 'OSH_CITY', 'Ош шаары', 'город Ош', 9)
    ON CONFLICT (code) DO NOTHING;

-- 2. Sample districts (not a complete official list)
INSERT INTO reference.districts (id, region_id, code, name_ky, name_ru)
VALUES
    ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000007', 'CHUY', 'Чүй району', 'Чуйский район'),
    ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000007', 'SOKULUK', 'Сокулук району', 'Сокулукский район'),
    ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000005', 'KARASUU', 'Кара-Суу району', 'Кара-Суйский район')
    ON CONFLICT (code) DO NOTHING;

-- 3. Crops
INSERT INTO reference.crops (id, code, name_ky, name_ru, active)
VALUES
    ('30000000-0000-0000-0000-000000000001', 'WHEAT', 'Буудай', 'Пшеница', true),
    ('30000000-0000-0000-0000-000000000002', 'BARLEY', 'Арпа', 'Ячмень', true),
    ('30000000-0000-0000-0000-000000000003', 'CORN', 'Жүгөрү', 'Кукуруза', true),
    ('30000000-0000-0000-0000-000000000004', 'POTATO', 'Картошка', 'Картофель', true),
    ('30000000-0000-0000-0000-000000000005', 'COTTON', 'Пахта', 'Хлопок', true)
    ON CONFLICT (code) DO NOTHING;

-- 4. Crop unit factors
-- KG = 1 is implicit; TON = 1000 kg.
INSERT INTO reference.crop_unit_factors (crop_id, unit, kg_per_unit)
SELECT id, 'TON', 1000.000
FROM reference.crops
WHERE code IN ('WHEAT', 'BARLEY', 'CORN', 'POTATO', 'COTTON')
    ON CONFLICT (crop_id, unit) DO NOTHING;

-- 5. Root categories
INSERT INTO reference.categories
(id, parent_id, level, code, name_ky, name_ru, kind, sort_order, active)
VALUES
    ('40000000-0000-0000-0000-000000000001', NULL, 1, 'GRAINS', 'Дан өсүмдүктөрү', 'Зерновые культуры', 'HARVEST', 1, true),
    ('40000000-0000-0000-0000-000000000002', NULL, 1, 'VEGETABLES', 'Жашылчалар', 'Овощи', 'HARVEST', 2, true),
    ('40000000-0000-0000-0000-000000000003', NULL, 1, 'AGRO_GOODS', 'Айыл чарба товарлары', 'Товары для сельского хозяйства', 'AGRO_GOODS', 3, true)
    ON CONFLICT (code) DO NOTHING;

-- 6. Child categories
INSERT INTO reference.categories
(id, parent_id, level, code, name_ky, name_ru, kind, sort_order, active)
VALUES
    ('40000000-0000-0000-0000-000000000011', '40000000-0000-0000-0000-000000000001', 2, 'WHEAT', 'Буудай', 'Пшеница', 'HARVEST', 1, true),
    ('40000000-0000-0000-0000-000000000012', '40000000-0000-0000-0000-000000000001', 2, 'BARLEY', 'Арпа', 'Ячмень', 'HARVEST', 2, true),
    ('40000000-0000-0000-0000-000000000013', '40000000-0000-0000-0000-000000000002', 2, 'POTATO', 'Картошка', 'Картофель', 'HARVEST', 1, true),
    ('40000000-0000-0000-0000-000000000014', '40000000-0000-0000-0000-000000000003', 2, 'FERTILIZERS', 'Жер семирткичтер', 'Удобрения', 'AGRO_GOODS', 1, true),
    ('40000000-0000-0000-0000-000000000015', '40000000-0000-0000-0000-000000000003', 2, 'SEEDS', 'Үрөндөр', 'Семена', 'AGRO_GOODS', 2, true)
    ON CONFLICT (code) DO NOTHING;
