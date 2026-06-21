# Social Panel — Android (native Kotlin)

iOS native (SwiftUI) ile birebir özellikli, **native Kotlin + Jetpack Compose** Android uygulaması.
Cross-platform değil — her platform kendi native dilinde (iOS: Swift, Android: Kotlin).

## Özellikler
- 5 sekme: Genel · Analiz · Paylaş · Öneriler · Ayarlar
- Hesap + metrik geçmişi takibi (Room), 13 sosyal platform
- Pro abonelik (Google Play Billing), 14 dil, otomatik Google yedekleme, CSV dışa aktarma
- Glassmorphism UI + animasyonlu mesh arka plan (iOS Liquid Glass estetiği)

## Mimari
- UI: Jetpack Compose + Material 3
- Veri: Room (SwiftData karşılığı) + DataStore/SharedPreferences
- Ödeme: Play Billing 7 (StoreKit karşılığı)
- Min SDK 26, Target 35, Kotlin 2.0, AGP 8.7

## Yayın
GitHub Actions → AAB → Google Play `internal` track. Paket: `com.nickdegs.sosyalpanel`.
