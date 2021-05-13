package com.example.mytaxi.ui.login;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mytaxi.MainActivity;
import com.example.mytaxi.R;
import com.example.mytaxi.data.model.LoggedInUser;
import com.example.mytaxi.ui.login.LoginViewModel;
import com.example.mytaxi.ui.login.LoginViewModelFactory;
import com.example.mytaxi.databinding.ActivityLoginBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel loginViewModel;
    //Переменная для работы с БД
    private SQLiteDatabase mDb;
    public EditText usernameEditText;

    private FirebaseAuth mAuth;
    ProgressDialog loadingBar;
    DatabaseReference CustomerDatabaseRef;
    String OnlineCustomerID;
    DatabaseReference DriverDatabaseRef;
    String OnlineDriverID;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);

        mAuth = FirebaseAuth.getInstance();

        usernameEditText = findViewById(R.id.mail);
        final EditText passwordEditText = findViewById(R.id.password);
        final EditText fioEditText = findViewById(R.id.user_fio);
        final Button loginButton = findViewById(R.id.login);
        final Button registrButton = findViewById(R.id.registr);
        final ProgressBar loadingProgressBar = findViewById(R.id.loading);
        final CheckBox checkBox = findViewById(R.id.checkPass);
        final CheckBox checkDriver = findViewById(R.id.checkDriver);
        loadingBar = new ProgressDialog(this);

        loginViewModel.getLoginFormState().observe(this, new Observer<LoginFormState>() {
            @Override
            public void onChanged(@Nullable LoginFormState loginFormState) {
                if (loginFormState == null) {
                    return;
                }
                loginButton.setEnabled(loginFormState.isDataValid());
                if (loginFormState.getUsernameError() != null) {
                    usernameEditText.setError(getString(loginFormState.getUsernameError()));
                }
                if (loginFormState.getPasswordError() != null) {
                    passwordEditText.setError(getString(loginFormState.getPasswordError()));
                }
            }
        });

        loginViewModel.getLoginResult().observe(this, new Observer<LoginResult>() {
            @Override
            public void onChanged(@Nullable LoginResult loginResult) {
                if (loginResult == null) {
                    return;
                }
                loadingProgressBar.setVisibility(View.GONE);
                if (loginResult.getError() != null) {
                    showLoginFailed(loginResult.getError());
                }
                if (loginResult.getSuccess() != null) {
                    updateUiWithUser(loginResult.getSuccess());
                }
                setResult(Activity.RESULT_OK);

                //Complete and destroy login activity once successful
                //finish();
            }
        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.loginDataChanged(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        };
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginViewModel.login(usernameEditText.getText().toString(),
                            passwordEditText.getText().toString());
                }
                return false;
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkDriver.isChecked()){
                    SignInDriver(usernameEditText.getText().toString(),
                            passwordEditText.getText().toString());
                }
                else SignInCustomer(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        });
        registrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkDriver.isChecked()){
                    RegisterDriver(usernameEditText.getText().toString(),
                            passwordEditText.getText().toString());
                }
                else RegisterCustomer(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        });

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                } else {
                    passwordEditText.setInputType(129);
                }
            }
        });
    }

    private void updateUiWithUser(LoggedInUserView model) {
        String welcome = getString(R.string.welcome) + model.getDisplayName();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("mail", usernameEditText.getText().toString());

        startActivity(intent);

    }

    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }

    private void SignInCustomer(String email, String password) {

        loadingBar.setTitle("Вход для клиентов");
        loadingBar.setMessage("Пожалуйста, дождитесь загрузки");
        loadingBar.show();

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful())
                {
                    Toast.makeText(LoginActivity.this, getString(R.string.login_well_done), Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();

                    Intent customerIntent = new Intent(LoginActivity.this, MainActivity.class);
                    customerIntent.putExtra("mail", usernameEditText.getText().toString());
                    customerIntent.putExtra("login_type", "customer");
                    startActivity(customerIntent);
                }
                else
                {
                    Toast.makeText(LoginActivity.this, getString(R.string.login_failed), Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
            }
        });
    }
    private void RegisterCustomer(String email, String password)
    {
        loadingBar.setTitle("Регистрация клиента");
        loadingBar.setMessage("Пожалуйста, дождитесь загрузки");
        loadingBar.show();

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful())
                {
                    OnlineCustomerID = mAuth.getCurrentUser().getUid();
                    CustomerDatabaseRef = FirebaseDatabase.getInstance().getReference()
                            .child("Users").child("Customers").child(OnlineCustomerID);
                    CustomerDatabaseRef.setValue(true);

                    Intent customerIntent = new Intent(LoginActivity.this, MainActivity.class);
                    customerIntent.putExtra("mail", usernameEditText.getText().toString());
                    customerIntent.putExtra("login_type", "customer");
                    startActivity(customerIntent);

                    Toast.makeText(LoginActivity.this, getString(R.string.registration_well_done), Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
                else
                {
                    Toast.makeText(LoginActivity.this, getString(R.string.registration_failed), Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
            }
        });
    }

    private void SignInDriver(String email, String password)
    {
        loadingBar.setTitle("Вход водителя");
        loadingBar.setMessage("Пожалуйста, дождитесь загрузки");
        loadingBar.show();

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful())
                {
                    Toast.makeText(LoginActivity.this, getString(R.string.login_well_done), Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                    Intent driverIntent = new Intent(LoginActivity.this, MainActivity.class);
                    driverIntent.putExtra("mail", usernameEditText.getText().toString());
                    driverIntent.putExtra("login_type", "driver");

                    startActivity(driverIntent);
                }
                else
                {
                    Toast.makeText(LoginActivity.this, getString(R.string.login_failed), Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
            }
        });
    }

    private void RegisterDriver(String email, String password)
    {
        loadingBar.setTitle("Регистрация водителя");
        loadingBar.setMessage("Пожалуйста, дождитесь загрузки");
        loadingBar.show();

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful())
                {
                    OnlineDriverID = mAuth.getCurrentUser().getUid();
                    DriverDatabaseRef = FirebaseDatabase.getInstance().getReference()
                            .child("Users").child("Drivers").child(OnlineDriverID);
                    DriverDatabaseRef.setValue(true);

                    Intent driverIntent = new Intent(LoginActivity.this, MainActivity.class);
                    driverIntent.putExtra("mail", usernameEditText.getText().toString());
                    driverIntent.putExtra("login_type", "driver");
                    startActivity(driverIntent);

                    Toast.makeText(LoginActivity.this, getString(R.string.registration_well_done), Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();

                }
                else
                {
                    Toast.makeText(LoginActivity.this, getString(R.string.registration_failed), Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();
                }
            }
        });
}
}