package com.example.icstmgsfbstud.helpers;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;

import com.example.icstmgsfbstud.R;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Helper class for Bottom Navigation animations and customizations
 * Uses only public APIs to avoid compatibility issues
 */
public class BottomNavAnimationHelper {

    /**
     * Apply initial enter animation to BottomAppBar
     */
    public static void applyEnterAnimation(Context context, BottomAppBar bottomAppBar) {
        Animation slideInAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom);
        bottomAppBar.startAnimation(slideInAnimation);
    }

    /**
     * Apply state animation to navigation icons
     * This version uses public API methods only
     */
    public static void applyNavItemStateAnimator(BottomNavigationView bottomNavigationView) {
        // Set listener to animate menu items when selected
        bottomNavigationView.setOnItemSelectedListener(item -> {
            // Animate the selected item
            View view = bottomNavigationView.findViewById(item.getItemId());
            if (view != null) {
                createRippleEffect(view);
            }
            return true;
        });
    }

    /**
     * Apply elevation change animation when selecting items
     */
    public static void applyElevationAnimation(BottomAppBar bottomAppBar, float fromElevation, float toElevation) {
        ValueAnimator elevationAnimator = ValueAnimator.ofFloat(fromElevation, toElevation);
        elevationAnimator.setDuration(300);
        elevationAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        elevationAnimator.addUpdateListener(animation -> {
            float elevation = (float) animation.getAnimatedValue();
            ViewCompat.setElevation(bottomAppBar, elevation);
        });
        elevationAnimator.start();
    }

    /**
     * Creates a ripple effect on the navigation item
     * This method works with public APIs
     */
    public static void createRippleEffect(View view) {
        ScaleAnimation scaleUp = new ScaleAnimation(
                1.0f, 1.2f, 1.0f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scaleUp.setDuration(100);

        ScaleAnimation scaleDown = new ScaleAnimation(
                1.2f, 1.0f, 1.2f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scaleDown.setDuration(100);

        view.startAnimation(scaleUp);

        new Handler().postDelayed(() -> {
            view.startAnimation(scaleDown);
        }, 100);
    }

    /**
     * Apply ripple effect to a menu item by ID in a compatible way
     * @param bottomNavigationView The BottomNavigationView
     * @param itemId The ID of the menu item to animate
     */
    public static void applyRippleToMenuItem(BottomNavigationView bottomNavigationView, int itemId) {
        View view = bottomNavigationView.findViewById(itemId);
        if (view != null) {
            createRippleEffect(view);
        }
    }

    /**
     * Animates the badge when new notifications are received
     * This uses only public APIs for badge animation
     */
    public static void animateBadge(BottomNavigationView bottomNavigationView, int itemId) {
        BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(itemId);
        if (badge != null && badge.isVisible()) {
            // Since we can't directly access the badge view, we'll animate the whole menu item
            // and rely on the badge being attached to it
            View view = bottomNavigationView.findViewById(itemId);
            if (view != null) {
                // Create pulse animation for the badge
                ScaleAnimation pulse = new ScaleAnimation(
                        1.0f, 1.2f, 1.0f, 1.2f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);
                pulse.setDuration(200);
                pulse.setRepeatMode(Animation.REVERSE);
                pulse.setRepeatCount(1);

                view.startAnimation(pulse);
            }
        }
    }

    /**
     * Setup item selection listener with animations
     * @param bottomNavigationView The BottomNavigationView to setup
     * @param listener The listener that will be called when an item is selected
     */
    public static void setupWithAnimatedListener(BottomNavigationView bottomNavigationView,
                                                 OnNavigationItemSelectedListener listener) {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            // Apply animation to the selected item
            View view = bottomNavigationView.findViewById(item.getItemId());
            if (view != null) {
                createRippleEffect(view);
            }

            // Call the listener
            return listener.onNavigationItemSelected(item);
        });
    }

    /**
     * Interface for navigation item selection listener
     */
    public interface OnNavigationItemSelectedListener {
        boolean onNavigationItemSelected(@NonNull MenuItem item);
    }
}