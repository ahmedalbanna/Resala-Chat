package com.example.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.AuthManager
import kotlinx.coroutines.launch

/**
 * Phase 4: Onboarding — phone-auth + permissions rationale + empty-state CTA.
 * Arabic-first, a11y labelled, navigates to chatList on complete.
 * Permissions are requested with rationale (Android 13 POST_NOTIFICATIONS, RECORD_AUDIO, CAMERA, LOCATION).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var permStatus by remember { mutableStateOf(mapOf<String, Boolean>()) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permStatus = permStatus + (Manifest.permission.POST_NOTIFICATIONS to granted)
    }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permStatus = permStatus + (Manifest.permission.RECORD_AUDIO to granted)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permStatus = permStatus + (Manifest.permission.CAMERA to granted)
    }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permStatus = permStatus + (Manifest.permission.ACCESS_FINE_LOCATION to granted)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Box(
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Shield, contentDescription = "شعار رِسالة للتراسل الآمن", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            }
            Text("مرحباً بك في رِسالة", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center, modifier = Modifier.testTag("onboarding_title"))
            Text(
                "تراسل مشفّر، محلي، وعربي أولاً. سجّل رقم هاتفك، امنح الأذونات، وابدأ محادثة آمنة.",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
            )

            // Phone auth card
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Phone, contentDescription = "أيقونة الهاتف", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text("تسجيل برقم الهاتف", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it; error = null },
                        label = { Text("رقم الهاتف (+966 ...)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("onboarding_phone_input")
                    )
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { otp = it },
                        label = { Text("رمز التحقق OTP (اختياري للتجربة)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("onboarding_otp_input")
                    )
                    if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    Button(
                        onClick = {
                            if (phone.length < 9) { error = "أدخل رقم هاتف صحيح"; return@Button }
                            isLoading = true
                            scope.launch {
                                try {
                                    val auth = AuthManager(context)
                                    val res = auth.signInWithPhone(phone, otp.ifBlank { null })
                                    if (res.isSuccess) onComplete() else error = "تعذر التسجيل، حاول مجدداً"
                                } catch (e: Exception) { error = e.message } finally { isLoading = false }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().testTag("onboarding_verify_button")
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else Text("تحقق ومتابعة")
                    }
                    TextButton(onClick = onComplete, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("تخطي (وضع محلي)", fontSize = 12.sp)
                    }
                }
            }

            // Permissions rationale
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("الأذونات — لماذا نحتاجها؟", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    PermissionRow(icon = Icons.Default.Notifications, title = "الإشعارات", desc = "لتنبيهك برسائل جديدة حتى مع قفل التطبيق", granted = permStatus[Manifest.permission.POST_NOTIFICATIONS] ?: false, onRequest = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) })
                    PermissionRow(icon = Icons.Default.Mic, title = "الميكروفون", desc = "لتسجيل رسائل صوتية مشفرة ومكالمات صوتية", granted = permStatus[Manifest.permission.RECORD_AUDIO] ?: false, onRequest = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) })
                    PermissionRow(icon = Icons.Default.Videocam, title = "الكاميرا", desc = "لالتقاط صور مشفرة ومكالمات مرئية", granted = permStatus[Manifest.permission.CAMERA] ?: false, onRequest = { cameraLauncher.launch(Manifest.permission.CAMERA) })
                    PermissionRow(icon = Icons.Default.LocationOn, title = "الموقع", desc = "لمشاركة موقعك المشفر عند الحاجة فقط", granted = permStatus[Manifest.permission.ACCESS_FINE_LOCATION] ?: false, onRequest = { locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) })
                }
            }

            // CTA
            Button(
                onClick = onComplete,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().testTag("onboarding_start_cta")
            ) {
                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("ابدأ التراسل الآمن")
            }
            Text("يمكنك تغيير الأذونات لاحقاً من إعدادات النظام. التشفير لا يتطلب أذونات.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PermissionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = "أيقونة $title", tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (granted) {
            Icon(Icons.Default.CheckCircle, contentDescription = "ممنوح", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        } else {
            OutlinedButton(onClick = onRequest, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                Text("سماح", fontSize = 12.sp)
            }
        }
    }
}
