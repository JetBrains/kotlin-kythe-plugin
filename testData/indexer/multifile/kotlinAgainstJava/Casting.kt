package interop

import interop.Interface

class Casting() {
  private fun foo(obj: Interface) {
    if (obj.chars is String) {
      val str = obj.chars as String
    }
  }
}
