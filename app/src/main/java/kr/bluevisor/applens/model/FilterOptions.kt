package kr.bluevisor.applens.model

enum class SortOrder {
    NAME_ASC,
    NAME_DESC,
    INSTALL_DATE_ASC,
    INSTALL_DATE_DESC,
    UPDATE_DATE_DESC,
    SIZE_DESC
}

enum class AppFilter {
    ALL,
    USER_APPS_ONLY
}

data class FilterState(
    val sortOrder: SortOrder = SortOrder.NAME_ASC,
    val appFilter: AppFilter = AppFilter.ALL,
    val searchQuery: String = ""
)