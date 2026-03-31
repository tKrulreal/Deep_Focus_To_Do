package com.example.deepfocustodo.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.fragments.HomeFragment;
import com.example.deepfocustodo.fragments.SettingsFragment;
import com.example.deepfocustodo.fragments.StatisticsFragment;
import com.example.deepfocustodo.fragments.TabRefreshable;
import com.example.deepfocustodo.fragments.TasksFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Fragment homeFragment;
    private Fragment tasksFragment;
    private Fragment statisticsFragment;
    private Fragment settingsFragment;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // SỬA LỖI: MainActivity phải sử dụng activity_main.xml làm layout chính
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            homeFragment = new HomeFragment();
            tasksFragment = new TasksFragment();
            statisticsFragment = new StatisticsFragment();
            settingsFragment = new SettingsFragment();

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, settingsFragment, "settings")
                    .hide(settingsFragment)
                    .add(R.id.fragment_container, statisticsFragment, "stats")
                    .hide(statisticsFragment)
                    .add(R.id.fragment_container, tasksFragment, "tasks")
                    .hide(tasksFragment)
                    .add(R.id.fragment_container, homeFragment, "home")
                    .commit();

            activeFragment = homeFragment;
        } else {
            homeFragment = getSupportFragmentManager().findFragmentByTag("home");
            tasksFragment = getSupportFragmentManager().findFragmentByTag("tasks");
            statisticsFragment = getSupportFragmentManager().findFragmentByTag("stats");
            settingsFragment = getSupportFragmentManager().findFragmentByTag("settings");

            if (homeFragment == null) {
                homeFragment = new HomeFragment();
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, homeFragment, "home")
                        .hide(homeFragment)
                        .commitNow();
            }
            if (tasksFragment == null) {
                tasksFragment = new TasksFragment();
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, tasksFragment, "tasks")
                        .hide(tasksFragment)
                        .commitNow();
            }
            if (statisticsFragment == null) {
                statisticsFragment = new StatisticsFragment();
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, statisticsFragment, "stats")
                        .hide(statisticsFragment)
                        .commitNow();
            }
            if (settingsFragment == null) {
                settingsFragment = new SettingsFragment();
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, settingsFragment, "settings")
                        .hide(settingsFragment)
                        .commitNow();
            }

            if (homeFragment != null && homeFragment.isVisible()) {
                activeFragment = homeFragment;
            } else if (tasksFragment != null && tasksFragment.isVisible()) {
                activeFragment = tasksFragment;
            } else if (statisticsFragment != null && statisticsFragment.isVisible()) {
                activeFragment = statisticsFragment;
            } else if (settingsFragment != null && settingsFragment.isVisible()) {
                activeFragment = settingsFragment;
            }
        }

        if (activeFragment == null) {
            showFragment(homeFragment);
            activeFragment = homeFragment;
        }

        if (activeFragment == homeFragment) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        } else if (activeFragment == tasksFragment) {
            bottomNavigationView.setSelectedItemId(R.id.nav_tasks);
        } else if (activeFragment == statisticsFragment) {
            bottomNavigationView.setSelectedItemId(R.id.nav_statistics);
        } else if (activeFragment == settingsFragment) {
            bottomNavigationView.setSelectedItemId(R.id.nav_settings);
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                showFragment(homeFragment);
            } else if (itemId == R.id.nav_tasks) {
                showFragment(tasksFragment);
            } else if (itemId == R.id.nav_statistics) {
                showFragment(statisticsFragment);
            } else if (itemId == R.id.nav_settings) {
                showFragment(settingsFragment);
            } else {
                return false;
            }

            return true;
        });
    }

    private void showFragment(Fragment fragment) {
        if (fragment == null || activeFragment == fragment) {
            if (fragment instanceof TabRefreshable) {
                ((TabRefreshable) fragment).onTabSelected();
            }
            return;
        }

        if (activeFragment == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .show(fragment)
                    .commit();
            activeFragment = fragment;
            return;
        }

        getSupportFragmentManager()
                .beginTransaction()
                .hide(activeFragment)
                .show(fragment)
                .commit();

        activeFragment = fragment;

        if (fragment instanceof TabRefreshable) {
            ((TabRefreshable) fragment).onTabSelected();
        }
    }
}