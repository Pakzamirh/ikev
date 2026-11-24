package com.sepehr.ikev2.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sepehr.ikev2.data.CountryGroupUi
import com.sepehr.ikev2.data.ServerNodeUi
import com.sepehr.ikev2.ui.MainUiState
import com.sepehr.ikev2.ui.components.AnimatedConnectRing
import com.sepehr.ikev2.ui.components.CountryListShimmer
import com.sepehr.ikev2.ui.theme.*
import com.sepehr.ikev2.vpn.PingResultUi
import com.sepehr.ikev2.vpn.PingStatus
import com.sepehr.ikev2.vpn.VpnConnectionState

@Composable
fun VpnHomeScreen(
    uiState: MainUiState,
    onSearchChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onPingAll: () -> Unit,
    onPingCountry: (CountryGroupUi) -> Unit,
    onPickBest: () -> Unit,
    onSelectNode: (ServerNodeUi) -> Unit,
    onToggleConnection: () -> Unit,
    onLogout: () -> Unit
) {
    var expandedCountryCode by remember { mutableStateOf<String?>(null) }

    val hasReachablePing = remember(uiState.pingResults) {
        uiState.pingResults.values.any { it.status == PingStatus.REACHABLE }
    }

    val filteredCountries = remember(uiState.countries, uiState.searchQuery, uiState.pingResults) {
        val q = uiState.searchQuery.trim()
        val base = if (q.isEmpty()) uiState.countries
        else uiState.countries.filter { group ->
            group.countryNameFa.contains(q, ignoreCase = true) ||
                group.countryCode.contains(q, ignoreCase = true) ||
                group.nodes.any { it.cityLabel.contains(q, ignoreCase = true) }
        }
        base.sortedBy { group ->
            group.nodes.minOfOrNull { node ->
                val p = uiState.pingResults[node.opaqueId]
                if (p?.status == PingStatus.REACHABLE && p.latencyMs != null) p.latencyMs else Int.MAX_VALUE
            } ?: Int.MAX_VALUE
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "سپهر IKEv2",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "کاربر: ${uiState.loggedInUsername}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = onRefresh, enabled = !uiState.isRefreshingServers) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "به‌روزرسانی سرورها", tint = TextPrimary)
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "خروج", tint = TextSecondary)
                }
            }
        }

        // Hero Connection Status Panel (Shows ONLY Country Flag, Country Name & City)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceCard)
                .border(1.dp, BorderHairline, RoundedCornerShape(24.dp))
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedConnectRing(
                state = uiState.vpnState,
                enabled = uiState.selectedNode != null,
                onClick = onToggleConnection
            )

            Spacer(modifier = Modifier.height(14.dp))

            val statusLabel = when (uiState.vpnState) {
                VpnConnectionState.CONNECTED -> "متصل و محافظت‌شده"
                VpnConnectionState.CONNECTING -> "در حال برقراری تونل امن IKEv2..."
                VpnConnectionState.ERROR -> uiState.vpnError ?: "خطا در اتصال"
                VpnConnectionState.DISCONNECTED -> "آماده اتصال"
            }

            Text(
                text = statusLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = when (uiState.vpnState) {
                    VpnConnectionState.CONNECTED -> EmeraldShield
                    VpnConnectionState.ERROR -> CrimsonAlert
                    VpnConnectionState.CONNECTING -> CobaltAccent
                    else -> TextPrimary
                }
            )

            if (uiState.vpnState == VpnConnectionState.CONNECTED) {
                val mins = uiState.connectedSeconds / 60
                val secs = uiState.connectedSeconds % 60
                Text(
                    text = String.format("%02d:%02d", mins, secs),
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            uiState.selectedNode?.let { node ->
                val selectedPing = uiState.pingResults[node.opaqueId]
                val pingSuffix = if (selectedPing?.status == PingStatus.REACHABLE && selectedPing.latencyMs != null) {
                    " · ${selectedPing.latencyMs} ms"
                } else ""
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${node.flagEmoji}  ${node.countryNameFa} · ${node.cityLabel}$pingSuffix",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Real IKEv2 Ping Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onPingAll,
                enabled = !uiState.isPingingAll && uiState.countries.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (uiState.isPingingAll) "در حال تست هندشیک IKEv2..." else "تست پینگ واقعی همه",
                    fontSize = 12.sp
                )
            }

            if (hasReachablePing) {
                Button(
                    onClick = onPickBest,
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldShield)
                ) {
                    Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("بهترین", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Country Search Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("جستجوی کشور...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Country List (Grouped strictly by Flag & Country — zero IP/domain exposure)
        if (uiState.isRefreshingServers && uiState.countries.isEmpty()) {
            CountryListShimmer()
        } else if (uiState.serverLoadError != null && uiState.countries.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(uiState.serverLoadError, color = CrimsonAlert, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onRefresh) { Text("تلاش مجدد") }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredCountries, key = { it.countryCode }) { group ->
                    CountryAccordionCard(
                        group = group,
                        isExpanded = expandedCountryCode == group.countryCode,
                        selectedNodeId = uiState.selectedNode?.opaqueId,
                        pingResults = uiState.pingResults,
                        onToggleExpand = {
                            expandedCountryCode =
                                if (expandedCountryCode == group.countryCode) null else group.countryCode
                        },
                        onPingCountry = {
                            expandedCountryCode = group.countryCode
                            onPingCountry(group)
                        },
                        onSelectNode = onSelectNode
                    )
                }
            }
        }
    }
}

@Composable
private fun CountryAccordionCard(
    group: CountryGroupUi,
    isExpanded: Boolean,
    selectedNodeId: String?,
    pingResults: Map<String, PingResultUi>,
    onToggleExpand: () -> Unit,
    onPingCountry: () -> Unit,
    onSelectNode: (ServerNodeUi) -> Unit
) {
    val hasSelectedNode = group.nodes.any { it.opaqueId == selectedNodeId }
    val bestMs = group.nodes
        .mapNotNull { pingResults[it.opaqueId] }
        .filter { it.status == PingStatus.REACHABLE && it.latencyMs != null }
        .minOfOrNull { it.latencyMs!! }
    val isAnyPinging = group.nodes.any { pingResults[it.opaqueId]?.status == PingStatus.PINGING }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(
                width = 1.dp,
                color = if (hasSelectedNode) CobaltAccent.copy(alpha = 0.7f) else BorderHairline,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = group.flagEmoji, fontSize = 26.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = group.countryNameFa,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    val subtitle = buildString {
                        append("${group.nodes.size} سرور")
                        if (bestMs != null) append(" · ${bestMs} ms")
                        else if (isAnyPinging) append(" · در حال تست...")
                    }
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = if (bestMs != null) EmeraldShield else TextSecondary
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPingCountry) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = "تست پینگ کشور",
                        tint = if (isAnyPinging) CobaltAccent else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                group.nodes.forEach { node ->
                    val isSelected = node.opaqueId == selectedNodeId
                    val ping = pingResults[node.opaqueId]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) CobaltAccent.copy(alpha = 0.16f) else SurfaceElevated)
                            .clickable { onSelectNode(node) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = node.cityLabel,
                            fontSize = 13.sp,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (ping?.status) {
                                PingStatus.PINGING -> Text("تست...", fontSize = 11.sp, color = CobaltAccent)
                                PingStatus.REACHABLE -> Text(
                                    text = "${ping.latencyMs} ms",
                                    fontSize = 11.sp,
                                    color = EmeraldShield
                                )
                                PingStatus.UNREACHABLE -> Text("عدم پاسخ", fontSize = 11.sp, color = CrimsonAlert)
                                else -> Unit
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldShield,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
