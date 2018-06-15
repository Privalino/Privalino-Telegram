package org.telegram.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Image;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

import de.privalino.telegram.AnimationHelper;
import de.privalino.telegram.PrivalinoOnboardHandler;
import de.privalino.telegram.model.RegisterResponse;
import de.privalino.telegram.rest.PrivalinoOnboardApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static de.privalino.telegram.AppConstants.INTENT_EXTRA_KEY_FROM_INTRO;
import static de.privalino.telegram.AppConstants.INTENT_EXTRA_KEY_IS_PARENT;
import static de.privalino.telegram.AppConstants.SHAREDPREFS_KEY_USER_TYPE_SELECTED;
import static de.privalino.telegram.AppConstants.SHAREDRPREFS_KEY_ON_BOARDING_INFO;
import static de.privalino.telegram.PrivalinoOnboardHandler.childModel;
import static de.privalino.telegram.PrivalinoOnboardHandler.parentModel;

public class FinishOnboardActivity extends Activity {


    Boolean isParent = null;
    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finish_onboard);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            isParent = extras.getBoolean(INTENT_EXTRA_KEY_IS_PARENT);
        }

        preferences = ApplicationLoader.applicationContext.getSharedPreferences(SHAREDRPREFS_KEY_ON_BOARDING_INFO, Activity.MODE_PRIVATE);

        setText(isParent, (TextView) findViewById(R.id.large_text), (TextView) findViewById(R.id.small_text));

        final ImageView doneButton = (ImageView) findViewById(R.id.done_button);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                postModel(doneButton);
            }
        });


    }

    private void nextActivity() {
        Intent intent = new Intent(FinishOnboardActivity.this, LaunchActivity.class);
        intent.putExtra(INTENT_EXTRA_KEY_FROM_INTRO, true);
        startActivity(intent);
        AnimationHelper.transitionAnimation(this);
    }

    private void setText(Boolean isParent, TextView largeText, TextView smallText) {
        if (isParent == null) {
            return;
        }

        if (isParent) {
            largeText.setText(R.string.parent_outro_title);
            smallText.setText(R.string.parent_outro_text);
        } else {
            largeText.setText(R.string.child_outro_title);
            smallText.setText(R.string.child_outro_text);
        }
    }

    /**
     * Posts the model to server using a Retrofit handler.
     * Disables the next button while waiting for the servers response and posts a message
     * if the response is negative.
     * @param nextButton
     */
    private void postModel(final ImageView nextButton) {
        PrivalinoOnboardHandler handler = new PrivalinoOnboardHandler();
        nextButton.setClickable(false);
        if (isParent) {

            parentModel.updateMissingFromSharedPrefs();

            handler.getAPI().parent(PrivalinoOnboardApi.type, parentModel).enqueue(new Callback<RegisterResponse>() {
                @Override
                public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                    updateSharedPrefs();
                    nextButton.setClickable(true);
                    nextActivity();
                }

                @Override
                public void onFailure(Call<RegisterResponse> call, Throwable throwable) {
                    Toast.makeText(FinishOnboardActivity.this, R.string.error_warning, Toast.LENGTH_LONG).show();
                    nextButton.setClickable(true);

                }
            });
        } else {

            childModel.updateMissingFromSharedPrefs();

            handler.getAPI().child(PrivalinoOnboardApi.type, PrivalinoOnboardHandler.childModel).enqueue(new Callback<RegisterResponse>() {
                @Override
                public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                    updateSharedPrefs();
                    nextButton.setClickable(true);
                    nextActivity();
                }

                @Override
                public void onFailure(Call<RegisterResponse> call, Throwable throwable) {
                    Toast.makeText(FinishOnboardActivity.this, R.string.error_warning, Toast.LENGTH_LONG).show();
                    nextButton.setClickable(true);
                }
            });
        }
    }

    /**
     * Updates the shared preferences with information about the screens user filled during the on
     * boarding.
     */
    private void updateSharedPrefs(){
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SHAREDPREFS_KEY_USER_TYPE_SELECTED, true);
        if(isParent){
            if (parentModel.getEmail() != null){
                editor.putBoolean(ParentEmailActivity.class.toString(), true);
            }
            if (!parentModel.getChildren().isEmpty()){
               editor.putBoolean(AddChildPhoneActivity.class.toString(), true);
                if (parentModel.getChildren().get(0).getBirthYear() > 0){
                    editor.putBoolean(AddChildAgeActivity.class.toString(), true);
                }
            }
        } else {
            if (PrivalinoOnboardHandler.childModel.getParentsNumbers() != null){
                editor.putBoolean(AddParentPhoneActivity.class.toString(), true);
            }

        }
        editor.apply();
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        AnimationHelper.transitionAnimation(this);
    }
}
