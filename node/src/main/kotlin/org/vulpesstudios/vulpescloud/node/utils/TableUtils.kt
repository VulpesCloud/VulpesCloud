/*
 * Copyright 2024-2026 VulpesStudios & Contributers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vulpesstudios.vulpescloud.node.utils

object TableUtils {

    private val TAG_REGEX = Regex("(?<!\\\\)</?[a-zA-Z#!?][^<>]*>")
    private val ANSI_REGEX = Regex("\u001B\\[[0-9;]*m")

    private fun visibleLength(text: String): Int =
        text
            .replace(ANSI_REGEX, "")
            .replace(TAG_REGEX, "")
            .replace("\\<", "<")   // MiniMessage drops the escaping backslash
            .length

    fun newTable(): Table = Table()

    /**
     * Context passed to formatters.
     *
     * @property column the column id
     * @property row 0-based index among the content rows, or -1 for the header
     * @property value the value to be displayed (already replaced by the missing value if absent)
     * @property missing true if the row had no entry for this column
     */
    data class Cell(
        val column: String,
        val row: Int,
        val value: String,
        val missing: Boolean,
        val columnIndex: Int = 0
    )

    class Table {

        private var header: Map<String, String>? = null
        private val content = mutableListOf<Map<String, String>>()

        private var defaultMissing = "N/A"
        private val columnMissing = mutableMapOf<String, String>()

        private var defaultPadding = 1
        private val columnPadding = mutableMapOf<String, Int>()

        private var defaultHeaderFormatter: ((Cell) -> String)? = null
        private val columnHeaderFormatters = mutableMapOf<String, (Cell) -> String>()

        private var defaultContentFormatter: ((Cell) -> String)? = null
        private val columnContentFormatters = mutableMapOf<String, (Cell) -> String>()

        private var headerSeparator = true
        private var separatorChar = '-'

        fun defaultStyle(): Table {
            headerFormatter { cell ->
                "<color:#FF9F43><bold>${cell.value}</bold></color>"
            }
            contentFormatter { cell ->
                val color = if ((cell.row + cell.columnIndex) % 2 == 0) "#E8E8E8" else "#8A8A8A"
                "<color:$color>${cell.value}</color>"
            }
            return this
        }

        // ---------------------------------------------------------------- data

        /** Sets the header row. Calling it again replaces the previous header. */
        fun header(header: Map<String, String>): Table {
            this.header = header
            return this
        }

        fun headerSeparator(enabled: Boolean, char: Char = '-'): Table {
            headerSeparator = enabled
            separatorChar = char
            return this
        }

        /** Adds a content row. */
        fun addContent(row: Map<String, String>): Table {
            content.add(row)
            return this
        }

        // ------------------------------------------------------------- missing

        /** Value shown for missing content cells, for the whole table. Default: "N/A". */
        fun missingValue(value: String): Table {
            defaultMissing = value
            return this
        }

        /** Value shown for missing content cells in a specific column. Overrides the table default. */
        fun missingValue(column: String, value: String): Table {
            columnMissing[column] = value
            return this
        }

        // ------------------------------------------------------------- padding

        /** Spaces on each side inside every cell, for the whole table. Default: 1. */
        fun padding(spaces: Int): Table {
            require(spaces >= 0) { "padding must be >= 0" }
            defaultPadding = spaces
            return this
        }

        /** Spaces on each side inside the cells of a specific column. Overrides the table default. */
        fun padding(column: String, spaces: Int): Table {
            require(spaces >= 0) { "padding must be >= 0" }
            columnPadding[column] = spaces
            return this
        }

        // ---------------------------------------------------------- formatters

        /** Formatter for all header cells. */
        fun headerFormatter(formatter: (Cell) -> String): Table {
            defaultHeaderFormatter = formatter
            return this
        }

        /** Formatter for the header cell of one column. Overrides the table-wide header formatter. */
        fun headerFormatter(column: String, formatter: (Cell) -> String): Table {
            columnHeaderFormatters[column] = formatter
            return this
        }

        /** Formatter for all content cells. Use [Cell.row] / [Cell.column] to differentiate. */
        fun contentFormatter(formatter: (Cell) -> String): Table {
            defaultContentFormatter = formatter
            return this
        }

        /** Formatter for the content cells of one column. Overrides the table-wide content formatter. */
        fun contentFormatter(column: String, formatter: (Cell) -> String): Table {
            columnContentFormatters[column] = formatter
            return this
        }

        // ----------------------------------------------------------- rendering

        /** Renders all columns, in order of first appearance. */
        fun lines(): List<String> =
            lines((listOfNotNull(header) + content).flatMap { it.keys }.distinct())

        /**
         * Renders the table as lines containing only the given columns, in the given order.
         * The header (if set) comes first, followed by the content rows.
         */
        fun lines(columns: List<String>): List<String> {
            if (columns.isEmpty()) return emptyList()

            val renderedRows = mutableListOf<List<String>>()

            header?.let { header ->
                renderedRows.add(columns.mapIndexed { i, c -> renderHeaderCell(c, i, header) })
            }
            content.forEachIndexed { index, row ->
                renderedRows.add(columns.mapIndexed { i, c -> renderContentCell(c, i, index, row) })
            }
            if (renderedRows.isEmpty()) return emptyList()

            val widths = columns.indices.map { col ->
                renderedRows.maxOf { visibleLength(it[col]) }
            }

            val rendered = renderedRows.map { cells ->
                cells.indices.joinToString(separator = "|", prefix = "|", postfix = "|") { col ->
                    val pad = " ".repeat(columnPadding[columns[col]] ?: defaultPadding)
                    val fill = " ".repeat(widths[col] - visibleLength(cells[col]))
                    pad + cells[col] + fill + pad
                }
            }

            if (header == null || !headerSeparator) return rendered

            val separator = columns.indices.joinToString(separator = "|", prefix = "|", postfix = "|") { col ->
                val pad = columnPadding[columns[col]] ?: defaultPadding
                separatorChar.toString().repeat(widths[col] + pad * 2)
            }

            return listOf(rendered.first(), separator) + rendered.drop(1)
        }

        private fun renderHeaderCell(column: String, columnIndex: Int, row: Map<String, String>): String {
            val cell = Cell(column, -1, row[column] ?: "", column !in row, columnIndex)
            val formatter = columnHeaderFormatters[column] ?: defaultHeaderFormatter
            return formatter?.invoke(cell) ?: cell.value
        }

        private fun renderContentCell(column: String, columnIndex: Int, index: Int, row: Map<String, String>): String {
            val missing = column !in row
            val value = row[column] ?: (columnMissing[column] ?: defaultMissing)
            val cell = Cell(column, index, value, missing, columnIndex)
            val formatter = columnContentFormatters[column] ?: defaultContentFormatter
            return formatter?.invoke(cell) ?: cell.value
        }
    }
}