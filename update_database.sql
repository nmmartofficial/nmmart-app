-- =============================================
-- UPDATE DATABASE FOR NM MART APP FEATURES
-- =============================================

-- 1. Add FCM Token column to Users table (for Push Notifications)
ALTER TABLE "users" 
ADD COLUMN IF NOT EXISTS "fcm_token" TEXT;

-- 2. Add Payment ID column to Orders table (for Razorpay Payment Tracking)
ALTER TABLE "orders" 
ADD COLUMN IF NOT EXISTS "payment_id" TEXT;

-- =============================================
-- DONE! Now the database supports:
-- - FCM tokens for push notifications
-- - Razorpay payment IDs for order tracking
-- =============================================
