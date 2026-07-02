package com.clxmhcs.zhongyaocai

import android.content.ContentResolver
import android.net.Uri
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 不依赖服务器或桌面库的最小 XLSX 读写器。
 * 导出文件为标准 OpenXML .xlsx，可由 Excel、WPS 和 Numbers 打开。
 */
object XlsxCodec {
    data class Sheet(
        val headers: List<String>,
        val rows: List<List<String>>,
        val redRows: Set<Int> = emptySet(),
        val widths: List<Int> = emptyList()
    )

    suspend fun write(resolver: ContentResolver, uri: Uri, sheet: Sheet) = withContext(Dispatchers.IO) {
        resolver.openOutputStream(uri)?.use { output -> writeWorkbook(output, sheet) }
            ?: error("无法写入所选文件")
    }

    fun herbSheet(herbs: List<Herb>): Sheet {
        val sorted = herbs.sortedWith(compareBy<Herb> { it.pinyin }.thenBy { it.name })
        val rows = sorted.map { herb ->
            listOf(herb.name, herb.stock.toString(), herb.warningLevel.toString(), herb.dailyUsage.toString(), compactDouble(herb.pricePerKg))
        }
        val headers = listOf("药材名", "库存(g)", "预警值(g)", "日用量(g)", "价格(元/kg)")
        return Sheet(headers, rows, sorted.mapIndexedNotNull { index, herb -> if (herb.stock <= herb.warningLevel) index else null }.toSet())
    }

    fun prescriptionSheet(prescriptions: List<Prescription>): Sheet {
        val sorted = prescriptions.sortedBy { it.timestamp }
        val names = sorted.flatMap { it.items.map(PrescriptionItem::herbName) }.distinct().sorted()
        val headers = listOf("药材名") + sorted.map { it.dateString }
        val rows = names.map { name ->
            listOf(name) + sorted.map { p -> p.items.firstOrNull { it.herbName == name }?.grams?.let(::compactDouble) ?: "" }
        }.toMutableList()
        rows += List(headers.size) { "" }
        rows += listOf("水泡是否减轻") + sorted.map { p -> when (p.blisterReduced) { true -> "是"; false -> "否"; null -> "" } }
        return Sheet(headers, rows)
    }

    fun profileJson(profiles: List<HerbProfile>): String = kotlinx.serialization.json.Json { prettyPrint = true; encodeDefaults = true }
        .encodeToString(kotlinx.serialization.builtins.ListSerializer(HerbProfile.serializer()), profiles)

    suspend fun readHerbs(resolver: ContentResolver, uri: Uri): List<Herb> = withContext(Dispatchers.IO) {
        val bytes = resolver.openInputStream(uri)?.use(InputStream::readBytes) ?: error("无法读取所选文件")
        val rows = readXlsxRows(bytes)
        parseHerbRows(rows)
    }

