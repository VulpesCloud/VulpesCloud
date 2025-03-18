package de.vulpescloud.api.utils

class RowFormatter {

    private var rows = mutableListOf<List<String>>()

    init {
        rows = mutableListOf()
    }

    fun addRow(vararg columns: String): RowFormatter {
        rows.add(columns.toList())
        return this
    }

    fun build(): List<String> {
        val columnWidths = rows.first().indices.map { columnIndex ->
            rows.maxOf { it.getOrNull(columnIndex)?.length ?: 0 } + 2
        }

        val divider = columnWidths.joinToString("+") { "-".repeat(it) }
        val tableLines = mutableListOf<String>()

        rows.forEachIndexed { index, row ->
            if (index == 0) tableLines.add("|${divider}|")
            tableLines.add(
                "|" + row.mapIndexed { i, cell -> cell.padEnd(columnWidths[i]) }.joinToString("|") + "|"
            )
            tableLines.add("|${divider}|")
        }

        return tableLines
    }

}