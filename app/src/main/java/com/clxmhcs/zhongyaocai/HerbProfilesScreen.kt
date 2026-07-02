package com.clxmhcs.zhongyaocai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HerbProfilesScreen(data: AppData, viewModel: MainViewModel) {
    var editor by remember { mutableStateOf<HerbProfile?>(null) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("药材资料录入", fontWeight = FontWeight.Bold); Text("资料独立保存，可导出 JSON；不会改动库存、处方和出入库记录。", color = Color.Gray, fontSize = 12.sp) }
            if (data.herbProfiles.isEmpty()) item { EmptyHint("暂无药材资料。点击右下角按钮新增。") }
            items(data.herbProfiles.sortedBy { it.name }, key = { it.id }) { profile ->
                PageCard(Modifier.fillMaxWidth().clickable { editor = profile }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(profile.name, fontWeight = FontWeight.SemiBold, fontSize = 17.sp); if (profile.alias.isNotBlank()) Text("别名：${profile.alias}", color = Color.Gray); if (profile.efficacy.isNotBlank()) Text(profile.efficacy, maxLines = 1, color = Color.Gray, fontSize = 12.sp) }
                        Box(Modifier.size(42.dp).rotate(-45f), contentAlignment = Alignment.Center) { Text("药性", color = Color(0xFF429B56), fontSize = 12.sp) }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { editor = HerbProfile(name = "") }, containerColor = AppPurple, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) { Icon(Icons.Default.Add, "新增资料", tint = Color.White) }
    }
    editor?.let { ProfileEditorDialog(it, viewModel, onDismiss = { editor = null }) }
}

@Composable
private fun ProfileEditorDialog(initial: HerbProfile, viewModel: MainViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var alias by remember { mutableStateOf(initial.alias) }
    var part by remember { mutableStateOf(initial.medicinalPart) }
    var flavor by remember { mutableStateOf(initial.natureFlavor) }
    var meridian by remember { mutableStateOf(initial.meridian) }
    var efficacy by remember { mutableStateOf(initial.efficacy) }
    var indications by remember { mutableStateOf(initial.indications) }
    var compatibility by remember { mutableStateOf(initial.compatibility) }
    var dosage by remember { mutableStateOf(initial.usageDosage) }
    var contraindication by remember { mutableStateOf(initial.contraindication) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.name.isBlank()) "新增药材资料" else "编辑药材资料") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item { OutlinedTextField(name, { name = it }, label = { Text("药材名（必填）") }, modifier = Modifier.fillMaxWidth()) }
                item { ProfileField("别名", alias) { alias = it } }
                item { ProfileField("药用部位", part) { part = it } }
                item { ProfileField("性味", flavor) { flavor = it } }
                item { ProfileField("归经", meridian) { meridian = it } }
                item { ProfileField("功效", efficacy) { efficacy = it } }
                item { ProfileField("主治", indications) { indications = it } }
                item { ProfileField("配伍", compatibility) { compatibility = it } }
                item { ProfileField("用法用量", dosage) { dosage = it } }
                item { ProfileField("禁忌", contraindication) { contraindication = it } }
                error?.let { item { Text(it, color = Color.Red) } }
            }
        },
        confirmButton = { TextButton(onClick = { if (name.trim().isEmpty()) error = "药材名不能为空" else { viewModel.saveProfile(initial.copy(name = name.trim(), alias = alias.trim(), medicinalPart = part.trim(), natureFlavor = flavor.trim(), meridian = meridian.trim(), efficacy = efficacy.trim(), indications = indications.trim(), compatibility = compatibility.trim(), usageDosage = dosage.trim(), contraindication = contraindication.trim(), updatedAt = System.currentTimeMillis())); onDismiss() } }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ProfileField(label: String, value: String, change: (String) -> Unit) { OutlinedTextField(value, change, label = { Text(label) }, modifier = Modifier.fillMaxWidth()) }
