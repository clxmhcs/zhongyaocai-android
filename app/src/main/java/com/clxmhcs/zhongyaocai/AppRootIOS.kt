package com.clxmhcs.zhongyaocai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRootIOS(viewModel: MainViewModel) {
    val data by viewModel.data.collectAsState()
    var route by rememberSaveable { mutableStateOf(AppRoute.Home) }
    var parent by rememberSaveable { mutableStateOf(AppRoute.Home) }
    var detail by remember { mutableStateOf<Prescription?>(null) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var expandAll by rememberSaveable { mutableStateOf(true) }
    fun go(target: AppRoute) { parent = route; route = target }

    Scaffold(
        topBar = {
            if (route != AppRoute.Home) CenterAlignedTopAppBar(
                title = { Text(route.title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (route == AppRoute.Manual) TextButton(onClick = { route = parent }) { Text("关闭") }
                    else IconButton(onClick = { route = parent }) { Icon(Icons.Default.ArrowBack, "返回") }
                },
                actions = { if (route == AppRoute.Manual) TextButton(onClick = { expandAll = !expandAll }) { Text(if (expandAll) "全部收起" else "全部展开") } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AppPaleGreen)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(if (route == AppRoute.Home) Color.Transparent else AppPaleGreen)) {
            when (route) {
                AppRoute.Home -> HomeScreen(data, { go(it) }, { value -> searchText = value; go(AppRoute.Search) })
                AppRoute.Search -> InventorySearchScreen(data, searchText)
                AppRoute.InventoryOverview -> InventoryOverviewScreen(data)
                AppRoute.Overview7Days -> RemainingDaysScreen(data, route.title, 0.0, 7.0)
                AppRoute.Overview14Days -> RemainingDaysScreen(data, route.title, 7.0, 14.0)
                AppRoute.LowStock -> LowStockScreen(data)
                AppRoute.Quick, AppRoute.FastInbound, AppRoute.FastOutbound -> QuickInOutScreen(data, viewModel, { go(AppRoute.HistoryDetail) }, { go(AppRoute.InboundHistory) })
                AppRoute.HighValue -> HighValueHerbsScreen(data)
                AppRoute.HerbSettings -> HerbSettingsHubScreen({ go(AppRoute.AddHerb) }, { go(AppRoute.PriceSettings) }, { go(AppRoute.WarningSettings) })
                AppRoute.PriceSettings -> PriceSettingsScreen(data, viewModel)
                AppRoute.WarningSettings -> WarningSettingsScreen(data, viewModel)
                AppRoute.PrescriptionUsage -> IOSPrescriptionUsageScreen(data, viewModel)
                AppRoute.Prescriptions -> PrescriptionHistoryScreen(data, viewModel, { go(AppRoute.PrescriptionOverview) }, { item -> detail = item; go(AppRoute.PrescriptionDetail) })
                AppRoute.PrescriptionOverview -> PrescriptionOverviewScreen(data, viewModel, { go(AppRoute.AddPrescription) }, { item -> detail = item; go(AppRoute.PrescriptionDetail) })
                AppRoute.TotalPrice -> IOSPrescriptionCostCalculatorScreen(data, viewModel)
                AppRoute.Database -> DatabaseScreen(data, viewModel, ::go)
                AppRoute.AddHerb -> AddHerbScreen(data, viewModel) { route = AppRoute.HerbSettings; parent = AppRoute.Home }
                AppRoute.AddPrescription -> AddPrescriptionScreen(data, viewModel) { route = AppRoute.Prescriptions; parent = AppRoute.Home }
                AppRoute.Manual -> ManualScreen(expandAll)
                AppRoute.Profiles -> HerbProfilesScreenIOS(data, viewModel)
                AppRoute.HistoryDetail -> HistoryDetailScreen(data)
                AppRoute.InboundHistory -> InboundHistoryScreen(data)
                AppRoute.PrescriptionDetail -> detail?.let { PrescriptionDetailScreen(it, data, viewModel) } ?: Text("未找到处方")
            }
        }
    }
}
