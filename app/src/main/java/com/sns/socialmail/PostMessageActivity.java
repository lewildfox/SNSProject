package com.sns.socialmail;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PostMessageActivity extends AppCompatActivity {


    private static final String SHARED_PREFERENCES_NAME = "AuthStatePreference";
    private static final String AUTH_STATE = "AUTH_STATE";
    private static final String ACC_TYPE = "ACC_TYPE";
    private static final String LOG_TAG = "POST_MESSAGE";


    //AppAuth state
    AuthState lAuthState;
    AuthorizationService lAuthorizationService;

    //MailSender
    private static final int PICK_FROM_GALLERY = 101;
    private static final int PERMISSION_REQUEST_STORAGE = 0;
    String attachmentPath = null;
    String attachmentType = null;
    GetPathUtil PathUtil = new GetPathUtil();
    String senderMail;
    String emailBody = "";
    String toEmails;
    List toEmailList;

    //Buttons
    Button send;
    Button attachment;

    String profurl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        send = findViewById(R.id.bt_send);
        attachment = findViewById(R.id.bt_attach);

        //appauth
        lAuthState = restoreAuthState();
        lAuthorizationService = new AuthorizationService(this);
        getAccountType();

        send.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                Log.i(LOG_TAG, "Send Button Clicked.");

                toEmails = ((TextView) findViewById(R.id.toInput))
                        .getText().toString();
                toEmailList = Arrays.asList(toEmails.split("\\s*,\\s*"));
                Log.i(LOG_TAG, "To List: " + toEmailList);
                emailBody = ((TextView) findViewById(R.id.bodyInput))
                        .getText().toString();

                //URL to get user email account
                if (getAccountType() == "gmail"){
                    //profurl = "https://graph.microsoft.com/v1.0/me/";}
                    profurl = "https://www.googleapis.com/gmail/v1/users/me/profile";
                }
                else{
                    profurl = "https://apis.live.net/v5.0/me";
                }

                //GET OWN AUTHORIZED EMAIL ADDRESS
                lAuthState.performActionWithFreshTokens(lAuthorizationService, new AuthState.AuthStateAction() {
                    @Override
                    public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException exception) {
                        new AsyncTask<String, Void, JSONObject>() {

                            @Override
                            protected JSONObject doInBackground(String... tokens) {
                                OkHttpClient client = new OkHttpClient();
                                Request request = new Request.Builder()
                                        .url(profurl) //Get own email address
                                        .addHeader("Authorization", String.format("Bearer %s", tokens[0]))
                                        .build();

                                try {
                                    Response response = client.newCall(request).execute();
                                    String jsonBody = response.body().string();
                                    return new JSONObject(jsonBody);
                                } catch (Exception exception) {
                                    Log.w(LOG_TAG, exception);
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(JSONObject userInfo) {
                                if (userInfo != null) {
                                    //If GMAIL
                                    if(getAccountType() == "gmail") {
                                        senderMail = userInfo.optString("emailAddress");
                                        Log.i(LOG_TAG, "Sender Email: " + senderMail);
                                        if (senderMail != null) {
                                            sendEmail("gmail"); //EXECUTE SEND MAIL TASK After Getting User's Email address
                                        }
                                    }
                                    //IF MICROSOFT
                                    else{
                                        Log.i(LOG_TAG, "userinfo JSON: " + userInfo.toString());
                                        try {
                                            //nameLive = userInfo.getString("name");
                                            senderMail = userInfo.getJSONObject("emails").getString("account");
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        if (senderMail != null) {
                                            sendEmail("outlook"); //EXECUTE SEND MAIL TASK After Getting User's Email address
                                        }

                                    }
                                }
                            }
                        }.execute(accessToken);
                    }
                });


            }

        });

        //attachment button listener
        attachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileBrowserTask();
            }
        });

    }


    //EXECUTE SEND MAIL TASK After Getting User's Email address
    private void sendEmail(final String accType) {
        lAuthState.performActionWithFreshTokens(lAuthorizationService, new AuthState.AuthStateAction() {
            @Override
            public void execute(
                    String accessToken,
                    String idToken,
                    AuthorizationException ex) {
                if (ex != null) {
                    // negotiation for fresh tokens failed, check ex for more details
                    return;
                }
                Log.i(LOG_TAG, String.format("Sending email from " + senderMail + " with [Access Token: %s, ID Token: %s]", accessToken, idToken));

                if (attachmentType != null) {
                    new SendMailTask(PostMessageActivity.this).execute(senderMail,
                            accessToken, toEmailList, "---POST " + attachmentType.toUpperCase(), emailBody, attachmentPath, accType);
                } else {
                    new SendMailTask(PostMessageActivity.this).execute(senderMail,
                            accessToken, toEmailList, "---POST STATUS", emailBody, "null", accType);
                }
            }
        });
    }


    @Nullable
    public AuthState restoreAuthState() {
        String jsonString = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(AUTH_STATE, null);
        if (!TextUtils.isEmpty(jsonString)) {
            try {
                return AuthState.fromJson(jsonString);
            } catch (JSONException jsonException) {
                // should never happen
            }
        }
        return null;
    }

    //GET ACCOUNT TYPES
    private String getAccountType() {
        String acctype = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(ACC_TYPE, null);
        Log.i(LOG_TAG, "account: " + acctype);

        return acctype;
    }


    //METHODS RELATED TO FILE ACCESS BELOW & ATTACHMENT

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_FROM_GALLERY && resultCode == RESULT_OK) {
            Uri selectedImage = data.getData();

            //Get file mimetype
            attachmentType = getMimeType(selectedImage);

            //Get file path from URI
            try {
                attachmentPath = PathUtil.getFilePath(PostMessageActivity.this, selectedImage);

                //Display attachment filename
                TextView attachedFile = findViewById(R.id.attachFilename);
                if (attachmentPath != null) {
                    attachedFile.setText("Attached File: " + new File(attachmentPath).getName());
                } else {
                    attachedFile.setText("Attached File: null");
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

            Log.i(LOG_TAG, "Attachment URI: " + selectedImage.toString());
            Log.i(LOG_TAG, "Attachment Type: " + attachmentType);
            Log.i(LOG_TAG, "Attachment Path: " + attachmentPath);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        View view = findViewById(R.id.post_layout_id);
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            // Request for storage permission.
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // if permission granted
                Snackbar.make(view, "Storage permission granted! Opening gallery...",
                        Snackbar.LENGTH_SHORT)
                        .show();
                openFileBrowser();
            } else {
                // if permission denied.
                Snackbar.make(view, "Storage permission request was denied.",
                        Snackbar.LENGTH_SHORT)
                        .show();
            }
        }
    }



    //OPEN FILE BROWSER: Attachment Button Action
    private void openFileBrowserTask() {
        View view = findViewById(R.id.post_layout_id);
        // Check if storage permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission is already available
            Snackbar.make(view,
                    "Storage permission is granted.",
                    Snackbar.LENGTH_SHORT).show();
            openFileBrowser();
        } else {
            // Permission is missing and must be requested.
            requestStoragePermission();
        }
    }

    //OPEN FILE BROWSER: OPEN INTENT
    private void openFileBrowser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        //intent.setType("image/*,video/*");
        //startActivityForResult(intent, PICK_FROM_GALLERY);
        intent.setType("*/*");
        String[] mimetypes = {"image/*", "video/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        startActivityForResult(intent, PICK_FROM_GALLERY);
    }

    //OPEN FILE BROWSER: CHECK IF STORAGE PERMISSION IS GRANTED - REQUEST OTHERWISE
    private void requestStoragePermission() {
        // Permission has not been granted and must be requested.
        View view = findViewById(R.id.post_layout_id);
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with a button to request the missing permission.
            Snackbar.make(view, "Storage access is required to open files.",
                    Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(PostMessageActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            PERMISSION_REQUEST_STORAGE);
                }
            }).show();

        } else {
            Snackbar.make(view,
                    "Permission is not available. Requesting storage permission.",
                    Snackbar.LENGTH_SHORT).show();
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_STORAGE);
        }
    }

    //GET FILE MIMETYPE (Image or Video)
    private  String getMimeType(Uri uri) {
        /*String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        if (extension != null) {
            attachmentType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }*/
        ContentResolver cr = this.getContentResolver();
        String type = cr.getType(uri);
        attachmentType = type.split("/")[0];
        return attachmentType;
    }

}
