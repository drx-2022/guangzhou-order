-- =====================================================
-- SCRIPT UPDATE TRACKING STATUS (Chạy sau khi có tracking_id)
-- =====================================================

-- Xem danh sách tracking:
-- SELECT * FROM order_tracking;
-- SELECT * FROM tracking_milestones WHERE tracking_id = <your_tracking_id>;

-- =====================================================
-- CÁCH 1: Update milestone cụ thể
-- =====================================================

-- Bước 1: Update milestone thành completed
UPDATE tracking_milestones 
SET is_completed = true, 
    is_current = false,
    timestamp = NOW()
WHERE tracking_id = 1 AND step_order = 1;

-- Bước 2: Bật is_current cho milestone tiếp theo
UPDATE tracking_milestones 
SET is_current = true
WHERE tracking_id = 1 AND step_order = 2;

-- Bước 3: Update tracking chính
UPDATE order_tracking
SET current_status = 'AT_FACTORY',
    current_location = 'Guangzhou Sorting Center'
WHERE tracking_id = 1;

-- =====================================================
-- CÁCH 2: Update nhiều bước cùng lúc
-- =====================================================

-- Ví dụ: Đã hoàn thành bước 1, 2, 3 - đang ở bước 4
UPDATE tracking_milestones 
SET is_completed = true, 
    is_current = false,
    timestamp = NOW()
WHERE tracking_id = 1 AND step_order IN (1, 2, 3);

UPDATE tracking_milestones 
SET is_current = true
WHERE tracking_id = 1 AND step_order = 4;

UPDATE order_tracking
SET current_status = 'IN_TRANSIT',
    current_location = 'Shenzhen - Dongguan Highway'
WHERE tracking_id = 1;

-- =====================================================
-- CÁCH 3: Đánh dấu đã giao hàng thành công
-- =====================================================

UPDATE tracking_milestones 
SET is_completed = true, 
    is_current = false,
    timestamp = NOW()
WHERE tracking_id = 1;

UPDATE order_tracking
SET current_status = 'DELIVERED',
    current_location = 'Customer Address',
    is_delivered = true,
    actual_delivery = NOW()
WHERE tracking_id = 1;

-- =====================================================
-- CÁCH 4: Reset về trạng thái ban đầu
-- =====================================================

UPDATE tracking_milestones 
SET is_completed = false, 
    is_current = false,
    timestamp = NULL
WHERE tracking_id = 1;

UPDATE tracking_milestones 
SET is_completed = true,
    is_current = true,
    timestamp = NOW()
WHERE tracking_id = 1 AND step_order = 1;

UPDATE order_tracking
SET current_status = 'PICKED_UP',
    current_location = 'Guangzhou Warehouse',
    is_delivered = false,
    actual_delivery = NULL
WHERE tracking_id = 1;

-- =====================================================
-- STEP ORDER MAPPING:
-- 1 = PICKED_UP (Đã lấy hàng)
-- 2 = AT_FACTORY (Tại xưởng/lưu kho)
-- 3 = CUSTOMS_EXPORT (Hải quan xuất)
-- 4 = IN_TRANSIT (Đang vận chuyển)
-- 5 = CUSTOMS_IMPORT (Hải quan nhập)
-- 6 = OUT_FOR_DELIVERY (Đang giao hàng)
-- 7 = DELIVERED (Đã giao)
-- =====================================================
