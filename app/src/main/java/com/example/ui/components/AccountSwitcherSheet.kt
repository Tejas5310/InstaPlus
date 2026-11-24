package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.InstaPlusAuthSession
import com.example.ui.theme.InstagramStoryBrush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitcherSheet(
    instaPlusAccounts: List<InstaPlusAuthSession>,
    activeInstaPlusUid: String,
    onSwitchInstaPlusAccount: (InstaPlusAuthSession) -> Unit,
    onAddInstaPlusAccount: (createNew: Boolean) -> Unit,
    onRemoveInstaPlusAccount: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.instaplus_app_icon),
                    contentDescription = "InstaPlus",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Column {
                    Text(
                        text = "InstaPlus Accounts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Switch between your verified standalone InstaPlus accounts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            instaPlusAccounts.forEach { acc ->
                val isActive = acc.uid == activeInstaPlusUid
                Surface(
                    color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .then(
                            if (isActive) Modifier.border(
                                1.5.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(14.dp)
                            ) else Modifier
                        )
                        .clickable { onSwitchInstaPlusAccount(acc) }
                        .testTag("instaplus_account_row_${acc.username}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .then(
                                    if (isActive) Modifier.border(2.dp, InstagramStoryBrush, CircleShape)
                                    else Modifier.background(MaterialTheme.colorScheme.background)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = acc.username,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = acc.fullName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "@${acc.username} • ${acc.email}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isActive) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Active InstaPlus account",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            IconButton(
                                onClick = { onRemoveInstaPlusAccount(acc.uid) },
                                modifier = Modifier.testTag("remove_instaplus_account_${acc.username}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Remove InstaPlus account",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onAddInstaPlusAccount(false) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("switch_login_another_account_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log In Another")
                }
                OutlinedButton(
                    onClick = { onAddInstaPlusAccount(true) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("switch_create_another_account_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create Account")
                }
            }
        }
    }
}
