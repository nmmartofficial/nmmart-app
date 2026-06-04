-- =============================================
-- FIX RLS POLICIES - ALLOW PUBLIC READ ACCESS
-- =============================================

-- First disable RLS temporarily to make changes
ALTER TABLE "banners" DISABLE ROW LEVEL SECURITY;
ALTER TABLE "categories" DISABLE ROW LEVEL SECURITY;
ALTER TABLE "subcategories" DISABLE ROW LEVEL SECURITY;
ALTER TABLE "brands" DISABLE ROW LEVEL SECURITY;
ALTER TABLE "products" DISABLE ROW LEVEL SECURITY;
ALTER TABLE "pincode_master" DISABLE ROW LEVEL SECURITY;
ALTER TABLE "offers_master" DISABLE ROW LEVEL SECURITY;
ALTER TABLE "home_config" DISABLE ROW LEVEL SECURITY;
ALTER TABLE "app_config" DISABLE ROW LEVEL SECURITY;
ALTER TABLE "coupons" DISABLE ROW LEVEL SECURITY;

-- Now DROP existing policies (clean slate)
DROP POLICY IF EXISTS "Allow read access to everyone" ON "banners";
DROP POLICY IF EXISTS "Allow read access to everyone" ON "categories";
DROP POLICY IF EXISTS "Allow read access to everyone" ON "subcategories";
DROP POLICY IF EXISTS "Allow read access to everyone" ON "brands";
DROP POLICY IF EXISTS "Allow read access to everyone" ON "products";
DROP POLICY IF EXISTS "Allow read access to everyone" ON "pincode_master";
DROP POLICY IF EXISTS "Allow read access to everyone" ON "offers_master";
DROP POLICY IF EXISTS "Allow read access to everyone" ON "home_config";
DROP POLICY IF EXISTS "Allow read access to everyone" ON "app_config";
DROP POLICY IF EXISTS "Allow read access to everyone" ON "coupons";

-- Now CREATE NEW PERMISSIVE POLICIES
CREATE POLICY "Enable read access for all users" ON "banners"
    FOR SELECT USING (true);

CREATE POLICY "Enable read access for all users" ON "categories"
    FOR SELECT USING (true);

CREATE POLICY "Enable read access for all users" ON "subcategories"
    FOR SELECT USING (true);

CREATE POLICY "Enable read access for all users" ON "brands"
    FOR SELECT USING (true);

CREATE POLICY "Enable read access for all users" ON "products"
    FOR SELECT USING (true);

CREATE POLICY "Enable read access for all users" ON "pincode_master"
    FOR SELECT USING (true);

CREATE POLICY "Enable read access for all users" ON "offers_master"
    FOR SELECT USING (true);

CREATE POLICY "Enable read access for all users" ON "home_config"
    FOR SELECT USING (true);

CREATE POLICY "Enable read access for all users" ON "app_config"
    FOR SELECT USING (true);

CREATE POLICY "Enable read access for all users" ON "coupons"
    FOR SELECT USING (true);

-- Now RE-ENABLE RLS
ALTER TABLE "banners" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "categories" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "subcategories" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "brands" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "products" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "pincode_master" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "offers_master" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "home_config" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "app_config" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "coupons" ENABLE ROW LEVEL SECURITY;

-- Done! Now the app can read data!
