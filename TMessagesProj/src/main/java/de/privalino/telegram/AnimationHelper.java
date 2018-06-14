package de.privalino.telegram;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.view.View;
import android.widget.RelativeLayout;

public class AnimationHelper {

    public static void crossfade(final RelativeLayout oldView, RelativeLayout newView) {

        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.
        newView.setAlpha(0f);
        newView.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        newView.animate()
                .alpha(1f)
                .setDuration(500)
                .setListener(null);

        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)
        oldView.animate()
                .alpha(0f)
                .setDuration(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        oldView.setVisibility(View.GONE);
                    }
                });
    }



    public static void transitionAnimation(Activity activity){
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
