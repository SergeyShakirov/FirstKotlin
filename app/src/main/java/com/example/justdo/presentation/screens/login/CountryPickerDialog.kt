package com.example.justdo.presentation.screens.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.justdo.data.models.CountryInfo
import com.example.justdo.ui.theme.TelegramColors

@Composable
fun CountryPickerDialog(
    countries: List<CountryInfo>,
    onDismiss: () -> Unit,
    onCountrySelected: (CountryInfo) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 350.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = TelegramColors.Background
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Выберите страну",
                    style = MaterialTheme.typography.titleLarge,
                    color = TelegramColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = TelegramColors.TextSecondary.copy(alpha = 0.3f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    countries.forEach { country ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCountrySelected(country) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${country.flagEmoji} ${country.name}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TelegramColors.TextPrimary,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = country.code,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TelegramColors.Primary
                            )
                        }

                        HorizontalDivider(color = TelegramColors.TextSecondary.copy(alpha = 0.1f))
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TelegramColors.Primary
                    )
                ) {
                    Text("Отмена")
                }
            }
        }
    }
}