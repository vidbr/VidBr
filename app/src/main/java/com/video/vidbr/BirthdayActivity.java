package com.video.vidbr;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar; // Adicione esta importação

import java.util.Calendar;

public class BirthdayActivity extends AppCompatActivity {

    private TextView birthdayTextView;
    private Button confirmButton;
    private String email;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_birthday);

        birthdayTextView = findViewById(R.id.birthday_text);
        confirmButton = findViewById(R.id.confirm_button);

        // Receber e-mail e senha
        email = getIntent().getStringExtra("email");
        password = getIntent().getStringExtra("password");

        birthdayTextView.setOnClickListener(v -> showDatePickerDialog());

        confirmButton.setOnClickListener(v -> {
            String birthday = birthdayTextView.getText().toString();
            if (!birthday.equals(getString(R.string.birthday_placeholder))) {
                if (isOlderThan13(birthday)) {
                    // Ir para a próxima atividade
                    Intent intent = new Intent(BirthdayActivity.this, RealnameActivity.class);
                    intent.putExtra("email", email);
                    intent.putExtra("password", password);
                    intent.putExtra("birthday", birthday); // Passar a data de nascimento
                    startActivity(intent);
                    finish();
                } else {
                    // Exibir Snackbar se o usuário for menor de 13 anos
                    Toast.makeText(BirthdayActivity.this, getString(R.string.age_error_message), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(BirthdayActivity.this, "Por favor, selecione sua data de nascimento", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                BirthdayActivity.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    birthdayTextView.setText(selectedDate);
                },
                year, month, day
        );

        datePickerDialog.show();
    }

    private boolean isOlderThan13(String birthday) {
        String[] parts = birthday.split("/");
        int day = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]) - 1; // Janeiro é 0
        int year = Integer.parseInt(parts[2]);

        Calendar birthDate = Calendar.getInstance();
        birthDate.set(year, month, day);

        Calendar currentDate = Calendar.getInstance();
        currentDate.add(Calendar.YEAR, -13); // Subtrair 13 anos para comparação

        return birthDate.before(currentDate);
    }
}
