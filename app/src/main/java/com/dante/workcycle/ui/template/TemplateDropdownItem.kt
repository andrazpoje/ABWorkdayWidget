package com.dante.workcycle.ui.template

sealed class TemplateDropdownItem {

    data class Header(
        val title: String
    ) : TemplateDropdownItem() {
        override fun toString(): String = title
    }

    data class Option(
        val templateId: String,
        val title: String
    ) : TemplateDropdownItem() {
        override fun toString(): String = title
    }
}