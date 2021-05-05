package gbge.ui.eps.tm

abstract sealed class DataFetchStatus
case object LOADING extends DataFetchStatus
case object LOADED extends DataFetchStatus
case object LOADING_FAILED extends DataFetchStatus

abstract sealed class ComponentDisplayMode
case object PPRINT extends ComponentDisplayMode
case object COMPONENT extends ComponentDisplayMode

abstract sealed class CSState
case object CS_NOT_SELECTED extends CSState
case object CS_LOADING extends CSState
case object CS_LOADING_FAILED extends CSState