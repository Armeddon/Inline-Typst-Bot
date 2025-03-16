package example

sealed trait ResultMessage
final object ResultMessageImage extends ResultMessage
final object ResultMessageText extends ResultMessage
