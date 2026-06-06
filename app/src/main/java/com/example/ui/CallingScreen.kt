package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CallingScreen(
    callState: CallState,
    onMuteToggle: () -> Unit,
    onCameraToggle: () -> Unit,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0B132B), // Luxury Midnight dark block for calling HUD
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 48.dp, horizontal = 24.dp)
        ) {
            // Header information
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF1C2541).copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Https,
                            contentDescription = "اتصال مشفر",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "اتصال آمن WebRTC P2P 🛡️",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
                
                Text(
                    text = callState.peerName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = callState.status,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                if (callState.durationSecs > 0) {
                    Text(
                        text = String.format("%02d:%02d", callState.durationSecs / 60, callState.durationSecs % 60),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34D399),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Central Portrait Avatar / Mirror Video Card Simulation
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1C2541)),
                contentAlignment = Alignment.Center
            ) {
                if (callState.isVideo) {
                    // Simulated WebRTC Local/Remote Video stream camera feedback
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "بث الفيديو نشط",
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(72.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF34D399).copy(alpha = 0.1f))
                    )
                } else {
                    Text(
                        text = callState.peerName.take(1),
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Bottom Actions HUD Bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Mute icon toggle
                    Button(
                        onClick = onMuteToggle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (callState.isMuted) Color(0xFFEF4444) else Color(0xFF1C2541)
                        ),
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "كتم الصوت",
                            tint = Color.White
                        )
                    }

                    // Video Camera toggle
                    Button(
                        onClick = onCameraToggle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (callState.isVideo) Color(0xFF34D399) else Color(0xFF1C2541)
                        ),
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = if (callState.isVideo) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "تشغيل الكاميرا",
                            tint = Color.White
                        )
                    }

                    // Red End Call floating button
                    Button(
                        onClick = onEndCall,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444)
                        ),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("end_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "إنهاء المكالمة",
                            tint = Color.White
                        )
                    }
                }
                
                Text(
                    text = "بروتوكول بث الفيديو والتواصل الصوتي مأخوذ من بروتوكول الكود المفتوح WebRTC P2P",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}
