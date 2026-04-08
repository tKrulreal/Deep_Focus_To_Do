package com.example.deepfocustodo.fragments;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.database.AppDatabase;
import com.example.deepfocustodo.models.DailyStats;
import com.example.deepfocustodo.models.FocusSession;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatisticsFragment extends Fragment implements TabRefreshable {

    private static final String BLOCK_PREFS = "BlockedAppsPrefs";
    private static final String KEY_BLOCKED_ATTEMPTS = "blocked_attempt_count";

    private MaterialButtonToggleGroup toggleGroup;
    private LinearLayout layoutNavigation;
    private TextView tvTimeLabel, tvTotalFocusMinutes, tvCompletedSessions, tvTotalPoints;
    private TextView tvBadge, tvStreak, tvBlockedAttempts;

    private ProgressBar progressStreak;
    private TextView tvNextGoal;
    private ImageView ivBadgeIcon;
    private ImageButton btnPrev, btnNext;
    private BarChart barChart;

    private LinearLayout layoutGeneralCharts;
    private BarChart barChartWeekly;
    private PieChart pieChartTimeDistribution;
    private CardView cardBlockedAttempts;

    private CardView cardCompletedTasks;
    private TextView tvCompletedTasks;



    private AppDatabase db;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Biến lưu trữ ngày đang xem hiện tại (Mặc định là hôm nay)
    private Calendar selectedDate = Calendar.getInstance();
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public StatisticsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ View
        toggleGroup = view.findViewById(R.id.toggleGroup);
        layoutNavigation = view.findViewById(R.id.layoutNavigation);
        tvTimeLabel = view.findViewById(R.id.tvTimeLabel);
        btnPrev = view.findViewById(R.id.btnPrev);
        btnNext = view.findViewById(R.id.btnNext);
        barChart = view.findViewById(R.id.barChart);

        tvTotalFocusMinutes = view.findViewById(R.id.tvTotalFocusMinutes);
        tvCompletedSessions = view.findViewById(R.id.tvCompletedSessions);
        tvTotalPoints = view.findViewById(R.id.tvTotalPoints);
        tvBadge = view.findViewById(R.id.tvBadge);
        tvStreak = view.findViewById(R.id.tvStreak);
        tvBlockedAttempts = view.findViewById(R.id.tvBlockedAttempts);

        progressStreak = view.findViewById(R.id.progressStreak);
        tvNextGoal = view.findViewById(R.id.tvNextGoal);
        ivBadgeIcon = view.findViewById(R.id.ivBadgeIcon);

        layoutGeneralCharts = view.findViewById(R.id.layoutGeneralCharts);
        barChartWeekly = view.findViewById(R.id.barChartWeekly);
        pieChartTimeDistribution = view.findViewById(R.id.pieChartTimeDistribution);

        cardCompletedTasks = view.findViewById(R.id.cardCompletedTasks);
        tvCompletedTasks = view.findViewById(R.id.tvCompletedTasks);

        db = AppDatabase.getInstance(requireContext());

        // Cấu hình UI ban đầu cho Chart
        setupChart();

        setupWeeklyChart(); // Hàm cài đặt UI chart 7 ngày
        setupPieChart();    // Hàm cài đặt UI chart tròn

        cardBlockedAttempts = view.findViewById(R.id.cardBlockedAttempts);

        // Cập nhật lại sự kiện chuyển Tab
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            if (checkedId == R.id.btnDay) {
                layoutNavigation.setVisibility(View.VISIBLE);
                barChart.setVisibility(View.VISIBLE); // Chart của ngày

                layoutGeneralCharts.setVisibility(View.GONE); // Ẩn cụm chart chung

                cardBlockedAttempts.setVisibility(View.GONE);

                // Ẩn thẻ nhiệm vụ ở chế độ ngày
                cardCompletedTasks.setVisibility(View.GONE);

                loadDayStatistics();
            } else if (checkedId == R.id.btnTotal) {
                layoutNavigation.setVisibility(View.GONE);
                barChart.setVisibility(View.GONE);

                layoutGeneralCharts.setVisibility(View.VISIBLE); // Hiện cụm chart chung

                cardBlockedAttempts.setVisibility(View.VISIBLE);

                // Hiện thẻ nhiệm vụ ở chế độ chung
                cardCompletedTasks.setVisibility(View.VISIBLE);

                loadAllTimeStatistics();
            }
        });

        // Xử lý nút điều hướng thời gian
        btnPrev.setOnClickListener(v -> {
            selectedDate.add(Calendar.DAY_OF_YEAR, -1);
            loadDayStatistics();
        });

        btnNext.setOnClickListener(v -> {
            selectedDate.add(Calendar.DAY_OF_YEAR, 1);
            loadDayStatistics();
        });

        // Load dữ liệu mặc định lần đầu
        loadDayStatistics();
    }

    @Override
    public void onResume() {
        super.onResume();
        onTabSelected();
    }

    @Override
    public void onTabSelected() {
        if (!isAdded() || getView() == null) return;
        if (toggleGroup.getCheckedButtonId() == R.id.btnDay) {
            loadDayStatistics();
        } else {
            loadAllTimeStatistics();
        }
    }

    // ==========================================
    // XỬ LÝ BIỂU ĐỒ & DỮ LIỆU CHẾ ĐỘ "NGÀY"
    // ==========================================

    private void setupChart() {
        // Tự động nhận diện màu theo giao diện Sáng/Tối
        int textColor = isDarkMode() ? Color.WHITE : Color.parseColor("#333333");
        int gridColor = isDarkMode() ? Color.parseColor("#444444") : Color.parseColor("#DDDDDD");

        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.getAxisRight().setEnabled(false); // Tắt trục Y bên phải
        barChart.setNoDataText("Chưa có dữ liệu tập trung"); // Chữ hiển thị khi ko có data
        barChart.setNoDataTextColor(textColor);

        // Cấu hình trục X (Giờ trong ngày 0-23)
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelCount(8);
        xAxis.setTextColor(textColor);     // Màu chữ trục X
        xAxis.setAxisLineColor(textColor); // Màu đường kẻ trục X
        xAxis.setValueFormatter(new IndexAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + ":00";
            }
        });

        // Cấu hình trục Y bên trái
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setTextColor(textColor);     // Màu chữ trục Y
        leftAxis.setAxisLineColor(textColor); // Màu đường kẻ trục Y
        leftAxis.setGridColor(gridColor);     // Màu đường kẻ lưới ngang
        leftAxis.setGranularity(1f);  // Hiển thị số nguyên
        leftAxis.setGranularityEnabled(true);
    }

    private void setupWeeklyChart() {
        int textColor = isDarkMode() ? Color.WHITE : Color.parseColor("#333333");
        int gridColor = isDarkMode() ? Color.parseColor("#444444") : Color.parseColor("#DDDDDD");

        barChartWeekly.getDescription().setEnabled(false);
        barChartWeekly.getLegend().setEnabled(false);
        barChartWeekly.setDrawGridBackground(false);
        barChartWeekly.getAxisRight().setEnabled(false);
        barChartWeekly.setNoDataText("Chưa có dữ liệu 7 ngày qua");
        barChartWeekly.setNoDataTextColor(textColor);

        XAxis xAxis = barChartWeekly.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(textColor);

        YAxis leftAxis = barChartWeekly.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(textColor);
        leftAxis.setGridColor(gridColor);
    }

    private void setupPieChart() {
        int textColor = isDarkMode() ? Color.WHITE : Color.parseColor("#333333");

        pieChartTimeDistribution.getDescription().setEnabled(false);
        pieChartTimeDistribution.setDrawEntryLabels(false); // Ẩn chữ trên miếng bánh cho đỡ rối
        pieChartTimeDistribution.setNoDataText("Chưa có dữ liệu phân bổ");
        pieChartTimeDistribution.setNoDataTextColor(textColor);
        pieChartTimeDistribution.setHoleColor(Color.TRANSPARENT); // Lỗ giữa trong suốt
        pieChartTimeDistribution.setHoleRadius(50f); // Kích thước lỗ

        // Đặt Legend (Chú thích Sáng, Chiều...)
        pieChartTimeDistribution.getLegend().setTextColor(textColor);
        pieChartTimeDistribution.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM);
        pieChartTimeDistribution.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        pieChartTimeDistribution.getLegend().setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
        pieChartTimeDistribution.getLegend().setWordWrapEnabled(true);
    }

    private void loadDayStatistics() {
        updateDateLabel();

        // Tính toán mili-giây bắt đầu và kết thúc của selectedDate
        Calendar startOfDay = (Calendar) selectedDate.clone();
        normalizeToStartOfDay(startOfDay);
        long startMs = startOfDay.getTimeInMillis();

        Calendar endOfDay = (Calendar) startOfDay.clone();
        endOfDay.add(Calendar.DAY_OF_YEAR, 1);
        long endMs = endOfDay.getTimeInMillis() - 1;

        executorService.execute(() -> {
            // Lấy danh sách session trong ngày
            List<FocusSession> daySessions = db.focusSessionDao().getSessionsInDay(startMs, endMs);

            float[] hourlyFocus = new float[24];
            int totalMinutesInDay = 0;
            int completedSessionsInDay = 0;
            int totalPointsInDay = 0; // Thêm biến tính tổng điểm trong ngày

            // LẤY DỮ LIỆU LỊCH SỬ TÍNH ĐẾN NGÀY ĐANG CHỌN (Dùng cho Badge & Streak)
            Integer historicalPoints = db.focusSessionDao().getTotalPointsUpTo(endMs);
            List<Long> historicalStartTimes = db.focusSessionDao().getCompletedSessionStartTimesUpTo(endMs);

            int safeHistoricalPoints = historicalPoints == null ? 0 : historicalPoints;
            // Tính chuỗi lùi từ ngày đang chọn (startMs là đầu ngày đang chọn)
            int historicalStreak = computeHistoricalStreakDays(historicalStartTimes, startMs);

            // Cập nhật thẻ Badge theo lịch sử (Gọi 1 lần duy nhất ở đây)
            updateBadgeUI(historicalStreak, safeHistoricalPoints);

            for (FocusSession session : daySessions) {
                if ("COMPLETED".equals(session.getStatus())) {
                    completedSessionsInDay++;
                    totalMinutesInDay += session.getDurationMinutes();
                    totalPointsInDay += session.getPointsEarned(); // Cộng dồn điểm

                    // Tìm xem session này thuộc giờ nào
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(session.getStartTime());
                    int hour = cal.get(Calendar.HOUR_OF_DAY);

                    hourlyFocus[hour] += session.getDurationMinutes();
                }
            }

            // Đưa dữ liệu vào Chart
            List<BarEntry> entries = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                entries.add(new BarEntry(i, hourlyFocus[i]));
            }

            BarDataSet dataSet = new BarDataSet(entries, "Phút tập trung");
            dataSet.setColor(Color.parseColor("#4CAF50")); // Màu cột (Xanh lá)
            dataSet.setDrawValues(false); // Ẩn số trên đỉnh cột

            BarData barData = new BarData(dataSet);


            // Chuyển dữ liệu final để ném vào UI Thread
            int finalTotalMinutes = totalMinutesInDay;
            int finalCompleted = completedSessionsInDay;
            int finalTotalPoints = totalPointsInDay;

            if (!isAdded()) return;

            // Cập nhật UI
            requireActivity().runOnUiThread(() -> {
                tvTotalFocusMinutes.setText(finalTotalMinutes + " phút");
                tvCompletedSessions.setText(finalCompleted + " phiên");

                // Cập nhật điểm trong ngày
                tvTotalPoints.setText(finalTotalPoints + " điểm");

                barChart.setData(barData);
                barChart.invalidate();
                barChart.animateY(800);

            });
        });
    }

    private void updateDateLabel() {
        Calendar today = Calendar.getInstance();
        if (selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                selectedDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            tvTimeLabel.setText("Hôm nay - " + dateFormatter.format(selectedDate.getTime()));
        } else {
            tvTimeLabel.setText(dateFormatter.format(selectedDate.getTime()));
        }
    }

    // ==========================================
    // XỬ LÝ DỮ LIỆU CHẾ ĐỘ "CHUNG"
    // ==========================================

    private void loadAllTimeStatistics() {
        executorService.execute(() -> {
            long now = System.currentTimeMillis();

            Integer totalFocus = db.focusSessionDao().getTotalFocusMinutes();
            Integer totalPoints = db.focusSessionDao().getTotalPointsUpTo(now);
            List<Long> completedStartTimes = db.focusSessionDao().getCompletedSessionStartTimesUpTo(now);
            int completedSessions = db.focusSessionDao().getCompletedSessionCount();
            int blockedAttempts = getBlockedAttempts();

            int safeTotalPoints = totalPoints == null ? 0 : totalPoints;
            int streakDays = computeHistoricalStreakDays(completedStartTimes, now);
            updateBadgeUI(streakDays, safeTotalPoints);

            int completedTasksCount = db.taskDao().getCompletedTaskCount();

            // ==========================================
            // 1. TÍNH TOÁN BIỂU ĐỒ 7 NGÀY (BarChart)
            // ==========================================
            List<DailyStats> recentStats = db.focusSessionDao().getRecentDailyStats(7);
            // Vì SQL lấy từ mới -> cũ, ta cần đảo ngược lại thành cũ -> mới cho đồ thị hiển thị đúng
            Collections.reverse(recentStats);

            List<BarEntry> weeklyEntries = new ArrayList<>();
            List<String> xLabels = new ArrayList<>(); // Lưu nhãn ngày (VD: 15/10)

            for (int i = 0; i < recentStats.size(); i++) {
                DailyStats stat = recentStats.get(i);
                weeklyEntries.add(new BarEntry(i, stat.getTotalMinutes()));
                xLabels.add(stat.getDateLabel());
            }

            BarDataSet weeklyDataSet = new BarDataSet(weeklyEntries, "Phút");
            weeklyDataSet.setColor(Color.parseColor("#0483FF")); // Màu xanh dương
            weeklyDataSet.setDrawValues(true);
            weeklyDataSet.setValueTextColor(isDarkMode() ? Color.WHITE : Color.BLACK);
            weeklyDataSet.setValueTextSize(10f);
            BarData weeklyData = new BarData(weeklyDataSet);

            // ==========================================
            // 2. TÍNH TOÁN BIỂU ĐỒ TRÒN (PieChart)
            // ==========================================
            List<FocusSession> allHistory = db.focusSessionDao().getAllHistory();
            float morning = 0, afternoon = 0, evening = 0, night = 0;

            for (FocusSession session : allHistory) {
                if ("COMPLETED".equals(session.getStatus())) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(session.getStartTime());
                    int hour = cal.get(Calendar.HOUR_OF_DAY);

                    if (hour >= 6 && hour < 12) morning += session.getDurationMinutes();
                    else if (hour >= 12 && hour < 18) afternoon += session.getDurationMinutes();
                    else if (hour >= 18 && hour < 24) evening += session.getDurationMinutes();
                    else night += session.getDurationMinutes(); // 0h - 6h
                }
            }

            List<PieEntry> pieEntries = new ArrayList<>();
            if (morning > 0) pieEntries.add(new PieEntry(morning, "Sáng (6-12h)"));
            if (afternoon > 0) pieEntries.add(new PieEntry(afternoon, "Chiều (12-18h)"));
            if (evening > 0) pieEntries.add(new PieEntry(evening, "Tối (18-24h)"));
            if (night > 0) pieEntries.add(new PieEntry(night, "Đêm (0-6h)"));

            PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
            // Các màu tươi sáng, dễ nhìn
            pieDataSet.setColors(
                    Color.parseColor("#FFC107"), // Sáng - Vàng
                    Color.parseColor("#FF5722"), // Chiều - Cam đỏ
                    Color.parseColor("#3F51B5"), // Tối - Xanh đậm
                    Color.parseColor("#607D8B")  // Đêm - Xám lam
            );
            pieDataSet.setValueTextSize(14f);
            pieDataSet.setValueTextColor(Color.WHITE);
            pieDataSet.setSliceSpace(2f); // Khoảng cách giữa các miếng bánh
            PieData pieData = new PieData(pieDataSet);

            // ==========================================
            // ĐƯA DATA LÊN UI THREAD
            // ==========================================
            if (!isAdded()) return;

            requireActivity().runOnUiThread(() -> {
                tvTotalFocusMinutes.setText((totalFocus == null ? 0 : totalFocus) + " phút");
                tvCompletedSessions.setText(completedSessions + " phiên");
                tvTotalPoints.setText(safeTotalPoints + " điểm");
                tvBlockedAttempts.setText(blockedAttempts + " lần");
                tvCompletedTasks.setText(completedTasksCount + " nhiệm vụ");

                // Setup nhãn Trục X cho BarChart
                barChartWeekly.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));
                // Tránh việc MPAndroidChart vẽ thừa nhãn nếu số cột < 7
                barChartWeekly.getXAxis().setLabelCount(xLabels.size());

                // Vẽ biểu đồ
                barChartWeekly.setData(weeklyData);
                barChartWeekly.invalidate();
                barChartWeekly.animateY(1000);

                if (pieEntries.isEmpty()) {
                    pieChartTimeDistribution.clear();
                } else {
                    pieChartTimeDistribution.setData(pieData);
                    pieChartTimeDistribution.invalidate();
                    pieChartTimeDistribution.animateY(1000);
                }
            });
        });
    }

    private int getBlockedAttempts() {
        SharedPreferences prefs = requireContext().getSharedPreferences(BLOCK_PREFS, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_BLOCKED_ATTEMPTS, 0);
    }

    private int computeHistoricalStreakDays(List<Long> startTimes, long anchorDateMs) {
        if (startTimes == null || startTimes.isEmpty()) {
            return 0;
        }

        Set<Long> activeDays = new HashSet<>();
        Calendar calendar = Calendar.getInstance();
        for (Long startTime : startTimes) {
            if (startTime == null) continue;
            calendar.setTimeInMillis(startTime);
            normalizeToStartOfDay(calendar);
            activeDays.add(calendar.getTimeInMillis());
        }

        int streak = 0;
        // Bắt đầu đếm ngược từ ngày đang được chọn (anchorDate)
        Calendar cursor = Calendar.getInstance();
        cursor.setTimeInMillis(anchorDateMs);
        normalizeToStartOfDay(cursor);

        // Nếu ngày được chọn có học, đếm lùi về quá khứ
        while (activeDays.contains(cursor.getTimeInMillis())) {
            streak++;
            cursor.add(Calendar.DAY_OF_YEAR, -1);
        }
        return streak;
    }


    private void updateBadgeUI(int streakDays, int totalPoints) {
        String badgeName;
        String nextGoalText;
        int progressMax;
        int currentProgress;
        String colorHex; // Màu chủ đạo cho cấp độ

        // 1. PHẦN TÍNH TOÁN (Có thể chạy trên Background Thread thoải mái)
        if (streakDays >= 7 && totalPoints >= 2000) {
            badgeName = "Deep Focus Master";
            nextGoalText = "Bạn đã đạt danh hiệu cao nhất!";
            progressMax = 100;
            currentProgress = 100;
            colorHex = "#9C27B0"; // Màu Tím
        }
        else if (streakDays >= 3 && totalPoints >= 800) {
            badgeName = "On Fire";
            int pointsLeft = 2000 - totalPoints;
            int daysLeft = 7 - streakDays;

            if (pointsLeft <= 0) {
                nextGoalText = "Còn " + daysLeft + " ngày chuỗi để đạt Deep Focus Master";
            } else if (daysLeft <= 0) {
                nextGoalText = "Còn " + pointsLeft + " điểm để đạt Deep Focus Master";
            } else {
                nextGoalText = "Còn " + pointsLeft + " điểm & " + daysLeft + " ngày chuỗi -> Deep Focus Master";
            }

            progressMax = 2000 - 800;
            currentProgress = totalPoints - 800;
            colorHex = "#F44336"; // Màu Đỏ
        }
        else if (totalPoints >= 300) {
            badgeName = "Getting Started";
            int pointsLeft = 800 - totalPoints;
            int daysLeft = Math.max(0, 3 - streakDays);

            if (daysLeft == 0) {
                nextGoalText = "Còn " + pointsLeft + " điểm để đạt On Fire";
            } else {
                nextGoalText = "Còn " + pointsLeft + " điểm & " + daysLeft + " ngày chuỗi -> On Fire";
            }

            progressMax = 800 - 300;
            currentProgress = totalPoints - 300;
            colorHex = "#4CAF50"; // Màu Xanh lá
        }
        else {
            badgeName = "Newcomer";
            int pointsLeft = 300 - totalPoints;
            nextGoalText = "Còn " + pointsLeft + " điểm để đạt Getting Started";

            progressMax = 300;
            currentProgress = totalPoints;
            colorHex = "#FF9800"; // Màu Cam
        }

        // Kiểm tra an toàn: Nếu Fragment đã bị đóng, không cập nhật UI nữa để tránh crash
        if (!isAdded() || getActivity() == null) return;

        // 2. PHẦN CẬP NHẬT GIAO DIỆN (BẮT BUỘC PHẢI CHẠY TRÊN UI THREAD)
        requireActivity().runOnUiThread(() -> {
            tvBadge.setText(badgeName);
            tvNextGoal.setText(nextGoalText);
            tvStreak.setText(streakDays + " ngày liên tiếp");

            progressStreak.setMax(progressMax);
            progressStreak.setProgress(Math.max(0, currentProgress));

            int parsedColor = Color.parseColor(colorHex);

            ivBadgeIcon.setColorFilter(parsedColor);
            ivBadgeIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#20" + colorHex.substring(1))));

            tvBadge.setTextColor(parsedColor);
            tvBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#20" + colorHex.substring(1))));

            progressStreak.setProgressTintList(ColorStateList.valueOf(parsedColor));
        });
    }

    private String buildDailyChart(List<DailyStats> stats) {
        if (stats == null || stats.isEmpty()) {
            return "Bieu do 7 ngay\nChua co du lieu";
        }

        Collections.reverse(stats);
        int maxMinutes = 1;
        for (DailyStats item : stats) {
            maxMinutes = Math.max(maxMinutes, item.getTotalMinutes());
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Bieu do 7 ngay (phut)\n");
        for (DailyStats item : stats) {
            int barLength = Math.max(1, Math.round((item.getTotalMinutes() * 16f) / maxMinutes));
            builder.append(String.format(
                    Locale.getDefault(),
                    "%s | %-16s %3d m (%d phien)\n",
                    item.getDateLabel(),
                    repeat("#", barLength),
                    item.getTotalMinutes(),
                    item.getSessionCount()
            ));
        }

        return builder.toString().trim();
    }

    private String repeat(String token, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(token);
        }
        return builder.toString();
    }

    private void normalizeToStartOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private boolean isDarkMode() {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }
}

