package net.lubble.util.controller

interface V1MixBaseController<C, U, R, P, ID> : V1BaseController<C, U, P, ID>, RSocketController<C, U, R, ID>