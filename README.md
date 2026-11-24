# FindIt Campus

Aplikasi lost and found kampus berbasis Java 25, Spring Boot, Maven, Thymeleaf, dan PostgreSQL. Mahasiswa dapat mencari dan melaporkan barang hilang/temuan, mengajukan klaim barang temuan, serta menyetujui atau menolak klaim sebagai pemilik laporan.

## Fitur

- Beranda dan papan laporan dengan pencarian berdasarkan nama barang, deskripsi, atau lokasi; dapat difilter berdasarkan barang hilang/temuan.
- Registrasi, login, dan logout dengan kata sandi yang di-hash BCrypt.
- Pembuatan, pengeditan, dan penghapusan laporan barang hilang/temuan oleh pelapor atau admin, dengan gambar barang opsional.
- Pengajuan klaim beserta bukti kepemilikan; pemilik laporan mengelola klaim dan menandai barang telah dikembalikan.
- Komentar teks dan gambar pada laporan, serta pemberitahuan jika ada temuan yang mungkin cocok dengan barang hilang.
- Profil pengguna dengan nama, NIM, dan program studi yang ditampilkan pada laporan.
- Dashboard admin responsif dengan pengelolaan pengguna/laporan dan grafik harian, bulanan, serta tahunan.
- Dashboard laporan dan klaim milik pengguna, UI responsif, serta validasi form dan pembatasan akses di backend.

## Prasyarat

- JDK 25
- Maven 3.9+
- PostgreSQL 15+

## Menyiapkan PostgreSQL

Buat database:

```sql
CREATE DATABASE findit;
```

Default koneksi menggunakan `jdbc:postgresql://localhost:5432/findit`, username `postgres`, dan password `postgres`. Sesuaikan koneksi lewat environment variable; jangan simpan kredensial asli di source control.

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/findit"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="password-postgresql-anda"
```

Skema dikelola Hibernate (`spring.jpa.hibernate.ddl-auto=update`). Beberapa lokasi kampus contoh akan dibuat otomatis saat aplikasi pertama kali dijalankan.
Untuk menyiapkan skema secara manual lewat pgAdmin, buka dan jalankan [`database/findit.sql`](./database/findit.sql) pada database `findit`.
Gambar komentar disimpan di folder `uploads` pada direktori kerja aplikasi; ubah lokasinya dengan environment variable `APP_UPLOAD_DIR`.

## Menjalankan aplikasi

```powershell
mvn spring-boot:run
```

Buka [http://localhost:8080](http://localhost:8080), lalu buat akun dari menu **Buat akun**.

### Mengaktifkan admin pertama

Daftarkan akun admin lewat aplikasi, lalu ubah rolenya secara langsung di PostgreSQL menggunakan pgAdmin atau `psql`:

```sql
UPDATE app_users SET role = 'ADMIN' WHERE email = 'admin@kampus.ac.id';
```

Keluar lalu masuk kembali dengan akun tersebut, kemudian buka menu **Admin** atau [http://localhost:8080/admin](http://localhost:8080/admin). Hanya akun `ADMIN` yang dapat membuka panel. Mengubah role akan mengakhiri sesi pengguna terkait agar perubahan akses langsung berlaku. Setidaknya satu admin harus dipertahankan.
Pengguna dapat melengkapi NIM dan program studi melalui menu **Profil**. Admin dapat mengelola akun dan laporan di dashboard.
Navigasi tidak menampilkan tautan admin; buka `/admin` secara langsung. Jika tepat satu akun ber-role `ADMIN`, akun itu juga dapat masuk dengan username alias `admin` tanpa mengubah email akun. Jangan gunakan kredensial pengembangan di deployment publik; ganti password sebelum aplikasi dipublikasikan.

### Klaim dan notifikasi kecocokan

Pada laporan barang temuan, pengguna lain dapat mengirim klaim beserta bukti ciri khusus. Pemilik laporan meninjau klaim dari halaman detail atau menu **Klaim**, menyetujui atau menolak, lalu menandai barang sudah dikembalikan setelah klaim disetujui. FindIt juga membandingkan kategori dan kata kunci judul pada laporan temuan baru dengan laporan kehilangan yang masih terbuka; pemilik laporan kehilangan yang berpotensi cocok mendapat notifikasi. Pencocokan ini hanya saran, bukan verifikasi otomatis.

## Build dan test

```powershell
mvn test
mvn package
```

## Struktur kode

```text
src/main/java/com/findit/campus/
├── config/       # Security, seed lokasi, dan data view bersama
├── controller/   # Halaman dan alur web
├── dto/          # Form input dan validasi
├── entity/       # User, lokasi, laporan, dan klaim
├── repository/   # Spring Data JPA
└── service/      # Aturan bisnis
```

Template Thymeleaf berada di `src/main/resources/templates`; stylesheet utama di `src/main/resources/static/css/main.css`.
