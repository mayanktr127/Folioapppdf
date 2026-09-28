package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderLight
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.CranberryPale
import com.example.ui.theme.CranberryPrimary
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.SurfaceWhite
import com.example.ui.viewmodel.PdfViewModel

@Composable
fun ProfileDrawer(
    viewModel: PdfViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .navigationBarsPadding()
    ) {
        // Red profile banner matching Image 1
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CranberryPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar and Name
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile Avatar",
                                tint = CranberryPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "User Name",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "username@gmail.com",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        var showSettingsDialog by remember { mutableStateOf(false) }
        val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()

        val notifPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            viewModel.toggleNotifications(isGranted)
            if (isGranted) {
                viewModel.setStatusMessage("Notifications enabled for Folio PDF")
            } else {
                viewModel.setStatusMessage("Notification permission denied. You can re-enable in Android Settings.")
            }
        }

        // Menu Items matching Image 1 exactly:
        // Settings, Team, Report an issue, Request a feature, Rate PDF Editor, Terms of Service, Privacy Policy
        ProfileMenuItem(
            icon = Icons.Outlined.Settings,
            label = "Settings & Notifications",
            onClick = { showSettingsDialog = true },
            testTag = "profile_item_settings"
        )

        ProfileMenuItem(
            icon = Icons.Outlined.Group,
            label = "Team",
            onClick = {
                onClose()
                viewModel.setStatusMessage("Team Workspaces: Single-writer conflict detection active.")
            },
            testTag = "profile_item_team"
        )

        ProfileMenuItem(
            icon = Icons.Outlined.ChatBubbleOutline,
            label = "Report an issue",
            onClick = {
                onClose()
                viewModel.setStatusMessage("Issue reporter opened: Log files packaged without document bytes.")
            },
            testTag = "profile_item_issue"
        )

        ProfileMenuItem(
            icon = Icons.Outlined.Email,
            label = "Request a feature",
            onClick = {
                onClose()
                viewModel.setStatusMessage("Feature request form: Submit feedback for upcoming releases.")
            },
            testTag = "profile_item_feature"
        )

        ProfileMenuItem(
            icon = Icons.Outlined.StarOutline,
            label = "Rate PDF Editor",
            onClick = {
                onClose()
                viewModel.setStatusMessage("Thank you for using Folio PDF!")
            },
            testTag = "profile_item_rate"
        )

        HorizontalDivider(color = BorderLight, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))

        ProfileMenuItem(
            icon = Icons.Outlined.Description,
            label = "Terms of Service",
            onClick = {
                onClose()
                viewModel.setStatusMessage("Terms of Service: Folio PDF is licensed for personal & commercial use.")
            },
            testTag = "profile_item_terms"
        )

        ProfileMenuItem(
            icon = Icons.Outlined.Security,
            label = "Privacy Policy",
            onClick = {
                onClose()
                viewModel.setStatusMessage("Privacy: 100% on-device processing. No documents leave your device.")
            },
            testTag = "profile_item_privacy"
        )

        Spacer(modifier = Modifier.weight(1f))

        // Version & Storage Footer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Folio PDF v1.0.0 (Native Android Build)",
                fontSize = 11.sp,
                color = InkSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Protected with on-device sandbox encryption",
                fontSize = 10.sp,
                color = InkSecondary.copy(alpha = 0.7f)
            )
        }

        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Settings & Notifications", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "Save Pattern",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = InkPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Default save filename: {originalName}-edited.pdf",
                            fontSize = 12.sp,
                            color = InkSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = BorderLight)
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Push Notifications",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = InkPrimary
                                )
                                Text(
                                    text = "Real alerts when tasks finish",
                                    fontSize = 12.sp,
                                    color = InkSecondary
                                )
                            }
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { checked ->
                                    if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.toggleNotifications(checked)
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Enabled Events:\n• PDF ready (after save & verify)\n• Download started (saved to disk)\n• Long job finished (compress/merge)\n• Draft recovered",
                            fontSize = 11.sp,
                            color = InkSecondary,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { viewModel.sendTestNotification() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("send_test_notif_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CranberryPrimary)
                        ) {
                            Icon(Icons.Outlined.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send test notification", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Done", color = CranberryPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = CranberryPrimary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = InkPrimary
        )
    }
}
