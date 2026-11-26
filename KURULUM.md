# Araç Ekran Video – Kurulum Kılavuzu

Jaecoo 7, Omoda 5 ve Chery (Tiggo vb.) araçlarda, **Android telefondan** YouTube ve diğer siteleri araç ekranında açar.

## Garanti açısından önemli

- Uygulama **sadece telefona** kurulur.
- Araç ekranına / multimedya sistemine **hiçbir şey yüklenmez**, araç yazılımı **değişmez**, araca kablo/kutu takılmaz.
- Araç ile telefon arasındaki bağlantı, aracın fabrikadan gelen **Android Auto** özelliğidir (normal navigasyon ya da müzik uygulaması gibi).
- Telefonu araçtan ayırdığınızda araçta hiçbir iz kalmaz.

Yine de içiniz rahat etsin diye yetkili servisten yazılı teyit almanızı öneririz.

## Güvenlik (park kilidi)

- Görüntü **sadece araç dururken** gösterilir.
- Araç hareket edince ekran kararır, **ses devam eder**. Araç ~3 saniye durunca görüntü geri gelir.
- Hız iki kaynaktan okunur: Android Auto'nun araçtan aldığı hız ve telefonun GPS'i. **Hız okunamazsa görüntü kapalı kalır** (güvenli taraf).
- Bu yüzden telefonda **konum izni** verilmelidir. Kapalı otoparkta GPS çekmezse ve araç hız vermiyorsa görüntü açılmaz.

---

## 1. APK dosyasını oluşturma (bir kez)

İki yoldan biri:

### Yol A – GitHub ile (bilgisayara program kurmadan)

1. github.com'da ücretsiz hesap açın, **yeni bir depo (repository)** oluşturun (Private seçebilirsiniz).
2. Bu klasördeki tüm dosyaları depoya yükleyin ("Add file → Upload files"). `.github` klasörü de yüklenmeli.
3. Deponun **Actions** sekmesine gidin. "APK derle" işi kendiliğinden başlar (başlamazsa "Run workflow").
4. Yaklaşık 5 dakika sonra iş yeşil olur. İşe tıklayın, en alttaki **AracEkranVideo-apk** dosyasını indirin. İçinden `app-release.apk` çıkar.

### Yol B – Android Studio ile

1. Bilgisayara **Android Studio** kurun (developer.android.com/studio).
2. "Open" ile bu klasörü açın, Gradle'ın indirmesini bekleyin.
3. Menüden **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
4. APK: `app/build/outputs/apk/debug/app-debug.apk`

## 2. Telefona kurma

1. APK dosyasını telefona atın (WhatsApp'tan kendinize, kablo ile, Google Drive ile…).
2. Dosyaya dokunun. "Bilinmeyen uygulamalar" uyarısı çıkarsa o uygulamaya (Dosyalar / Chrome vb.) izin verin ve **Yükle**'ye basın.
3. Uygulamayı açın ve **"Konum iznini ver"** düğmesine basın → "Uygulamayı kullanırken" seçin.

## 3. Android Auto'da açma (her telefonda bir kez)

Uygulama Play Store dışından kurulduğu için Android Auto'ya göstermek gerekir:

1. Telefonda **Ayarlar → Bağlı cihazlar → Android Auto** (Samsung'da: Ayarlar'da "Android Auto" diye arayın). Uygulamadaki **"Android Auto ayarlarını aç"** düğmesi de buraya götürür.
2. En aşağıdaki **"Sürüm"** yazısına **10 kez** dokunun → "Geliştirici ayarlarına izin verilsin mi?" → **Tamam**.
3. Sağ üstteki **⋮ (üç nokta) → Geliştirici ayarları**.
4. **"Bilinmeyen kaynaklar"** kutusunu işaretleyin.
5. Android Auto ayarlarına geri dönün → **"Başlatıcıyı özelleştir"** → **Araç Ekran Video** işaretli olsun.
6. Telefonu araca bağlayın (kablolu ya da kablosuz). Bağlıysa bir kez ayırıp tekrar bağlayın.

## 4. Kullanım

Araç ekranında uygulama listesinden **Araç Ekran Video**'ya dokunun.

**Araç ekranındaki düğmeler**
| Düğme | Görevi |
|---|---|
| ← (ok) | Önceki sayfa / tam ekran videodan çıkış |
| ⌂ (ev) | Ana sayfa: kayıtlı sitelerdeki ilk site (varsayılan YouTube) |
| ⟳ (yenile) | Sayfayı yeniden yükle |
| Siteler | Kayıtlı siteler listesi |
| ✥ (kaydırma) | Parmakla sayfayı kaydırma modu |
| ˄ ˅ | Sayfayı yukarı / aşağı kaydır |

Sayfadaki videoya, bağlantılara **dokunarak** tıklayabilirsiniz. Yazı yazmak için telefonu kullanın.

**Telefondan**
- Adres yazıp **"Araç ekranında aç"** → araç ekranında açılır.
- YouTube uygulamasında bir videoda **Paylaş → Araç Ekran Video** → video doğrudan araç ekranında açılır.
- Site ekleme/silme, yazı büyüklüğü ve "Masaüstü sitesi modu" ayarları telefondadır.

## Sorun giderme

| Sorun | Çözüm |
|---|---|
| Araç ekranında uygulama görünmüyor | 3. adımdaki "Bilinmeyen kaynaklar"ı kontrol edin, "Başlatıcıyı özelleştir"de işaretli mi bakın, telefonu ayırıp yeniden bağlayın. |
| "Araç hızı okunamıyor" yazıyor | Konum iznini verin, telefonun konumu (GPS) açık olsun, açık alanda deneyin. |
| Yazılar çok küçük/büyük | Telefonda "Görünüm" bölümünden Normal / Büyük / Çok büyük seçin. |
| Bir site araç ekranında düzgün görünmüyor | "Masaüstü sitesi modu"nu açıp deneyin. |
| Bir Android Auto güncellemesinden sonra çalışmadı | "Bilinmeyen kaynaklar" ayarı bazen sıfırlanır; 3. adımı tekrarlayın. |

## Bilinmesi gerekenler

- Bu uygulama Google Play'de yayınlanamaz (Google araç ekranında video uygulamalarına henüz izin vermiyor). Bu yüzden APK ile kurulur.
- Google ileride Android Auto'yu değiştirirse uygulamanın güncellenmesi gerekebilir.
- Uygulama sadece yasal / lisanslı içerik için kullanılmalıdır.
- iPhone için bu yöntem çalışmaz.
