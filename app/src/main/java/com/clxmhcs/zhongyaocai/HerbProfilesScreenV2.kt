package com.clxmhcs.zhongyaocai

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** iOS HerbManagementView + HerbDetailView hierarchy. */
@Composable
fun HerbProfilesScreenIOS(data: AppData, viewModel: MainViewModel) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var detail by remember { mutableStateOf<HerbProfile?>(null) }
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
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(herbs, key = { it.id }) { herb ->
                    val profile = data.herbProfiles.firstOrNull { it.name.trim() == herb.name.trim() }
                    HerbProfileIOSGridCell(herb.name, profile != null) {
                        if (profile == null) editor = HerbProfile(name = herb.name) else detail = profile
                    }
                }
            }
        }
    }
    detail?.let { profile -> HerbProfileDetailDialog(profile, onEdit = { editor = profile }, onDismiss = { detail = null }) }
    editor?.let { profile -> HerbProfileEditorDialog(profile, viewModel, onDismiss = { editor = null }) }
}

@Composable
private fun HerbProfileIOSGridCell(name: String, hasProfile: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFFF2F2F7), RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, Modifier.weight(1f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (hasProfile) "录" else "无资料", fontSize = 10.sp, color = if (hasProfile) Color(0xFF34A853) else Color.Gray, modifier = Modifier.background(if (hasProfile) Color(0x1F34A853) else Color(0x1F808080), RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
        Icon(Icons.Default.ArrowForwardIos, null, tint = Color.Gray, modifier = Modifier.padding(start = 5.dp))
    }
}

@Composable
private fun HerbProfileDetailDialog(profile: HerbProfile, onEdit: () -> Unit, onDismiss: () -> Unit) {
    val nature = extractNature(profile.natureFlavor)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(profile.name, color = Color(0xFF007AFF), fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); TextButton(onClick = onEdit) { Icon(Icons.Default.Edit, "编辑"); Text("编辑") } } },
        text = {
            Box {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { DetailCard("入药部位", profile.medicinalPart) }
                    item {
                        Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(11.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            BasicProfileRow("中文名", profile.name)
                            BasicProfileRow("别名", profile.alias)
                            BasicProfileRow("性味", profile.natureFlavor)
                            BasicProfileRow("归经", profile.meridian)
                        }
                    }
                    if (profile.efficacy.isNotBlank()) item { DetailCard("功效", profile.efficacy) }
                    if (profile.indications.isNotBlank()) item { DetailCard("主治", profile.indications) }
                    if (profile.usageDosage.isNotBlank()) item { DetailCard("用法用量", profile.usageDosage) }
                    if (profile.contraindication.isNotBlank()) item { DetailCard("注意事项、禁忌", profile.contraindication) }
                    if (profile.compatibility.isNotBlank()) item { DetailCard("临床应用", profile.compatibility) }
                }
                if (nature.isNotBlank()) {
                    Box(Modifier.align(Alignment.TopEnd).size(76.dp).background(Color.Transparent, CircleShape).rotate(-45f), contentAlignment = Alignment.Center) {
                        Text(nature, color = Color(0x384CAF50), fontSize = 28.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))) { Text("关闭") } }
    )
}

@Composable
private fun DetailCard(title: String, text: String) {
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)).padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(text.ifBlank { "暂无资料" }, fontSize = 16.sp, color = if (text.isBlank()) Color.Gray else Color.Unspecified)
    }
}

@Composable
private fun BasicProfileRow(label: String, text: String) {
    if (text.isNotBlank()) Row(Modifier.fillMaxWidth()) { Text(label, Modifier.weight(.34f), fontSize = 17.sp, fontWeight = FontWeight.Medium); Text(text, Modifier.weight(.66f), fontSize = 17.sp, color = Color.Gray) }
}

@Composable
private fun HerbProfileEditorDialog(initial: HerbProfile, viewModel: MainViewModel, onDismiss: () -> Unit) {
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
        text = { LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item { OutlinedTextField(name, { name = it }, label = { Text("药材名（必填）") }, modifier = Modifier.fillMaxWidth()) }
            item { HerbField("别名", alias) { alias = it } }; item { HerbField("药用部位", part) { part = it } }; item { HerbField("性味", flavor) { flavor = it } }; item { HerbField("归经", meridian) { meridian = it } }; item { HerbField("功效", efficacy) { efficacy = it } }; item { HerbField("主治", indications) { indications = it } }; item { HerbField("配伍", compatibility) { compatibility = it } }; item { HerbField("用法用量", dosage) { dosage = it } }; item { HerbField("禁忌", contraindication) { contraindication = it } }
            error?.let { item { Text(it, color = Color.Red) } }
        } },
        confirmButton = { TextButton(onClick = { if (name.trim().isBlank()) error = "药材名不能为空" else { viewModel.saveProfile(initial.copy(name = name.trim(), alias = alias.trim(), medicinalPart = part.trim(), natureFlavor = flavor.trim(), meridian = meridian.trim(), efficacy = efficacy.trim(), indications = indications.trim(), compatibility = compatibility.trim(), usageDosage = dosage.trim(), contraindication = contraindication.trim(), updatedAt = System.currentTimeMillis())); onDismiss() } }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun HerbField(label: String, value: String, change: (String) -> Unit) { OutlinedTextField(value, change, label = { Text(label) }, modifier = Modifier.fillMaxWidth()) }

private fun extractNature(raw: String): String {
    val base = raw.substringBefore("（").substringBefore("(").trim()
    val index = base.indexOf("性")
    if (index < 0) return ""
    return base.substring(index + 1).split('，', ',', '、', '；', ';', '。', '.').firstOrNull()?.trim().orEmpty()
}
