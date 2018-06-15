package org.telegram.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

import java.util.List;

import de.privalino.telegram.AnimationHelper;
import de.privalino.telegram.PrivalinoOnboardHandler;
import de.privalino.telegram.model.Parent;
import de.privalino.telegram.model.RegisterResponse;
import de.privalino.telegram.rest.PrivalinoOnboardApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static de.privalino.telegram.AnimationHelper.crossfade;
import static de.privalino.telegram.AppConstants.INTENT_EXTRA_KEY_IS_PARENT;
import static de.privalino.telegram.AppConstants.SHAREDPREFS_KEY_CHILDREN;
import static de.privalino.telegram.AppConstants.SHAREDRPREFS_KEY_ON_BOARDING_INFO;

public class AddChildAgeActivity extends Activity {

    private RelativeLayout messageLayout;
    private RelativeLayout ageLayout;
    ViewGroup vgChildAges;
    TextView skipButton;
    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_child_age);

        messageLayout = (RelativeLayout) findViewById(R.id.message_layout);
        ageLayout = (RelativeLayout) findViewById(R.id.age_layout);

        vgChildAges = (LinearLayout) findViewById(R.id.age_fields_list);

        skipButton = (TextView) findViewById(R.id.skip_button);

        preferences = ApplicationLoader.applicationContext.getSharedPreferences(SHAREDRPREFS_KEY_ON_BOARDING_INFO, Activity.MODE_PRIVATE);

        onClickListeners();

        addElements();

    }

    private void onClickListeners(){
        final ImageView nextButton = (ImageView) findViewById(R.id.next_button);
        final RelativeLayout backButton = (RelativeLayout) findViewById(R.id.back_button);


        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (messageLayout.getVisibility() == View.VISIBLE) {
                    crossfade(messageLayout, ageLayout);
                    skipButton.setVisibility(View.VISIBLE);
                    backButton.bringToFront();

                } else {
                    updateModel();
                    nextActivity();
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
     */
    private void updateModel() {
        for (int i = 0; i< vgChildAges.getChildCount(); ++i){
            String name = ((TextView) vgChildAges.getChildAt(i).findViewById(R.id.child_name_text)).getText().toString();
            for(Parent.Child child: PrivalinoOnboardHandler.parentModel.getChildren()){
                if(child.getName().equals(name) || child.getPhoneNumber().equals(name)){
                    String year = ((TextView) vgChildAges.getChildAt(i).findViewById(R.id.age_text)).getText().toString();
                    child.setBirthYear(Integer.parseInt(year));
                }
            }
        }
        preferences.edit().putString(SHAREDPREFS_KEY_CHILDREN, PrivalinoOnboardHandler.parentModel.getChildrenAsString()).apply();
    }

    private void nextActivity(){
        Intent intent = new Intent(AddChildAgeActivity.this, FinishOnboardActivity.class);
        intent.putExtra(INTENT_EXTRA_KEY_IS_PARENT, true);
        startActivity(intent);
        AnimationHelper.transitionAnimation(this);
    }

    /**
     * Takes data stored in the shared preferences, initializes the model and adds an element to the
     * view group for each child in the model
     */
    private void addElements(){
        if (PrivalinoOnboardHandler.parentModel.getChildren().size() == 0){
            PrivalinoOnboardHandler.parentModel.parseChildren(preferences.getString(SHAREDPREFS_KEY_CHILDREN, ""));
        }
        List<Parent.Child> children = PrivalinoOnboardHandler.parentModel.getChildren();
        for(Parent.Child child: children){
            int birthYear = (child.getBirthYear() == 0)? 2008 : child.getBirthYear();
            if (!child.getName().isEmpty()){
                addElement(child.getName(), birthYear);
            } else {
                addElement(child.getPhoneNumber(), birthYear);
            }
        }

    }


    private void addElement(String name, int age){
        final View item = getLayoutInflater().inflate(R.layout.child_age_item, null);

        final RelativeLayout subtractButton = (RelativeLayout) item.findViewById(R.id.minus_button);
        final RelativeLayout addButton = (RelativeLayout) item.findViewById(R.id.plus_button);
        final TextView ageText = (TextView) item.findViewById(R.id.age_text);
        TextView childNameText = (TextView) item.findViewById(R.id.child_name_text);

        childNameText.setText(name);
        ageText.setText(String.format("%d",age));

        subtractButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ageText.setText(String.format("%d",Integer.parseInt(ageText.getText().toString()) - 1));
            }
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ageText.setText(String.format("%d",Integer.parseInt(ageText.getText().toString()) + 1));
            }
        });

        addButton.bringToFront();
        subtractButton.bringToFront();

        vgChildAges.addView(item);
    }


    public void onBackPressed() {
        if (ageLayout.getVisibility() == View.VISIBLE) {
            crossfade(ageLayout, messageLayout);
            skipButton.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
            AnimationHelper.transitionAnimation(this);
        }
    }
}