    private fun parseHerbRows(rows: List<List<String>>): List<Herb> {
        if (rows.isEmpty()) error("表格为空")
        var headerIndex = -1
        var nameCol = 0
        var stockCol = 1
        var warnCol = 2
        var dailyCol: Int? = null
        var priceCol = 3
        for (i in 0 until minOf(20, rows.size)) {
            val row = rows[i]
            fun locate(keys: List<String>): Int? = row.indexOfFirst { cell -> keys.any { key -> cell.contains(key, ignoreCase = true) } }.takeIf { it >= 0 }
            val n = locate(listOf("药材名", "药材", "名称", "品名"))
            val s = locate(listOf("库存", "余量"))
            val w = locate(listOf("预警", "警戒"))
            val d = locate(listOf("日用量", "每日用量"))
            val p = locate(listOf("价格", "单价", "元/kg", "元/千克"))
            if (n != null && s != null && w != null && p != null) {
                headerIndex = i
                nameCol = n; stockCol = s; warnCol = w; dailyCol = d; priceCol = p
                break
            }
        }
        val start = if (headerIndex >= 0) headerIndex + 1 else 0
        val usedNames = mutableSetOf<String>()
        val imported = mutableListOf<Herb>()
        val problems = mutableListOf<String>()
        for (index in start until rows.size) {
            val row = rows[index]
            fun cell(col: Int): String = row.getOrNull(col)?.trim().orEmpty()
            val name = cell(nameCol)
            val stockText = cell(stockCol)
            val warnText = cell(warnCol)
            val dailyText = dailyCol?.let(::cell).orEmpty()
            val priceText = cell(priceCol)
            if (name.isEmpty() && stockText.isEmpty() && warnText.isEmpty() && dailyText.isEmpty() && priceText.isEmpty()) continue
            val rowNumber = index + 1
            if (name.isBlank()) { problems += "第 ${rowNumber} 行：药材名为空"; continue }
            if (!usedNames.add(name)) { problems += "第 ${rowNumber} 行【$name】：药材名重复"; continue }
            val stock = numberToInt(stockText)
            val warn = numberToInt(warnText)
            val daily = if (dailyCol == null || dailyText.isBlank()) 0 else numberToInt(dailyText)
            val price = numberToDouble(priceText)
            if (stock == null) problems += "第 ${rowNumber} 行【$name】：库存(g) 为空或不是整数（当前：$stockText）"
            if (warn == null) problems += "第 ${rowNumber} 行【$name】：预警值(g) 为空或不是整数（当前：$warnText）"
            if (daily == null) problems += "第 ${rowNumber} 行【$name】：日用量(g) 为空或不是整数（当前：$dailyText）"
            if (price == null) problems += "第 ${rowNumber} 行【$name】：价格(元/kg) 为空或不是数字（当前：$priceText）"
            if ((stock ?: 0) < 0 || (warn ?: 0) < 0 || (daily ?: 0) < 0 || (price ?: 0.0) < 0) problems += "第 ${rowNumber} 行【$name】：数值不能为负数"
            if (stock != null && warn != null && daily != null && price != null && stock >= 0 && warn >= 0 && daily >= 0 && price >= 0) {
                imported += Herb(name = name, stock = stock, warningLevel = warn, dailyUsage = daily, pricePerKg = price, pinyin = makePinyinKey(name))
            }
        }
        if (problems.isNotEmpty()) error("导入被拒绝：\n" + problems.take(20).joinToString("\n") + if (problems.size > 20) "\n...（共 ${problems.size} 条错误）" else "")
        if (imported.isEmpty()) error("表格为空")
        return imported
    }

    private fun writeWorkbook(output: OutputStream, sheet: Sheet) {
        ZipOutputStream(output).use { zip ->
            fun entry(path: String, text: String) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(text.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            entry("[Content_Types].xml", """<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/></Types>""")
            entry("_rels/.rels", """<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>""")
            entry("xl/workbook.xml", """<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"药材数据\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>""")
            entry("xl/_rels/workbook.xml.rels", """<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/><Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>""")
            entry("xl/styles.xml", stylesXml())
            entry("xl/worksheets/sheet1.xml", worksheetXml(sheet))
        }
    }

    private fun worksheetXml(sheet: Sheet): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><cols>")
        val allRows = listOf(sheet.headers) + sheet.rows
        val widths = sheet.headers.indices.map { i -> sheet.widths.getOrNull(i) ?: ((allRows.maxOfOrNull { it.getOrNull(i)?.length ?: 0 } ?: 8) + 2).coerceIn(8, 42) }
        widths.forEachIndexed { i, width -> append("<col min=\"${i + 1}\" max=\"${i + 1}\" width=\"$width\" customWidth=\"1\"/>") }
        append("</cols><sheetData>")
        fun row(index: Int, values: List<String>, style: Int) {
            append("<row r=\"$index\">")
            values.forEachIndexed { col, value ->
                val ref = columnName(col + 1) + index
                append("<c r=\"$ref\" t=\"inlineStr\" s=\"$style\"><is><t>${escape(value)}</t></is></c>")
            }
            append("</row>")
        }
        row(1, sheet.headers, 1)
        sheet.rows.forEachIndexed { index, values -> row(index + 2, values, if (index in sheet.redRows) 2 else 0) }
        append("</sheetData></worksheet>")
    }

