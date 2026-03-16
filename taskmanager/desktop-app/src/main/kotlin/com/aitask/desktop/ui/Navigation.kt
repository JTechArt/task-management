package com.aitask.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class NavigationItem {
    DASHBOARD,
    PROJECTS,
    TASKS,
    ACTIVITY,
    RULES,
    INTEGRATIONS,
    SETTINGS
}

@Composable
fun NavigationSidebar(
    selectedItem: NavigationItem,
    onItemSelected: (NavigationItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(250.dp)
            .background(
                color = Color(0xFF1F2C3F)
            )
            .padding(18.dp)
    ) {
        // Brand section
        Row(
            modifier = Modifier.padding(bottom = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Brand mark
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        color = Color(0xFF1976D2)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AI",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Brand copy
            Column {
                Text(
                    text = "AiTask",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Developer control center",
                    color = Color(0xFFE2E8F0).copy(alpha = 0.72f),
                    fontSize = 12.sp
                )
            }
        }
        
        // Navigation label
        Text(
            text = "PRIMARY",
            color = Color(0xFFBFCCDC).copy(alpha = 0.74f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 10.dp, bottom = 10.dp)
        )
        
        // Navigation items
        NavigationItem.entries.forEach { item ->
            NavItem(
                label = item.name.lowercase().replaceFirstChar { it.uppercase() },
                isSelected = item == selectedItem,
                onClick = { onItemSelected(item) }
            )
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        Color(0xFF1976D2).copy(alpha = 0.82f)
    } else {
        Color.Transparent
    }
    
    val textColor = if (isSelected) {
        Color.White
    } else {
        Color(0xFFF1F5F9).copy(alpha = 0.88f)
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 14.sp
        )
    }
}

