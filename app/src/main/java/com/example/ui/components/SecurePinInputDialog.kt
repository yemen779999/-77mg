package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SecurePinInputDialog(
    onDismiss: () -> Unit,
    onPinConfirmed: (String) -> Unit,
    title: String = "إدخال رمز الأمان (PIN) 🔐"
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "يرجى إدخال رمز الأمان المكون من 4 أرقام المكتوب على شاشة الكمبيوتر:",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pin = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 24.sp, letterSpacing = 8.sp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onPinConfirmed(pin) },
                enabled = pin.length == 4,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("تأكيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
