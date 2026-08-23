package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SecureGreen
import com.example.ui.theme.VibrantPrimary

/**
 * Phase 4: Consolidated design system — reusable atoms extracted from
 * ChatListScreen/ChatDetailScreen for consistency (was duplicated across screens).
 * These are now the source of truth for E2EE visual language, avatars, banners.
 */

@Composable
fun E2EEBanner(
    title: String = "تشفير تام نهاية لنهاية (E2EE)",
    subtitle: String = "رسائل ومكالمات رِسالة مشفرة ومحمية بخصوصية معززة.",
    icon: ImageVector = Icons.Default.Security,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = "اتصال موثق", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, letterSpacing = 0.5.sp)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
fun SecureAvatar(
    initials: String,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    showOnlineDot: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(contentAlignment = Alignment.BottomEnd, modifier = modifier) {
        Box(
            modifier = Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.35).sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (showOnlineDot) {
            Box(
                modifier = Modifier.size((size.value * 0.28).dp).clip(CircleShape).background(SecureGreen)
            )
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(start = 18.dp, bottom = 4.dp, top = 4.dp)
    )
}
