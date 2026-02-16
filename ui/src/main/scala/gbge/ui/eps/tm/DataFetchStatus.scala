package gbge.ui.eps.tm

import uiglue.{Event, UIState}

sealed trait DataFetchStatus
case object LOADING extends DataFetchStatus
case object LOADED extends DataFetchStatus
case object LOADING_FAILED extends DataFetchStatus

sealed trait ComponentDisplayMode
case object PPRINT extends ComponentDisplayMode
case object COMPONENT extends ComponentDisplayMode

sealed trait CSState
case object CS_NOT_SELECTED extends CSState
case object CS_LOADING extends CSState
case object CS_LOADING_FAILED extends CSState
case class CSState_Loaded(state: UIState[Event, _]) extends CSState