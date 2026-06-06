package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import com.example.ui.theme.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Message
import com.example.data.User
import com.example.util.EncryptionEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    user: User,
    messages: List<Message>,
    isTyping: Boolean,
    showDecrypted: Boolean,
    onSendMessage: (String) -> Unit,
    onSendLocation: (lat: Double, lng: Double) -> Unit,
    onSendImage: (String) -> Unit,
    onSendVoice: (duration: Int) -> Unit,
    onStartCall: (isVideo: Boolean) -> Unit,
    onBack: () -> Unit,
    onToggleDecrypted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember { mutableStateOf("") }
    var showInfoDialog by remember { mutableStateOf(false) }
    var isRecordingVoicenote by remember { mutableStateOf(false) }
    var recordingDurationSecs by remember { mutableStateOf(0) }
    
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Voice recording timer simulator
    LaunchedEffect(isRecordingVoicenote) {
        if (isRecordingVoicenote) {
            recordingDurationSecs = 0
            while (isRecordingVoicenote) {
                delay(1000)
                recordingDurationSecs++
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.clickable { showInfoDialog = true }
                    ) {
                        // Initials Avatar
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.fullName.take(1),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 15.sp
                            )
                        }

                        Column {
                            Text(
                                text = user.fullName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isTyping) "جاري الكتابة بخصوصية... 💬" else user.lastSeen,
                                fontSize = 11.sp,
                                color = if (isTyping) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = if (isTyping) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "للخلف"
                        )
                    }
                },
                actions = {
                    // Call WebRTC controls
                    IconButton(onClick = { onStartCall(false) }) {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = "مكالمة صوتية WebRTC")
                    }
                    IconButton(onClick = { onStartCall(true) }) {
                        Icon(imageVector = Icons.Default.Videocam, contentDescription = "مكالمة مرئية WebRTC")
                    }

                    // Encryption toggler icon indicator
                    IconButton(onClick = onToggleDecrypted) {
                        Icon(
                            imageVector = if (showDecrypted) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = "عرض محتوى قاعدة البيانات المشفر",
                            tint = if (showDecrypted) MaterialTheme.colorScheme.primary else Color(0xFFFBBF24)
                        )
                    }
                    
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "تفاصيل الجلسة المشفرة")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // E2EE System Banner Alert
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "شفافية الحماية",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showDecrypted) 
                            "المحادثة مشفرة بالكامل (E2EE) ببروتوكول Double Ratchet 🛡️" 
                            else "تنبيه: أنت تعرض الآن البيانات الخام (المشفرة في SQLite) 🔒",
                        fontSize = 11.sp,
                        color = if (showDecrypted) MaterialTheme.colorScheme.primary else Color(0xFFFBBF24),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Messages Container
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    MessageBubbleItem(
                        msg = msg,
                        peerPublicKey = user.publicKey,
                        showDecrypted = showDecrypted
                    )
                }

                if (isTyping) {
                    item {
                        TypingBubbleItem(peerName = user.fullName)
                    }
                }
            }

            // Input Control Dashboard
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = isRecordingVoicenote,
                    transitionSpec = {
                        slideInVertically { it } togetherWith slideOutVertically { it }
                    }
                ) { recordingState ->
                    if (recordingState) {
                        // Voice Recording Status Screen
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                    // simple infinite pulsing indicator can be native, keep stationary for reliable builds
                                )
                                Text(
                                    text = "جاري تسجيل مقطع صوتي مشفر: ${String.format("%02d:%02d", recordingDurationSecs / 60, recordingDurationSecs % 60)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red
                                )
                            }
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { isRecordingVoicenote = false }
                                ) {
                                    Text("حذف", color = Color.Gray, fontSize = 13.sp)
                                }
                                
                                Button(
                                    onClick = {
                                        isRecordingVoicenote = false
                                        val finalDuration = if (recordingDurationSecs == 0) 4 else recordingDurationSecs
                                        onSendVoice(finalDuration)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("إنهاء وإرسال 🎙️", fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        // Normal keyboard input dash
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .imePadding()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Left actions: Photo / GPS Location
                                IconButton(
                                    onClick = {
                                        // Send realistic mock secure photos
                                        val presetPhotos = listOf("desert_oasis", "secure_vault", "green_forest", "night_sky")
                                        val chosen = presetPhotos.random()
                                        onSendImage(chosen)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "إرسال صورة مشفرة",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        // Send a randomized secure coordinates location (e.g. around Riyadh, Saudi Arabia)
                                        val riyadhLat = 24.7136 + (Math.random() - 0.5) * 0.05
                                        val riyadhLng = 46.6753 + (Math.random() - 0.5) * 0.05
                                        onSendLocation(miyadhFormatted(riyadhLat), miyadhFormatted(riyadhLng))
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "إرسال موقع جغرافي مشفر",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Text field
                                OutlinedTextField(
                                    value = textValue,
                                    onValueChange = { textValue = it },
                                    placeholder = { Text("اكتب رسالة مشفرة...", fontSize = 13.sp) },
                                    maxLines = 4,
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                        .testTag("chat_detail_text_input")
                                )

                                // Right primary action: Send or Recording hold
                                if (textValue.trim().isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            onSendMessage(textValue)
                                            textValue = ""
                                        },
                                        modifier = Modifier.testTag("send_msg_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "إرسال الرسالة",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = { isRecordingVoicenote = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "تسجيل مقطع صوتي",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInfoDialog) {
        SecurityDetailsDialog(
            user = user,
            onDismiss = { showInfoDialog = false }
        )
    }
}

private fun miyadhFormatted(num: Double): Double {
    return Math.round(num * 10000.0) / 10000.0
}

@Composable
fun MessageBubbleItem(
    msg: Message,
    peerPublicKey: String,
    showDecrypted: Boolean
) {
    val isMe = msg.senderId == "me"
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val bubbleColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    Column(
        horizontalAlignment = alignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // E2EE Lock Icon representation if drawing raw cipher text
                if (!showDecrypted && msg.isE2ee) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Https,
                            contentDescription = "تشغير تام",
                            tint = if (isMe) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Ciphertext (SQLite)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMe) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Render based on Msg Type
                when (msg.msgType) {
                    "IMAGE" -> {
                        Column {
                            // Draw a beautiful simulated photo container
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(1.dp, if (isMe) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Filter,
                                        contentDescription = "صورة مشفرة",
                                        tint = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Text(
                                        text = "وسيط معمد ومحمي 🔒",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (showDecrypted) 
                                    EncryptionEngine.decrypt(msg.contentCipher, peerPublicKey) 
                                    else msg.contentCipher,
                                color = textColor,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                    "LOCATION" -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Map,
                                            contentDescription = "خريطة",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "موقع جغرافي متزامن 📍",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "إحداثيات: ${msg.latitude}, ${msg.longitude}",
                                            fontSize = 10.sp,
                                            color = textColor.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (showDecrypted) 
                                    "تحديد آمن: في الخريطة المصغرة بمحيط الرياض."
                                    else msg.contentCipher,
                                color = textColor,
                                fontSize = 13.sp
                            )
                        }
                    }
                    "AUDIO" -> {
                        // Interactive Voice Note with seek bar simulator
                        var isPlaying by remember { mutableStateOf(false) }
                        var playProgress by remember { mutableStateOf(0.0f) }
                        
                        LaunchedEffect(isPlaying) {
                            if (isPlaying) {
                                playProgress = 0.0f
                                while (playProgress < 1.0f && isPlaying) {
                                    delay(100)
                                    playProgress += 0.05f
                                }
                                isPlaying = false
                                playProgress = 0.0f
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { isPlaying = !isPlaying },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isMe) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary),
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "تشغيل",
                                        tint = if (isMe) Color.White else MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Custom drawn sound wave canvas
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(24.dp)
                                    ) {
                                        val count = 20
                                        val spacing = 4f
                                        val waveWidth = (size.width - (spacing * (count - 1))) / count
                                        
                                        clipRect {
                                            for (i in 0 until count) {
                                                // Deterministic wave amplitudes
                                                val amplitude = when (i % 5) {
                                                    0 -> 0.3f
                                                    1 -> 0.7f
                                                    2 -> 0.9f
                                                    3 -> 0.5f
                                                    else -> 0.2f
                                                }
                                                val waveHeight = size.height * amplitude
                                                val active = (i.toFloat() / count) <= playProgress

                                                drawRect(
                                                    color = if (active) {
                                                        if (isMe) Color.White else EmeraldPrimary
                                                    } else {
                                                        if (isMe) Color.White.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.3f)
                                                    },
                                                    topLeft = Offset(i * (waveWidth + spacing), (size.height - waveHeight) / 2),
                                                    size = Size(waveWidth, waveHeight)
                                                )
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = "0:${String.format("%02d", msg.audioDuration)}",
                                    fontSize = 10.sp,
                                    color = textColor.copy(alpha = 0.8f)
                                )
                            }
                            Text(
                                text = if (showDecrypted) "🎙️ رسالة صوتية مشفرة بالخلفية." else msg.contentCipher,
                                color = textColor,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    else -> {
                        // Standard TEXT Message
                        Text(
                            text = if (showDecrypted) 
                                EncryptionEngine.decrypt(msg.contentCipher, peerPublicKey) 
                                else msg.contentCipher,
                            color = textColor,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Start
                        )
                    }
                }

                // Bottom timestamp & read checkmarks
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatMessageTime(msg.timestamp),
                        fontSize = 9.sp,
                        color = textColor.copy(alpha = 0.6f)
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (msg.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = "استلام",
                            tint = if (msg.isRead) AccentBlue else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TypingBubbleItem(peerName: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$peerName يكتب الآن...",
                    fontSize = 12.sp,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.SemiBold
                )
                // Simulated pulsing ellipsis representing loading
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
            }
        }
    }
}

@Composable
fun SecurityDetailsDialog(
    user: User,
    onDismiss: () -> Unit
) {
    val fingerprint = EncryptionEngine.generateFingerprint(user.publicKey, "IK-my-local-identity-hash")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "توثيق التشفير",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("تثبيت التحقق الأمني 🔐", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "بروتوكول نهاية لنهاية (End-to-End Encryption) نشط بنجاح مع جهة الاتصال: ${user.fullName}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Text(
                    text = "رقم البصمة المشتركة (Fingerprint):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Fingerprint code grid
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = fingerprint,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.5.sp
                    )
                }
                
                Text(
                    text = "تحقق من تطابق هذا الرقم مع الطرف الآخر لتجنب هجمات Man-In-The-Middle وضمان تشفير تام وفوري للمكالمات والملفات المتبادلة.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "مفتاح الهوية العام للاتصال:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = user.publicKey,
                        fontSize = 10.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("تم التحقق وتأكيد الأمان")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
