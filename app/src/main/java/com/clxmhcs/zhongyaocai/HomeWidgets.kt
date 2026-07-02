package com.clxmhcs.zhongyaocai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun IOSHomeAlert(names: List<String>, prefix: String, nameColor: Color, onClick: () -> Unit) {
    if (names.isEmpty()) return
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.WarningAmber, null, tint = Color.Gray, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(prefix, fontSize = 13.sp, color = Color.Gray, maxLines = 1)
        Text(names.joinToString("、"), fontSize = 13.sp, color = nameColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@Composable
fun IOSHomeSearchCard(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    headline: String,
    subtitle: String,
    accent: Color,
    onSearch: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = .46f)).padding(horizontal = 5.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier.weight(1f).height(50.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = .48f)).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = Color.Black.copy(alpha = .45f))
            Spacer(Modifier.width(8.dp))
            if (keyword.isEmpty()) {
                Column(Modifier.weight(1f)) {
                    Text(headline, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.Gray.copy(alpha = .72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, fontSize = 12.sp, color = Color.Gray.copy(alpha = .6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                OutlinedTextField(keyword, onKeywordChange, modifier = Modifier.weight(1f), singleLine = true)
            }
            if (keyword.isNotEmpty()) Text("×", fontSize = 22.sp, color = Color.Gray, modifier = Modifier.clickable { onKeywordChange("") })
        }
        Spacer(Modifier.width(8.dp))
        Button(onClick = onSearch, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = accent), modifier = Modifier.height(44.dp)) {
            Icon(Icons.Default.Search, null)
            Text("搜索", modifier = Modifier.padding(start = 3.dp))
        }
    }
}

@Composable
fun IOSFastInOutHomeCard(onDefault: () -> Unit, onInbound: () -> Unit, onOutbound: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = .46f)).clickable(onClick = onDefault).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).background(Brush.linearGradient(listOf(Color(0xFF5BCF8A), Color(0xFF9BE7D0))), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.SwapHoriz, null, tint = Color.White) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("快速 入库/支出", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("快速登记入库或支出", fontSize = 13.sp, color = Color.Gray)
        }
        Text("入库", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onInbound))
        Text("/", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(horizontal = 6.dp))
        Text("支出", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onOutbound))
        Icon(Icons.Default.ArrowForwardIos, null, tint = Color.Gray, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
fun IOSHomeTile(title: String, subtitle: String, icon: ImageVector, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = .55f)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).background(accent.copy(alpha = .18f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun IOSHomeWideTile(title: String, subtitle: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = .55f)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).background(accent.copy(alpha = .18f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Default.ArrowForwardIos, null, tint = Color.Gray)
    }
}
