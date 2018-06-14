package org.telegram.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v13.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.Manifest;
import org.telegram.messenger.R;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.privalino.telegram.AnimationHelper;
import de.privalino.telegram.PrivalinoOnboardHandler;
import de.privalino.telegram.model.RegisterResponse;
import de.privalino.telegram.rest.PrivalinoOnboardApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static de.privalino.telegram.AnimationHelper.crossfade;
import static de.privalino.telegram.AppConstants.INTENT_EXTRA_KEY_IS_PARENT;
import static de.privalino.telegram.AppConstants.MY_PERMISSIONS_REQUEST_READ_CONTACTS;
import static de.privalino.telegram.AppConstants.RESULT_PICK_CONTACT;
import static de.privalino.telegram.AppConstants.SHAREDPREFS_KEY_PARENTS;
import static de.privalino.telegram.AppConstants.SHAREDRPREFS_KEY_ON_BOARDING_INFO;
import static de.privalino.telegram.PrivalinoOnboardHandler.childModel;

public class AddParentPhoneActivity extends Activity {

    ViewGroup vgParentPhones;
    private boolean expanded = false;
    RelativeLayout messageLayout;
    RelativeLayout phonesLayout;
    TextView skipButton;
    int items = 0;
    EditText[] setFromContact = new EditText[2];
    SharedPreferences preferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_parent_phone);

        vgParentPhones = (LinearLayout) findViewById(R.id.phone_numbers_list);

        messageLayout = (RelativeLayout) findViewById(R.id.message_layout);
        phonesLayout = (RelativeLayout) findViewById(R.id.phones_layout);
        preferences = ApplicationLoader.applicationContext.getSharedPreferences(SHAREDRPREFS_KEY_ON_BOARDING_INFO, Activity.MODE_PRIVATE);

        onClickListeners();

        initialize();


    }

    /**
     * Initializes the model from the data stored in shared preferences, if there is any.
     */
    private void initialize() {
        String data = preferences.getString(SHAREDPREFS_KEY_PARENTS, "");
        if (!data.isEmpty()){
            childModel.parseParentsNumbersString(data);
            for (String parentPhone: childModel.getParentsNumbers()){
                initializeElement(parentPhone);
            }
        } else {
            addElement();
        }

    }

    private void onClickListeners() {
        ImageView addButton = (ImageView) findViewById(R.id.add_button);
        final ImageView nextButton = (ImageView) findViewById(R.id.next_button);
        final RelativeLayout backButton = (RelativeLayout) findViewById(R.id.back_button);


        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (items == getConfirmedItemsCount()) {
                    if (items < 3){
                        addElement();
                    } else {
                        Toast.makeText(AddParentPhoneActivity.this, R.string.parent_limit_warning, Toast.LENGTH_LONG);
                    }
                }

            }
        });

        skipButton = (TextView) findViewById(R.id.skip_button);
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

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (messageLayout.getVisibility() == View.VISIBLE) {
                    crossfade(messageLayout, phonesLayout);
                    skipButton.setVisibility(View.VISIBLE);
                    backButton.bringToFront();
                } else {
                    if (updateModel()) {
                        nextActivity();
                    } else {
                        Toast.makeText(AddParentPhoneActivity.this, R.string.no_phones_warning, Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

    }

    private void nextActivity() {
        Intent intent = new Intent(AddParentPhoneActivity.this, FinishOnboardActivity.class);
        intent.putExtra(INTENT_EXTRA_KEY_IS_PARENT, false);
        startActivity(intent);
        AnimationHelper.transitionAnimation(this);
    }

    /**
     * Creates a new element and initializes its edit text to the given value.
     * @param phoneNumber
     */
    private void initializeElement(String phoneNumber){
        EditText[] fields = addElement();
        fillWithPhoneNumber(phoneNumber, fields[0], fields[1]);
        vgParentPhones.requestFocus();
    }

    /**
     * Adds an element to the view group and returns the field of edit texts (in order name, country
     * code, phone number) that can be used to initialize those values.
     * @return field of edit texts
     */
    private EditText[] addElement() {
        final View item = getLayoutInflater().inflate(R.layout.parent_phone_number_item, null);
        final EditText phoneEditText = (EditText) item.findViewById(R.id.phone_edit_text);
        final EditText codeEditText = (EditText) item.findViewById(R.id.code_edit_text);
        final ImageView source = (ImageView) item.findViewById(R.id.edit_text_field);
        final RelativeLayout deleteButton = (RelativeLayout) item.findViewById(R.id.delete_button);
        final RelativeLayout phoneNumberLayout = (RelativeLayout) item.findViewById(R.id.phone_number_layout);
        final RelativeLayout addFromContactsButton = (RelativeLayout) item.findViewById(R.id.add_from_contacts);

        phoneEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    expand(phoneEditText, codeEditText, source, deleteButton, addFromContactsButton);
                } else if (!codeEditText.hasFocus()) {
                    confirmInput(phoneEditText, codeEditText, phoneNumberLayout, source, deleteButton, addFromContactsButton);
                }
            }
        });


        codeEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    expand(phoneEditText, codeEditText, source, deleteButton, addFromContactsButton);
                } else if (!phoneEditText.hasFocus()) {
                    confirmInput(phoneEditText, codeEditText, phoneNumberLayout, source, deleteButton, addFromContactsButton);
                }
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (items > 1) {
                    vgParentPhones.removeView(item);
                    items--;
                } else {
                    Toast.makeText(AddParentPhoneActivity.this, R.string.phone_number_delete_warning, Toast.LENGTH_LONG).show();
                }
            }
        });

        addFromContactsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setFromContact[0] = codeEditText;
                setFromContact[1] = phoneEditText;
                if (ContextCompat.checkSelfPermission(AddParentPhoneActivity.this,
                        android.Manifest.permission.READ_CONTACTS)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(AddParentPhoneActivity.this,
                            new String[]{android.Manifest.permission.READ_CONTACTS},
                            MY_PERMISSIONS_REQUEST_READ_CONTACTS);

                } else {
                    startPicker();
                }

            }
        });

        vgParentPhones.addView(item);
        vgParentPhones.requestFocus();
        items++;

        return new EditText[]{codeEditText, phoneEditText};

    }


    private void expand(EditText phoneEditText, EditText codeEditText, ImageView source, RelativeLayout deleteButton, RelativeLayout contactsButton) {

        //implements logic for expanding the element when it is clicked on to edit

        if (!expanded) {
            source.setImageDrawable(getResources().getDrawable(R.drawable.input_field));

            phoneEditText.setTextColor(getResources().getColor(R.color.white));

            if (codeEditText.getText().toString().isEmpty()) {
                codeEditText.setText(R.string.germany_country_code_text);
            }
            codeEditText.setTextColor(getResources().getColor(R.color.white));
            deleteButton.setVisibility(View.GONE);
            contactsButton.setVisibility(View.VISIBLE);

            expanded = true;
        }
    }

    private void confirmInput(EditText phoneEditText, EditText codeEditText, RelativeLayout phoneNumberLayout,
                              ImageView source, RelativeLayout deleteButton, RelativeLayout contactsButton) {

        //implements logic for confirming the input in the element after the user clicks outside of the element

        String phone = phoneEditText.getText().toString();
        if (phone.isEmpty()) {
            Toast.makeText(AddParentPhoneActivity.this, R.string.phone_empty_warning, Toast.LENGTH_LONG).show();
            hideKeyboard();
            return;
        } else if (!Patterns.PHONE.matcher(codeEditText.getText().toString() + phone).matches()) {
            Toast.makeText(AddParentPhoneActivity.this, R.string.phone_invalid_warning, Toast.LENGTH_LONG).show();
            hideKeyboard();
            return;
        }


        source.setImageDrawable(getResources().getDrawable(R.drawable.text_inputed_normal));


        phoneEditText.setTextColor(getResources().getColor(R.color.black));
        codeEditText.setTextColor(getResources().getColor(R.color.black));
        phoneNumberLayout.bringToFront();

        deleteButton.setVisibility(View.VISIBLE);
        deleteButton.bringToFront();

        contactsButton.setVisibility(View.GONE);

        hideKeyboard();

        expanded = false;
    }

    /**
     * Updates the static model used to hold the on boarding information.
     * @return true if the update was successful, false otherwise
     */
    private boolean updateModel() {
        if (getConfirmedItemsCount() < 1) {
            return false;
        }
        List<String> numbers = new ArrayList<>();
        for (int i = 0; i < vgParentPhones.getChildCount(); ++i) {
            String phone = ((TextView) vgParentPhones.getChildAt(i).findViewById(R.id.code_edit_text)).getText().toString();
            if (phone.isEmpty()) continue;
            phone += ((TextView) vgParentPhones.getChildAt(i).findViewById(R.id.phone_edit_text)).getText().toString();
            numbers.add(phone);
        }
        childModel.setParentsNumbers(numbers);
        preferences.edit().putString(SHAREDPREFS_KEY_PARENTS, childModel.getParentsNumbersAsString()).apply();
        return true;
    }



    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startPicker();
                } else if (grantResults[0] == -1){
                Toast.makeText(AddParentPhoneActivity.this,
                        R.string.permission_warning, Toast.LENGTH_SHORT);
            }
                return;
            }
        }
    }




    public void startPicker() {
        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(contactPickerIntent, RESULT_PICK_CONTACT);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_PICK_CONTACT){
            contactPicked(data);
        }



    }

    public void contactPicked(Intent data) {
        Cursor cursor = null;
        try {
            String phoneNo = null ;
            // getData() method will have the Content Uri of the selected contact
            Uri uri = data.getData();
            //Query the content uri
            cursor = this.getContentResolver().query(uri, null, null, null, null);
            cursor.moveToFirst();
            // column index of the phone number
            int  phoneIndex =cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            phoneNo = cursor.getString(phoneIndex);
            phoneNo = phoneNo.replaceAll("[^[\\d|+]]","");
            fillWithPhoneNumber(phoneNo, setFromContact[0], setFromContact[1]);

        } catch (Exception e) {
            e.printStackTrace();
        }
        if(cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    /**
     * Fills the edit text fields of the element that is currently under editing with the data
     * gotten from in parameters.
     * @param phoneNumber
     * @param codeEditText
     * @param phoneEditText
     */
    public void fillWithPhoneNumber(String phoneNumber, EditText codeEditText, EditText phoneEditText){
        if(phoneNumber.charAt(0) == '0'){
            phoneEditText.setText(phoneNumber.substring(1));
        } else {
            codeEditText.setText(phoneNumber.substring(0,3));
            phoneEditText.setText(phoneNumber.substring(3));
        }
        codeEditText.requestFocus();
    }

    private void hideKeyboard() {
        vgParentPhones.requestFocus();
        View view = getCurrentFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private int getConfirmedItemsCount() {
        int count = 0;
        for (int i = 0; i < vgParentPhones.getChildCount(); ++i) {
            if (!((TextView) vgParentPhones.getChildAt(i).findViewById(R.id.phone_edit_text)).getText().toString().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public void onBackPressed() {
        if (phonesLayout.getVisibility() == View.VISIBLE) {
            crossfade(phonesLayout, messageLayout);
            skipButton.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
            AnimationHelper.transitionAnimation(this);
        }
    }
}
