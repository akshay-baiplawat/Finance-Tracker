package com.example.financetracker.presentation.group.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financetracker.data.local.entity.GroupType
import com.example.financetracker.presentation.model.GroupUiModel
import com.example.financetracker.presentation.theme.FinosMint

@Composable
fun GroupCard(
    group: GroupUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Use MaterialTheme colors which correctly follow the app's theme setting
    val cardBackground = MaterialTheme.colorScheme.surface
    val borderColor = Color(android.graphics.Color.parseColor(group.colorHex)).copy(alpha = 0.2f)

    // Determine if background is dark to set appropriate text colors
    // Check if the surface color is dark (luminance < 0.5)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkBackground = surfaceColor.red * 0.299f + surfaceColor.green * 0.587f + surfaceColor.blue * 0.114f < 0.5f

    // Text colors based on actual card background brightness
    val primaryTextColor = if (isDarkBackground) Color.White else Color(0xFF1E293B)
    val secondaryTextColor = if (isDarkBackground) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)
    val tertiaryTextColor = if (isDarkBackground) Color.White.copy(alpha = 0.6f) else Color(0xFF94A3B8)
    val dividerColor = if (isDarkBackground) Color.White.copy(alpha = 0.1f) else Color(0xFF1E293B).copy(alpha = 0.1f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row: Icon, Name, Type badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Group icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(group.colorHex)).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getGroupTypeIcon(group.type),
                        contentDescription = "Group type: ${getGroupTypeDisplayName(group.type)}",
                        tint = Color(android.graphics.Color.parseColor(group.colorHex)),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name and member count
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.People,
                            contentDescription = "Members",
                            modifier = Modifier.size(14.dp),
                            tint = tertiaryTextColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${group.memberCount} member${if (group.memberCount > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = tertiaryTextColor
                        )
                    }
                }

                // Type badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(android.graphics.Color.parseColor(group.colorHex)).copy(alpha = 0.1f)
                ) {
                    Text(
                        text = getGroupTypeDisplayName(group.type),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(android.graphics.Color.parseColor(group.colorHex)),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar (for trips/savings)
            if (group.budgetLimitFormatted != null || group.targetAmountFormatted != null) {
                val progressLabel = when (group.type) {
                    GroupType.SAVINGS_GOAL.name -> "Saved"
                    else -> "Spent"
                }
                val progressValue = when (group.type) {
                    GroupType.SAVINGS_GOAL.name -> group.totalContributedFormatted ?: "₹0"
                    else -> group.totalSpentFormatted
                }
                val targetLabel = when (group.type) {
                    GroupType.SAVINGS_GOAL.name -> "Goal"
                    else -> "Budget"
                }
                val targetValue = when (group.type) {
                    GroupType.SAVINGS_GOAL.name -> group.targetAmountFormatted
                    else -> group.budgetLimitFormatted
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$progressLabel: $progressValue",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryTextColor
                        )
                        Text(
                            text = "$targetLabel: $targetValue",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryTextColor
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress indicator
                    LinearProgressIndicator(
                        progress = { group.progressPercentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (group.progressPercentage > 1f && group.type != GroupType.SAVINGS_GOAL.name) {
                            Color(0xFFEF4444) // Over budget
                        } else {
                            Color(android.graphics.Color.parseColor(group.colorHex))
                        },
                        trackColor = dividerColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Progress percentage
                    Text(
                        text = "${(group.progressPercentage * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (group.progressPercentage > 1f && group.type != GroupType.SAVINGS_GOAL.name) {
                            Color(0xFFEF4444)
                        } else {
                            Color(android.graphics.Color.parseColor(group.colorHex))
                        },
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            } else {
                // For split expense groups without budget, show total spent
                // For GENERAL groups, show net total (income - expense)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (group.type == GroupType.GENERAL.name) "Net Total" else "Total expenses",
                        style = MaterialTheme.typography.bodySmall,
                        color = tertiaryTextColor
                    )
                    Text(
                        text = group.totalSpentFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor
                    )
                }
            }
        }
    }
}

@Composable
private fun getGroupTypeIcon(type: String) = when (type) {
    GroupType.TRIP.name -> Icons.Rounded.Category
    GroupType.SPLIT_EXPENSE.name -> Icons.Rounded.Receipt
    GroupType.SAVINGS_GOAL.name -> Icons.Rounded.Savings
    else -> Icons.Rounded.Group
}

private fun getGroupTypeDisplayName(type: String) = when (type) {
    GroupType.TRIP.name -> "Group By Category"
    GroupType.SPLIT_EXPENSE.name -> "Split Bill"
    GroupType.SAVINGS_GOAL.name -> "Savings"
    GroupType.GENERAL.name -> "General"
    else -> type
}
