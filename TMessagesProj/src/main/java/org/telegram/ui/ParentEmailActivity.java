package org.telegram.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

import de.privalino.telegram.AnimationHelper;
import de.privalino.telegram.PrivalinoOnboardHandler;
import de.privalino.telegram.model.Parent;
import de.privalino.telegram.model.RegisterResponse;
import de.privalino.telegram.rest.PrivalinoOnboardApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static de.privalino.telegram.AnimationHelper.crossfade;
import static de.privalino.telegram.AppConstants.INTENT_EXTRA_KEY_FROM_SETTINGS;
import static de.privalino.telegram.AppConstants.INTENT_EXTRA_KEY_IS_PARENT;
import static de.privalino.telegram.AppConstants.SHAREDPREFS_KEY_EMAIL;
import static de.privalino.telegram.AppConstants.SHAREDRPREFS_KEY_ON_BOARDING_INFO;


public class ParentEmailActivity extends Activity {


    private RelativeLayout messageLayout;
    private RelativeLayout emailLayout;
    TextView skipButton;
    SharedPreferences preferences;

    boolean fromSettings = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_parent_email);


        messageLayout = (RelativeLayout) findViewById(R.id.message_layout);
        emailLayout = (RelativeLayout) findViewById(R.id.add_email_layout);

        skipButton = (TextView) findViewById(R.id.skip_button);

        preferences = ApplicationLoader.applicationContext.getSharedPreferences(SHAREDRPREFS_KEY_ON_BOARDING_INFO, Activity.MODE_PRIVATE);

        Bundle extras = getIntent().getExtras();
        if (extras != null){
            fromSettings = extras.getBoolean(INTENT_EXTRA_KEY_FROM_SETTINGS);
        }

        initialize();

        onClickListeners();


    }

    /**
     * Initializes the model from the data stored in shared preferences, if there is any.
     */
    private void initialize() {
        String email = preferences.getString(SHAREDPREFS_KEY_EMAIL, "");
        if (!email.isEmpty()){
            ((TextView) findViewById(R.id.email_edit_text)).setText(email);
        }
    }

    private void onClickListeners() {
        ImageView nextButton = (ImageView) findViewById(R.id.next_button);
        RelativeLayout backButton = (RelativeLayout) findViewById(R.id.back_button);


        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (messageLayout.getVisibility() == View.VISIBLE) {
                    crossfade(messageLayout, emailLayout);
                    skipButton.setVisibility(View.VISIBLE);

                } else {
                    if(updateModel()){
                        nextActivity();
                    } else {
                        Toast.makeText(ParentEmailActivity.this, R.string.email_warning, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextActivity();
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    /**
     * Updates the static model used to hold the on boarding information.
     * @return true if the update was successful, false otherwise
     */
    private boolean updateModel() {
        String email = ((TextView) findViewById(R.id.email_edit_text)).getText().toString().trim();
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            return false;
        }
        PrivalinoOnboardHandler.parentModel.setEmail(email);
        preferences.edit().putString(SHAREDPREFS_KEY_EMAIL, email).apply();
        return true;
    }


    private void nextActivity() {
        //find the next activity taking into account was the screen reached from settings (usual flow)
        //or if an activity skipped on last on boarding (start it)
        Class next = null;
        if (fromSettings || !preferences.getBoolean(AddChildPhoneActivity.class.toString(), false)){
            next = AddChildPhoneActivity.class;
        } else if (!preferences.getBoolean(AddChildAgeActivity.class.toString(), false)) {
            next = AddChildAgeActivity.class;
        } else {
            next = FinishOnboardActivity.class;
        }
        Intent intent = new Intent(ParentEmailActivity.this, next);
        if (next == FinishOnboardActivity.class){
            intent.putExtra(INTENT_EXTRA_KEY_IS_PARENT, true);
        }
        startActivity(intent);
        AnimationHelper.transitionAnimation(this);
    }

    @Override
    public void onBackPressed() {
        if (emailLayout.getVisibility() == View.VISIBLE) {
            crossfade(emailLayout, messageLayout);
            skipButton.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
            AnimationHelper.transitionAnimation(this);
        }
    }
}
