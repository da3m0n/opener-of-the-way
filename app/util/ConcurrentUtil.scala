/*
 * Copyright (c) 2013 Scott Abernethy.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package util

import play.api.libs.iteratee.{Enumerator, Input, Done}
import play.api.libs.json.{Json, JsValue}

object ConcurrentUtil {
  def errorSocket(errorMessage: String) = {
    val iteratee = Done[JsValue,Unit]((),Input.EOF)
    val enumerator = Enumerator[JsValue](Json.obj("type" -> "error", "message" -> errorMessage)).andThen(Enumerator.enumInput(Input.EOF))
    (iteratee, enumerator)
  }
}
