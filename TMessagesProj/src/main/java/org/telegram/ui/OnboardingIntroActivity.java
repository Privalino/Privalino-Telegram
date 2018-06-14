package org.telegram.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.Image;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

import de.privalino.telegram.AnimationHelper;
import de.privalino.telegram.PrivalinoOnboardHandler;
import de.privalino.telegram.model.Child;
import de.privalino.telegram.model.Parent;

import static de.privalino.telegram.AnimationHelper.crossfade;
import static de.privalino.telegram.AppConstants.INTENT_EXTRA_KEY_FROM_INTRO;
import static de.privalino.telegram.AppConstants.SHAREDPREFS_KEY_IS_PARENT;
import static de.privalino.telegram.AppConstants.SHAREDRPREFS_KEY_ON_BOARDING_INFO;

public class OnboardingIntroActivity extends Activity {

    RelativeLayout mainLayout;
    RelativeLayout welcomeLayout;
    RelativeLayout chooseLayout;
    private Boolean parentSelected = null;
    TextView skipButton;
    RelativeLayout backButton;
    int currentColor;
    ImageView selectParent;
    ImageView selectChild;
    TextView parentText;
    TextView childText;

    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_intro);

        mainLayout = (RelativeLayout) findViewById(R.id.main_layout);
        welcomeLayout = (RelativeLayout) findViewById(R.id.welcomeLayout);
        chooseLayout = (RelativeLayout) findViewById(R.id.chooseLayout);
        skipButton = (TextView) findViewById(R.id.skip_button);
        backButton = (RelativeLayout) findViewById(R.id.back_button);
        currentColor = getResources().getColor(R.color.privolino_background);
        selectParent = (ImageView) findViewById(R.id.selectParentButton);
        selectChild = (ImageView) findViewById(R.id.selectChildButton);
        parentText = (TextView) findViewById(R.id.parentText);
        childText = (TextView) findViewById(R.id.childText);
        preferences = ApplicationLoader.applicationContext.getSharedPreferences(SHAREDRPREFS_KEY_ON_BOARDING_INFO, MODE_PRIVATE);

        onClickListeners();

    }

    private void onClickListeners() {

        ImageView nextButton = (ImageView) findViewById(R.id.nextButton);
        final ImageView selectParent = (ImageView) findViewById(R.id.selectParentButton);
        final ImageView selectChild = (ImageView) findViewById(R.id.selectChildButton);
        final TextView parentText = (TextView) findViewById(R.id.parentText);
        final TextView childText = (TextView) findViewById(R.id.childText);


        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (welcomeLayout.getVisibility() == View.VISIBLE) {
                    crossfade(welcomeLayout, chooseLayout);
                    skipButton.setVisibility(View.VISIBLE);
                    backButton.setVisibility(View.VISIBLE);
                } else if (chooseLayout.getVisibility() == View.VISIBLE) {
                    if (parentSelected == null) {
                        Toast.makeText(OnboardingIntroActivity.this,
                                R.string.user_selection_warning, Toast.LENGTH_SHORT);
                        return;
                    }
                    Intent intent = null;
                    if (parentSelected) {
                        PrivalinoOnboardHandler.parentModel = new Parent();
                        PrivalinoOnboardHandler.parentModel.initialize(getApplicationContext());
                        intent = new Intent(OnboardingIntroActivity.this, ParentEmailActivity.class);
                    } else {
                        PrivalinoOnboardHandler.childModel = new Child();
                        PrivalinoOnboardHandler.childModel.initialize(getApplicationContext());
                        intent = new Intent(OnboardingIntroActivity.this, AddParentPhoneActivity.class);
                    }
                    preferences.edit().putBoolean(SHAREDPREFS_KEY_IS_PARENT, parentSelected).apply();
                    startActivity(intent);
                    AnimationHelper.transitionAnimation(OnboardingIntroActivity.this);
                }
            }
        });

        parentText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                parentSelected = true;
                changeColor(getResources().getColor(R.color.parent_background));
                selectButton(parentText, selectParent, childText, selectChild);
            }
        });

        childText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                parentSelected = false;
                changeColor(getResources().getColor(R.color.child_background));
                selectButton(childText, selectChild, parentText, selectParent);
            }
        });

        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(OnboardingIntroActivity.this, LaunchActivity.class);
                intent.putExtra(INTENT_EXTRA_KEY_FROM_INTRO, true);
                startActivity(intent);
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });


    }

    private void selectButton(TextView selectedText, ImageView selectedView, TextView unselectedText, ImageView unselectedView) {

        setSelected(selectedText, selectedView);
        setUnselected(unselectedText, unselectedView);
    }

    private void setUnselected(TextView unselectedText, ImageView unselectedView) {
        unselectedText.setTextColor(getResources().getColor(R.color.white));
        unselectedView.setImageDrawable(getResources().getDrawable(R.drawable.select_button));
    }

    private void setSelected(TextView selectedText, ImageView selectedView) {
        selectedText.setTextColor(getResources().getColor(R.color.black));
        selectedView.setImageDrawable(getResources().getDrawable(R.drawable.selected_button));
    }

    /**
     * Changes the color of the main layout to the given new color.
     * Interpolates between the current color and the given new color to create a transition.
     * @param newColor color to set tha main layouts background to
     */
    private void changeColor(int newColor) {
        ColorDrawable[] color = {new ColorDrawable(currentColor), new ColorDrawable(newColor)};
        TransitionDrawable trans = new TransitionDrawable(color);
        mainLayout.setBackground(trans);
        trans.startTransition(1000);
        currentColor = newColor;
    }



    @Override
    public void onBackPressed() {
        if (chooseLayout.getVisibility() == View.VISIBLE) {
            crossfade(chooseLayout, welcomeLayout);
            skipButton.setVisibility(View.GONE);
            backButton.setVisibility(View.GONE);
            changeColor(getResources().getColor(R.color.privolino_background));
            setUnselected(parentText, selectParent);
            setUnselected(childText, selectChild);
        } else {
            super.onBackPressed();
            AnimationHelper.transitionAnimation(this);
        }

    }
}
