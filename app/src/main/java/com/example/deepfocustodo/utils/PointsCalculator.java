package com.example.deepfocustodo.utils;

import com.example.deepfocustodo.models.Task;

public class PointsCalculator {

    // Logic: 
    // 1 minute = 1 point
    // Completed session bonus = 5 points
    // Priority multiplier: High = 1.5, Medium = 1.2, Low = 1.0
    // Streak bonus: +2 for each previous session today (capped at 10)

    public static int calculatePoints(int actualMinutes, boolean isCompleted, String type, Task task, int sessionsToday) {
        if (!"FOCUS".equals(type)) {
            return 0; // No points for breaks
        }

        double points = actualMinutes * 1.0;

        if (isCompleted) {
            points += 5; // Base completion bonus
            
            // Apply priority multiplier
            if (task != null) {
                switch (task.getPriority()) {
                    case 3: points *= 1.5; break; // High
                    case 2: points *= 1.2; break; // Medium
                    default: points *= 1.0; break; // Low
                }
            }
            
            // Streak bonus
            int streakBonus = Math.min(sessionsToday * 2, 10);
            points += streakBonus;
        } else {
            // If failed, give 0.5 points per minute as "effort points"
            points = actualMinutes * 0.5;
        }

        return (int) Math.round(points);
    }
}