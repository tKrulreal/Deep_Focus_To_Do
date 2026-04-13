# DeepFocusToDo

Ứng dụng Pomodoro + To‑Do (Java, Android) với:
- Timer Pomodoro chạy bằng `Service` + Notification
- Chặn app gây xao nhãng (Blocker)
- Lịch sử phiên tập trung, thống kê
- Chọn nhạc nền cho phiên tập trung

> Repo này dùng **Gradle** và module chính là `app/`.

---

## 1) Yêu cầu môi trường

- Android Studio (khuyến nghị bản stable mới)
- JDK theo cấu hình dự án (Android Studio sẽ tự kèm)
- Thiết bị Android hoặc Emulator

---

## 2) Cấu trúc chính

- `app/src/main/java/com/example/deepfocustodo/services/PomodoroService.java`
  - Service chạy timer Pomodoro, phát nhạc nền khi focus (nếu bật), broadcast state ra UI
- `app/src/main/java/com/example/deepfocustodo/fragments/HomeFragment.java`
  - Home dashboard: timer dạng vòng tròn, nhạc nền đang chọn, phiên hôm nay, streak ngày
- `app/src/main/java/com/example/deepfocustodo/activities/FocusMusicActivity.java`
  - Màn hình chọn nhạc nền (playlist), nghe thử (preview)
- `app/src/main/java/com/example/deepfocustodo/utils/PreferenceHelper.java`
  - Lưu cài đặt (focus/break, music enabled, selected playlist id, …)
- `app/src/main/res/layout/fragment_home.xml`
  - UI Home (NestedScrollView + CircularProgressIndicator)
- `app/src/main/res/layout/activity_focus_music.xml`
  - UI chọn nhạc nền

---

## 2.1) Chức năng hệ thống (mô tả tổng quan)

### A. Pomodoro Timer (Tập trung / Nghỉ)
- Bấm **Bắt đầu / Tạm dừng / Tiếp tục / Dừng / Reset** để điều khiển phiên.
- Timer chạy trong nền bằng `PomodoroService` để:
  - Không bị dừng khi bạn thoát app.
  - Hiển thị trạng thái trong Notification.
  - Broadcast trạng thái timer về UI theo chu kỳ 1 giây.

### B. Nhiệm vụ (To‑Do) + chọn nhiệm vụ cho phiên
- Danh sách task hiển thị ở Home.
- Có thể chọn **1 nhiệm vụ** làm “nhiệm vụ đang chọn cho tập trung”.
- Khi đang có phiên chạy/đang có thể resume, app hạn chế đổi nhiệm vụ để tránh sai lệch tiến trình.

### C. Nhạc nền (Background Focus Music)
- Có màn hình **Focus Music** để chọn playlist/nhạc nền.
- Có thể **nghe thử (preview)** ngay trong màn Focus Music.
- Playlist được lưu bằng `SharedPreferences` (`selected_playlist_id`).
- Khi Pomodoro đang ở chế độ focus và `music_enabled = true`, `PomodoroService` sẽ phát nhạc nền theo playlist đã chọn.
- Nếu đổi playlist khi đang focus, service sẽ tự đổi track (recreate `MediaPlayer`).

### D. Dashboard Home (UI mới)
- Timer dạng **vòng tròn (CircularProgressIndicator)**.
- Hiển thị:
  - **Nhạc nền đã chọn**
  - **Số phiên hoàn thành hôm nay**
  - **Chuỗi ngày tập trung (streak)**
- Phần chọn nhiệm vụ đặt phía dưới và Home hỗ trợ **cuộn dọc** để xem danh sách task dễ hơn.

### E. Lịch sử phiên tập trung (History)
- Xem danh sách phiên đã ghi nhận.
- Có thể xóa lịch sử (tuỳ theo UI hiện tại trong app).

### F. Thống kê (Statistics)
- Chế độ **Ngày** và **Tổng**:
  - Tổng phút tập trung, số phiên hoàn thành, điểm
  - Biểu đồ theo ngày/7 ngày
  - Streak và badge (danh hiệu)

### G. Chặn ứng dụng gây xao nhãng (Blocker)
- Trong phiên focus, hệ thống có thể bật cơ chế chặn/màn popup tuỳ cấu hình.
- Một số quyền đặc biệt có thể được yêu cầu (Usage Access / Overlay, ...).

---

## 3) Build/Run nhanh

### Build Compile (Windows PowerShell)

```powershell
Set-Location "C:\Users\admin\AndroidStudioProjects\DeepFocusToDo"
.\gradlew.bat :app:compileDebugJavaWithJavac --no-daemon
```

### Chạy app

- Mở project bằng Android Studio
- `Run > Run 'app'`

---

## 4) PomodoroService – Cơ chế cập nhật UI

`PomodoroService` phát broadcast:
- Action: `PomodoroService.ACTION_STATE`
- Extras:
  - `EXTRA_TIME_LEFT` (ms)
  - `EXTRA_IS_RUNNING` (boolean)
  - `EXTRA_IS_FOCUS` (boolean)
  - `EXTRA_COMPLETED_FOCUS` (int)
  - `EXTRA_SESSION_IN_PROGRESS` (boolean)

`HomeFragment` lắng nghe broadcast để cập nhật:
- Text thời gian (`tvTimer`)
- Chế độ (`tvMode`)
- Vòng tròn tiến độ (`progressTimer`)

---

## 5) Nhạc nền (Focus Music)

### Files nhạc
Đặt trong `app/src/main/res/raw/`:
- `lofi1.mp3`
- `lofi2.mp3`
- `lofi3.mp3`
- `lofi4.mp3`

### Map playlist -> nhạc
- UI chọn playlist lưu vào preference: `selected_playlist_id`
- `PomodoroService` đọc `PreferenceHelper.getSelectedPlaylistId()` và map:
  - `lofi_beats` -> `R.raw.lofi1`
  - `classical_focus` -> `R.raw.lofi2`
  - `nature_sounds` -> `R.raw.lofi3`
  - `ambient_study` -> `R.raw.lofi4`

### Preview
`FocusMusicActivity` có `MediaPlayer` riêng để nghe thử.
Khi bạn chọn playlist khác, preview đang chạy sẽ dừng.

---

## 6) Thống kê trên Home

Home hiển thị:
- **Phiên hôm nay**: query `FocusSessionDao.getCompletedSessionsCountInDay(start,end)`
- **Streak ngày tập trung**: lấy danh sách `getCompletedSessionStartTimesUpTo(now)` và tính chuỗi ngày liên tiếp.

> Lưu ý: streak hiện tính theo rule “mỗi ngày có ít nhất 1 session COMPLETED”.

---

## 7) Troubleshooting

### Lỗi AAPT: attribute không tồn tại
Nếu gặp lỗi kiểu `attribute ... not found` trong `CircularProgressIndicator`, hãy kiểm tra version Material Components hoặc thuộc tính trong XML.
Dự án hiện đã loại bỏ thuộc tính không tương thích để build được.

### RecyclerView trong ScrollView
Home dùng `NestedScrollView` + `RecyclerView`:
- `android:nestedScrollingEnabled="false"`
- `layout_height="wrap_content"`

---

## 8) Gợi ý cải tiến tiếp theo

- Bấm card nhạc nền ở Home để mở nhanh `FocusMusicActivity`
- Animation mượt cho vòng tròn tiến độ
- Tách logic tính streak ra class dùng chung (Home/Statistics)

---

## License

N/A

