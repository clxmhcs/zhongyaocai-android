package com.clxmhcs.zhongyaocai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Current iOS HerbManagementView: searchable two-column grid based on the herb master list. */
@Composable
fun HerbProfilesScreen(data: AppData, viewModel: MainViewModel) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var editor by remember { mutableStateOf<HerbProfile?>(null) }
    val herbs = data.herbs.filter { it.name.contains(searchText.trim()) }.sortedWith(compareBy<Herb> { it.pinyin }.thenBy { it.name })
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(searchText, { searchText = it }, label = { Text("搜索药材名") }, singleLine = true, modifier = Modifier.weight(1f))
            IconButton(onClick = { editor = HerbProfile(name = "") }) { Icon(Icons.Default.Add, "新增资料") }
        }
        if (data.herbs.isEmpty()) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("暂无药材", fontWeight = FontWeight.SemiBold)
                Text("当前药材总库为空", color = Color.Gray, modifier = Modifier.padding(top = 6.dp))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(herbs, key = { it.id }) { herb ->
                    val record = data.herbProfiles.firstOrNull { it.name.trim() == herb.name.trim() }
                    HerbProfileGridCell(herb.name, record != null, onClick = { editor = record ?: HerbProfile(name = herb.name) })
                }
            }
        }
    }
    editor?.let { ProfileEditorDialog(it, viewModel, onDismiss = { editor = null }) }
}

@Composable
private fun HerbProfileGridCell(name: String, hasRecord: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(Color(0xFFF2F2F7), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name.ifBlank { "未命名药材" }, modifier = Modifier.weight(1f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            if (hasRecord) "录" else "无资料",
            fontSize = 10.sp,
            color = if (hasRecord) Color(0xFF34A853) else Color.Gray,
            modifier = Modifier.background(if (hasRecord) Color(0x1F34A853) else Color(0x1F808080), RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
        )
        Icon(Icons.Default.ArrowForwardIos, null, tint = Color.Gray, modifier = Modifier.padding(start = 5.dp))
    }
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
    var deleteConfirm by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.name.isBlank()) "新增药材资料" else "编辑药材资料") },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
        confirmButton = { TextButton(onClick = {
            if (name.trim().isEmpty()) error = "药材名不能为空" else {
                viewModel.saveProfile(initial.copy(name = name.trim(), alias = alias.trim(), medicinalPart = part.trim(), natureFlavor = flavor.trim(), meridian = meridian.trim(), efficacy = efficacy.trim(), indications = indications.trim(), compatibility = compatibility.trim(), usageDosage = dosage.trim(), contraindication = contraindication.trim(), updatedAt = System.currentTimeMillis()))
                onDismiss()
            }
        }) { Text("保存") } },
        dismissButton = { Row { if (initial.name.isNotBlank()) TextButton(onClick = { deleteConfirm = true }) { Text("删除资料", color = Color.Red) }; TextButton(onClick = onDismiss) { Text("取消") } } }
    )
    if (deleteConfirm) AlertDialog(
        onDismissRequest = { deleteConfirm = false },
        title = { Text("确认删除") },
        text = { Text("当前操作会删除此药材已录入的详细资料，但不会从药材总库中移除，确认删除吗？") },
        confirmButton = { TextButton(onClick = { viewModel.deleteProfile(initial.id); onDismiss() }) { Text("删除", color = Color.Red) } },
        dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text("取消") } }
    )
}

@Composable
private fun ProfileField(label: String, value: String, change: (String) -> Unit) {
    OutlinedTextField(value, change, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}
