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

val AppPurple = Color(0xFF7E57C2)
val AppCyan = Color(0xFF21A7C4)
val AppOrange = Color(0xFFFF9D3D)
val AppPaleGreen = Color(0xFFF2FAEF)

enum class AppRoute(val title: String) {
    Home(""), Search("快速查询"), InventoryOverview("药材余量总览"), Overview7Days("余量不足7天药材"), Overview14Days("余量不足14天药材"), LowStock("低库存药材"), Quick("快速入库 / 支出"), FastInbound("快速入库 / 支出"), FastOutbound("快速入库 / 支出"), HighValue("高价值药材"), HerbSettings("药材信息设置"), PrescriptionUsage("处方用量可用天数测算"), Prescriptions("处方历史记录"), PrescriptionOverview("处方总览"), TotalPrice("药方总价计算"), Database("数据库管理"), AddHerb("新增药材"), AddPrescription("保存处方"), Manual("APP说明书"), Profiles("药材资料录入"), HistoryDetail("历史记录明细"), InboundHistory("入库明细"), PrescriptionDetail("处方详情")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(viewModel: MainViewModel) {
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
                AppRoute.HerbSettings -> HerbListScreen(data, viewModel, { go(AppRoute.AddHerb) }, { go(AppRoute.LowStock) })
                AppRoute.PrescriptionUsage -> PrescriptionUsageScreen(data)
                AppRoute.Prescriptions -> PrescriptionHistoryScreen(data, viewModel, { go(AppRoute.PrescriptionOverview) }, { item -> detail = item; go(AppRoute.PrescriptionDetail) })
                AppRoute.PrescriptionOverview -> PrescriptionOverviewScreen(data, viewModel, { go(AppRoute.AddPrescription) }, { item -> detail = item; go(AppRoute.PrescriptionDetail) })
                AppRoute.TotalPrice -> PrescriptionCostCalculatorScreen(data)
                AppRoute.Database -> DatabaseScreen(data, viewModel, ::go)
                AppRoute.AddHerb -> AddHerbScreen(data, viewModel) { route = AppRoute.HerbSettings }
                AppRoute.AddPrescription -> AddPrescriptionScreen(data, viewModel) { route = AppRoute.Prescriptions }
                AppRoute.Manual -> ManualScreen(expandAll)
                AppRoute.Profiles -> HerbProfilesScreen(data, viewModel)
                AppRoute.HistoryDetail -> HistoryDetailScreen(data)
                AppRoute.InboundHistory -> InboundHistoryScreen(data)
                AppRoute.PrescriptionDetail -> detail?.let { PrescriptionDetailScreen(it, data, viewModel) } ?: Text("未找到处方")
            }
        }
    }
}
