package com.clxmhcs.zhongyaocai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
    Home("中药材管理"), Herbs("药材库"), Quick("快速入库 / 支出"), Prescriptions("处方总览"), Database("数据库管理"),
    AddHerb("新增药材"), LowStock("低库存药材"), AddPrescription("保存处方"), Manual("APP说明书"), Profiles("药材资料录入")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(viewModel: MainViewModel) {
    val data by viewModel.data.collectAsState()
    var route by rememberSaveable { mutableStateOf(AppRoute.Home) }
    val isMain = route in setOf(AppRoute.Home, AppRoute.Herbs, AppRoute.Quick, AppRoute.Prescriptions, AppRoute.Database)
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(route.title, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    if (!isMain) IconButton(onClick = {
                        route = when (route) {
                            AppRoute.AddHerb, AppRoute.LowStock -> AppRoute.Herbs
                            AppRoute.AddPrescription -> AppRoute.Prescriptions
                            else -> AppRoute.Database
                        }
                    }) { Icon(Icons.Default.ArrowBack, "返回") }
                },
                actions = { if (route != AppRoute.Manual) IconButton(onClick = { route = AppRoute.Manual }) { Icon(Icons.Default.Article, "说明书") } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AppPaleGreen)
            )
        },
        bottomBar = {
            if (isMain) NavigationBar {
                AppNavItem(route, AppRoute.Home, Icons.Default.Home, "首页") { route = AppRoute.Home }
                AppNavItem(route, AppRoute.Herbs, Icons.Default.Inventory2, "药材") { route = AppRoute.Herbs }
                AppNavItem(route, AppRoute.Quick, Icons.Default.ReceiptLong, "出入库") { route = AppRoute.Quick }
                AppNavItem(route, AppRoute.Prescriptions, Icons.Default.Article, "处方") { route = AppRoute.Prescriptions }
                AppNavItem(route, AppRoute.Database, Icons.Default.Settings, "管理") { route = AppRoute.Database }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(AppPaleGreen)) {
            when (route) {
                AppRoute.Home -> HomeScreen(data) { route = it }
                AppRoute.Herbs -> HerbListScreen(data, viewModel, onAdd = { route = AppRoute.AddHerb }, onLowStock = { route = AppRoute.LowStock })
                AppRoute.Quick -> QuickInOutScreen(data, viewModel)
                AppRoute.Prescriptions -> PrescriptionOverviewScreen(data, viewModel, onAdd = { route = AppRoute.AddPrescription })
                AppRoute.Database -> DatabaseScreen(data, viewModel, onRoute = { route = it })
                AppRoute.AddHerb -> AddHerbScreen(data, viewModel, onDone = { route = AppRoute.Herbs })
                AppRoute.LowStock -> LowStockScreen(data)
                AppRoute.AddPrescription -> AddPrescriptionScreen(data, viewModel, onDone = { route = AppRoute.Prescriptions })
                AppRoute.Manual -> ManualScreen()
                AppRoute.Profiles -> HerbProfilesScreen(data, viewModel)
            }
        }
    }
}

@Composable
private fun AppNavItem(current: AppRoute, target: AppRoute, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    NavigationBarItem(selected = current == target, onClick = onClick, icon = { Icon(icon, label) }, label = { Text(label) })
}
