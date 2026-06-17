
# 📦 NM MART APP - COMPLETE PROJECT SUMMARY

---

## 1. Home &amp; Main Screens
| Screen Name           | Java File (Logic)                              | Layout XML (Design)                     |
|-----------------------|------------------------------------------------|-----------------------------------------|
| **Splash Screen**     | `SplashActivity.java`                           | `activity_splash.xml`                   |
| **Onboarding**        | `OnboardingActivity.java`                       | `activity_onboarding.xml`               |
| **Home Screen**       | `MainActivity.java`                             | `activity_main.xml`                     |
| **Categories List**   | `CategoriesActivity.java`                       | `activity_categories.xml`               |
| **Subcategory List**  | `SubCategoryActivity.java`                      | `activity_sub_category.xml`             |

---

## 2. Product Screens
| Screen Name           | Java File (Logic)                              | Layout XML (Design)                     |
|-----------------------|------------------------------------------------|-----------------------------------------|
| **Product List**      | `ProductListActivity.java`                     | `activity_product_list.xml`             |
| **Product Detail**    | `ProductDetailActivity.java`                   | `activity_product_detail.xml`           |
| **Product Detail Menu** | `menu_product_detail.xml` (menu file) | —                                       |
| **Search**            | (Integrated in MainActivity)                    | —                                       |
| **Barcode Scan**      | `ScanActivity.java`                            | `activity_scan.xml`                     |
| **Self Checkout Cart** | `SelfCheckoutCartActivity.java`               | `activity_self_checkout_cart.xml`       |
| **Exit Pass**         | `ExitPassActivity.java`                        | `activity_exit_pass.xml`                |

---

## 3. Cart &amp; Checkout Screens
| Screen Name           | Java File (Logic)                              | Layout XML (Design)                     |
|-----------------------|------------------------------------------------|-----------------------------------------|
| **Cart**              | `CartActivity.java`                            | `activity_cart.xml`                     |
| **Checkout**          | `CheckoutActivity.java`                        | `activity_checkout.xml`                 |
| **Order Success**     | `OrderSuccessActivity.java`                    | `activity_order_success.xml`            |

---

## 4. Order &amp; Address Screens
| Screen Name           | Java File (Logic)                              | Layout XML (Design)                     |
|-----------------------|------------------------------------------------|-----------------------------------------|
| **My Orders (Order History)** | `OrderHistoryActivity.java`                 | `activity_order_history.xml`            |
| **Order History Menu** | `menu_order_history.xml` (menu file) | —                                       |
| **Order Tracking**    | `OrderTrackingActivity.java`                   | `activity_order_tracking.xml`           |
| **My Addresses**      | `AddressListActivity.java`                     | `activity_address_list.xml`             |
| **Add/Edit Address**  | `AddressActivity.java`                         | `activity_address.xml`                  |

---

## 5. User &amp; Wallet Screens
| Screen Name           | Java File (Logic)                              | Layout XML (Design)                     |
|-----------------------|------------------------------------------------|-----------------------------------------|
| **My Profile**        | `ProfileActivity.java`                         | `activity_profile.xml`                  |
| **NM Wallet**         | `WalletActivity.java`                          | `activity_wallet.xml`                   |
| **Wallet Transactions** | `WalletTransactionHistoryActivity.java`      | `activity_wallet_transaction_history.xml` |
| **Refer &amp; Earn**      | `ReferEarnActivity.java`                       | `activity_refer_earn.xml`               |
| **Offers &amp; Coupons**  | `CouponsActivity.java`                         | `activity_coupons.xml`                  |

---

## 6. Other Screens
| Screen Name           | Java File (Logic)                              | Layout XML (Design)                     |
|-----------------------|------------------------------------------------|-----------------------------------------|
| **Wishlist**          | `WishlistActivity.java`                        | `activity_wishlist.xml`                 |
| **Notifications**     | `NotificationsActivity.java`                   | `activity_notifications.xml`            |
| **Help &amp; Support**    | `CustomerSupportActivity.java`                 | `activity_customer_support.xml`         |
| **About Us**          | `AboutUsActivity.java`                         | `activity_about_us.xml`                 |
| **Settings**          | `SettingsActivity.java`                        | `activity_settings.xml`                 |
| **Location Selection** | `LocationSelectionActivity.java`              | `activity_location_selection.xml`       |
| **Debug Audit**       | `DebugAuditActivity.java` (for testing)        | `activity_debug_audit.xml`              |

---

## 7. Auth Screens
| Screen Name           | Java File (Logic)                              | Layout XML (Design)                     |
|-----------------------|------------------------------------------------|-----------------------------------------|
| **Login**             | `LoginActivity.java`                           | `activity_login.xml`                    |
| **Sign Up**           | `SignupActivity.java`                          | `activity_signup.xml`                   |

---

## 8. Important Menu Files
| Menu Type              | File Name                                      |
|------------------------|------------------------------------------------|
| **Bottom Navigation**  | `bottom_nav_menu.xml`                          |
| **Drawer Menu**        | `drawer_menu.xml`                              |

---

## 9. Key Utility &amp; Storage Files
| File Name               | Purpose                                      |
|--------------------------|----------------------------------------------|
| `NotificationStorage.java` | Local notification storage                 |
| `OfflineStorage.java`  | Offline data caching (banners, products, etc.) |
| `WishlistManager.java` | Local wishlist management                    |
| `CartManager.java`     | Local cart management                        |
| `SessionManager.java`  | User login session &amp; preferences             |
| `SupabaseRepository.java` | All Supabase API calls &amp; database operations |
| `MainActivity.java`    | **IMPORTANT**: Home screen + FCM sync + drawer logic |

---

## 10. Database Update File
| File Name               | Purpose                                      |
|--------------------------|----------------------------------------------|
| `update_database.sql`    | SQL commands to add FCM token and payment ID columns in Supabase |

---

## 11. Complete Feature List
- ✅ Home Screen (Banners, Categories, Best Selling, Recently Viewed)
- ✅ Product Search (Text + Voice + Barcode Scan)
- ✅ Product List (Sorting &amp; Filtering)
- ✅ Product Detail (Images, Reviews, Quantity Controls, Add to Cart, Buy Now)
- ✅ Cart Management (Local + Online Sync)
- ✅ Checkout (Address Selection, Date/Time Slots, Coupons, Payment Methods)
- ✅ Payment Gateway (Razorpay Integration)
- ✅ Order History (Cancel, Return, Order Again, Invoice Download)
- ✅ Order Tracking
- ✅ Wallet &amp; Loyalty Points
- ✅ Notifications (Local Storage + Unread Badge)
- ✅ Offline Mode (All Data Cached)
- ✅ FCM Token Sync to Supabase
- ✅ Wishlist
- ✅ Refer &amp; Earn
- ✅ Share App &amp; Products
- ✅ Self Checkout (Barcode Scan + Exit Pass)
- ✅ Address Management
- ✅ Profile Management
- ✅ Login/Sign Up

---

## Project Structure
- Java files: `app/src/main/java/com/nmmart/retailos/`
- Layout files: `app/src/main/res/layout/`
- Menu files: `app/src/main/res/menu/`
- Values files: `app/src/main/res/values/`