    private fun stylesXml(): String = """<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font><font><sz val=\"11\"/><color rgb=\"FFFF0000\"/><name val=\"Calibri\"/></font></fonts><fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFD9EAF7\"/><bgColor indexed=\"64\"/></patternFill></fill></fills><borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs><cellXfs count=\"3\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/><xf numFmtId=\"0\" fontId=\"0\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFill=\"1\"/><xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/></cellXfs></styleSheet>"""

    private fun readXlsxRows(bytes: ByteArray): List<List<String>> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                if (!e.isDirectory) entries[e.name] = zip.readBytes()
                zip.closeEntry()
                e = zip.nextEntry
            }
        }
        val shared = entries["xl/sharedStrings.xml"]?.let(::parseSharedStrings).orEmpty()
        val sheetName = entries.keys.firstOrNull { it.startsWith("xl/worksheets/") && it.endsWith(".xml") } ?: error("无法读取工作表")
        return parseSheet(entries.getValue(sheetName), shared)
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val parser = Xml.newPullParser().apply { setInput(ByteArrayInputStream(bytes), "UTF-8") }
        val list = mutableListOf<String>()
        var inSi = false
        var text = ""
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> if (parser.name == "si") { inSi = true; text = "" }
                XmlPullParser.TEXT -> if (inSi) text += parser.text
                XmlPullParser.END_TAG -> if (parser.name == "si") { list += text; inSi = false }
            }
            parser.next()
        }
        return list
    }

    private fun parseSheet(bytes: ByteArray, shared: List<String>): List<List<String>> {
        val parser = Xml.newPullParser().apply { setInput(ByteArrayInputStream(bytes), "UTF-8") }
        val output = mutableListOf<MutableMap<Int, String>>()
        var row: MutableMap<Int, String>? = null
        var column = 0
        var type = ""
        var currentText = ""
        var inCell = false
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> row = mutableMapOf()
                    "c" -> {
                        inCell = true
                        val ref = parser.getAttributeValue(null, "r").orEmpty()
                        column = columnIndex(ref)
                        type = parser.getAttributeValue(null, "t").orEmpty()
                        currentText = ""
                    }
                }
                XmlPullParser.TEXT -> if (inCell) currentText += parser.text
                XmlPullParser.END_TAG -> when (parser.name) {
                    "c" -> {
                        val value = if (type == "s") shared.getOrNull(currentText.trim().toIntOrNull() ?: -1).orEmpty() else currentText
                        row?.put(column, value)
                        inCell = false
                    }
                    "row" -> row?.let(output::add)
                }
            }
            parser.next()
        }
        return output.map { map ->
            val last = map.keys.maxOrNull() ?: -1
            List(last + 1) { i -> map[i].orEmpty() }
        }
    }

    private fun numberToInt(value: String): Int? {
        val clean = value.trim().replace(",", "").replace("，", "").replace("g", "", true).replace("克", "").trim()
        return clean.toIntOrNull() ?: clean.toDoubleOrNull()?.takeIf { it % 1.0 == 0.0 }?.toInt()
    }
    private fun numberToDouble(value: String): Double? = value.trim().replace(",", "").replace("，", "").replace("g", "", true).replace("克", "").trim().toDoubleOrNull()
    private fun compactDouble(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.4f".format(Locale.US, value).trimEnd('0').trimEnd('.')
    private fun escape(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    private fun columnName(index: Int): String { var n = index; val sb = StringBuilder(); while (n > 0) { val remainder = (n - 1) % 26; sb.append(('A'.code + remainder).toChar()); n = (n - 1) / 26 }; return sb.reverse().toString() }
    private fun columnIndex(ref: String): Int { val letters = ref.takeWhile { it.isLetter() }; var n = 0; letters.forEach { n = n * 26 + (it.uppercaseChar().code - 'A'.code + 1) }; return (n - 1).coerceAtLeast(0) }
}
