package com.example.deepfocustodo.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.deepfocustodo.R;
import com.example.deepfocustodo.fragments.HomeFragment;
import com.example.deepfocustodo.fragments.SettingsFragment;
import com.example.deepfocustodo.fragments.StatisticsFragment;
import com.example.deepfocustodo.fragments.TabRefreshable;
import com.example.deepfocustodo.fragments.TasksFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
      private Fragment activeFragment;

      private static final String TAG_HOME = "tab_home";
      private static final String TAG_TASKS = "tab_tasks";
      private static final String TAG_STATS = "tab_stats";
      private static final String TAG_SETTINGS = "tab_settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
              Fragment home = new HomeFragment();
              activeFragment = home;
              getSupportFragmentManager()
                  .beginTransaction()
                  .add(R.id.fragment_container, home, TAG_HOME)
                  .commit();
            } else {
              activeFragment = getSupportFragmentManager().findFragmentByTag(TAG_HOME);
              if (activeFragment == null) {
                activeFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
              }
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
              String targetTag = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                targetTag = TAG_HOME;
            } else if (itemId == R.id.nav_tasks) {
                targetTag = TAG_TASKS;
            } else if (itemId == R.id.nav_statistics) {
                targetTag = TAG_STATS;
            } else if (itemId == R.id.nav_settings) {
                targetTag = TAG_SETTINGS;
            }

              if (targetTag != null) {
                switchToFragment(targetTag);
                return true;
            }
            return false;
        });
    }

          private void switchToFragment(String tag) {
            FragmentManager fm = getSupportFragmentManager();
            Fragment target = fm.findFragmentByTag(tag);

            if (target == null) {
              target = createFragment(tag);
            }
            if (target == null || target == activeFragment) {
              if (activeFragment instanceof TabRefreshable) {
                ((TabRefreshable) activeFragment).onTabSelected();
              }
              return;
            }

                    FragmentTransaction transaction = fm.beginTransaction();
            if (activeFragment != null) {
              transaction.hide(activeFragment);
            }
            if (target.isAdded()) {
              transaction.show(target);
            } else {
              transaction.add(R.id.fragment_container, target, tag);
            }
            transaction.commit();

            activeFragment = target;
            if (activeFragment instanceof TabRefreshable) {
              ((TabRefreshable) activeFragment).onTabSelected();
            }
          }

          private Fragment createFragment(String tag) {
            switch (tag) {
              case TAG_HOME:
                return new HomeFragment();
              case TAG_TASKS:
                return new TasksFragment();
              case TAG_STATS:
                return new StatisticsFragment();
              case TAG_SETTINGS:
                return new SettingsFragment();
              default:
                return null;
            }
          }
}